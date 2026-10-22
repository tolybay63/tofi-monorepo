<template>
  <div class="no-padding no-margin">
    <q-splitter
      v-model="splitterModel"
      class="no-padding no-margin"
      horizontal
      style="height: calc(100vh - 300px); width: 100%"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <q-table
          style="height: 100%; width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          dense
          selection="single"
          v-model:selected="selected"
          :rows-per-page-options="[10, 15, 20, 0]"
          @update:selected="updSelected"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td
              colspan="100%"
              v-else-if="rows.length > 0"
              class="text-bold"
            >
              {{ $t("infoRow") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("relclss") }}</div>
            <q-space />

            <q-btn
              v-if="hasTarget('mdl:mn_ds:reltyp:memb:ins')"
              dense
              icon="edit_note"
              color="secondary"
              :disable="loading"
              @click="createGroup"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("addGroupRecords") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:reltyp:memb:ins')"
              dense
              class="q-ml-sm"
              icon="delete_sweep"
              color="secondary"
              :disable="loading"
              @click="deleteGroup"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deleteGroupRecords") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:reltyp:memb:upd')"
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
              v-if="hasTarget('mdl:mn_ds:reltyp:memb:del')"
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

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel')"
              dense
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading || selected.length === 0"
              @click="relClsSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary" />
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <rel-cls-member :relcls="relclsId" ref="childMember" />
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import FD_Consts from "@/pages/all-consts";
import UpdateRelCls from "@/pages/reltyp/relcls/UpdateRelCls.vue";
import RelClsMember from "@/pages/reltyp/relcls/RelClsMember.vue";
import UpdateGroupRelCls from "@/pages/reltyp/relcls/UpdateGroupRelCls.vue";
import DeleteGroupRelCls from "@/pages/reltyp/relcls/DeleteGroupRelCls.vue";

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(70);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);
const FD_AccessLevel = reactive(new Map());
const dataBase = reactive(new Map());
const reltypId = ref(0);
const relclsId = ref(0);
const lang = ref("");
const childMember = ref(null);

const infoSelected = (row) => {
  return " " + row.cod + "-" + row.name;
};

const updSelected = (sel) => {
  if (childMember.value) {
    childMember.value.selected = [];
    if (sel.length > 0) {
      childMember.value.fetchData(sel[0].id);
    } else {
      childMember.value.fetchData(0);
    }
  }
};

const relClsSelect = () => {
  router.push({
    name: "relclsSelected",
    params: {
      reltyp: reltypId.value,
      relcls: selected.value[0].id,
      tab: "vers",
    },
  });
};

const createGroup = () => {
  const lg = { name: lang.value };
  $q.dialog({
    component: UpdateGroupRelCls,
    componentProps: {
      data: { relTyp: reltypId.value },
      lg: lg,
    },
  }).onOk((r) => {
    if (r.res) {
      fetchData(reltypId.value);
    }
  });
};

const deleteGroup = () => {
  const lg = { name: lang.value };
  $q.dialog({
    component: DeleteGroupRelCls,
    componentProps: {
      relTyp: reltypId.value,
      lg: lg,
    },
  }).onOk((r) => {
    fetchData(reltypId.value);
    selected.value = [];
    updSelected(selected.value);

    if (r !== "") {
      notifyInfo(r);
    }
  });
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    cod: null,
    accessLevel: FD_Consts.FD_AccessLevel.common,
    name: "",
    fullName: "",
    relTyp: reltypId.value,
    isOpenness: true,
    dataBase: null,
    ord: null,
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      cod: rec.cod,
      accessLevel: rec.accessLevel,
      name: rec.name,
      fullName: rec.fullName,
      relTyp: reltypId.value,
      isOpenness: rec.isOpenness,
      dataBase: rec.dataBase,
      ord: rec.ord,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateRelCls,
    componentProps: {
      data: data,
      mode: mode,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
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
      "-" +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    let index = rows.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: "relcls/delete",
        params: [rec],
      })
      .then(() => {
        rows.value.splice(index, 1);
        selected.value = [];
        updSelected(selected.value);
        notifySuccess(proxy?.$t("success") || "success");
      });
  });
};

const fetchData = (reltyp) => {
  loading.value = true;

  api
    .post("", {
      method: "relcls/load",
      params: [reltyp],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      selected.value = [];

      if (relclsId.value > 0) {
        let index = rows.value.findIndex((row) => row.id === relclsId.value);
        if (index > -1) {
          selected.value.push(rows.value[index]);
          updSelected(selected.value);
        }
      }
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const getColumns = () => [
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
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) =>
      FD_AccessLevel ? FD_AccessLevel.get(val) : null,
  },
  {
    name: "dataBase",
    label: proxy?.$t("dbNameLabel") || "dbNameLabel",
    field: "dataBase",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) =>
      dataBase ? dataBase.get(val) : null,
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
];

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
      method: "database/loadDbForSelect",
      params: [],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        dataBase.set(it.id, it.name);
      });
    });

  cols.value = getColumns();

  reltypId.value = parseInt(route.params.reltyp, 10);
  relclsId.value = parseInt(route.params.relcls, 10);
  fetchData(reltypId.value);
});
</script>

<style scoped></style>
