<template>
  <div class="no-padding no-margin">
    <q-splitter v-model="splitterModel" :limits="[60, 100]">
      <template v-slot:before>
        <q-table
          style="height: calc(100vh - 250px); width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          :dense="dense"
          selection="single"
          v-model:selected="selected"
          @update:selected="updSelected"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td
              colspan="100%"
              v-else-if="rows.length > 0"
              class="text-bold"
            >
              {{ $t("infoRole") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("roles2") }}</div>

            <q-space />
            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:role:ins')"
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
              v-if="hasTarget('mdl:mn_ds:typ:sel:role:upd')"
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
              v-if="hasTarget('mdl:mn_ds:typ:sel:role:del')"
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
          </template>

          <template #loading>
            <q-inner-loading :showing="loading" color="secondary" />
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <q-table
          style="height: calc(100vh - 250px); width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          :columns="cols2"
          :rows="rows2"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading2"
          :dense="dense"
          selection="single"
          v-model:selected="selected2"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected2.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected2(selected2[0]) }} </span>
            </q-td>
            <q-td
              colspan="100%"
              v-else-if="rows.length > 0"
              class="text-bold"
            >
              {{ $t("infoLife") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("life") }}</div>
            <q-space />

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:role:life:ins')"
              :dense="dense"
              icon="post_add"
              color="secondary"
              :disable="loading && loading2"
              @click="editRow2(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:role:life:upd')"
              :dense="dense"
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              :disable="loading2 || selected2.length === 0"
              @click="editRow2(selected2[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:role:life:del')"
              :dense="dense"
              icon="delete"
              color="red"
              class="q-ml-sm"
              :disable="loading2 || selected2.length === 0"
              @click="removeRow2(selected2[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
          </template>

          <template #loading>
            <q-inner-loading :showing="loading2" color="secondary" />
          </template>
        </q-table>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar, date } from "quasar";
import { api } from "@/boot/axios";
import UpdateTypRole from "@/pages/typ/typrole/UpdateTypRole.vue";
import UpdateTypRoleLifeInterval from "@/pages/typ/typrole/UpdateTypRoleLifeInterval.vue";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(100);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);
const dense = ref(true);

const cols2 = ref([]);
const rows2 = ref([]);
const loading2 = ref(false);
const selected2 = ref([]);

const typId = ref(0);
const lang = ref("");

const updSelected = (cur) => {
  selected2.value = [];
  if (cur.length > 0) {
    fetchData2(cur[0].id);
    splitterModel.value = 60;
  } else {
    splitterModel.value = 100;
  }
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
  })
    .onOk(() => {
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "typrole/deleteTypRole",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            splitterModel.value = 100;
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

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    typ: typId.value,
    role: 0,
    cmtVer: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      typ: rec.typ,
      role: rec.role,
      cmt: rec.cmt,
    };
  }
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateTypRole,
    componentProps: {
      data: data,
      typ: typId.value,
      mode: mode,
      lg: lg,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.push(r);
      selected.value = [];
      selected.value.push(r);
      splitterModel.value = 60;
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
      method: "typrole/loadTypRole",
      params: [typ],
    })
    .then(
      (response) => {
        rows.value = response.data.result.records;
      },
      (error) => {
        router.push("/");
        let msg;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
        else msg = error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const removeRow2 = (rec) => {
  let d1 =
    rec.dbeg === "1800-01-01"
      ? "..."
      : date.formatDate(rec.dbeg, "DD.MM.YYYY");
  let d2 =
    rec.dend === "3333-12-31"
      ? "..."
      : date.formatDate(rec.dend, "DD.MM.YYYY");

  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
      '<div style="color: plum">(' +
      d1 +
      " - " +
      d2 +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
  })
    .onOk(() => {
      let index = rows2.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "typrole/deleteTypRoleLife",
          params: [{ rec: rec }],
        })
        .then(
          () => {
            rows2.value.splice(index, 1);
            selected2.value = [];
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

const editRow2 = (rec, mode) => {
  let data = {
    id: 0,
    typrole: selected.value[0].id,
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      typrole: rec.typrole,
      dbeg: rec.dbeg > "1800-01-01" ? rec.dbeg : null,
      dend: rec.dend < "3333-12-31" ? rec.dend : null,
      cmt: rec.cmt,
    };
  }
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateTypRoleLifeInterval,
    componentProps: {
      data: data,
      mode: mode,
      lg: lg,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows2.value.push(r);
      selected2.value = [];
      selected2.value.push(r);
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key];
        }
      }
    }
  });
};

const fetchData2 = (typrole) => {
  loading2.value = true;
  api
    .post("", {
      method: "typrole/loadTypRoleLife",
      params: [typrole],
    })
    .then(
      (response) => {
        rows2.value = response.data.result.records;
      },
      (error) => {
        router.push("/");
        let msg;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
        else msg = error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading2.value = false;
    });
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 35%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 45%",
  },
];

const getColumns2 = () => [
  {
    name: "dbeg",
    label: proxy?.$t("fldDbeg") || "fldDbeg",
    field: "dbeg",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 90px",
    format: (val) =>
      val <= "1800-01-01" ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
  {
    name: "dend",
    label: proxy?.$t("fldDend") || "fldDend",
    field: "dend",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em;",
    style: "width: 90px",
    format: (val) =>
      val >= "3333-12-01" ? "..." : date.formatDate(val, "DD.MM.YYYY"),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 40%",
  },
];

const infoSelected = (row) => {
  return " " + row.name;
};

const infoSelected2 = (row) => {
  let d1 =
    row.dbeg === "1800-01-01"
      ? "..."
      : date.formatDate(row.dbeg, "DD.MM.YYYY");
  let d2 =
    row.dend === "3333-12-31"
      ? "..."
      : date.formatDate(row.dend, "DD.MM.YYYY");
  return " " + d1 + " - " + d2;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();
  cols2.value = getColumns2();

  typId.value = parseInt(route.params.typ, 10);
  fetchData(typId.value);
});
</script>

<style scoped></style>
