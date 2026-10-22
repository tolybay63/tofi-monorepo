<template>
  <q-table
    style="height: 100%; width: 100%"
    color="primary"
    card-class="bg-amber-1"
    row-key="id"
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    table-header-class="text-bold text-white bg-blue-grey-13"
    separator="cell"
    :loading="loading"
    dense
    selection="single"
    v-model:selected="selected"
    :rows-per-page-options="[0]"
  >
    <template #bottom-row>
      <q-td colspan="100%" v-if="selected.length > 0">
        <span class="text-blue"> {{ $t("selectedRow") }}: </span>
        <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
      </q-td>
      <q-td
        v-else-if="rows.length > 0"
        colspan="100%"
        class="text-bold"
      >
        {{ $t("infoApp") }}
      </q-td>
    </template>

    <template v-slot:top>
      <div style="font-size: 1.2em; font-weight: bold;">
        {{ $t("multiPropItemMeter") }}
      </div>
      <q-space />
      <q-btn
        v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:ins')"
        dense
        icon="post_add"
        color="secondary"
        @click="editRow('ins')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("newRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:upd')"
        dense
        icon="edit"
        color="secondary"
        class="q-ml-sm"
        :disable="loading || selected.length === 0"
        @click="editRow('upd')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:del')"
        dense
        icon="delete"
        color="red"
        class="q-ml-sm"
        :disable="loading || selected.length === 0"
        @click="removeRow"
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
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError } from "@/utils/jsutils";
import UpdateDimMultiPropItemMeter from "@/pages/multiprop/dim/UpdateDimMultiPropItemMeter.vue";

const props = defineProps({
  dimMultiPropItem: [Number, String],
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);

const infoSelected = (row) => {
  if (row) return " " + row["measureName"];
  return null;
};

const editRow = (mode) => {
  let data = { dimMultiPropItem: props.dimMultiPropItem };
  if (mode === "upd") {
    data = selected.value[0];
  }

  $q.dialog({
    component: UpdateDimMultiPropItemMeter,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((resData) => {
    selected.value = [];
    loadDimMultiPropItemMeter(props.dimMultiPropItem);
    selected.value.push(resData);
  });
};

const removeRow = () => {
  let rec = selected.value[0];
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      rec["measureName"] +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "dimMultiProp/deleteDimMultiPropItemMeter",
        params: [rec.id],
      })
      .then(
        () => {
          loadDimMultiPropItemMeter(props.dimMultiPropItem);
          selected.value = [];
        },
        (error) => {
          notifyError(error.response.data.error.message);
        }
      );
  });
};

const getColumns = () => [
  {
    name: "measureName",
    label: proxy?.$t("measure") || "measure",
    field: "measureName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "minVal",
    label: proxy?.$t("minVal") || "minVal",
    field: "minVal",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "maxVal",
    label: proxy?.$t("maxVal") || "maxVal",
    field: "maxVal",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "digit",
    label: proxy?.$t("digit") || "digit",
    field: "digit",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const loadDimMultiPropItemMeter = (dmpi) => {
  loading.value = true;
  api
    .post("", {
      method: "dimMultiProp/loadDimMultiPropItemMeter",
      params: [dmpi],
    })
    .then((response) => {
      rows.value = response.data.result.records;
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

defineExpose({
  loadDimMultiPropItemMeter,
});

onMounted(() => {
  cols.value = getColumns();
  loadDimMultiPropItemMeter(props.dimMultiPropItem);
});
</script>

<style scoped></style>
