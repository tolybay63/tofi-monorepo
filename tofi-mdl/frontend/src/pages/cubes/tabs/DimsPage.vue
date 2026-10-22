<template>
  <q-table
    style="height: calc(100vh - 280px); width: 100%"
    color="primary"
    card-class="bg-amber-1"
    row-key="id"
    dense
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    :table-colspan="4"
    table-header-class="text-bold text-white bg-blue-grey-13"
    separator="cell"
    :loading="loading"
    :rows-per-page-options="[0]"
    selection="single"
    @update:selected="updSelection"
    v-model:selected="selected"
  >
    <template v-slot:top>
      <q-btn
        v-if="hasTarget('mdl:mn_ds:attr:ins')"
        dense
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
        v-if="hasTarget('mdl:mn_ds:attr:upd')"
        dense
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
        v-if="hasTarget('mdl:mn_ds:attr:del')"
        dense
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

      <q-btn
        v-if="hasTarget('mdl:mn_ds:attr:del')"
        dense
        no-caps
        label="Удалить прокуб свойств"
        color="red"
        class="q-ml-xl"
        @click="removeProcubeProp"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:attr:del')"
        dense
        no-caps
        label="Удалить прокуб объектов"
        color="red"
        class="q-ml-sm"
        @click="removeProcubeObj"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("deletingRecord") }}
        </q-tooltip>
      </q-btn>
    </template>

    <template #bottom-row>
      <q-td colspan="100%" v-if="selected.length > 0">
        <span class="text-blue"> {{ $t("selectedRow") }}: </span>
        <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
      </q-td>
      <q-td v-else-if="rows.length > 0" colspan="100%" class="text-bold">
        {{ $t("infoRow") }}
      </q-td>
    </template>
  </q-table>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useQuasar, date } from "quasar";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios.js";
import UpdateDims from "@/pages/cubes/tabs/UpdateDims.vue";
import { hasTarget, notifyError, notifySuccess } from "@/utils/jsutils.js";

const props = defineProps({
  cubeS: [Number, String],
  dOrg: String,
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const selected = ref([]);
const loading = ref(false);
const FD_CubeSDimType = reactive(new Map());
const typeProp = ref(0);

const updSelection = () => {};

const removeProcubeProp = () => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message: proxy?.$t("deleteProcubeProp"),
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    loading.value = true;
    api
      .post("", {
        method: "cubes/deleteProCubeProp",
        params: [props.cubeS],
      })
      .then(
        () => {
          notifySuccess(proxy?.$t("success"));
        },
        (error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        }
      )
      .finally(() => {
        loading.value = false;
      });
  });
};

const removeProcubeObj = () => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message: proxy?.$t("deleteProcubeObj"),
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    loading.value = true;
    api
      .post("", {
        method: "cubes/deleteProCubeObj",
        params: [props.cubeS],
      })
      .then(
        () => {
          notifySuccess(proxy?.$t("success"));
        },
        (error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        }
      )
      .finally(() => {
        loading.value = false;
      });
  });
};

const calcDimPropType = () => {
  rows.value.forEach((r) => {
    if (r.dimPropType && r.dimPropType !== 2) {
      typeProp.value = r.dimPropType;
    }
  });
};

const load = (cubes) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDims",
      params: [cubes, 0],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      calcDimPropType();
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

const editRow = (row, mode) => {
  let data = {
    id: 0,
    cubeS: props.cubeS,
    name: null,
    dimPropType: null,
    dimPropTypeName: null,
    dOrg: props.dOrg,
  };

  if (mode === "upd") {
    data = {
      id: row.id,
      cubeS: row.cubeS,
      name: row.name,
      cubeSDimType: row.cubeSDimType,
      dimPeriod: row.dimPeriod,
      dimProp: row.dimProp,
      dimObj: row.dimObj,
      dOrg: row.dOrg,
      dimPropType: row.dimPropType,
      dimPropTypeName: row.dimPropTypeName,
    };
  }

  $q.dialog({
    component: UpdateDims,
    componentProps: {
      data: data,
      mode: mode,
      typeProp: typeProp.value,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
      selected.value = [r];
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          row[key] = r[key];
        }
      }
    }
    calcDimPropType();
  });
};

const removeRow = (row) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      row.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "cubes/deleteCubeDim",
        params: [row.id],
      })
      .then(
        () => {
          selected.value = [];
          load(row.cubeS);
        },
        (error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        }
      );
  });
};

const infoSelected = (row) => {
  return " " + row.name;
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "cubeSDimType",
    label: proxy?.$t("cubeSDimType"),
    field: "cubeSDimType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
    format: (val) => FD_CubeSDimType.get(val),
  },
  {
    name: "dimPropTypeName",
    label: proxy?.$t("dimPropType"),
    field: "dimPropTypeName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "dOrg",
    label: proxy?.$t("dOrg"),
    field: "dOrg",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) =>
      val <= tofi_dbeg || val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
];

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_CubeSDimType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_CubeSDimType.set(it.id, it.text);
      });
    })
    .finally(() => {
      loading.value = false;
    });

  cols.value = getColumns();
  load(props.cubeS);
});

defineExpose({
  load,
});
</script>

<style scoped></style>
