<template>
  <div class="no-padding no-margin" style="height: 100%">
    <q-table
      class="sticky-header-table"
      style="height: 100%; width: 100%"
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
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("chargrtyp") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_ds:typ:sel:char:ins')"
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
          v-if="hasTarget('mdl:mn_ds:typ:sel:char:upd')"
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
          v-if="hasTarget('mdl:mn_ds:cgt:del')"
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
          @click="charGrTypSel"
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
        <q-inner-loading showing color="secondary" />
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
import UpdateTypCharGr from "@/pages/chargr/typchargr/UpdateTypCharGr.vue";

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
const tcg = ref(0);

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
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "typName",
    label: proxy?.$t("typ"),
    field: "typName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "fvName",
    label: proxy?.$t("clusterFV"),
    field: "fvName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
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
    headerStyle: "font-size: 1.2em; width: 15%",
  },
];

const fetchData = () => {
  loading.value = true;
  selected.value = [];

  api
    .post("", {
      method: "typ/loadTypCharGr",
      params: [],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .then(() => {
      if (tcg.value > 0) {
        let index = rows.value.findIndex((row) => row.id === tcg.value);
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

const charGrTypSel = () => {
  if (selected.value.length > 0) {
    router.push({
      name: "typCharGrProp",
      params: {
        typCharGr: selected.value[0].id,
      },
    });
  }
};

const fnToggle = (val) => {
  dense.value = val;
};

const selectedRow = (val) => {
  tcg.value = 0;
  emit("updateSelect", val);
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    accessLevel: 1,
    cod: "",
    name: "",
    fullName: "",
    typ: null,
    factorVal: null,
    cmt: null,
  };
  if (mode === "upd") {
    data = { ...rec };
  }

  $q.dialog({
    component: UpdateTypCharGr,
    componentProps: {
      data: data,
      mode: mode,
      dense: dense.value,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
      selected.value = [r];
      selectedRow(selected.value);
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
          method: "typ/deleteTypCharGr",
          params: [rec],
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
  tcg.value = 0;
  if (tab === "typchargr") {
    tcg.value = parseInt(route.params.chargr, 10);
  }
  fetchData();
});
</script>

<style scoped>
.sticky-header-table {
  max-height: 100%;
  overflow: auto;
}

.sticky-header-table table {
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background-color: #607d8b;
}

.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>
