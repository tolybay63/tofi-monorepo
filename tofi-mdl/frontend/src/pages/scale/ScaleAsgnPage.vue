<template>
  <q-splitter
    v-model="splitterModel"
    class="no-padding no-margin"
    horizontal
    style="height: calc(100vh - 250px); width: 100%"
    separator-class="bg-red"
  >
    <template v-slot:before>
      <q-table
        style="height: 100%; width: 100%"
        dense color="primary"
        card-class="bg-amber-1"
        row-key="id"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :loading="loading"
        :rows-per-page-options="[20, 25, 0]"
        selection="single"
        v-model:selected="selected"
        @update:selected="rowSelected"
        @request="requestData"
      >
        <template #bottom-row>
          <q-td colspan="100%" v-if="selected.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
          </q-td>
          <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
            {{ $t("infoRow") }}
          </q-td>
        </template>

        <template v-slot:top>
          <div style="font-size: 1.2em; font-weight: bold;">
            {{ $t("scaleSettings") }}
          </div>

          <q-space />
          <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ins')"
            dense icon="post_add" color="secondary"
            :disable="loading" @click="editRow(null, 'ins')"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("newRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:upd')"
            dense icon="edit" color="secondary" class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0], 'upd')"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("editRecord") }}
            </q-tooltip>
          </q-btn>
          <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:del')"
            dense icon="delete" color="red" class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="removeRow(selected[0])"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("deletingRecord") }}
            </q-tooltip>
          </q-btn>
        </template>

        <template #loading>
          <q-inner-loading :showing="loading" color="secondary" />
        </template>
      </q-table>
    </template>

    <template v-slot:after>
      <q-table
        style="height: 100%; width: 100%"
        dense color="primary"
        card-class="bg-amber-1"
        row-key="id"
        :columns="cols2"
        :rows="rows2"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :loading="loading2"
        :rows-per-page-options="[20, 25, 0]"
      >
        <template v-slot:body="props">
          <q-tr>
            <q-td key="val" :props="props">
              {{ props.row.val }}
            </q-td>

            <q-td key="name" :props="props">
              {{ props.row.name }}
            </q-td>

            <q-td key="fullName" :props="props">
              {{ props.row.fullName }}
            </q-td>

            <q-td key="scaleValColor" :props="props">
              <q-avatar size="28px" :style="fnStyle(props.row)" />
            </q-td>
          </q-tr>
        </template>

        <template v-slot:top>
          <div style="font-size: 1.2em; font-weight: bold;">
            {{ $t("scaleValSettings") }}
          </div>

          <q-space />
          <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ins')"
            dense icon="edit_note" color="secondary"
            :disable="loading2 || selected.length === 0" @click="editRow2"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("update") }}
            </q-tooltip>
          </q-btn>
        </template>

        <template #loading>
          <q-inner-loading :showing="loading2" color="secondary" />
        </template>
      </q-table>
    </template>
  </q-splitter>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar, date } from "quasar";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios";
import { hasTarget, notifyError, notifySuccess } from "@/utils/jsutils";
import UpdateScaleAsgn from "@/pages/scale/UpdateScaleAsgn.vue";
import UpdateScaleValAsgnUpd from "@/pages/scale/UpdateScaleValAsgnUpd.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const FD_AccessLevel = reactive(new Map());
const loading = ref(false);
const selected = ref([]);
const splitterModel = ref(50);

const cols2 = ref([]);
const rows2 = ref([]);
const loading2 = ref(false);

const scale = ref(0);

const rowSelected = (row) => {
  if (row.length > 0) {
    fetchData2(scale.value, row[0].id);
  } else {
    fetchData2(0, 0);
  }
};

const fnStyle = (row) => {
  return "background:" + row["scaleValColor"];
};

const requestData = () => {};

const fetchData = (scaleVal) => {
  loading.value = true;
  api
    .post("", {
      method: "scale/loadScaleAsgn",
      params: [scaleVal],
    })
    .then(
      (response) => {
        rows.value = response.data.result.records;
        selected.value = [];
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
  }).onOk(() => {
    let index = rows.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: "scale/deleteScaleAsgn",
        params: [rec],
      })
      .then(
        () => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success") || "success");
        },
        (error) => {
          let msg = error.message;
          if (error.response?.data?.error?.message)
            msg = error.response.data.error.message;

          notifyError(msg);
        }
      );
  });
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    cod: "",
    name: "",
    fullName: "",
    accessLevel: 1,
    scale: scale.value,
    dbeg: null,
    dend: null,
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      cod: rec.cod,
      name: rec.name,
      fullName: rec.fullName,
      accessLevel: rec.accessLevel,
      scale: rec.scale,
      dbeg: rec.dbeg === "1800-01-01" ? null : rec.dbeg,
      dend: rec.dend === "3333-12-31" ? null : rec.dend,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateScaleAsgn,
    componentProps: {
      data: data,
      mode: mode,
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

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
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
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
    format: (val) =>
      FD_AccessLevel ? FD_AccessLevel.get(val) : null,
  },
  {
    name: "dbeg",
    label: proxy?.$t("fldDbeg") || "fldDbeg",
    field: "dbeg",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) =>
      val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
  {
    name: "dend",
    label: proxy?.$t("fldDend") || "fldDend",
    field: "dend",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
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

const fetchData2 = (scaleVal, scaleAsgn) => {
  loading2.value = true;
  api
    .post("", {
      method: "scale/loadScaleValAsgn",
      params: [scaleVal, scaleAsgn],
    })
    .then(
      (response) => {
        rows2.value = response.data.result.records;
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

        notifyError(msg);
      }
    )
    .finally(() => {
      loading2.value = false;
    });
};

const editRow2 = () => {
  $q.dialog({
    component: UpdateScaleValAsgnUpd,
    componentProps: {
      scale: scale.value,
      scaleAsgn: selected.value[0].id,
      nmAsgn: selected.value[0].name,
    },
  }).onOk((r) => {
    if (r.res) {
      fetchData2(scale.value, selected.value[0].id);
    }
  });
};

const getColumns2 = () => [
  {
    name: "val",
    label: proxy?.$t("val") || "val",
    field: "val",
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
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 60%",
  },
  {
    name: "scaleValColor",
    label: proxy?.$t("scaleColor") || "scaleColor",
    field: "scaleValColor",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
];

onMounted(() => {
  cols.value = getColumns();
  cols2.value = getColumns2();

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

  scale.value = parseInt(route.params.scale, 10);
  fetchData(scale.value);
});
</script>

<style scoped></style>
