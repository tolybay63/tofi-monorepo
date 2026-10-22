<template>
  <q-table
    color="primary"
    card-class="bg-amber-1"
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
        v-if="hasTarget('mdl:mn_ds:pm:sel:dim:edit')"
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

      <q-btn
        v-if="hasTarget('mdl:mn_ds:pm:sel:dim:ord')"
        dense
        icon="swipe_up_alt"
        color="secondary"
        class="q-ml-lg"
        @click="fnUp(true)"
        :disable="onoffUp()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("up") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:pm:sel:dim:ord')"
        dense
        icon="swipe_down_alt"
        color="secondary"
        class="q-ml-sm"
        @click="fnUp(false)"
        :disable="onoffDown()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("down") }}
        </q-tooltip>
      </q-btn>
    </template>

    <template v-slot:body="props">
      <q-tr :props="props">
        <td style="width: 5px">
          <q-btn
            :dense="dense"
            flat
            color="blue"
            :icon="
              selected.length === 1 && props.row.id === selected[0].id
                ? 'check_box'
                : 'check_box_outline_blank'
            "
            @click="selectedRow(props.row)"
          />
        </td>

        <q-td key="cod" :props="props">
          {{ props.row.cod }}
        </q-td>

        <q-td key="name" :props="props">
          {{ props.row.name }}
        </q-td>

        <q-td key="fullName" :props="props">
          {{ props.row.fullName }}
        </q-td>

        <q-td key="isFilled" :props="props">
          <q-icon
            :dense="dense"
            flat
            color="blue"
            size="24px"
            :name="
              props.row.isFilled === 1 ? 'check_box' : 'check_box_outline_blank'
            "
          />
        </q-td>

        <q-td key="dimNumber" :props="props">
          {{ props.row.dimNumber }}
        </q-td>
      </q-tr>
    </template>

    <template #loading>
      <q-inner-loading showing color="secondary" />
    </template>
  </q-table>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError } from "@/utils/jsutils";
import UpdaterMultiPropDim from "@/pages/multiprop/UpdaterMultiPropDim.vue";

const route = useRoute();
const router = useRouter();
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

const fnUp = (up) => {
  api
    .post("", {
      method: "multiProp/changeOrdMultiPropDim",
      params: [{ rec: selected.value[0], up: up }],
    })
    .then(
      () => {
        load(multiPropId.value);
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    );
};

const onoffUp = () => {
  if (selected.value[0] === undefined) return true;
  else {
    return indexOf(selected.value[0].id) <= 0;
  }
};

const onoffDown = () => {
  if (selected.value[0] === undefined) return true;
  else {
    return indexOf(selected.value[0].id) >= maxLen.value - 1;
  }
};

const indexOf = (id) => {
  let rez = -1;
  rows.value.forEach((row, index) => {
    if (row.id === id) {
      rez = index;
    }
  });
  return rez;
};

const load = (multiProp) => {
  loading.value = true;
  api
    .post("", {
      method: "multiProp/loadMultiPropDim",
      params: [multiProp],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      maxLen.value = rows.value.length;

      if (selected.value.length > 0) {
        let curId = selected.value[0].id;
        selected.value = [];
        let index = rows.value.findIndex((row) => row.id === curId);
        if (index > -1) {
          selected.value[0] = rows.value[index];
        }
      }
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

const editData = () => {
  $q.dialog({
    component: UpdaterMultiPropDim,
    componentProps: {
      multiPropId: multiPropId.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      load(multiPropId.value);
    }
  });
};

const getColumns = () => [
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
    headerStyle: "font-size: 1.2em; width: 50%",
  },
  {
    name: "isFilled",
    label: proxy?.$t("isFilled") || "isFilled",
    field: "isFilled",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
  {
    name: "dimNumber",
    label: "Номер измерения",
    field: "dimNumber",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
];

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  multiPropId.value = parseInt(route.params.mp, 10) || 0;
  load(multiPropId.value);
});
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}
</style>
