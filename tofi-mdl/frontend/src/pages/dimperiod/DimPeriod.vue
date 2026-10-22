<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :dense="dense"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      @update:pagination="updPagination"
      :rows-per-page-options="[20, 25, 0]"
      :max="pagesNumber()"
      @request="requestData"
      selection="single"
      v-model:pagination="pagination"
      v-model:selected="selected"
      @update:selected="updSelection"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td v-else-if="rows.length > 0" colspan="100%" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="format_shapes" />
          {{ $t("dimsPeriod") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmper:ins')"
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
          v-if="hasTarget('mdl:mn_dp:dmper:upd')"
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
          v-if="hasTarget('mdl:mn_dp:dmper:del')"
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
          v-if="hasTarget('mdl:mn_dp:dmper:sel')"
          :dense="dense"
          icon="pan_tool_alt"
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="dpChoise"
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

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmper:view')"
          dense
          icon="visibility"
          color="secondary"
          class="q-ml-sm"
          @click="fnView"
          :disable="loading || selected.length === 0"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("view") }}
          </q-tooltip>
        </q-btn>

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
import { useRouter, useRoute } from "vue-router";
import { useQuasar, date, extend } from "quasar";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdaterDimPeriod from "@/pages/dimperiod/UpdaterDimPeriod.vue";
import DimPeriodView from "@/pages/dimperiod/DimPeriodView.vue";

const router = useRouter();
const route = useRoute();
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
const filter = ref("");
const loading = ref(false);
const FD_AccessLevel = reactive(new Map());

const pagination = ref({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
});
const selected = ref([]);
const dense = ref(true);
const dp = ref(0);
const lang = ref("");

const fnView = () => {
  $q.dialog({
    component: DimPeriodView,
    componentProps: {
      dimperiod: selected.value[0].id,
      dense: dense.value,
      lg: lang.value,
      cmdDate: true,
    },
  }).onOk(() => {});
};

const dpChoise = () => {
  router.push({
    name: "DimPeriodItem",
    params: {
      dimperiod: selected.value[0].id,
      name: selected.value[0].name,
    },
  });
};

const updSelection = () => {};

const editRow = (rec, mode) => {
  let data = {};
  if (mode === "ins") {
    api
      .post("", {
        method: "dimperiod/newRec",
        params: [{}],
      })
      .then((response) => {
        data = response.data.result.records[0];
        $q.dialog({
          component: UpdaterDimPeriod,
          componentProps: {
            data: data,
            mode: mode,
            dense: dense.value,
          },
        }).onOk((r) => {
          rows.value.push(r);
          selected.value = [r];
        });
      });
  } else {
    data = extend({}, rec);
    $q.dialog({
      component: UpdaterDimPeriod,
      componentProps: {
        data: data,
        mode: mode,
        dense: dense.value,
      },
    }).onOk((r) => {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key];
        }
      }
    });
  }
};

const pagesNumber = () => {
  return 1;
};

const fetchData = (requestProps) => {
  loading.value = true;
  selected.value = [];
  const page = requestProps.rowsPerPage === undefined ? 1 : requestProps.page;
  const rowsPerPage =
    requestProps.rowsPerPage === 0 ? pagination.value.rowsNumber : requestProps.rowsPerPage;
  const orderBy = requestProps.sortBy;
  const reqFilter = requestProps.filter;

  api
    .post("", {
      id: "1",
      method: "dimperiod/loadDimPeriodPaginate",
      params: [
        {
          page: page,
          limit: rowsPerPage,
          orderBy: orderBy,
          filter: reqFilter,
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
      },
      (error) => {
        let msg;
        if (error.response) msg = proxy?.$t(error.response.data.error.message);
        else msg = error.message;
        notifyError(msg);
      }
    )
    .then(() => {
      if (dp.value > 0) {
        let index = rows.value.findIndex((row) => row.id === dp.value);
        if (index !== -1) {
          selected.value.push(rows.value[index]);
        }
      }
    })
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
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "dimperiod/delete",
          params: [{ rec: rec }],
        })
        .then(() => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success"));
        })
        .catch(() => {
          notifyInfo(proxy?.$t("hasChild"));
        });
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
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "dbeg",
    label: proxy?.$t("fldDbeg"),
    field: "dbeg",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) => (val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY")),
  },
  {
    name: "dend",
    label: proxy?.$t("fldDend"),
    field: "dend",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) => (val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY")),
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel"),
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
    format: (val) => (FD_AccessLevel ? FD_AccessLevel.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
];

const infoSelected = (row) => {
  return " " + row.name;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

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
      console.log(error.message);
      notifyError(proxy?.$t(error.message) || error.message);
    });

  dp.value = parseInt(route.params.dimperiod, 10) || 0;
  fetchData(requestParam);
});
</script>

<style></style>
