<template>
  <q-page class="q-pa-md">
    <q-table
      style="height: calc(100vh - 160px); width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :dense="dense"
      selection="single"
      :rows-per-page-options="[0]"
      v-model:selected="selected"
      @update:selected="updSelection"
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
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="table_rows" />
          {{ $t("flatTable") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:ft:ins')"
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
          v-if="hasTarget('mdl:mn_ds:ft:upd')"
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
          v-if="hasTarget('mdl:mn_ds:ft:del')"
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
        <q-inner-loading :showing="loading" color="secondary" />
      </template>
    </q-table>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifySuccess } from "@/utils/jsutils";
import UpdaterFlatTable from "@/pages/flattable/UpdaterFlatTable.vue";

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const filter = ref("");
const loading = ref(false);
const selected = ref([]);
const dense = ref(true);
const lang = ref("");
const FD_AccessLevel = reactive(new Map());

const updSelection = () => {};

const infoSelected = (row) => {
  return " " + row.nameTable;
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    cod: null,
    accessLevel: 1,
    nameTable: "",
    cls: null,
    relCls: null,
    name: null,
    fullName: null,
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      cod: rec.cod,
      accessLevel: rec.accessLevel,
      nameTable: rec.nameTable,
      cls: rec.cls,
      relCls: rec.relCls,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
  }
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdaterFlatTable,
    componentProps: {
      rec: data,
      mode: mode,
      lg: lg,
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

const load = () => {
  loading.value = true;

  api
    .post("", {
      method: "flatTable/load",
      params: [],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      selected.value = [];
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;

      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      rec.nameTable +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
  }).onOk(() => {
    let index = rows.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: "flatTable/deleteFlatTable",
        params: [rec],
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
  });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
  {
    name: "nameTable",
    label: proxy?.$t("tableName") || "tableName",
    field: "nameTable",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
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
    name: "nameCls",
    label: proxy?.$t("objClsRelCls") || "objClsRelCls",
    field: "nameCls",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "nameDb",
    label: proxy?.$t("dbNameLabel") || "dbNameLabel",
    field: "nameDb",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
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
  load();
});
</script>

<style></style>
