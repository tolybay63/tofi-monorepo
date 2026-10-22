<template>
  <q-table
    style="height: 100%; width: 100%"
    color="primary"
    card-class="bg-amber-1"
    table-class="text-grey-8"
    row-key="cod"
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    table-header-class="text-bold text-white bg-blue-grey-13"
    table-header-style="size: 3em"
    separator="cell"
    :loading="loading"
    :dense="dense"
    :rows-per-page-options="[20, 25, 0]"
  >
    <template v-slot:top>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:val:period')"
        dense
        icon="edit_note"
        color="secondary"
        class="q-ml-sm"
        @click="editData"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-table>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import UpdaterPropPeriodType from "@/pages/prop/values/UpdaterPropPeriodType.vue";
import { hasTarget, notifyError } from "@/utils/jsutils";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const rows = ref([]);
const cols = ref([]);
const FD_PeriodType = reactive(new Map());
const loading = ref(false);
const propId = ref(null);
const dense = ref(true);
const lang = ref("");

const editData = () => {
  $q.dialog({
    component: UpdaterPropPeriodType,
    componentProps: {
      prop: propId.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      fetchData(propId.value);
    }
  });
};

const fetchData = (prop) => {
  loading.value = true;
  api
    .post("", {
      method: "prop/loadPropPeriodType",
      params: [prop],
    })
    .then((response) => {
      rows.value = response.data.result.records;
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

const getColumns = () => [
  {
    name: "periodType",
    label: proxy?.$t("periodType") || "periodType",
    field: "periodType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 100%",
    format: (val) =>
      FD_PeriodType ? FD_PeriodType.get(val) : null,
  },
];

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PeriodType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_PeriodType.set(it.id, it.text);
      });
    });

  propId.value = parseInt(route.params.prop, 10);
  fetchData(propId.value);
});
</script>

<style scoped></style>
