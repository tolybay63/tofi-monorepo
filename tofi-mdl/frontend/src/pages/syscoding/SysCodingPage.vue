<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      style="height: 100%; width: 100%"
      dense color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      :rows-per-page-options="[20, 25, 0]"
      selection="single"
      v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="pin" />
          {{ $t("syscoding") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mdl:mn_dop:syscoding:ins')"
          dense icon="post_add" color="secondary"
          :disable="loading" @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_dop:syscoding:upd')"
          dense icon="edit" color="secondary" class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_dop:syscoding:del')"
          dense icon="delete" color="red" class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="false"
          dense icon="pan_tool_alt" color="secondary" class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="selectSysCoding"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-space />
        <q-input
          dense debounce="300" color="primary" v-model="filter"
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
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifySuccess } from "@/utils/jsutils";
import UpdateSysCoding from "@/pages/syscoding/UpdateSysCoding.vue";

const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const FD_AccessLevel = reactive(new Map());
const FD_SysCodingType = reactive(new Map());
const filter = ref("");
const loading = ref(false);
const selected = ref([]);

const fetchData = () => {
  loading.value = true;
  api
    .post("", {
      method: "syscoding/load",
      params: [],
    })
    .then(
      (response) => {
        rows.value = response.data.result.records;
        selected.value = [];
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const selectSysCoding = () => {
  router.push({
    name: "SysCodingSelectedPage",
    params: {
      syscoding: selected.value[0].id,
    },
  });
};

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      rec.cod +
      ": " +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    let index = rows.value.findIndex((row) => row.id === rec.id);
    api
      .post("", {
        method: "syscoding/delete",
        params: [rec],
      })
      .then(
        () => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success") || "success");
        },
        (error) => {
          let msg = error.message;
          if (error.response?.data?.error?.message)
            msg = error.response.data.error.message;

          notifyError(msg);
        }
      );
  });
};

const editRow = (rec, mode) => {
  let data = {};
  if (mode === "ins") {
    loading.value = true;
    api
      .post("", {
        method: "syscoding/newRec",
        params: [],
      })
      .then(
        (response) => {
          data = response.data.result.records[0];
        },
        (error) => {
          let msg = error.message;
          if (error.response?.data?.error?.message)
            msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
          notifyError(msg);
        }
      )
      .finally(() => {
        loading.value = false;
      });
  } else {
    data = {
      id: rec.id,
      cod: rec.cod,
      accessLevel: rec.accessLevel,
      sysCodingType: rec.sysCodingType,
      sourceStock: rec.sourceStock,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateSysCoding,
    componentProps: {
      data: data,
      mode: mode,
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

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "sysCodingType",
    label: proxy?.$t("sysCodingType") || "sysCodingType",
    field: "sysCodingType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
    format: (val) =>
      FD_SysCodingType ? FD_SysCodingType.get(val) : null,
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel") || "accessLevel",
    field: "accessLevel",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
    format: (val) =>
      FD_AccessLevel ? FD_AccessLevel.get(val) : null,
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
];

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_AccessLevel.set(it.id, it.text);
      });
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_SysCodingType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_SysCodingType.set(it.id, it.text);
      });
    });

  fetchData();
});
</script>

<style scoped></style>
