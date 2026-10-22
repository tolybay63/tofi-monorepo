<template>
  <q-page class="q-pa-md" style="height: calc(100vh - 150px);">
    <q-table
      class="sticky-header-table"
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="cod"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      dense
      selection="single"
      v-model:selected="selected"
      :rows-per-page-options="[25, 0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoApp") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="storage" />
          {{ $t("database") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:ins')"
          icon="post_add"
          color="secondary"
          dense
          :disable="loading"
          @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:fac:upd')"
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
          v-if="hasTarget('mdl:mn_ds:fac:del')"
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

        <q-space />
        <q-input
          dense
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
import { useQuasar } from "quasar";
import { useUserStore } from "@/stores/user-store";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdateDataBase from "@/pages/database/UpdateDataBase.vue";

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const filter = ref("");
const loading = ref(false);
const selected = ref([]);
const FD_DataBaseType = reactive(new Map());

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; bold",
    style: "width: 5%",
  },
  {
    name: "modelName",
    label: proxy?.$t("modelNameLabel"),
    field: "modelName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
  },
  {
    name: "dataBaseType",
    label: proxy?.$t("dataBaseType"),
    field: "dataBaseType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) => (FD_DataBaseType ? FD_DataBaseType.get(val) : null),
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 30%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
];

const load = () => {
  loading.value = true;
  api
    .post("", {
      method: "database/load",
      params: [],
    })
    .then(
      (response) => {
        rows.value = response.data.result.records;
      },
      (error) => {
        const store = useUserStore();
        store.setUserName("");
        let msg = error.message;
        if (error.response) {
          msg = proxy?.$t(error.response.data.error.message) || msg;
        }
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const editRow = (rec, mode) => {
  let data = {};
  if (mode === "ins") {
    loading.value = true;
    api
      .post("", {
        method: "database/newRec",
        params: [],
      })
      .then(
        (response) => {
          data = response.data.result.records[0];
          openDialog(data, mode, rec);
        },
        (error) => {
          const store = useUserStore();
          store.setUserName("");
          let msg = error.message;
          if (error.response) {
            msg = proxy?.$t(error.response.data.error.message) || msg;
          }
          notifyError(msg);
        }
      )
      .finally(() => {
        loading.value = false;
      });
  } else {
    data = {
      id: rec.id,
      cod: rec.cod,
      modelName: rec.modelName,
      dataBaseType: rec.dataBaseType,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
    openDialog(data, mode, rec);
  }
};

const openDialog = (data, mode, rec) => {
  $q.dialog({
    component: UpdateDataBase,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
      selected.value = [r];
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
        method: "database/delete",
        params: [rec],
      })
      .then(
        () => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success"));
        },
        (error) => {
          notifyInfo(error.message);
        }
      );
  });
};

onMounted(() => {
  cols.value = getColumns();
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DataBaseType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_DataBaseType.set(it.id, it.text);
      });
    });

  load();
});
</script>

<style lang="sass">
.sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 140px)

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  /* this is when the loading indicator appears */

  &.q-table--loading thead tr:last-child th
    /* height of all previous header rows */
    top: 48px

  /* prevent scrolling behind sticky top row on focus */

  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px
</style>
