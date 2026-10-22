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
    @update:selected="selectedCheck"
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
        {{ $t("multiPropItemFactor") }}
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
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError } from "@/utils/jsutils";
import allConsts from "@/pages/all-consts";
import UpdateDimMultiPropItemFactor from "@/pages/multiprop/dim/UpdateDimMultiPropItemFactor.vue";

const props = defineProps({
  dimMultiPropItem: [Number, String],
});

const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);
const FD_VisualFormat = reactive(new Map());

const selectedCheck = () => {};

const infoSelected = (row) => {
  if (row) return " " + row.name;
  return null;
};

const loadDimMultiPropItemFactor = (dmpi) => {
  loading.value = true;
  api
    .post("", {
      method: "dimMultiProp/loadDimMultiPropItemFactor",
      params: [dmpi],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .catch((error) => {
      router.push("/");
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const editRow = (mode) => {
  let data = {
    dimMultiPropItem: props.dimMultiPropItem,
    visualFormat: allConsts.FD_VisualFormat.cod_short,
  };
  if (mode === "upd") {
    data = selected.value[0];
  }

  $q.dialog({
    component: UpdateDimMultiPropItemFactor,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((resData) => {
    selected.value = [];
    loadDimMultiPropItemFactor(props.dimMultiPropItem);
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
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "dimMultiProp/deleteDimMultiPropItemFactor",
        params: [rec.id],
      })
      .then(
        () => {
          loadDimMultiPropItemFactor(props.dimMultiPropItem);
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
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 70%",
  },
  {
    name: "visualFormat",
    label: proxy?.$t("visualFormat") || "visualFormat",
    field: "visualFormat",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
    format: (val) => (val ? FD_VisualFormat.get(val) : null),
  },
];

defineExpose({
  loadDimMultiPropItemFactor,
});

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_VisualFormat" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_VisualFormat.set(it.id, it.text);
      });
    });

  cols.value = getColumns();
  loadDimMultiPropItemFactor(props.dimMultiPropItem);
});
</script>

<style scoped></style>
