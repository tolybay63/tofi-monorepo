<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: calc(100vh - 220px); width: 100%"
      color="primary"
      card-class="bg-amber-1 text-brown"
      row-key="cod"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :dense="value"
      selection="single"
      v-model:selected="selected"
      @selection="fnSelection"
      :rows-per-page-options="[0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoFactorVal") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("factorVals") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:sel:fv:ins')"
          :dense="value"
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
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], false)"
          v-if="hasTarget('mdl:mn_ds:fac:sel:fv:upd')"
          :dense="value"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          icon="delete"
          color="red"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
          v-if="hasTarget('mdl:mn_ds:fac:sel:fv:del')"
          :dense="value"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:sel:fv:ord')"
          :dense="value"
          icon="swipe_up_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(true)"
          :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:sel:fv:ord')"
          :dense="value"
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

        <q-space />

        <q-btn
          :dense="value"
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

        <q-toggle
          style="margin-left: 10px"
          v-model="value"
          :label="$t('isDense')"
        />

        <q-space />
        <q-input
          :dense="value"
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar, exportFile } from "quasar";
import UpdateFactor from "@/pages/factor/UpdateFactor.vue";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const requestParam = {
  factor: 0,
  filter: "",
  descending: false,
  sortBy: null,
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
const selected = ref([]);
const factor1_id = ref(null);
const maxLen = ref(0);
const value = ref(true);
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
      method: "factor/changeOrdFV",
      params: [{ rec: selected.value[0], up: up }],
    })
    .then(
      () => {
        requestParam.factor = factor1_id.value;
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

const fnSelection = (par) => {
  console.log("par", par);
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
  data.parent = factor1_id.value;
  const upd = { isIns: ins };
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateFactor,
    componentProps: {
      form: data,
      upd: upd,
      lg: lg,
      action: "factor",
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

  const status = exportFile("FactorVals_" + factor1_id.value + ".txt", content, "text/cvs");

  if (status !== true) {
    notifyInfo(proxy?.$t("browserDenied") || "browserDenied");
  }
};

const fetchData = (requestProps) => {
  loading.value = true;
  const orderBy = requestProps.sortBy;
  const reqFilter = requestProps.filter;
  const reqFactor = requestProps.factor;

  api
    .post("", {
      id: "1",
      method: "factor/loadFactorVal",
      params: [
        {
          factor: reqFactor,
          orderBy: orderBy,
          filter: reqFilter,
        },
      ],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      maxLen.value = rows.value.length;

      if (selected.value.length > 0) {
        let curId = selected.value[0].id;
        selected.value = [];
        let index = rows.value.findIndex((row) => row.id === curId);
        if (index !== -1) {
          selected.value[0] = rows.value[index];
        }
      }
    })
    .catch((error) => {
      console.log(error);
      notifyError(error.message);
    })
    .finally(() => {
      loading.value = false;
    });
};

const requestData = (requestProps) => {
  if (requestProps.sortBy === null) {
    requestParam.sortBy = null;
  } else {
    if (requestProps.descending)
      requestParam.sortBy = requestParam.sortBy + " desc";
    else
      requestParam.sortBy = requestParam.sortBy + " asc";
  }
  fetchData(requestParam);
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
          method: "factor/delete",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            notifySuccess(proxy?.$t("success") || "success");
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

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 5%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) => (FD_AccessLevel.size > 0 ? FD_AccessLevel.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 40%",
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
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_AccessLevel.set(it.id, it.text);
      });
    });

  cols.value = getColumns();

  factor1_id.value = parseInt(route.params.factor, 10) || 0;
  requestParam.factor = factor1_id.value;
  fetchData(requestParam);
});
</script>

<style></style>
