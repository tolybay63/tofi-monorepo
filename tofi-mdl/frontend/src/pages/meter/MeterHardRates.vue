<template>
  <div class="q-pa-sm-sm" style="height: calc(100vh - 290px)">
    <q-banner dense inline-actions class="bg-amber-1">
      <div style="font-size: 1.2em; font-weight: bold;">{{ $t("mrHardMeter") }}</div>
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
          v-if="hasTarget('mdl:mn_ds:meter:sel:hard:ins')"
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("create1level") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:hard:ins')"
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins', true)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("createChild") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:hard:upd')"
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('upd', null)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:hard:del')"
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

    <div
      class="q-table-container q-table--dense wrap bg-amber-1"
      style="height: 100%; width: 100%"
    >
      <div class="q-pa-sm-sm">
        <div class="q-table-middle scroll">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr style="text-align: left">
              <th style="font-size: 1.3em">{{ columns[0]?.label }}</th>
              <th style="font-size: 1.3em">{{ columns[1]?.label }}</th>
              <th style="font-size: 1.3em">{{ columns[2]?.label }}</th>
              <th style="font-size: 1.3em">{{ columns[3]?.label }}</th>
              <th style="font-size: 1.3em">{{ columns[4]?.label }}</th>
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
              <td :data-th="columns[1]?.name" style="width: 20%">
                {{ item.name }}
              </td>
              <!--fullName-->
              <td :data-th="columns[2]?.name" style="width: 20%">
                {{ item.fullName }}
              </td>
              <!--al-->
              <td :data-th="columns[3]?.name" style="width: 10%">
                {{ fnAL(item.accessLevel) }}
              </td>
              <!--cmt-->
              <td :data-th="columns[4]?.name" style="width: 25%">
                {{ item.cmt }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { collapsAll, expand, expandAll, getParentNode, hasTarget, notifyInfo, pack } from "@/utils/jsutils";
import UpdateHardMeterRate from "@/pages/meter/UpdateHardMeterRate.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const isExpanded = ref(true);
const selected = ref([]);
const currentNode = ref(null);
const itemId = ref(null);
const FD_AccessLevel = reactive(new Map());
const columns = ref([]);
const table = ref([]);

const separator = ref("cell");
const meter_id = ref(null);
const loading = ref(false);
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

const fnExpand = () => {
  expandAll(table.value);
};

const fnCollapse = () => {
  collapsAll(table.value);
};

const fnIns = (mode, isChild) => {
  let data = {
    id: 0,
    cod: "",
    meter: meter_id.value,
    accessLevel: 1,
    name: "",
    fullName: "",
    cmt: null,
  };

  let parent = null;
  let parentName = null;
  if (isChild) {
    parent = currentNode.value.id;
    parentName = currentNode.value.fullName;
  }
  if (mode === "ins") {
    data.parent = parent;
  } else if (mode === "upd") {
    data = {
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
  }

  $q.dialog({
    component: UpdateHardMeterRate,
    componentProps: {
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      data: data,
    },
  }).onOk((resData) => {
    if (resData.res) {
      fetchData();
      expand(currentNode.value);
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
          method: "meterrate/deleteHardMR",
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
  loading.value = true;
  api
    .post("", {
      method: "meterrate/loadHardMR",
      params: [meter_id.value],
    })
    .then((response) => {
      table.value = pack(response.data.result.records, "ord");
    })
    .finally(() => {
      loading.value = false;
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

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(table.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
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
    });

  meter_id.value = parseInt(route.params.meter, 10) || 0;
  fetchData(meter_id.value);
});
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}
</style>
