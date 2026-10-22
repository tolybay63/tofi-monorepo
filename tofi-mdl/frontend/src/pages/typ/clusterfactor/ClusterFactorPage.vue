<template>
  <div class="no-padding no-margin">
    <q-table
      style="height: calc(100vh - 220px); width: 100%"
      color="primary"
      card-class="bg-amber-1"
      table-class="text-grey-8"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      :separator="separator"
      :filter="filter"
      :loading="loading"
      :dense="dense"
      :rows-per-page-options="[0]"
      selection="single"
      v-model:selected="selected"
    >
      <template v-slot:body="props">
        <q-tr :props="props">
          <q-td style="width: 5px">
            <q-btn
              dense
              flat
              color="blue"
              :icon="
                selected.length === 1 && selected[0].id === props.row.id
                  ? 'check_box'
                  : 'check_box_outline_blank'
              "
              @click="selectedRow(props.row)"
            />
          </q-td>

          <q-td key="name" :props="props">
            <q-icon name="brightness_1" :color="getIcon(props.row)" />
            {{ props.cols[0].value }}
          </q-td>
          <q-td key="fullName" :props="props">
            {{ props.cols[1].value }}
          </q-td>
          <q-td key="isReq" :props="props">
            {{ props.cols[2].value }}
          </q-td>
          <q-td key="isUniq" :props="props">
            {{ props.cols[3].value }}
          </q-td>
        </q-tr>
      </template>

      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0" class="text-bold">
          {{ infoSelected(selected[0]) }}
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoClusterFactor") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("clusterFactors") }}</div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:typ:sel:fac:ins')"
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
          v-if="hasTarget('mdl:mn_ds:typ:sel:fac:upd')"
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
          v-if="hasTarget('mdl:mn_ds:typ:sel:fac:del')"
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

        <q-toggle
          style="margin-left: 10px"
          :dense="dense"
          v-model="dense"
          :label="$t('isDense')"
        />

        <q-space />
        <q-input
          :dense="dense"
          debounce="300"
          color="primary"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary" />
      </template>
    </q-table>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdateClusterFactor from "@/pages/typ/clusterfactor/UpdateClusterFactor.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const filter = ref("");
const loading = ref(false);
const separator = ref("cell");
const selected = ref([]);
const typId = ref(0);
const dense = ref(true);
const lang = ref("");

const getIcon = (row) => {
  if (row.isOwn === 1) return "green";
  else return "gray";
};

const selectedRow = (row) => {
  if (selected.value.length > 0) {
    if (selected.value[0].id !== row.id) {
      selected.value = [];
      selected.value.push(row);
    } else {
      selected.value = [];
    }
  } else {
    selected.value.push(row);
  }
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    name: "",
    fullName: "",
    typ: typId.value,
    factor: 0,
    isReq: true,
    isUniq: true,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      name: rec.name,
      fullName: rec.fullName,
      typ: rec.typ,
      factor: rec.factor,
      isReq: rec.isReq,
      isUniq: rec.isUniq,
    };
  }
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateClusterFactor,
    componentProps: {
      data: data,
      mode: mode,
      lg: lg,
      dense: dense.value,
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

const fetchData = (typ) => {
  loading.value = true;
  api
    .post("", {
      method: "cluster/loadTypClusterFactor",
      params: [typ],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      selected.value = [];
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = error.response.data.error.message;
      notifyError(msg);
    })
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
  })
    .onOk(() => {
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "cluster/deleteTypClusterFactor",
          params: [rec],
        })
        .then(() => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success") || "success");
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 30%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 40%",
  },
  {
    name: "isReq",
    label: proxy?.$t("isReq") || "isReq",
    field: "isReq",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) => (val ? proxy?.$t("yes") || "yes" : proxy?.$t("no") || "no"),
  },
  {
    name: "isUniq",
    label: proxy?.$t("isUniq") || "isUniq",
    field: "isUniq",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 15%",
    format: (val) => (val ? proxy?.$t("yes") || "yes" : proxy?.$t("no") || "no"),
  },
];

const infoSelected = (row) => {
  return (proxy?.$t("selectedRow") || "selectedRow") + ": " + row.name;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  typId.value = parseInt(route.params.typ, 10);
  fetchData(typId.value);
});
</script>

<style></style>
