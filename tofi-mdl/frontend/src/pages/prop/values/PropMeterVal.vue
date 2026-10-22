<template>
  <q-bar class="bg-orange-1 q-mb-sm" style="height: 48px">
    <q-btn
      v-if="hasTarget('mdl:mn_ds:prop:val:edit')"
      dense
      icon="edit_note"
      color="secondary"
      @click="editData"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("update") }}
      </q-tooltip>
    </q-btn>
  </q-bar>

  <QTreeTable :cols="cols" :rows="rows" :icon_leaf="''" ref="childComp" />
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useQuasar } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import { expandAll, hasTarget, notifyError, pack } from "@/utils/jsutils";
import UpdaterPropMeterVal from "@/pages/prop/values/UpdaterPropMeterVal.vue";

const props = defineProps({
  propGr: [Number, String],
  prop: [Number, String],
  meter: [Number, String],
  meterRate: [Number, String],
  meterStruct: [Number, String],
  dense: Boolean,
});

const emit = defineEmits(["ok", "hide"]);

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const separator = ref("cell");
const loading = ref(false);
const childComp = ref(null);
const lang = ref("");

const loadPropMeter = (propId) => {
  loading.value = true;
  api
    .post("", {
      method: "prop/loadPropMeter",
      params: [propId],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
      expandAll(rows.value);
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const editData = () => {
  $q.dialog({
    component: UpdaterPropMeterVal,
    componentProps: {
      prop: props.prop,
      meter: props.meter,
      meterRate: props.meterRate,
      meterStruct: props.meterStruct,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      loadPropMeter(props.prop);
    }
  });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 35%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
];

const show = () => {
  // dialog ref control if needed
};

const hide = () => {
  emit("hide");
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();
  loadPropMeter(props.prop);
});
</script>

<style scoped></style>
