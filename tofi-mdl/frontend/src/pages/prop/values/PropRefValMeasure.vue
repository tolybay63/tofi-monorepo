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
import { useRouter } from "vue-router";
import { useQuasar } from "quasar";
import QTreeTable from "@/components/QTreeTable.vue";
import { expandAll, hasTarget, notifyError, pack } from "@/utils/jsutils";
import UpdaterPropRefValMeasure from "@/pages/prop/values/UpdaterPropRefValMeasure.vue";
import { api } from "@/boot/axios";

const props = defineProps({
  prop: [Number, String],
  dense: Boolean,
});

const emit = defineEmits(["ok", "hide"]);

const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const separator = ref("cell");
const loading = ref(false);
const childComp = ref(null);
const lang = ref("");

const load = (propId) => {
  loading.value = true;
  api
    .post("", {
      method: "prop/loadPropVal",
      params: [propId, "Measure"],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "id");
      expandAll(rows.value);
    })
    .catch((error) => {
      router.push("/");
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
    component: UpdaterPropRefValMeasure,
    componentProps: {
      prop: props.prop,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      load(props.prop);
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
  load(props.prop);
});
</script>

<style scoped></style>
