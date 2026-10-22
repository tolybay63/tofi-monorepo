<template>
  <q-page class="q-pa-sm-sm" style="height: auto">
    <q-banner dense inline-actions class="bg-orange-1">
      <div style="font-size: 1.2em; font-weight: bold;">{{ $t("meterFactors") }}</div>
      <template v-slot:action>
        <q-btn dense icon="expand_more" color="secondary" @click="fnExpand">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          dense
          icon="expand_less"
          color="secondary"
          class="q-ml-sm"
          @click="fnCollapse"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addFactor") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:del')"
          dense
          icon="delete"
          color="red"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="
            currentNode == null || (currentNode.id < 0)
          "
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("delFactor") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:ord')"
          dense
          icon="swipe_up_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(true)"
          :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:ord')"
          dense
          icon="swipe_down_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(false)"
          :disable="onoffDown()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("down") }}
          </q-tooltip>
        </q-btn>
        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:dim')"
          dense
          icon="vertical_split"
          color="secondary"
          class="q-ml-sm"
          @click="fnNewDim"
          :disable="onoffDim()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newDimension") }}
          </q-tooltip>
        </q-btn>
        <q-space />
      </template>
    </q-banner>

    <div class="q-pa-md-md">
      <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
      {{ nodeInfo() }}
    </div>

    <div
      class="q-table-container q-table--dense wrap bg-orange-1"
      style="height: calc(100vh - 300px); width: 100%"
    >
      <div class="q-pa-sm-sm">
        <div class="q-table-middle scroll">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr style="text-align: left">
              <th :style="columns[0]?.style">{{ columns[0]?.label }}</th>
              <th :style="columns[1]?.style">{{ columns[1]?.label }}</th>
              <th :style="columns[2]?.style">{{ columns[2]?.label }}</th>
            </tr>
            </thead>

            <tbody style="background: aliceblue; height: 100%">
            <tr v-for="(item, index) in arrayTreeObj" :key="index">
              <td :data-th="columns[0]?.name" @click="toggle(item)">
                  <span
                    class="q-tree-link q-tree-label"
                    :style="setPadding(item)"
                  >
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    />

                    <q-btn
                      dense
                      flat
                      color="blue"
                      :icon="
                        selected.length === 1 && item.id === selected[0].id
                          ? 'check_box'
                          : 'check_box_outline_blank'
                      "
                      @click.stop="selectedRow(item)"
                    />

                    {{ item.cod }}
                  </span>
              </td>
              <!--name-->
              <td :data-th="columns[1]?.name">{{ item.name }}</td>
              <!--fullName-->
              <td :data-th="columns[2]?.name">{{ item.fullName }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { collapsAll, expandAll, hasTarget, notifyError, notifyInfo, pack } from "@/utils/jsutils";
import UpdateMeterFactors from "@/pages/meter/UpdateMeterFactors.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const isExpanded = ref(true);
const selectedRowID = reactive({});
const selected = ref([]);
const currentNode = ref(null);
const itemId = ref(null);
const FD_AccessLevel = reactive(new Map());
const columns = ref([]);
const table = ref([]);

const separator = ref("cell");
const meter_id = ref(null);
const maxDim = ref(1);

const recursive = (obj, newObj, level, targetItemId, isExpend) => {
  if (!obj) return;
  obj.forEach((o) => {
    if (o.children && o.children.length !== 0) {
      o.level = level;
      o.leaf = false;
      newObj.push(o);
      if (o.id === targetItemId) {
        o.expend = isExpend;
      }
      if (o.expend) {
        recursive(o.children, newObj, o.level + 1, targetItemId, isExpend);
      }
    } else {
      o.level = level;
      o.leaf = true;
      newObj.push(o);
    }
  });
};

const iconName = (item) => {
  if (item.expend) {
    return "remove_circle_outline";
  }
  if (item.children && item.children.length > 0) {
    return "control_point";
  }
  return "";
};

const toggle = (item) => {
  itemId.value = item.id;
  item.leaf = false;
  if (!item.leaf && item.expend === undefined && item.children !== undefined) {
    if (item.children.length !== 0) {
      recursive(item.children, [], item.level + 1, item.id, true);
    }
  }
  if (item.expend && item.children !== undefined) {
    item.children.forEach((o) => {
      o.expend = undefined;
    });
    item.expend = ref(undefined);
    item.leaf = ref(false);
    itemId.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
  }
  currentNode.value = selected.value[0] !== undefined ? selected.value[0] : null;
};

const fnExpand = () => {
  expandAll(table.value);
};

const fnCollapse = () => {
  collapsAll(table.value);
};

const onoffUp = () => {
  if (currentNode.value === null) return true;
  if (currentNode.value.id < 0) {
    return true;
  } else {
    if (currentNode.value.ordFactorInDim > 1) return false;
    if (currentNode.value.ordDim > 1) return false;
  }
  return true;
};

const onoffDown = () => {
  if (currentNode.value === null) return true;
  if (currentNode.value.id < 0) {
    return true;
  } else {
    if (currentNode.value.ordDim < maxDim.value - 1) return false;
    else {
      let cnt = 1;
      let i = 0;
      while (i < table.value.length) {
        let nd = table.value[i];
        if (nd.id === currentNode.value.parent) {
          const { children } = nd;
          if (children) cnt = children.length;
          break;
        }
        i++;
      }
      return currentNode.value.ordFactorInDim >= cnt;
    }
  }
};

const onoffDim = () => {
  if (currentNode.value === null) return true;
  return currentNode.value.id < 0;
};

const fnUp = (up) => {
  api
    .post("", {
      method: "meterfactor/changeOrd",
      params: [{ rec: currentNode.value, up: up }],
    })
    .then(
      () => {
        fetchData(meter_id.value);
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    );
};

const fnNewDim = () => {
  api
    .post("", {
      method: "meterfactor/newDimension",
      params: [{ rec: currentNode.value, maxDim: maxDim.value }],
    })
    .then(
      () => {
        fetchData(meter_id.value);
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    );
};

const fnIns = () => {
  let data = null;
  api
    .post("", {
      method: "meterfactor/newRec",
      params: [{}],
    })
    .then((response) => {
      data = response.data.result.records[0];
      data.ordDim = maxDim.value;
      $q.dialog({
        component: UpdateMeterFactors,
        componentProps: {
          meterId: meter_id.value,
          data: data,
          dense: true,
          maxDim: maxDim.value,
        },
      }).onOk((resData) => {
        if (resData.res) {
          fetchData(meter_id.value);
        }
      });
    });
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      rec.cod +
      ": " +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
  })
    .onOk(() => {
      api
        .post("", {
          method: "meterfactor/delFactor",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            fetchData(meter_id.value);
          },
          () => {
            notifyInfo(proxy?.$t("hasValue") || "hasValue");
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const fetchData = (meter) => {
  api
    .post("", {
      method: "meterfactor/load",
      params: [{ meter: meter }],
    })
    .then((response) => {
      table.value = pack(response.data.result.records, "ordDim");
      if (table.value.length > 0)
        maxDim.value = table.value[table.value.length - 1].ordDim + 1;
    });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "font-size: 1.2em; text-align: left; width:20%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite;",
    style: "font-size: 1.2em;text-align: left; width:35%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "font-size: 1.2em;text-align: left; width:45%",
  },
];

const nodeInfo = () => {
  let res = "";
  if (currentNode.value) {
    res = currentNode.value.cod;
  }
  return res;
};

const isEmpty = () => {
  return !table.value.length > 0;
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(table.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  meter_id.value = parseInt(route.params.meter, 10) || 0;
  let ms = parseInt(route.params.meterStruct, 10) || 0;
  if (ms === 2) {
    fetchData(meter_id.value);
  }
});

onMounted(() => {
  columns.value = getColumns();

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_AccessLevel.set(it.id, it.text);
      });
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    });
});
</script>

<style></style>
