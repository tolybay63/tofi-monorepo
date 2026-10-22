<template>
  <q-table
    color="primary"
    card-class="bg-amber-1 text-brown"
    table-class="text-grey-8"
    row-key="id"
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    :table-colspan="4"
    table-header-class="text-bold text-white bg-blue-grey-13"
    separator="cell"
    :dense="dense"
    :rows-per-page-options="[0]"
    selection="single"
    v-model:selected="selected"
  >
    <template v-slot:top>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:pm:sel:cond:ins')"
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
        v-if="hasTarget('mdl:mn_ds:pm:sel:cond:upd')"
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
        v-if="hasTarget('mdl:mn_ds:pm:sel:cond:del')"
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
    </template>

    <template #loading>
      <q-inner-loading showing color="secondary" />
    </template>
  </q-table>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdaterMultiPropCond from "@/pages/multiprop/UpdaterMultiPropCond.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const props = defineProps({
  lg: Object,
  dense: Boolean,
});

const lang = ref(props.lg);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const multiPropId = ref(null);
const selected = ref([]);
const maxLen = ref(0);

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
  }
};

const load = (multiProp) => {
  loading.value = true;
  api
    .post("", {
      method: "multiProp/loadMultiPropCond",
      params: [multiProp],
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

const editRow = (rec, mode) => {
  let data = {
    id: null,
    multiProp: multiPropId.value,
    factor: null,
    typ: null,
    relTyp: null,
    isReq: false,
  };
  if (mode === "upd") {
    for (let key in rec) {
      if (Object.prototype.hasOwnProperty.call(rec, key)) {
        data[key] = rec[key];
      }
    }
  }
  $q.dialog({
    component: UpdaterMultiPropCond,
    componentProps: {
      data: data,
      mode: mode,
      multiProp: multiPropId.value,
      dense: props.dense,
    },
  }).onOk((r) => {
    if (r.res) {
      load(multiPropId.value);
    }
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
  })
    .onOk(() => {
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "multiProp/deleteMultiPropCond",
          params: [rec.id],
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
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
    style: "width: 25%",
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
    name: "flag",
    label: proxy?.$t("entityType") || "entityType",
    field: "flag",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
    format: (val) => proxy?.$t(val) || val,
  },
  {
    name: "isReq",
    label: proxy?.$t("isReq") || "isReq",
    field: "isReq",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
    format: (val) => (val ? proxy?.$t("yes") || "yes" : proxy?.$t("no") || "no"),
  },
];

onMounted(() => {
  cols.value = getColumns();
  multiPropId.value = parseInt(route.params.mp, 10) || 0;
  load(multiPropId.value);
});
</script>

<style scoped></style>
