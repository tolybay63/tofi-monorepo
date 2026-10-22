<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1 no-scroll"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="folder" />
              {{ $t("dimMultiPropGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_ds:pmdim:insgr')"
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
                v-if="hasTarget('mdl:mn_ds:pmdim:insgr')"
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
                v-if="hasTarget('mdl:mn_ds:pmdim:updgr')"
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
                v-if="hasTarget('mdl:mn_ds:pmdim:delgr')"
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
        <q-splitter
          horizontal
          v-model="splitterModel2"
          before-class="overflow-hidden q-mr-sm"
          after-class="overflow-hidden q-ml-sm"
          separator-class="bg-red"
          class="bg-amber-1 no-scroll"
        >
          <template v-slot:before>
            <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
              <div style="font-size: 1.2em; font-weight: bold;">
                <q-avatar color="black" text-color="white" icon="type_specimen" />
                {{ $t("dimMultiProp") }}
              </div>

              <template v-slot:action>
                <q-btn
                  v-if="hasTarget('mdl:mn_ds:pmdim:ins')"
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
                  v-if="hasTarget('mdl:mn_ds:pmdim:upd')"
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
                  v-if="hasTarget('mdl:mn_ds:pmdim:del')"
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
                  dense
                  icon="pan_tool_alt"
                  color="secondary"
                  class="q-ml-lg"
                  :disable="loading2 || selected2.length === 0"
                  @click="propSelect"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("chooseRecord") }}
                  </q-tooltip>
                </q-btn>
              </template>
            </q-banner>
            <q-table
              style="height: calc(100vh - 500px); width: 100%"
              color="primary"
              card-class="bg-amber-1 text-brown"
              row-key="id"
              dense
              :columns="cols2"
              :rows="rows2"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading2"
              selection="single"
              wrap-cells
              v-model:selected="selected2"
              :rows-per-page-options="[0]"
              @update:selected="onUpdateSelect2"
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
            </q-table>
          </template>

          <template v-slot:after>
            <q-table
              color="primary"
              card-class="bg-amber-1 text-brown"
              row-key="id"
              dense
              :columns="cols3"
              :rows="rows3"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading3"
              selection="single"
              wrap-cells
              v-model:selected="selected3"
              :rows-per-page-options="[0]"
            >
              <template v-slot:top>
                <div style="font-size: 1.2em; font-weight: bold;">
                  {{ $t("titleDop") }}
                </div>
                <q-space />
                <q-btn
                  dense
                  icon="post_add"
                  color="secondary"
                  :disable="loading3 || selected2.length === 0"
                  @click="editRow3('ins')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("newRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  dense
                  icon="edit"
                  color="secondary"
                  class="q-ml-sm"
                  :disable="loading3 || selected3.length === 0"
                  @click="editRow3('upd')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("editRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  dense
                  icon="delete"
                  color="red"
                  class="q-ml-sm"
                  :disable="loading3 || selected3.length === 0"
                  @click="removeRow3"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-space />

                <q-btn
                  dense
                  icon="swipe_up_alt"
                  color="secondary"
                  class="q-ml-lg"
                  @click="fnUp(true)"
                  :disable="onoffUp()"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("up") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
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
              </template>

              <template #loading>
                <q-inner-loading :showing="loading3" color="secondary" />
              </template>
            </q-table>
          </template>
        </q-splitter>
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
import UpdateDimMultiProp from "@/pages/multiprop/dim/UpdateDimMultiProp.vue";
import UpdateDimMultiPropMoreCols from "@/pages/multiprop/dim/UpdateDimMultiPropMoreCols.vue";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(30);
const splitterModel2 = ref(60);

const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const visible = ref(false);

const cols2 = ref([]);
const rows2 = ref([]);
const FD_AccessLevel = reactive(new Map());
const FD_DimMultiPropType = reactive(new Map());
const loading2 = ref(false);
const selected2 = ref([]);
const dmpGr = ref(0);
const dmp = ref(0);

const cols3 = ref([]);
const rows3 = ref([]);
const loading3 = ref(false);
const selected3 = ref([]);
const maxLen = ref(0);

const lang = ref("");
const childComp = ref(null);

const propSelect = () => {
  router.push({
    name: "DimMultiPropItem",
    params: {
      dimMultiPropGr: currentNode.value.id,
      dimMultiProp: selected2.value[0].id,
      dimMultiPropName: selected2.value[0].name,
      dimMultiPropType: selected2.value[0].dimMultiPropType,
    },
  });
};

const onUpdateSelect = (item) => {
  dmp.value = 0;
  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    api
      .post("", {
        method: "group/loadRec",
        params: [{ id: currentNode.value.id, tableName: "DimMultiPropGr" }],
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
      params: [{ tableName: "DimMultiPropGr" }],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, "id");
        fnExpand();
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
        notifyError(msg);
      }
    )
    .then(() => {
      if (dmpGr.value > 0) {
        let res = [];
        findNode(rows.value, "id", dmpGr.value, res);
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
      tableName: "DimMultiPropGr",
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
  rec.tableName = "DimMultiPropGr";
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
    component: UpdateDimMultiProp,
    componentProps: {
      data: data,
      mode: mode,
      lg: lg,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows2.value.push(r);
      selected2.value = [];
      selected2.value.push(r);
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
        method: "dimMultiProp/newRec",
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
          method: "dimMultiProp/delete",
          params: [{ rec: rec }],
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
      method: "dimMultiProp/loadDimMultiProp",
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
      if (dmp.value > 0) {
        const cur = rows2.value.filter((it) => {
          return it.id === dmp.value;
        });
        if (cur.length > 0) {
          selected2.value.push(cur[0]);
          onUpdateSelect2(selected2.value);
        }
      }
    })
    .finally(() => {
      visible.value = false;
    });
};

const fnUp = (up) => {
  api
    .post("", {
      method: "dimMultiProp/changeOrdMoreCols",
      params: [{ rec: selected3.value[0], up: up, dimMultiProp: selected2.value[0].id }],
    })
    .then(
      () => {
        loadDimMultiPropMoreCols(selected2.value[0].id);
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    );
};

const indexOf = (id) => {
  let rez = -1;
  rows3.value.forEach((row, index) => {
    if (row.id === id) {
      rez = index;
    }
  });
  return rez;
};

const onoffUp = () => {
  if (selected3.value[0] === undefined) return true;
  else {
    return indexOf(selected3.value[0].id) <= 0;
  }
};

const onoffDown = () => {
  if (selected3.value[0] === undefined) return true;
  else {
    return indexOf(selected3.value[0].id) >= maxLen.value - 1;
  }
};

const onUpdateSelect2 = (item) => {
  const id = item.length === 0 ? 0 : item[0].id;
  loadDimMultiPropMoreCols(id);
};

const loadDimMultiPropMoreCols = (dimMultiProp) => {
  loading3.value = true;
  selected3.value = [];
  api
    .post("", {
      method: "dimMultiProp/loadDimMultiPropMoreCols",
      params: [dimMultiProp],
    })
    .then((response) => {
      rows3.value = response.data.result.records;
      maxLen.value = rows3.value.length;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading3.value = false;
    });
};

const removeRow3 = () => {
  let rec = selected3.value[0];
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      rec.title +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    loading3.value = true;
    api
      .post("", {
        method: "dimMultiProp/deleteMoreCols",
        params: [rec.id],
      })
      .then(
        () => {
          loadDimMultiPropMoreCols(selected2.value[0].id);
          selected3.value = [];
        },
        (error) => {
          notifyError(error.response.data.error.message);
        }
      )
      .finally(() => {
        loading3.value = false;
      });
  });
};

const editRow3 = (mode) => {
  let data = { dimMultiProp: selected2.value[0].id };
  if (mode === "upd") {
    data = selected3.value[0];
  }

  $q.dialog({
    component: UpdateDimMultiPropMoreCols,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((resData) => {
    selected3.value = [];
    loadDimMultiPropMoreCols(selected2.value[0].id);
    selected3.value.push(resData);
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
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
    format: (val) => (FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null),
  },
  {
    name: "dimMultiPropType",
    label: proxy?.$t("dimMultiPropType") || "dimMultiPropType",
    field: "dimMultiPropType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
    format: (val) => (FD_DimMultiPropType.size > 0 ? FD_DimMultiPropType.get(val) : null),
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

const getColumns3 = () => [
  {
    name: "title",
    label: proxy?.$t("titleDop") || "titleDop",
    field: "title",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 100%",
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
      method: "dict/load",
      params: [{ dict: "FD_DimMultiPropType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_DimMultiPropType.set(it.id, it.text);
      });
    });

  cols.value = getColumns();
  cols2.value = getColumns2();
  cols3.value = getColumns3();

  dmpGr.value = parseInt(route.params.dmpGr, 10) || 0;
  dmp.value = parseInt(route.params.dmp, 10) || 0;
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
