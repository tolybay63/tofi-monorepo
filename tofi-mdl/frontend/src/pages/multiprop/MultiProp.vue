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
              {{ $t("multiPropGr") }}
            </div>
            <template v-slot:action>
              <q-btn
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
                v-if="hasTarget('mdl:mn_ds:pm:insgr')"
                dense
                icon="post_add"
                color="secondary"
                @click="fnInsGr('ins', true)"
                class="q-ml-sm img-vert"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:pm:updgr')"
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
                v-if="hasTarget('mdl:mn_ds:pm:delgr')"
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
          <div style="height: calc(100vh - 220px); width: 100%">
            <QTreeTable
              :cols="cols"
              :rows="rows"
              :icon_leaf="''"
              @updateSelect="onUpdateSelect"
              ref="childComp"
              checked_visible="true"
            />
          </div>
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="view_in_ar" />
            {{ $t("multiProps") }}
          </div>

          <template v-slot:action>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:pm:ins')"
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
              v-if="hasTarget('mdl:mn_ds:pm:upd')"
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
              v-if="hasTarget('mdl:mn_ds:pm:del')"
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
              v-if="hasTarget('mdl:mn_ds:pm:sel')"
              :dense="dense"
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading2 || selected2.length === 0"
              @click="multiPropSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <q-table
          style="height: 100%; width: 100%"
          :wrap-cells="true"
          color="primary"
          card-class="bg-amber-1 text-brown"
          row-key="id"
          :dense="dense"
          :columns="cols2"
          :rows="rows2"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading2"
          selection="single"
          @update:selected="onUpdateSelect2"
          v-model:selected="selected2"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected2.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected2[0]) }} </span>
            </q-td>
            <q-td colspan="100%" v-else-if="rows2.length > 0" class="text-bold">
              {{ $t("infoRow") }}
            </q-td>
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
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar } from "quasar";
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
import QTreeTable from "@/components/QTreeTable.vue";
import UpdateGroup from "@/pages/group/UpdateGroup.vue";
import UpdaterMultiProp from "@/pages/multiprop/UpdaterMultiProp.vue";

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
const FD_AccessLevel = reactive(new Map());
const loading2 = ref(false);
const selected2 = ref([]);

const mpGr = ref(0);
const mp = ref(0);
const lang = ref("");
const childComp = ref(null);

const multiPropSelect = () => {
  router.push({
    name: "MultiPropSelected",
    params: {
      mpGr: currentNode.value.id,
      mp: selected2.value[0].id,
    },
  });
};

const onUpdateSelect2 = (par) => {
  mp.value = 0;
  mpGr.value = 0;
  if (par.length > 0) {
    if (par[0].statusFactor) checkStatus(par[0].id);
    if (par[0].providerTyp) checkProvider(par[0].id);
    if (!par[0].isUniq) checkCondition(par[0].id);
  }
};

const checkCondition = (prop) => {
  api
    .post("", {
      method: "multiProp/checkCondition",
      params: [prop],
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t("notMultiPropCond") || "notMultiPropCond");
      }
    });
};

const checkStatus = (prop) => {
  api
    .post("", {
      method: "multiProp/checkStatus",
      params: [prop],
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t("notStatus") || "notStatus");
      }
    });
};

const checkProvider = (prop) => {
  api
    .post("", {
      method: "multiProp/checkProvider",
      params: [prop],
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t("notProvider") || "notProvider");
      }
    });
};

const fnAL = (val) => {
  return FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null;
};

const onUpdateSelect = (item) => {
  mp.value = 0;
  mpGr.value = 0;
  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    api
      .post("", {
        method: "group/loadRec",
        params: [{ id: currentNode.value.id, tableName: "MultiPropGr" }],
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

const fetchDataGr = () => {
  visible.value = true;
  currentNode.value = null;

  api
    .post("", {
      method: "group/loadGroup",
      params: [{ tableName: "MultiPropGr" }],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "id");
        fnExpand();
      },
      (error) => {
        router.push("/");
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

        notifyError(msg);
      }
    )
    .then(() => {
      if (mpGr.value > 0) {
        let res = [];
        findNode(rows.value, "id", mpGr.value, res);
        currentNode.value = res[0];
        childComp.value?.restoreSelect(currentNode.value);
        fetchData(currentNode.value.id);
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
  const lg = lang.value;
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
      parentName = parentNode[0]?.fullName;
      isChild = true;
    }
  }

  $q.dialog({
    component: UpdateGroup,
    componentProps: {
      data: data,
      mode: mode,
      isChild: isChild,
      tableName: "MultiPropGr",
      parentName: parentName,
      lg: lg,
      dense: true,
    },
  }).onOk((resData) => {
    childComp.value?.clrAny();
    fetchDataGr();
    currentNode.value = resData;
    childComp.value?.restoreSelect(currentNode.value);
    onUpdateSelect({ selected: resData });
  });
};

const fnDelGr = (rec) => {
  rec.tableName = "MultiPropGr";
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
  })
    .onOk(() => {
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
            let msg;
            if (error.response) msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const edit = (data, mode) => {
  const lg = lang.value;
  $q.dialog({
    component: UpdaterMultiProp,
    componentProps: {
      rec: data,
      mode: mode,
      lg: lg,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows2.value.push(r);
      selected2.value = [];
      selected2.value.push(r);
      onUpdateSelect2(selected2.value);
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          data[key] = r[key];
        }
      }
    }
  });
};

const fnIns = (mode) => {
  if (mode === "ins") {
    api
      .post("", {
        method: "multiProp/newRecMultiProp",
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
  })
    .onOk(() => {
      api
        .post("", {
          method: "multiProp/deleteMultiProp",
          params: [{ id: rec.id }],
        })
        .then(
          () => {
            selected2.value = [];
            fetchData(currentNode.value.id);
          },
          (error) => {
            let msg;
            if (error.response) msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const fetchData = (propGr) => {
  visible.value = true;
  selected2.value = [];

  api
    .post("", {
      method: "multiProp/loadMultiProp",
      params: [propGr],
    })
    .then((response) => {
      rows2.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .then(() => {
      if (mp.value > 0) {
        let index = rows2.value.findIndex((row) => row.id === mp.value);
        if (index > -1) selected2.value.push(rows2.value[index]);
      }
    })
    .finally(() => {
      visible.value = false;
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
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
    format: (val) => (FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
];

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

  mpGr.value = parseInt(route.params.mpGr, 10) || 0;
  mp.value = parseInt(route.params.mp, 10) || 0;

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

  cols.value = getColumns();
  cols2.value = getColumns2();

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
