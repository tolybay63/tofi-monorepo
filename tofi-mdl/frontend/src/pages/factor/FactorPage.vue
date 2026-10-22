<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      class="sticky-header-table"
      style="height: calc(100vh - 200px); width: 100%"
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
      @request="requestData"
      @update:pagination="updPagination"
      :rows-per-page-options="[20, 25, 0]"
      selection="single"
      v-model:selected="selected"
      v-model:pagination="pagination"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoFactor") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="account_tree" />
          {{ $t("factors") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:ins')"
          :dense="dense"
          icon="post_add"
          color="secondary"
          :disable="loading"
          @click="editRow(null, true)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:upd')"
          :dense="dense"
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:del')"
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
          v-if="hasTarget('mdl:mn_ds:fac:sel')"
          :dense="dense"
          icon="pan_tool_alt"
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="factorVal"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          :dense="dense"
          class="q-ml-lg"
          icon-right="archive"
          color="secondary"
          no-caps
          @click="exportTable"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("msgToFile") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:ord')"
          :dense="dense"
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
          v-if="hasTarget('mdl:mn_ds:fac:ord')"
          :dense="dense"
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
import { useRouter, useRoute } from "vue-router";
import { useQuasar, exportFile } from "quasar";
import UpdateFactor from "@/pages/factor/UpdateFactor.vue";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";

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
  sortBy: "id",
};

const wrapCsvValue = (val, formatFn) => {
  let formatted = formatFn !== void 0 ? formatFn(val) : val;
  formatted = formatted === void 0 || formatted === null ? "" : String(formatted);
  formatted = formatted.split('"').join('""');
  return `${formatted}`;
};

const cols = ref([]);
const rows = ref([]);
const FD_AccessLevel = reactive(new Map());
const filter = ref("");
const loading = ref(false);
const maxLen = ref(0);
const pagination = ref({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
});
const selected = ref([]);
const dense = ref(true);
const factorId = ref(0);
const lang = ref("");

const indexOf = (id) => {
  let rez = -1;
  rows.value.forEach((row, index) => {
    if (row.id === id) {
      rez = index;
    }
  });
  return rez;
};

const onoffUp = () => {
  if (selected.value[0] === undefined) return true;
  else {
    return indexOf(selected.value[0].id) <= 0;
  }
};

const onoffDown = () => {
  if (selected.value[0] === undefined) return true;
  else {
    return indexOf(selected.value[0].id) >= maxLen.value - 1;
  }
};

const fnUp = (up) => {
  api
    .post("", {
      method: "factor/changeOrdF",
      params: [{ rec: selected.value[0], up: up }],
    })
    .then(
      () => {
        fetchData(requestParam);
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    );
};

const factorVal = () => {
  router.push({
    name: "factorvalmain",
    params: {
      factor: selected.value[0].id,
    },
  });
};

const editRow = (rec, ins) => {
  let data = {
    id: 0,
    cod: "",
    accessLevel: 1,
    name: "",
    fullName: "",
    cmt: null,
  };
  if (!ins) {
    data = {
      id: rec.id,
      cod: rec.cod,
      accessLevel: rec.accessLevel,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
  }
  const upd = { isIns: ins };
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateFactor,
    componentProps: {
      form: data,
      upd: upd,
      lg: lg,
      action: "factor",
      dense: dense.value,
    },
  }).onOk((r) => {
    if (ins) {
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

const fetchData = (requestProps) => {
  loading.value = true;

  const page = requestProps.rowsPerPage === undefined ? 1 : requestProps.page;
  const rowsPerPage =
    requestProps.rowsPerPage === 0 ? pagination.value.rowsNumber : requestProps.rowsPerPage;
  const orderBy = requestProps.sortBy;
  const reqFilter = requestProps.filter;

  api
    .post("", {
      method: "factor/loadFactorPaginate",
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
        maxLen.value = rows.value.length;

        selected.value = [];
        if (factorId.value > 0) {
          let index = rows.value.findIndex((row) => row.id === factorId.value);
          if (index !== -1) {
            selected.value.push(rows.value[index]);
          }
        }
      },
      (error) => {
        let msg;
        if (error.response) msg = proxy?.$t(error.response.data.error.message);
        else msg = error.message;
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
          method: "factor/delete",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            notifySuccess(proxy?.$t("success"));
          },
          () => {
            notifyInfo(proxy?.$t("hasValue"));
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
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel"),
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) => (FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 35%",
  },
];

const exportTable = () => {
  const content = [cols.value.map((col) => wrapCsvValue(col.label))]
    .concat(
      rows.value.map((row) =>
        cols.value
          .map((col) =>
            wrapCsvValue(
              typeof col.field === "function"
                ? col.field(row)
                : row[col.field === void 0 ? col.name : col.field],
              col.format
            )
          )
          .join("\t")
      )
    )
    .join("\r\n");

  const status = exportFile(proxy?.$t("factors") + ".txt", content, "text/cvs");

  if (status !== true) {
    notifyInfo(proxy?.$t("browserDenied"));
  }
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

  cols.value = getColumns();

  factorId.value = parseInt(route.params.factor, 10) || 0;
  fetchData(requestParam);
});
</script>

<style scoped>
.sticky-header-table {
  max-height: 100%;
  overflow: auto;
}

.sticky-header-table table {
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background-color: #607d8b;
}

.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>
