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
    separator="cell"
    :loading="loading"
    :dense="dense"
  >
    <template v-slot:top>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:val:edit')"
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
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError } from "@/utils/jsutils";
import UpdaterPropEntityVal from "@/pages/prop/values/UpdaterPropEntityVal.vue";

const props = defineProps({
  entityType: [Number, String],
});

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const rows = ref([]);
const cols = ref([]);
const loading = ref(false);
const propId = ref(null);
const dense = ref(true);
const lang = ref("");

const editData = () => {
  $q.dialog({
    component: UpdaterPropEntityVal,
    componentProps: {
      prop: propId.value,
      entityType: props.entityType,
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
      method: "prop/loadPropValEntity",
      params: [prop, props.entityType],
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
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
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
    headerStyle: "font-size: 1.2em; width: 45%",
  },
];

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  propId.value = parseInt(route.params.prop, 10);
  fetchData(propId.value);
});
</script>

<style scoped></style>
