<template>
  <q-table
    color="primary"
    card-class="bg-amber-1"
    row-key="id"
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    :table-colspan="4"
    table-header-class="text-bold text-white bg-blue-grey-13"
    separator="cell"
    dense
    :rows-per-page-options="[0]"
  >
    <template v-slot:top>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:s:edit')"
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

    <template v-slot:body="props">
      <q-tr :props="props">
        <q-td key="cod" :props="props">
          {{ props.row.cod }}
        </q-td>

        <q-td key="name" :props="props">
          {{ props.row.name }}
        </q-td>

        <q-td key="isDefault" :props="props">
          <q-btn
            dense
            flat
            color="blue"
            :icon="
              props.row.isDefault
                ? 'check_box'
                : 'check_box_outline_blank'
            "
          />
        </q-td>
      </q-tr>
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
import UpdaterStatus from "@/pages/prop/UpdaterStatus.vue";
import { hasTarget } from "@/utils/jsutils.js";

const props = defineProps({
  act: String,
  fk: [Number, String],
  factor: [Number, String],
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const loadStatus = () => {
  loading.value = true;
  api
    .post("", {
      method: props.act + "/loadStatus",
      params: [props.fk],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });
};

const editData = () => {
  $q.dialog({
    component: UpdaterStatus,
    componentProps: {
      data: { act: props.act, fk: props.fk, factor: props.factor },
      dense: true,
    },
  }).onOk((resData) => {
    if (resData.res) {
      loadStatus();
    }
  });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
    style: "width: 25%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 70%",
    style: "width: 70%",
  },
  {
    name: "isDefault",
    label: proxy?.$t("isDefault") || "isDefault",
    field: "isDefault",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
    style: "width: 5%",
  },
];

onMounted(() => {
  cols.value = getColumns();
  loadStatus();
});
</script>

<style scoped></style>
