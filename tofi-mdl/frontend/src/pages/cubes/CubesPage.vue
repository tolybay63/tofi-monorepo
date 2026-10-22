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
              {{ $t("cubesGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:insgr')"
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
                v-if="hasTarget('mdl:mn_ds:prop:insgr')"
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
                v-if="hasTarget('mdl:mn_ds:prop:updgr')"
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
                v-if="hasTarget('mdl:mn_ds:prop:delgr')"
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

              <q-inner-loading :showing="loading" color="secondary" />
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
        <q-table
          style="height: calc(100vh - 150px); width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          dense
          :columns="cols2"
          :rows="rows2"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          :rows-per-page-options="[0]"
          selection="single"
          @update:selected="updSelection2"
          v-model:selected="selected2"
        >
          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="view_in_ar" />
              {{ $t("cubes") }}
            </div>
            <q-space />

            <q-btn
              v-if="hasTarget('mdl:mn_ds:attr:ins')"
              dense
              icon="post_add"
              color="secondary"
              :disable="loading"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:attr:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected2.length === 0"
              @click="editRow(selected2[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:attr:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              :disable="loading || selected2.length === 0"
              @click="removeRow(selected2[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:cgt:sel')"
              dense
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading || selected2.length === 0"
              @click="cubesSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <template #bottom-row>
            <q-td colspan="100%" v-if="selected2.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected2[0]) }} </span>
            </q-td>
            <q-td v-else-if="rows.length > 0" colspan="100%" class="text-bold">
              {{ $t("infoRow") }}
            </q-td>
          </template>
        </q-table>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar, date } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios.js";
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  pack,
} from "@/utils/jsutils.js";
import UpdateGroup from "@/pages/group/UpdateGroup.vue";
import UpdateCubes from "@/pages/cubes/UpdateCubes.vue";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(30);
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const loading = ref(false);

const cols2 = ref([]);
const rows2 = ref([]);
const selected2 = ref([]);

const FD_AccessLevel = reactive(new Map());
const FD_CubeSType = reactive(new Map());

const cubesId = ref(0);
const cubesGrId = ref(0);
const childComp = ref(null);

const lang = ref(localStorage.getItem("curLang") === "en-US" ? "en" : localStorage.getItem("curLang"));

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
    name: "accessLevel",
    label: proxy?.$t("accessLevel"),
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) => FD_AccessLevel.get(val),
  },
  {
    name: "cubeSType",
    label: proxy?.$t("cubesType"),
    field: "cubeSType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) => FD_CubeSType.get(val),
  },
  {
    name: "dOrg",
    label: proxy?.$t("dOrg"),
    field: "dOrg",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 5%",
    format: (val) =>
      val <= tofi_dbeg || val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
  },
];

const fnExpand = () => expandAll(rows.value);
const fnExpand2 = () => expandAll(rows2.value);

const fetchData = (cubesGr) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadCubes",
      params: [cubesGr, 0],
    })
    .then((response) => {
      rows2.value = pack(response.data.result.records, "ord");
      fnExpand2();
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .then(() => {
      if (cubesId.value > 0) {
        let res = [];
        findNode(rows2.value, "id", cubesId.value, res);
        if (res[0]) {
          selected2.value.push(res[0]);
        }
      }
    })
    .finally(() => {
      loading.value = false;
    });
};

const fetchDataGr = () => {
  currentNode.value = null;
  api
    .post("", {
      method: "group/loadGroup",
      params: [{ tableName: "CubeSGr" }],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "id");
      fnExpand();
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .then(() => {
      if (cubesGrId.value > 0 && childComp.value) {
        let res = [];
        findNode(rows.value, "id", cubesGrId.value, res);
        currentNode.value = res[0] || null;
        if (currentNode.value) {
          childComp.value.restoreSelect(currentNode.value);
          fetchData(currentNode.value.id);
        }
      }
    });
};

const onUpdateSelect = (item) => {
  cubesGrId.value = item.id;
  cubesId.value = 0;

  currentNode.value = item.selected !== undefined ? item.selected : null;
  if (currentNode.value) {
    api
      .post("", {
        method: "group/loadRec",
        params: [{ id: currentNode.value.id, tableName: "CubeSGr" }],
      })
      .then((response) => {
        currentNode.value = response.data.result.records[0];
        fetchData(currentNode.value.id);
      });
  } else {
    fetchData(0);
  }
};

const editRow = (row, mode) => {
  let data = {
    id: 0,
    cod: "",
    cubeSGr: currentNode.value?.id || 0,
    name: "",
    fullName: "",
    accessLevel: 1,
    cubeSType: 1,
    dOrg: date.formatDate(new Date(), "YYYY-MM-DD"),
    cmt: null,
  };
  if (mode === "upd") {
    data = { ...row };
  }

  $q.dialog({
    component: UpdateCubes,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows2.value.push(r);
      selected2.value = [r];
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          row[key] = r[key];
        }
      }
    }
  });
};

const removeRow = (rec) => {
  cubesId.value = 0;

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
  }).onOk(() => {
    api
      .post("", {
        method: "cubes/deleteCube",
        params: [rec],
      })
      .then(
        () => {
          selected2.value = [];
          fetchData(currentNode.value.id);
        },
        (error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        }
      );
  });
};

const cubesSelect = () => {
  router.push({
    name: "CubeSelected",
    params: {
      cubesGr: currentNode.value.id,
      cubes: selected2.value[0].id,
    },
  });
};

const updSelection2 = (par) => {
  console.log("updSelection2", par);
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
    data = { ...currentNode.value };
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
      tableName: "CubeSGr",
      parentName: parentName,
      lg: lang.value,
    },
  }).onOk((dataResult) => {
    fetchDataGr();
    currentNode.value = dataResult;
    childComp.value?.restoreSelect(dataResult);
    onUpdateSelect({ selected: dataResult });
  });
};

const fnDelGr = (rec) => {
  rec.tableName = "CubeSGr";
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

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

onMounted(() => {
  cubesId.value = parseInt(route.params.cubes, 10);
  cubesGrId.value = parseInt(route.params.cubesGr, 10);
  fetchDataGr();
});

onMounted(() => {
  loading.value = true;
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
      if (error.response?.data?.error) {
        notifyError(proxy?.$t(error.response.data.error.message));
      } else {
        notifyError(error.message);
      }
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_CubeSType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_CubeSType.set(it.id, it.text);
      });
    })
    .finally(() => {
      loading.value = false;
    });

  cols.value = getColumns();
  cols2.value = getColumns2();
});
</script>

<style scoped></style>
