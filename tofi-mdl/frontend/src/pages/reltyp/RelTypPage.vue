<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :dense="dense"
      @update:pagination="updPagination"
      :rows-per-page-options="[20, 25, 0]"
      :max="pagesNumber()"
      @request="requestData"
      selection="single"
      v-model:pagination="pagination"
      v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoRel") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="view_column" />
          {{ $t("reltyps") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:ins')"
          :dense="dense"
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
          v-if="hasTarget('mdl:mn_ds:reltyp:upd')"
          :dense="dense"
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
          v-if="hasTarget('mdl:mn_ds:reltyp:del')"
          :dense="dense"
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
          v-if="hasTarget('mdl:mn_ds:reltyp:sel')"
          style="margin-left: 30px"
          :dense="dense"
          icon="pan_tool_alt"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="relSelect"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-toggle
          style="margin-left: 10px"
          :dense="dense"
          v-model="dense"
          :label="$t('isDense')"
        />

        <q-space />
        <q-input
          :dense="dense"
          debounce="300"
          color="primary"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary" />
      </template>
    </q-table>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar, date } from "quasar";
import UpdateRelTyp from "@/pages/reltyp/UpdateRelTyp.vue";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

const cols = ref([]);
const rows = ref([]);
const FD_AccessLevel = reactive(new Map());
const filter = ref("");
const loading = ref(false);
const pagination = ref({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 15,
  rowsNumber: 0,
});
const selected = ref([]);
const dense = ref(true);
const reltypId = ref(0);
const lang = ref("");

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

const relSelect = () => {
  router.push({
    name: "reltypSelected",
    params: {
      reltyp: selected.value[0].id,
      relcls: 0,
      tab: "vers",
    },
  });
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    cod: "",
    accessLevel: 1,
    card: 1,
    isOpenness: true,
    lastVer: 1,
    name: "",
    fullName: "",
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      cod: rec.cod,
      accessLevel: rec.accessLevel,
      card: rec.card,
      isOpenness: rec.isOpenness,
      lastVer: rec.lastVer,
      dbeg: rec.dbeg > tofi_dbeg ? rec.dbeg : null,
      dend: rec.dend < tofi_dbeg ? rec.dend : null,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
  }

  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateRelTyp,
    componentProps: {
      data: data,
      mode: mode,
      lg: lg,
      dense: dense.value,
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

const pagesNumber = () => {
  return 1;
};

const fetchData = (requestProps) => {
  loading.value = true;

  const page = requestProps.rowsPerPage === undefined ? 1 : requestProps.page;
  const rowsPerPage =
    requestProps.rowsPerPage === 0
      ? pagination.value.rowsNumber
      : requestProps.rowsPerPage;
  const orderBy = requestProps.sortBy;
  const filterVal = requestProps.filter;

  api
    .post("", {
      method: "reltyp/loadRelTypPaginate",
      params: [
        {
          page: page,
          limit: rowsPerPage,
          orderBy: orderBy,
          filter: filterVal,
        },
      ],
    })
    .then(
      (response) => {
        rows.value = response.data.result.store.records;
        const meta = response.data.result.meta;
        pagination.value.page = meta.page;
        pagination.value.rowsPerPage = meta.limit;
        pagination.value.rowsNumber = meta.total;
        selected.value = [];

        if (reltypId.value > 0) {
          let index = rows.value.findIndex((row) => row.id === reltypId.value);
          if (index > -1) {
            selected.value.push(rows.value[index]);
          }
        }
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

const requestData = (requestProps) => {
  const sb = requestProps.pagination.sortBy;
  const des = requestProps.pagination.descending;
  if (sb === null) {
    requestParam.sortBy = null;
  } else {
    if (des === true) requestParam.sortBy = sb + " desc";
    else requestParam.sortBy = sb;
  }
  requestParam.descending = requestProps.pagination.descending;
  requestParam.filter = requestProps.filter;
  requestParam.page = requestProps.pagination.page;
  requestParam.rowsPerPage = requestProps.pagination.rowsPerPage;
  requestParam.rowsNumber = requestProps.pagination.rowsNumber;

  pagination.value.sortBy = requestProps.pagination.sortBy;
  pagination.value.descending = requestProps.pagination.descending;

  fetchData(requestParam);
};

const updPagination = () => {
  return (pagination.value.sortBy = requestParam.sortBy);
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
  })
    .onOk(() => {
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "reltyp/delete",
          params: [rec],
        })
        .then(() => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success") || "success");
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 30%",
  },
  {
    name: "dbeg",
    label: proxy?.$t("fldDbegShort") || "fldDbegShort",
    field: "dbeg",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) =>
      val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
  {
    name: "dend",
    label: proxy?.$t("fldDendShort") || "fldDendShort",
    field: "dend",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em;",
    style: "width: 10%",
    format: (val) =>
      val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
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

  cols.value = getColumns();

  reltypId.value = parseInt(route.params.reltyp, 10);
  fetchData(requestParam);
});
</script>

<style scoped></style>
