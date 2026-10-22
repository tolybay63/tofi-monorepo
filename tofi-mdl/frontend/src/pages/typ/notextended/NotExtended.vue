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
      :table-colspan="2"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :dense="dense"
      :rows-per-page-options="[0]"
      selection="single"
      v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0" class="text-bold">
          {{ infoSelected(selected[0]) }}
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("notextended") }}</div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:typ:sel:notext:ins')"
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
          v-if="hasTarget('mdl:mn_ds:typ:sel:notext:upd')"
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
          v-if="hasTarget('mdl:mn_ds:typ:sel:notext:del')"
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
        <q-inner-loading :showing="loading" color="secondary" />
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
import UpdateNotExtended from "@/pages/typ/notextended/UpdateNotExtended.vue";

const props = defineProps({
  typParent: [Number, String],
});

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const filter = ref("");
const loading = ref(false);
const separator = ref("cell");
const selected = ref([]);
const dense = ref(true);
const typ = ref(props.typParent);
const lang = ref("");

const infoSelected = (row) => {
  let o =
    row["nameObj"] !== undefined
      ? " - " + (proxy?.$t("obj") || "obj") + ": " + row["nameObj"]
      : "";
  return (proxy?.$t("cls") || "cls") + ": " + (row["nameClass"] || "") + o;
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    typ: props.typParent,
    clsOrObjCls: null,
    obj: null,
  };

  if (mode === "upd") {
    data = {
      id: rec.id,
      typ: rec.typ,
      clsOrObjCls: rec.clsOrObjCls,
      obj: rec.obj,
    };
  }

  $q.dialog({
    component: UpdateNotExtended,
    componentProps: {
      mode: mode,
      data: data,
      typ: props.typParent,
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

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      (rec["nameClass"] || "") +
      (rec["nameObj"] ? "; " + rec["nameObj"] : "") +
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
          method: "typ/deleteNotExtended",
          params: [rec],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            notifySuccess(proxy?.$t("success") || "success");
          },
          (error) => {
            let msg;
            if (error.response?.data?.error?.message) msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const fetchData = (targetTyp) => {
  loading.value = true;
  api
    .post("", {
      method: "typ/loadNotExtended",
      params: [targetTyp],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const getColumns = () => [
  {
    name: "nameClass",
    label: proxy?.$t("cls") || "cls",
    field: "nameClass",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "text-align: left; width:50%",
  },
  {
    name: "nameObj",
    label: proxy?.$t("obj") || "obj",
    field: "nameObj",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "text-align: left; width:50%",
  },
];

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  typ.value = parseInt(route.params.typ, 10);
  fetchData(props.typParent);
});
</script>

<style scoped></style>
