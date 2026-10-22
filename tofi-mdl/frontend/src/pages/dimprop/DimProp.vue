<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="folder" />
              {{ $t("dimPropGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:insgr')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('ins', false)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:insgr')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm img-vert"
                @click="fnInsGr('ins', true)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:updgr')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('upd', null)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:delgr')"
                dense
                icon="delete"
                color="red"
                class="q-ml-sm"
                @click="fnDelGr(currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary" />
            </template>
          </q-banner>

          <QTreeTable
            :cols="cols"
            :rows="rows"
            :icon_leaf="''"
            @updateSelect="onUpdateSelect"
            ref="childComp"
            checked_visible="true"
          />
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="app_registration" />
            {{ $t("dimsProp") }}
          </div>
          <template v-slot:action>
            <q-btn
              v-if="hasTarget('mdl:mn_dp:dmprop:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('ins')"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_dp:dmprop:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('upd')"
              :disable="loading2 || selected2.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_dp:dmprop:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              @click="fnDel(selected2[0])"
              :disable="loading2 || selected2.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

            <q-space />

            <q-btn
              v-if="
                selected2.length > 0 &&
                selected2[0].dimMultiPropType !== dynDimMultiPropType &&
                hasTarget('mdl:mn_dp:dmprop:sel')
              "
              :dense="dense"
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading2 || selected2.length === 0"
              @click="dimPropSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
          <q-table
            style="height: 100%; width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="name"
            :columns="cols2"
            :rows="rows2"
            :dense="dense"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2"
            v-model:selected="selected2"
            @update:selected="onUpdateSelect2"
            selection="single"
            :rows-per-page-options="[0]"
          >
            <template #bottom-row>
              <q-td colspan="100%" v-if="selected2.length > 0">
                <span class="text-blue"> {{ $t("selectedRow") }}: </span>
                <span class="text-bold"> {{ infoSelected(selected2[0]) }} </span>
              </q-td>
              <q-td v-else-if="rows2.length > 0" colspan="100%" class="text-bold">
                {{ $t("infoRow") }}
              </q-td>
            </template>
          </q-table>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import { api } from "@/boot/axios";
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  pack,
} from "@/utils/jsutils";
import UpdateGroup from "@/pages/group/UpdateGroup.vue";
import UpdaterDimProp from "@/pages/dimprop/UpdaterDimProp.vue";
import allConsts from "@/pages/all-consts.js";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(30);
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const visible = ref(false);
const dense = ref(true);

const cols2 = ref([]);
const rows2 = ref([]);
const FD_DimPropType = reactive(new Map());
const loading2 = ref(false);

const selected2 = ref([]);
const dimPropGr = ref(0);
const dimProp = ref(0);
const dynDimMultiPropType = allConsts.FD_DimMultiPropType.dynam;
const lang = ref("");
const childComp = ref(null);

const dimPropSelect = () => {
  router.push({
    name: "DimPropItem",
    params: {
      dimPropGr: currentNode.value.id,
      dimProp: selected2.value[0].id,
      dimPropType: selected2.value[0].dimPropType,
      name: selected2.value[0].name,
    },
  });
};

const onUpdateSelect = (item) => {
  dimPropGr.value = 0;
  dimProp.value = 0;
  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    api
      .post("", {
        method: "group/loadRec",
        params: [{ id: currentNode.value.id, tableName: "DimPropGr" }],
      })
      .then((response) => {
        currentNode.value = response.data.result.records[0];
        fetchData(currentNode.value.id);
      });
  } else {
    selected2.value = [];
    fetchData(0);
  }
};

const onUpdateSelect2 = (item) => {
  console.log(item);
};

const fnExpand = () => expandAll(rows.value);
const fnCollapse = () => collapsAll(rows.value);

const fetchData = (dimPropGrVal) => {
  visible.value = true;
  selected2.value = [];
  api
    .post("", {
      method: "dimprop/loadDimProp",
      params: [dimPropGrVal],
    })
    .then(
      (response) => {
        rows2.value = response.data.result.records;
      },
      (error) => {
        let msg = error.message;
        if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
        notifyError(msg);
      }
    )
    .then(() => {
      if (dimProp.value > 0) {
        const cur = rows2.value.filter((it) => it.id === dimProp.value);
        if (cur.length > 0) {
          selected2.value.push(cur[0]);
        }
      }
    })
    .finally(() => {
      visible.value = false;
    });
};

const fetchDataGr = () => {
  visible.value = true;
  currentNode.value = null;
  api
    .post("", {
      method: "group/loadGroup",
      params: [{ tableName: "DimPropGr" }],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "id");
        fnExpand();
      },
      (error) => {
        let msg = error.message;
        if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
        notifyError(msg);
      }
    )
    .then(() => {
      if (dimPropGr.value > 0 && childComp.value) {
        let res = [];
        findNode(rows.value, "id", dimPropGr.value, res);
        currentNode.value = res[0] || null;
        if (currentNode.value) {
          childComp.value.restoreSelect(currentNode.value);
          fetchData(currentNode.value.id);
        }
      }
    })
    .finally(() => {
      visible.value = false;
    });
};

const fnInsGr = (mode, isChild) => {
  let data = {
    id: 0,
    cod: "",
    name: "",
    fullName: "",
    accessLevel: 1,
    cmt: null,
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
      tableName: "DimPropGr",
      parentName: parentName,
      lg: lang.value,
      dense: true,
    },
  }).onOk((r) => {
    fetchDataGr();
    currentNode.value = r;
    childComp.value?.restoreSelect(r);
    onUpdateSelect({ selected: r });
  });
};

const fnDelGr = (rec) => {
  rec.tableName = "DimPropGr";
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.cod +
      ": " +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      api
        .post("", {
          method: "group/delete",
          params: [rec],
        })
        .then(
          () => {
            dimProp.value = 0;
            dimPropGr.value = 0;
            currentNode.value = null;
            selected2.value = [];
            fetchDataGr();
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

const edit = (data, mode) => {
  $q.dialog({
    component: UpdaterDimProp,
    componentProps: {
      data: data,
      mode: mode,
      lg: lang.value,
      dense: true,
    },
  }).onOk((r) => {
    dimProp.value = 0;
    fetchData(currentNode.value.id);
    selected2.value = [r];
  });
};

const fnIns = (mode) => {
  if (mode === "ins") {
    api
      .post("", {
        method: "dimprop/newRec",
        params: [currentNode.value.id],
      })
      .then((response) => {
        edit(response.data.result.records[0], mode);
      });
  } else {
    edit(selected2.value[0], mode);
  }
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.cod +
      ": " +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      api
        .post("", {
          method: "dimprop/delete",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            fetchData(currentNode.value.id);
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

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 70%",
  },
];

const getColumns2 = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "dimPropType",
    label: proxy?.$t("dimPropType"),
    field: "dimPropType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
    format: (val) => (FD_DimPropType ? FD_DimPropType.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimPropType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_DimPropType.set(it.id, it.text);
      });
    });

  cols.value = getColumns();
  cols2.value = getColumns2();

  dimPropGr.value = parseInt(route.params.dimPropGr, 10) || 0;
  dimProp.value = parseInt(route.params.dimProp, 10) || 0;

  fetchDataGr();
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
