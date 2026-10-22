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
      table-header-class="text-bold text-white bg-blue-grey-13"
      :dense="dense"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :rows-per-page-options="[0]"
      selection="single"
      v-model:selected="selected"
      @update:selected="selectedRow"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>

        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoClusterFactor") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("chargrrel") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:ins')"
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
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:upd')"
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
          v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:del')"
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

        <q-btn
          v-if="hasTarget('mdl:mn_ds:cgt:sel')"
          :dense="dense"
          icon="pan_tool_alt"
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="charGrRelSel"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-toggle
          style="margin-left: 10px"
          :dense="dense"
          v-model="dense"
          :label="$t('isDense')"
          @update:model-value="fnToggle"
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
        <q-inner-loading :showing="loading" color="secondary" />
      </template>
    </q-table>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdaterRelTypCharGr from "@/pages/chargr/relchargr/UpdaterRelTypCharGr.vue";

const emit = defineEmits(["updateSelect"]);

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const filter = ref("");
const loading = ref(false);
const selected = ref([]);
const dense = ref(true);
const rcg = ref(0);

const lang = ref(localStorage.getItem("curLang") === "en-US" ? "en" : localStorage.getItem("curLang"));

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "relClsName",
    label: proxy?.$t("relCls"),
    field: "relClsName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "modelName",
    label: proxy?.$t("model"),
    field: "modelName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "dbNames",
    label: proxy?.$t("database"),
    field: "dbNames",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
];

const fetchData = () => {
  loading.value = true;
  selected.value = [];

  api
    .post("", {
      method: "reltyp/loadRelTypCharGr",
      params: [],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .then(() => {
      if (rcg.value > 0) {
        let index = rows.value.findIndex((row) => row.id === rcg.value);
        if (index !== -1) {
          selected.value.push(rows.value[index]);
        }
      }
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

const charGrRelSel = () => {
  if (selected.value.length > 0) {
    router.push({
      name: "relTypCharGrProp",
      params: {
        relTypCharGr: selected.value[0].id,
      },
    });
  }
};

const fnToggle = (val) => {
  dense.value = val;
};

const selectedRow = (row) => {
  rcg.value = 0;
  emit("updateSelect", row);
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    accessLevel: 1,
    cod: "",
    name: "",
    fullName: "",
    relTyp: null,
    relCls: null,
    cmt: null,
  };
  if (mode === "upd") {
    data = { ...rec };
  }
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdaterRelTypCharGr,
    componentProps: {
      data: data,
      mode: mode,
      lg: lg,
      dense: dense.value,
    },
  }).onOk(() => {
    fetchData();
  });
};

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.cod +
      ": " +
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
          method: "relTyp/deleteRelTypCharGr",
          params: [rec.id],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            notifySuccess(proxy?.$t("success"));
          },
          (error) => {
            let msg = error.message;
            if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
    });
};

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

onMounted(() => {
  cols.value = getColumns();
  let tab = route.params.tab;
  rcg.value = 0;
  if (tab === "relchargr") {
    rcg.value = parseInt(route.params.chargr, 10);
  }
  fetchData();
});
</script>

<style></style>
