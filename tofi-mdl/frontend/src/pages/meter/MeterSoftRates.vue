<template>
  <q-banner dense inline-actions class="bg-orange-1">
    <div style="font-size: 1.2em; font-weight: bold;">{{ $t("mrSoftMeter") }}</div>
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
        v-if="hasTarget('mdl:mn_ds:meter:sel:soft:edit')"
        dense
        icon="edit_note"
        color="secondary"
        class="q-ml-sm"
        @click="fnEdit"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:meter:sel:soft:upd')"
        dense
        icon="edit"
        color="secondary"
        class="q-ml-sm"
        @click="fnUpd"
        :disable="currentNode == null"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:meter:sel:soft:del')"
        dense
        icon="delete"
        color="red"
        class="q-ml-sm"
        @click="fnDel(currentNode)"
        :disable="currentNode == null"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-banner>

  <div class="q-pa-md-md">
    <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
    {{ nodeInfo() }}
  </div>

  <div class="q-table--dense wrap bg-orange-1">
    <div class="q-pa-sm-sm scroll">
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr style="text-align: left">
          <th :style="columns[0]?.style">{{ columns[0]?.label }}</th>
          <th :style="columns[1]?.style">{{ columns[1]?.label }}</th>
          <th :style="columns[2]?.style">{{ columns[2]?.label }}</th>
          <th :style="columns[3]?.style">{{ columns[3]?.label }}</th>
          <th :style="columns[4]?.style">{{ columns[4]?.label }}</th>
        </tr>
        </thead>

        <tbody style="background: aliceblue; height: 100%">
        <tr v-for="(item, index) in arrayTreeObj" :key="index">
          <td
            :data-th="columns[0]?.name"
            style="width: 20%"
            @click="toggle(item)"
          >
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
          <!--al-->
          <td :data-th="columns[3]?.name">{{ fnAL(item.accessLevel) }}</td>
          <!--cmt-->
          <td :data-th="columns[4]?.name">{{ item.cmt }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack } from "@/utils/jsutils";
import UpdateSoftMeterRate from "@/pages/meter/UpdateSoftMeterRate.vue";
import UpdaterMeterSoftRatesUpd from "@/pages/meter/UpdaterMeterSoftRatesUpd.vue";

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
const lang = ref("");

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

const fnEdit = () => {
  $q.dialog({
    component: UpdaterMeterSoftRatesUpd,
    componentProps: {
      meter: meter_id.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      fetchData(meter_id.value);
    }
  });
};

const fnUpd = () => {
  let parentName = null;
  let isChild = false;
  let data = {
    id: currentNode.value.id,
    parent: currentNode.value.parent,
    meter: meter_id.value,
    cod: currentNode.value.cod,
    accessLevel: currentNode.value.accessLevel,
    name: currentNode.value.name,
    fullName: currentNode.value.fullName,
    cmt: currentNode.value.cmt,
  };
  if (currentNode.value.parent > 0) {
    let parentNode = [];
    getParentNode(table.value, currentNode.value.parent, parentNode);
    parentName = parentNode[0]?.fullName;
    isChild = true;
  }

  $q.dialog({
    component: UpdateSoftMeterRate,
    componentProps: {
      mode: "upd",
      isChild: isChild,
      parentName: parentName,
      data: data,
    },
  }).onOk((resData) => {
    if (resData.res) {
      fetchData();
    }
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
          method: "meterrate/deleteSoftMR",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            fetchData();
          },
          () => {
            notifyInfo(proxy?.$t("hasChild") || "hasChild");
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const fetchData = () => {
  api
    .post("", {
      method: "meterrate/loadSoftMR",
      params: [meter_id.value],
    })
    .then((response) => {
      table.value = pack(response.data.result.records, "ord");
    });
};

const fnAL = (val) => {
  return FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null;
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "text-align: left; width:5%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "text-align: left; width:15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "text-align: left; width:25%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "text-align: left; width:15%",
    format: (val) => (FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; color: #1976d2; background: antiquewhite",
    style: "text-align: left; width:40%",
  },
];

const nodeInfo = () => {
  let res = "";
  if (currentNode.value) {
    res = currentNode.value.cod;
  }
  return res;
};

const fnExpand = () => {
  expandAll(table.value);
};

const fnCollapse = () => {
  collapsAll(table.value);
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(table.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  columns.value = getColumns();

  meter_id.value = parseInt(route.params.meter, 10) || 0;
  fetchData(meter_id.value);

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

<style scoped></style>
