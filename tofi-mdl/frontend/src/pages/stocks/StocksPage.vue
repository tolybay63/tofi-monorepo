<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[0, 100]"
      after-class="overflow-hidden q-ml-sm"
      before-class="overflow-hidden q-mr-sm"
      class="bg-amber-1"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner class="bg-orange-1 q-mb-sm" dense inline-actions>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" icon="folder" text-color="white" />
              {{ $t("stocksGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:insgr')"
                class="q-ml-sm"
                color="secondary"
                dense
                icon="post_add"
                @click="fnInsGr('ins', false)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:insgr')"
                :disable="currentNode == null"
                class="q-ml-sm img-vert"
                color="secondary"
                dense
                icon="post_add"
                @click="fnInsGr('ins', true)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:updgr')"
                :disable="currentNode == null"
                class="q-ml-sm"
                color="secondary"
                dense
                icon="edit"
                @click="fnInsGr('upd', null)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:delgr')"
                :disable="currentNode == null"
                class="q-ml-sm"
                color="red"
                dense
                icon="delete"
                @click="fnDelGr(currentNode)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="loading" color="secondary" />
            </template>
          </q-banner>

          <QTreeTable
            ref="childComp"
            :cols="cols"
            :icon_leaf="''"
            :rows="rows"
            checked_visible="true"
            @updateSelect="onUpdateSelect"
          />
        </div>
      </template>

      <template v-slot:after>
        <q-table
          style="height: 100%; width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="cod"
          :columns="cols2"
          :rows="rows2"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          dense
          selection="single"
          v-model:selected="selected"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
              {{ $t("infoApp") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="storage" />
              {{ $t("stocks") }}
            </div>

            <q-space />
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:ins')"
              icon="post_add"
              color="secondary"
              dense
              :disable="loading || currentNode == null"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="editRow(selected[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="removeRow(selected[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary" />
          </template>
        </q-table>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  notifySuccess,
  pack,
} from "@/utils/jsutils.js";
import { api } from "@/boot/axios.js";
import UpdateGroup from "@/pages/group/UpdateGroup.vue";
import UpdateStock from "@/pages/stocks/UpdateStock.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(30);
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const loading = ref(false);
const stockId = ref(0);
const stockGrId = ref(0);
const cols2 = ref([]);
const rows2 = ref([]);
const selected = ref([]);
const childComp = ref(null);

const onUpdateSelect = (item) => {
  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    stockGrId.value = currentNode.value.id;
    api
      .post("", {
        method: "group/loadRec",
        params: [{ id: currentNode.value.id, tableName: "SourceStockGr" }],
      })
      .then((response) => {
        currentNode.value = response.data.result.records[0];
        fetchData(currentNode.value.id);
      });
  } else {
    stockGrId.value = 0;
    fetchData(0);
  }
};

const fetchDataGr = () => {
  loading.value = true;
  currentNode.value = null;

  api
    .post("", {
      method: "group/loadGroup",
      params: [{ tableName: "SourceStockGr" }],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "id");
        fnExpand();
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

        notifyError(msg);
      }
    )
    .then(() => {
      if (stockGrId.value > 0 && childComp.value) {
        let res = [];
        findNode(rows.value, "id", stockGrId.value, res);
        currentNode.value = res[0];
        childComp.value.restoreSelect(currentNode.value);
        fetchData(currentNode.value.id);
      }
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnInsGr = (mode, isChild) => {
  let data = {
    accessLevel: 1,
  };
  let parent = null;
  let parentName = null;
  if (isChild) {
    parent = currentNode.value.id;
    parentName = currentNode.value.name;
  }
  if (mode === "ins") {
    data.parent = parent;
  } else if (mode === "upd") {
    data = {
      id: currentNode.value.id,
      cod: currentNode.value.cod,
      name: currentNode.value.name,
      fullName: currentNode.value.fullName,
      accessLevel: currentNode.value.accessLevel,
      parent: currentNode.value.parent,
      cmt: currentNode.value.cmt,
    };
    if (currentNode.value.parent > 0) {
      let parentNode = [];
      getParentNode(rows.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0].fullName;
      isChild = true;
    }
  }

  $q.dialog({
    component: UpdateGroup,
    componentProps: {
      data: data,
      mode: mode,
      isChild: isChild,
      tableName: "SourceStockGr",
      parentName: parentName,
      dense: true,
    },
  }).onOk((dialogData) => {
    fetchDataGr();
    currentNode.value = dialogData;
    childComp.value.restoreSelect(dialogData);
    onUpdateSelect({ selected: dialogData });
  });
};

const fnDelGr = (rec) => {
  rec.tableName = "SourceStockGr";
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
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "group/delete",
        params: [rec],
      })
      .then(
        () => {
          fetchDataGr();
        },
        (error) => {
          let msg = error.message;
          if (error.response?.data?.error?.message) msg = error.response.data.error.message;

          notifyError(msg);
        }
      );
  }).onCancel(() => {
    notifyInfo(proxy?.$t("canceled") || "canceled");
  });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 70%",
  },
];

const getColumns2 = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 35%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const fetchData = (stockGr) => {
  loading.value = true;
  api
    .post("", {
      method: "stock/loadStocks",
      params: [stockGr],
    })
    .then(
      (response) => {
        rows2.value = response.data.result.records;
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const editRow = (rec, mode) => {
  let data = { parent: stockGrId.value };
  if (mode === "ins") {
    loading.value = true;
    api
      .post("", {
        method: "stock/newRec",
        params: [stockGrId.value],
      })
      .then(
        (response) => {
          data = response.data.result.records[0];
        },
        (error) => {
          let msg = error.message;
          if (error.response?.data?.error?.message)
            msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
          notifyError(msg);
        }
      )
      .finally(() => {
        loading.value = false;
      });
  } else {
    data = {
      id: rec.id,
      cod: rec.cod,
      parent: rec.parent,
      accessLevel: rec.accessLevel,
      sourceStockType: rec.sourceStockType,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateStock,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows2.value.push(r);
      selected.value = [];
      selected.value.push(r);
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key];
        }
      }
    }
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
    focus: "cancel",
  }).onOk(() => {
    let index = rows2.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: "stock/delete",
        params: [rec],
      })
      .then(
        () => {
          rows2.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success") || "success");
        },
        (error) => {
          notifyInfo(error.message);
        }
      );
  });
};

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

onMounted(() => {
  stockId.value = parseInt(route.params.stock, 10);
  stockGrId.value = parseInt(route.params.stockGr, 10);

  cols.value = getColumns();
  cols2.value = getColumns2();

  fetchDataGr();
});
</script>

<style scoped></style>
