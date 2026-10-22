<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div class="row no-margin no-padding" style="font-size: 1.2em; font-weight: bold;">
              {{ $t("leveles") }}
              <span style="color: black; margin-left: 10px">{{ dpName }}</span>
            </div>

            <template v-slot:action>
              <q-btn
                dense
                round
                color="secondary"
                icon="arrow_back"
                glossy
                @click="toBack"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("back") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnEdit('ins', false, null)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm img-vert"
                @click="fnEdit('ins', true, currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:upd')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnEdit('upd', null, currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:del')"
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
              <q-space />

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:view')"
                dense
                icon="visibility"
                color="secondary"
                class="q-ml-sm"
                @click="fnView"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("view") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary" />
            </template>
          </q-banner>

          <QTreeTable
            :cols="cols"
            :rows="rows"
            :icon_leaf="''"
            :FD_PeriodType="FD_PeriodType"
            :emptydate="''"
            @updateSelect="onUpdateSelect"
            checked_visible="true"
            ref="childComp"
          />
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            {{ $t("exclItems") }}
          </div>

          <template v-slot:action>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:prop:s:edit')"
              dense
              icon="edit_note"
              color="secondary"
              class="q-ml-sm"
              @click="fnExcl"
              :disable="currentNode === null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("update") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 215px); width: 100%" class="scroll">
          <q-table
            style="height: 100%; width: 100%"
            color="primary"
            card-class="bg-white"
            row-key="name"
            :columns="cols2"
            :rows="rows2"
            :dense="dense"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2"
            :rows-per-page-options="[0]"
          />
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar, date, extend } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios";
import { getParentNode, hasTarget, notifyError, notifyInfo, pack } from "@/utils/jsutils";
import UpdaterDimPeriodItem from "@/pages/dimperiod/UpdaterDimPeriodItem.vue";
import UpdaterNotIn from "@/pages/dimperiod/UpdaterNotIn.vue";
import DimPeriodView from "@/pages/dimperiod/DimPeriodView.vue";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(70);
const FD_PeriodType = reactive(new Map());
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const visible = ref(false);
const dense = ref(true);

const cols2 = ref([]);
const rows2 = ref([]);
const loading2 = ref(false);

const dimPeriod = ref(null);
const dpName = ref("");
const lang = ref("");
const childComp = ref(null);

const toBack = () => {
  router.push({
    name: "DimPeriod",
    params: {
      dimperiod: dimPeriod.value,
    },
  });
};

const fnView = () => {
  $q.dialog({
    component: DimPeriodView,
    componentProps: {
      dimperiod: dimPeriod.value,
      dense: true,
      lg: lang.value,
      cmdDate: false,
    },
  }).onOk(() => {});
};

const onUpdateSelect = (item) => {
  currentNode.value = item.selected !== undefined ? item.selected : null;

  if (currentNode.value) {
    api
      .post("", {
        method: "dimperiod/loadNotIn",
        params: [currentNode.value.id],
      })
      .then((response) => {
        rows2.value = response.data.result.records;
      })
      .catch((error) => {
        notifyError(error.message);
      });
  } else {
    rows2.value = [];
  }
};

const fetchData = (dimperiodVal) => {
  visible.value = true;

  api
    .post("", {
      method: "dimperiod/loadDPI",
      params: [dimperiodVal],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "ord");
      },
      (error) => {
        let msg = error.message;
        if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
        notifyError(msg);
      }
    )
    .finally(() => {
      childComp.value?.fnExpand();
      visible.value = false;
    });
};

const sendQuery = (data, mode, isChild, parentName, parentPeriodType, denseVal) => {
  $q.dialog({
    component: UpdaterDimPeriodItem,
    componentProps: {
      data: data,
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      parentPeriodType: parentPeriodType,
      dense: denseVal,
    },
  }).onOk(() => {
    fetchData(dimPeriod.value);
  });
};

const fnEdit = (mode, isChild, rec) => {
  let data = {};
  let parentName = null;
  let parentPeriodType = null;

  if (mode === "ins") {
    if (rec === null) rec = { dimperiod: dimPeriod.value };
    api
      .post("", {
        method: "dimperiod/newRecDPI",
        params: [isChild, rec],
      })
      .then((response) => {
        data = response.data.result.records[0];
        data.dimperiod = dimPeriod.value;
        if (isChild) {
          parentName = FD_PeriodType.get(currentNode.value?.priodType);
          parentPeriodType = currentNode.value?.periodType;
        }

        sendQuery(data, mode, isChild, parentName, parentPeriodType, dense.value);
      });
  } else if (mode === "upd") {
    extend(data, currentNode.value);
    if (data.dbeg <= tofi_dbeg) data.dbeg = null;
    if (data.dend >= tofi_dend) data.dend = null;
    if (currentNode.value.parent > 0) {
      let parentNode = [];
      getParentNode(rows.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0].name;
      isChild = true;
    }
    sendQuery(data, mode, isChild, parentName, parentPeriodType, dense.value);
  }
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      FD_PeriodType.get(rec.periodType) +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      api
        .post("", {
          method: "dimperiod/deleteDPI",
          params: [rec],
        })
        .then(
          () => {
            fetchData(dimPeriod.value);
          },
          (error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
    });
};

const fnExcl = () => {
  $q.dialog({
    component: UpdaterNotIn,
    componentProps: {
      dimPeriodItem: currentNode.value.id,
      dense: dense.value,
    },
  }).onOk((r) => {
    if (r.res) {
      api
        .post("", {
          method: "dimperiod/loadNotIn",
          params: [currentNode.value.id],
        })
        .then((response) => {
          rows2.value = response.data.result.records;
        })
        .catch((error) => {
          notifyError(error.message);
        });
    }
  });
};

const getColumns = () => [
  {
    name: "periodType",
    label: proxy?.$t("fldName"),
    field: "periodType",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "dbeg",
    label: proxy?.$t("fldDbeg"),
    field: "dbeg",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) => (val <= tofi_dbeg ? "" : date.formatDate(val, "DD.MM.YYYY")),
  },
  {
    name: "countPeriod",
    label: proxy?.$t("countPeriod"),
    field: "countPeriod",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "dend",
    label: proxy?.$t("fldDend"),
    field: "dend",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em;",
    style: "width: 10%",
    format: (val) => (val >= tofi_dend ? "" : date.formatDate(val, "DD.MM.YYYY")),
  },
  {
    name: "lagCurrentDate",
    label: proxy?.$t("lagCurrentDate"),
    field: "lagCurrentDate",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const getColumns2 = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 100%",
  },
];

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PeriodType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_PeriodType.set(it.id, it.text);
      });
    });

  cols.value = getColumns();
  cols2.value = getColumns2();

  dimPeriod.value = route.params.dimperiod;
  dpName.value = "(" + route.params.name + ")";
  fetchData(dimPeriod.value);
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
