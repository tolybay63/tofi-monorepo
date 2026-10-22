<!--@update:selected="updSelection"-->
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
    :rows-per-page-options="[25, 0]"
    selection="single"
    v-model:selected="selected"
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
import UpdaterProvider from "@/pages/prop/UpdaterProvider.vue";
import { hasTarget, notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  act: String,
  fk: [Number, String],
  typ: [Number, String],
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const selected = ref([]);
const loading = ref(false);

const loadProvider = () => {
  loading.value = true;
  api
    .post("", {
      method: props.act + "/loadProvider",
      params: [props.fk],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });
};

const removeRow = (rec) => {
  const inf = rec["nameCls"] + (rec["nameObj"] !== "" ? "-" + rec["nameObj"] : "");
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      inf +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    let index = rows.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: props.act + "/deleteProvider",
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
  });
};

const editRow = (row, mode) => {
  let data = {
    id: 0,
    prop: props.fk,
    cls: null,
    obj: null,
    isDefault: false,
  };
  if (props.act === "multiProp") data.multiProp = props.fk;

  if (mode === "upd") {
    data = {
      id: row.id,
      prop: row.prop,
      cls: row.cls,
      obj: row.obj,
      isDefault: row.isDefault,
    };
    if (props.act === "multiProp") data.multiProp = row.prop;
  }

  $q.dialog({
    component: UpdaterProvider,
    componentProps: {
      data: data,
      act: props.act,
      prop: props.fk,
      typ: props.typ,
      mode: mode,
    },
  }).onOk((resData) => {
    selected.value = [];
    loadProvider();
    selected.value.push(resData);
  });
};

const getColumns = () => [
  {
    name: "nameCls",
    label: proxy?.$t("cls") || "cls",
    field: "nameCls",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 45%",
  },
  {
    name: "nameObj",
    label: proxy?.$t("obj") || "obj",
    field: "nameObj",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 50%",
  },
  {
    name: "isDefault",
    label: proxy?.$t("isDefault") || "isDefault",
    field: "isDefault",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 5%",
    format: (val) => (val ? proxy?.$t("yes") || "yes" : proxy?.$t("no") || "no"),
  },
];

const infoSelected = (row) => {
  const inf = row["nameCls"] + (row["nameObj"] !== "" ? "-" + row["nameObj"] : "");
  return " " + inf;
};

onMounted(() => {
  cols.value = getColumns();
  loadProvider();
});
</script>

<style scoped></style>
