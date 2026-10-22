<!--
style="height: calc(100vh - 220px); width: 100%"
-->
<template>
  <div class="q-pa-sm-sm">
    <q-splitter
      v-model="splitterModel"
      :limits="[60, 100]"
      horizontal
      separator-class="bg-red"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
    >
      <template v-slot:before>
        <q-banner dense inline-actions class="bg-orange-1">
          <div style="font-size: 1.2em; font-weight: bold;">{{ $t("clss") }}</div>
          <template v-slot:action>
            <q-btn
              dense
              icon="expand_more"
              color="secondary"
              @click="fnExpand"
            >
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
              v-if="hasTarget('mdl:mn_ds:typ:sel:cls:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:cls:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="editRow(selected[0], 'upd')"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:cls:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              @click="removeRow(selected[0])"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel')"
              :dense="dense"
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading || selected.length === 0"
              @click="clsSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <q-inner-loading :showing="loading" color="secondary" />
        </q-banner>

        <div class="q-pa-sm-sm">
          <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
          {{ nodeInfo() }}
        </div>

        <div class="scroll" style="height: calc(100vh - 500px); width: 100%">
          <div class="q-pa-sm-sm q-table-container q-table--dense wrap bg-orange-1">
            <div class="q-table-middle scroll">
              <table class="q-table q-table--cell-separator q-table--bordered wrap">
                <thead v-if="cols.length > 0" class="text-bold text-white bg-blue-grey-13">
                <tr style="text-align: left">
                  <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
                  <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
                  <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
                  <th :style="cols[3].headerStyle">{{ cols[3].label }}</th>
                  <th :style="cols[4].headerStyle">{{ cols[4].label }}</th>
                  <th :style="cols[5].headerStyle">{{ cols[5].label }}</th>
                </tr>
                </thead>

                <tbody style="background: aliceblue">
                <tr v-for="(item, index) in arrayTreeObj" :key="index">
                  <td :data-th="cols[0].name" @click="toggle(item)">
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

                        <q-icon
                          name="brightness_1"
                          :color="getIconColor(item)"
                        />
                        {{ item.cod }}
                      </span>
                  </td>
                  <!--name-->
                  <td :data-th="cols[1].name">{{ item.name }}</td>
                  <!--fullName-->
                  <td :data-th="cols[2].name">{{ item.fullName }}</td>
                  <!--accessLevel-->
                  <td :data-th="cols[3].name">
                    {{ fnAL(item.accessLevel) }}
                  </td>
                  <!--db-->
                  <td :data-th="cols[4].name">{{ fnDb(item.dataBase) }}</td>
                  <!--cmt-->
                  <td :data-th="cols[5].name">{{ item.cmt }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 scroll">
          <div style="font-size: 1.2em; font-weight: bold;">{{ $t("clsFV") }}</div>

          <template v-slot:action>
            <q-btn
              dense
              icon="expand_more"
              color="secondary"
              @click="fnExpand2"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("expandAll") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              dense
              icon="expand_less"
              color="secondary"
              class="q-ml-sm"
              @click="fnCollapse2"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("collapseAll") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div class="q-table-container q-table--dense wrap bg-orange-1 scroll">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead v-if="cols2.length > 0" class="text-bold text-white bg-blue-grey-13">
            <tr style="text-align: left">
              <th :style="cols2[0].headerStyle">{{ cols2[0].label }}</th>
              <th :style="cols2[1].headerStyle">{{ cols2[1].label }}</th>
              <th :style="cols2[2].headerStyle">{{ cols2[2].label }}</th>
            </tr>
            </thead>

            <tbody style="background: aliceblue">
            <tr v-for="(item, index) in arrayTreeObj2" :key="index">
              <td :data-th="cols2[0].name" @click="toggle2(item)">
                  <span
                    class="q-tree-link q-tree-label"
                    :style="setPadding(item)"
                  >
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    />
                    <q-icon
                      :name="getIcon(item)"
                      :color="getIconColor(item)"
                    />
                    {{ item.name }}
                  </span>
              </td>
              <td :data-th="cols2[1].name">{{ item.fullName }}</td>
              <td :data-th="cols2[2].name">{{ item.cod }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios";
import UpdateCls from "@/pages/typ/cls/UpdateCls.vue";
import {
  collapsAll,
  expandAll,
  findNode,
  hasTarget,
  notifyError,
  notifyInfo,
  notifySuccess,
  pack,
} from "@/utils/jsutils";

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(60);
const cols = ref([]);
const rows = ref([]);
const FD_AccessLevel = reactive(new Map());
const dataBase = ref(null);
const loading = ref(false);
const selected = ref([]);
const isExpanded = ref(true);
const currentNode = ref(null);
const itemId = ref(null);
const dense = ref(true);

const cols2 = ref([]);
const rows2 = ref([]);
const isExpanded2 = ref(true);
const currentNode2 = ref(null);
const itemId2 = ref(null);
const typId = ref(0);
const clsId = ref(0);

const clsSelect = () => {
  router.push({
    name: "clsVer",
    params: {
      cls: selected.value[0].ent,
    },
  });
};

const getIconColor = (row) => {
  if (row.isOwn === 1) return "green";
  else return "dark-gray";
};

const getIcon = (row) => {
  if (row.parent) return "brightness_1";
  else return "square";
};

const fnAL = (val) => {
  return FD_AccessLevel ? FD_AccessLevel.get(val) : null;
};

const fnDb = (val) => {
  return dataBase.value ? dataBase.value.get(val) : null;
};

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
  }
  currentNode.value = selected.value[0] !== undefined ? selected.value[0] : null;
  updSelected(selected.value);
};

const updSelected = (cur) => {
  if (cur.length === 0) fetchData2(0, 0);
  else {
    fetchData2(typId.value, cur[0].ent);
  }
};

const fetchData2 = (typ, cls) => {
  loading.value = false;
  api
    .post("", {
      method: "cls/loadClsFV",
      params: [typ, cls],
    })
    .then((response) => {
      rows2.value = pack(response.data.result.records, "ord");
      fnExpand2();
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = error.response.data.error.message;

      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const fetchData = (typ) => {
  loading.value = true;
  selected.value = [];
  api
    .post("", {
      method: "cls/loadClsTree",
      params: [{ typ: typ, typNodeVisible: false }],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
      fnExpand();
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = error.response.data.error.message;

      notifyError(msg);
    })
    .then(() => {
      if (clsId.value > 0) {
        let res = [];
        findNode(rows.value, "ent", clsId.value, res);
        if (res[0]) {
          selected.value.push(res[0]);
          updSelected(selected.value);
        }
      }
    })
    .finally(() => {
      loading.value = false;
    });
};

const removeRow = (rec) => {
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
          method: "cls/deleteCls",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            fetchData(typId.value);
            updSelected([]);
            notifySuccess(proxy?.$t("success") || "success");
          },
          (error) => {
            let msg;
            if (error.response?.data?.error?.message)
              msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    cod: "",
    accessLevel: 1,
    typ: typId.value,
    isOpenness: true,
    dataBase: null,
    lastVer: 1,
    name: "",
    fullName: "",
    cmt: null,
    isOwn: 1,
  };
  if (mode === "upd") {
    data = {
      id: rec.ent,
      cod: rec.cod,
      accessLevel: rec.accessLevel,
      typ: rec.typ,
      isOpenness: rec.isOpenness,
      dataBase: rec.dataBase,
      lastVer: rec.lastVer,
      dbeg: rec.dbeg > tofi_dbeg ? rec.dbeg : null,
      dend: rec.dend < tofi_dend ? rec.dend : null,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
      isOwn: rec.isOwn,
    };
  }

  $q.dialog({
    component: UpdateCls,
    componentProps: {
      data: data,
      mode: mode,
      typ: typId.value,
      dense: dense.value,
    },
  }).onOk((r) => {
    if (r.res) {
      fetchData(typId.value);
    }
  });
};

const nodeInfo = () => {
  let res = "";
  if (currentNode.value) {
    res = currentNode.value.cod + " - " + currentNode.value.name;
  }
  return res;
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "dataBase",
    label: proxy?.$t("dbNameLabel") || "dbNameLabel",
    field: "dataBase",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
];

const getColumns2 = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
    style: "width: 30%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 50%",
    style: "width: 50%",
  },
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
    style: "width: 20%",
  },
];

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const fnExpand2 = () => {
  expandAll(rows2.value);
};

const fnCollapse2 = () => {
  collapsAll(rows2.value);
};

const recursive = (obj, newObj, level, currentItemId, isExpend) => {
  obj.forEach((o) => {
    if (o.children && o.children.length !== 0) {
      o.level = level;
      o.leaf = false;
      newObj.push(o);
      if (o.id === currentItemId) {
        o.expend = isExpend;
      }
      if (o.expend) {
        recursive(o.children, newObj, o.level + 1, currentItemId, isExpend);
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
    item.expend = undefined;
    item.leaf = false;
    itemId.value = null;
  }
};

const toggle2 = (item) => {
  itemId2.value = item.id;
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
    item.expend = undefined;
    item.leaf = false;
    itemId2.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${item.level * 30}px;`;
};

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

const arrayTreeObj2 = computed(() => {
  let newObj2 = [];
  recursive(rows2.value, newObj2, 0, itemId2.value, isExpanded2.value);
  return newObj2;
});

onMounted(() => {
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

  api
    .post("", {
      method: "database/loadDbForSelect",
      params: [],
    })
    .then((response) => {
      dataBase.value = new Map();
      response.data.result.records.forEach((it) => {
        dataBase.value.set(it.id, it.name);
      });
    });

  cols.value = getColumns();
  cols2.value = getColumns2();

  typId.value = parseInt(route.params.typ, 10) || 0;
  clsId.value = parseInt(route.params.cls, 10) || 0;

  fetchData(typId.value);
});
</script>

<style scoped></style>
