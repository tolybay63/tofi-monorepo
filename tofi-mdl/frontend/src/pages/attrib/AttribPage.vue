<template>
  <div class="q-pa-md" style="height: 100%">
    <q-splitter
      v-model="splitterModel"
      class="no-padding no-margin"
      horizontal
      style="height: calc(100vh - 150px); width: 100%"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <q-table
          style="height: 100%; width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          dense
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :filter="filter"
          :loading="loading"
          @update:pagination="updPagination"
          :rows-per-page-options="[20, 25, 0]"
          @request="requestData"
          selection="single"
          @update:selected="updSelection"
          v-model:selected="selected"
          v-model:pagination="pagination"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td
              v-else-if="rows.length > 0" colspan="100%" class="text-bold">
              {{ $t("infoAttrib") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="format_shapes"></q-avatar>
              {{ $t("attributs") }}
            </div>

            <q-space/>
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
              dense icon="edit"
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
              v-if="hasTarget('mdl:mn_ds:attr:del')"
              dense icon="delete"
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
              dense class="q-ml-lg"
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
              v-model="dense"
              :label="$t('isDense')" dense
            />

            <q-space/>
            <q-input
              dense
              debounce="300"
              color="primary"
              v-model="filter"
              :label="$t('txt_filter')"
            >
              <template v-slot:append>
                <q-icon name="search"/>
              </template>
            </q-input>
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <div
          v-if="selected.length > 0 && visVals.includes(selected[0].attribValType)"
        >
          <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
            {{ $t("formatValue") }}
          </q-bar>

          <div>
            <q-table
              style="height: 100%; width: 100%"
              color="primary"
              card-class="bg-amber-1 text-brown"
              row-key="id"
              dense
              :columns="vals.cols"
              :rows="vals.rows"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading2"
              selection="single"
              v-model:selected="selected2"
              :rows-per-page-options="[0]"
            >
              <template v-slot:top>
                <div style="align-self: center">
                  <q-btn
                    v-if="hasTarget('mdl:mn_ds:attr:val:ins')"
                    dense
                    icon="post_add"
                    color="secondary"
                    :disable="loading || vals.rows.length === 1"
                    @click="editRowChar(selected[0].attribValType, null, 'ins')"
                  >
                    <q-tooltip
                      transition-show="rotate"
                      transition-hide="rotate"
                    >
                      {{ $t("newRecord") }}
                    </q-tooltip>
                  </q-btn>
                  <q-btn
                    v-if="hasTarget('mdl:mn_ds:attr:val:upd')"
                    dense
                    icon="edit"
                    color="secondary"
                    class="q-ml-sm"
                    :disable="loading2 || selected2.length === 0"
                    @click="
                      editRowChar(
                        selected[0].attribValType,
                        selected2[0],
                        'upd'
                      )
                    "
                  >
                    <q-tooltip
                      transition-show="rotate"
                      transition-hide="rotate"
                    >
                      {{ $t("editRecord") }}
                    </q-tooltip>
                  </q-btn>
                  <q-btn
                    v-if="hasTarget('mdl:mn_ds:attr:val:del')"
                    dense
                    icon="delete"
                    color="red"
                    class="q-ml-sm"
                    :disable="loading2 || selected2.length === 0"
                    @click="removeRowChar(selected2[0])"
                  >
                    <q-tooltip
                      transition-show="rotate"
                      transition-hide="rotate"
                    >
                      {{ $t("deletingRecord") }}
                    </q-tooltip>
                  </q-btn>
                </div>
              </template>
            </q-table>
          </div>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { exportFile, useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdateAttrib from "@/pages/attrib/UpdateAttrib.vue";
import UpdateAttribChar from "@/pages/attrib/UpdateAttribChar.vue";

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const wrapCsvValue = (val, formatFn) => {
  let formatted = formatFn !== void 0 ? formatFn(val) : val;
  formatted = formatted === void 0 || formatted === null ? "" : String(formatted);
  formatted = formatted.split('"').join('""');
  return `${formatted}`;
};

const requestParam = reactive({
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
});

const cols = ref([]);
const rows = ref([]);

const FD_AccessLevel = reactive(new Map());
const FD_AttribValType = reactive(new Map());
const FD_EntityType = reactive(new Map());
const FD_PeriodType = reactive(new Map());

const filter = ref("");
const loading = ref(false);
const dense = ref(true);

const pagination = ref({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 15,
  rowsNumber: 0,
});

const selected = ref([]);
const splitterModel = ref(60);
const visVals = ref([2, 3, 4, 5, 7, 8, 9]);

const loading2 = ref(false);
const selected2 = ref([]);
const vals = reactive({
  rows: [],
  cols: [],
});

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; bold",
    style: "width: 8%",
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
    format: (val) => FD_AccessLevel.get(val),
  },
  {
    name: "attribValType",
    label: proxy?.$t("attribValType"),
    field: "attribValType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) => FD_AttribValType.get(val),
  },
  {
    name: "isMultiLang",
    label: proxy?.$t("multiLang"),
    field: "isMultiLang",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 5%",
    format: (val) => (val ? proxy?.$t("yes") : proxy?.$t("no")),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 40%",
  },
];

const getValsColumns = (avt) => {
  if (avt === 2)
    return [
      {
        name: "maskReg",
        label: proxy?.$t("maskVal"),
        field: "maskReg",
        align: "left",
        classes: "bg-blue-grey-1",
        headerStyle: "font-size: 1.2em",
      },
    ];
  if ([3, 4, 5].includes(avt))
    return [
      {
        name: "format",
        label: proxy?.$t("formatVal"),
        field: "format",
        align: "left",
        classes: "bg-blue-grey-1",
        headerStyle: "font-size: 1.2em",
      },
    ];
  if (avt === 7)
    return [
      {
        name: "fileExt",
        label: proxy?.$t("fileExt"),
        field: "fileExt",
        align: "left",
        classes: "bg-blue-grey-1",
        headerStyle: "font-size: 1.2em",
      },
    ];
  if (avt === 8)
    return [
      {
        name: "entityType",
        label: proxy?.$t("entityType"),
        field: "entityType",
        align: "left",
        classes: "bg-blue-grey-1",
        headerStyle: "font-size: 1.2em",
        format: (val) => FD_EntityType.get(val),
      },
    ];
  if (avt === 9)
    return [
      {
        name: "periodType",
        label: proxy?.$t("periodType"),
        field: "periodType",
        align: "left",
        classes: "bg-blue-grey-1",
        headerStyle: "font-size: 1.2em",
        format: (val) => FD_PeriodType.get(val),
      },
    ];
  return [];
};

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

const fetchData = (requestProps) => {
  loading.value = true;

  const page = requestProps.rowsPerPage === undefined ? 1 : requestProps.page;
  const rowsPerPage =
    requestProps.rowsPerPage === 0 ? pagination.value.rowsNumber : requestProps.rowsPerPage;
  const orderBy = requestProps.sortBy;
  const flt = requestProps.filter;

  api
    .post("", {
      method: "attrib/loadAttribPaginate",
      params: [{ page, limit: rowsPerPage, orderBy, filter: flt }],
    })
    .then((response) => {
      rows.value = response.data.result.store.records;
      const meta = response.data.result.meta;
      pagination.value.page = meta.page;
      pagination.value.rowsPerPage = meta.limit;
      pagination.value.rowsNumber = meta.total;
      selected.value = [];
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      console.error(msg);
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
    requestParam.sortBy = des === true ? sb + " desc" : sb;
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
  pagination.value.sortBy = requestParam.sortBy;
};

const updSelection = (par) => {
  if (par[0] === undefined) return;
  selected2.value = [];
  const attrib = par[0].id;
  const attribValType = par[0].attribValType;

  if (visVals.value.includes(attribValType)) {
    loading2.value = true;
    api
      .post("", {
        method: "attrib/loadAttribChar",
        params: [{ attrib }],
      })
      .then((response) => {
        vals.rows = response.data.result.records;
        vals.cols = getValsColumns(attribValType);
      })
      .catch((error) => {
        let msg = error.message;
        if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
        notifyError(msg);
      })
      .finally(() => {
        loading2.value = false;
      });
  }
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    cod: "",
    name: "",
    fullName: "",
    accessLevel: 1,
    attribValType: 1,
    isMultiLang: false,
    cmt: null,
  };
  if (mode === "upd") {
    data = { ...rec };
  }

  $q.dialog({
    component: UpdateAttrib,
    componentProps: { data, mode },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
      selected.value = [r];
      updSelection(selected.value);
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
    let index = rows.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: "attrib/delete",
        params: [{ rec }],
      })
      .then(() => {
        rows.value.splice(index, 1);
        selected.value = [];
        notifySuccess(proxy?.$t("success"));
      })
      .catch(() => {
        notifyInfo(proxy?.$t("hasValue"));
      });
  });
};

const editRowChar = (avt, rec, mode) => {
  let data = {
    id: 0,
    maskReg: "",
    format: "",
    fileExt: "",
    entityType: 1,
    periodType: 11,
  };
  if (mode === "upd") {
    data = { ...rec };
  }

  $q.dialog({
    component: UpdateAttribChar,
    componentProps: {
      avt,
      data,
      mode,
      attrib: selected.value[0].id,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      vals.rows.push(r);
      selected2.value = [r];
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key];
        }
      }
    }
  });
};

const removeRowChar = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message: proxy?.$t("deleteRecord") || "Удалить запись?",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      let index = vals.rows.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "attrib/deleteChar",
          params: [{ id: rec.id }],
        })
        .then(() => {
          vals.rows.splice(index, 1);
          selected2.value = [];
          notifySuccess(proxy?.$t("success"));
        })
        .catch(() => {
          notifyInfo(proxy?.$t("hasValue"));
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
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

  const status = exportFile(proxy?.$t("attributs") + ".txt", content, "text/cvs");

  if (status !== true) {
    notifyInfo(proxy?.$t("browserDenied"));
  }
};

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", { method: "dict/load", params: [{ dict: "FD_AccessLevel" }] })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_AccessLevel.set(it.id, it.text);
      });
    });

  api
    .post("", { method: "dict/load", params: [{ dict: "FD_AttribValType" }] })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_AttribValType.set(it.id, it.text);
      });
    });

  api
    .post("", { method: "dict/load", params: [{ dict: "FD_EntityType" }] })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_EntityType.set(it.id, it.text);
      });
    });

  api
    .post("", { method: "dict/load", params: [{ dict: "FD_PeriodType" }] })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_PeriodType.set(it.id, it.text);
      });
    });

  fetchData(requestParam);
});
</script>

<style></style>
