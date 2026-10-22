<template>
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
    :max="page"
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
        <q-avatar color="black" text-color="white" icon="device_thermostat" />
        {{ $t("scaleVal") }}
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

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifySuccess } from "@/utils/jsutils";
import UpdateScaleVal from "@/pages/scale/UpdateScaleVal.vue";

const emit = defineEmits(["updateSelect"]);

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const page = ref(1);
const selected = ref([]);
const scale = ref(0);

const rowSelected = (val) => {
  emit("updateSelect", { selected: val });
};

const requestData = () => {};

const fetchData = (scaleVal) => {
  loading.value = true;
  api
    .post("", {
      method: "scale/loadScaleVal",
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
        method: "scale/deleteScaleVal",
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
    scale: scale.value,
    minVal: null,
    isMinValOpen: false,
    maxVal: null,
    isMaxValOpen: false,
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      scale: scale.value,
      minVal: rec.minVal,
      isMinValOpen: rec.isMinValOpen,
      maxVal: rec.maxVal,
      isMaxValOpen: rec.isMaxValOpen,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateScaleVal,
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
  return " " + row.name;
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("val") || "val",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 60%",
  },
];

onMounted(() => {
  cols.value = getColumns();
  scale.value = parseInt(route.params.scale, 10);
  fetchData(scale.value);
});
</script>

<style scoped></style>
