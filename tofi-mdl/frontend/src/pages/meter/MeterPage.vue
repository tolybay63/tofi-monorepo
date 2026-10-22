<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      :columns="cols"
      :dense="dense"
      :filter="filter"
      :loading="loading"
      :max="pagesNumber"
      :rows="rows"
      :rows-per-page-options="[25, 0]"
      :table-colspan="4"
      :wrap-cells="true"
      card-class="bg-amber-1"
      class="sticky-header-table"
      color="primary"
      row-key="id"
      selection="single"
      separator="cell"
      style="height: 100%; width: 100%"
      table-header-class="text-bold text-white bg-blue-grey-13"
      @request="requestData"
      v-model:selected="selected"
      v-model:pagination="pagination"
    >
      <template #bottom-row>
        <q-td v-if="selected.length > 0" colspan="100%">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td v-else-if="rows.length > 0" class="text-bold" colspan="100%">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" icon="scale" text-color="white" />
          {{ $t("meters") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:ins')"
          :dense="dense"
          :disable="loading"
          color="secondary"
          icon="post_add"
          @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:upd')"
          :dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-sm"
          color="secondary"
          icon="edit"
          @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:del')"
          :dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-sm"
          color="red"
          icon="delete"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel')"
          :dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-lg"
          color="secondary"
          icon="pan_tool_alt"
          @click="meterChoise"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          :dense="dense"
          class="q-ml-lg"
          color="secondary"
          icon-right="archive"
          no-caps
          @click="exportTable"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("msgToFile") }}
          </q-tooltip>
        </q-btn>

        <q-toggle
          v-model="dense"
          :dense="dense"
          :label="$t('isDense')"
          style="margin-left: 10px"
        />

        <q-space />
        <q-input
          v-model="filter"
          :dense="dense"
          :label="$t('txt_filter')"
          color="primary"
          debounce="300"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading color="secondary" showing />
      </template>
    </q-table>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar, exportFile, extend } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdateMeter from "@/pages/meter/UpdateMeter.vue";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const requestParam = {
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
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
const filter = ref("");
const loading = ref(false);
const FD_AccessLevel = reactive(new Map());
const FD_MeterStruct = reactive(new Map());

const pagination = ref({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
  filter: "",
});
const selected = ref([]);
const meter = ref(0);
const dense = ref(true);
const lang = ref("");

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

const meterChoise = () => {
  router.push({
    name: "meterSelected",
    params: {
      meter: selected.value[0].id,
      meterStruct: selected.value[0].meterStruct,
    },
  });
};

const editRow = (rec, mode) => {
  let data = {};
  if (mode === "ins") {
    api
      .post("", {
        method: "meter/newRec",
        params: [{}],
      })
      .then((response) => {
        data = response.data.result.records[0];
        $q.dialog({
          component: UpdateMeter,
          componentProps: {
            data: data,
            mode: mode,
            dense: dense.value,
          },
        }).onOk((r) => {
          rows.value.push(r);
          selected.value = [];
          selected.value.push(r);
        });
      });
  } else {
    for (let key in rec) {
      if (Object.prototype.hasOwnProperty.call(rec, key)) {
        data[key] = rec[key];
      }
    }
    $q.dialog({
      component: UpdateMeter,
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

const fetchData = (reqParam) => {
  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "meter/loadMeterPaginate",
      params: [reqParam],
    })
    .then(
      (response) => {
        rows.value = response.data.result.store.records;
        const meta = response.data.result.meta;
        pagination.value.page = meta.page;
        pagination.value.rowsPerPage = meta.limit;
        pagination.value.rowsNumber = meta.total;

        selected.value = [];
        if (meter.value > 0) {
          let index = rows.value.findIndex((row) => row.id === meter.value);
          if (index !== -1) {
            selected.value.push(rows.value[index]);
          }
        }
      },
      (error) => {
        let msg;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
        else msg = error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const requestData = (requestProps) => {
  extend(true, requestParam, requestProps.pagination);
  requestParam.filter = requestProps.filter;
  extend(true, pagination.value, requestProps.pagination);
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
          method: "meter/delete",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            notifySuccess(proxy?.$t("success") || "success");
          },
          () => {
            notifyInfo(proxy?.$t("hasValue") || "hasValue");
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
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
  {
    name: "meterStruct",
    label: proxy?.$t("meterStruct") || "meterStruct",
    field: "meterStruct",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) => (FD_MeterStruct.size > 0 ? FD_MeterStruct.get(val) : null),
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
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
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 40%",
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

  const status = exportFile((proxy?.$t("meters") || "meters") + ".txt", content, "text/cvs");

  if (status !== true) {
    notifyInfo(proxy?.$t("browserDenied") || "browserDenied");
  }
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
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_MeterStruct" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_MeterStruct.set(it.id, it.text);
      });
    });

  meter.value = parseInt(route.params.meter, 10) || 0;
  fetchData(requestParam);
});
</script>

<style lang="sass">
.sticky-header-table
  height: calc(100vh - 140px)
  background-color: #bdbdbd

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    top: 48px

  tbody
    scroll-margin-top: 48px
</style>
