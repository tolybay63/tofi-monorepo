<template>
  <q-page padding>
    <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
      {{ $t("cls") }}:
      <span style="color: black; margin-left: 10px">
        {{ infoCls() }}
      </span>

      <q-space />
      <q-btn
        dense round color="secondary" glossy
        icon="arrow_back" @click="toClsPage"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
    </q-bar>

    <q-splitter
      v-model="splitterModel"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <div>
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
            dense
            selection="single"
            v-model:selected="selected"
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
                {{ $t("infoVer") }}
              </q-td>
            </template>

            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold;">{{ $t("vers") }}</div>

              <q-space />
              <q-btn
                v-if="hasTarget('mdl:mn_ds:typ:sel:ver:ins')"
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
                v-if="hasTarget('mdl:mn_ds:typ:sel:ver:upd')"
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
                v-if="hasTarget('mdl:mn_ds:typ:sel:ver:del')"
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
        </div>
      </template>

      <template v-slot:after>
        <div
          class="q-pa-md bg-amber-1"
          style="height: calc(100vh - 250px); width: 100%"
        >
          <div style="font-size: 1.2em; font-weight: bold;">
            {{ $t("valueOfProp") }}
          </div>

          <hr />

          <div class="text-blue">{{ $t("fldName") }}:</div>
          <div class="text-weight-bold">{{ getName() }}</div>
          <div class="text-blue">{{ $t("fldFullName") }}:</div>
          <div class="text-weight-bold">{{ getFullName() }}</div>
          <div class="text-blue">{{ $t("fldCmt") }}:</div>
          <div class="text-weight-bold">{{ getCmt() }}</div>
          <div class="text-blue">{{ $t("life") }}:</div>
          <div class="text-weight-bold">{{ getLife() }}</div>
        </div>
      </template>
    </q-splitter>
  </q-page>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar, date } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";
import UpdateClsVer from "@/pages/typ/cls/UpdateClsVer.vue";

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(30);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const selected = ref([]);
const clsId = ref(0);
const cls = ref({});

const infoCls = () => {
  return (cls.value.cod || "") + " - " + (cls.value.name || "");
};

const toClsPage = () => {
  router.push({
    name: "typSelected",
    params: {
      typ: cls.value.typ,
      cls: clsId.value,
      tab: "clss",
    },
  });
};

const removeRow = (rec) => {
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
      let index = rows.value.findIndex((row) => row.id === rec.id);
      api
        .post("", {
          method: "cls/deleteClsVer",
          params: [rec],
        })
        .then(
          () => {
            rows.value.splice(index, 1);
            selected.value = [];
            loadInfo(clsId.value);
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
    ownerVer: clsId.value,
    name: "",
    fullName: "",
    cmtVer: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      ownerVer: rec.ownerVer,
      name: rec.name,
      fullName: rec.fullName,
      dbeg: rec.dbeg > "1800-01-01" ? rec.dbeg : null,
      dend: rec.dend < "3333-12-31" ? rec.dend : null,
      cmtVer: rec.cmtVer,
    };
  }

  $q.dialog({
    component: UpdateClsVer,
    componentProps: {
      form: data,
      mode: mode,
      dense: true,
    },
  }).onOk((r) => {
    if (mode === "ins") {
      rows.value.unshift(r);
      selected.value = [];
      selected.value.push(r);
      loadInfo(clsId.value);
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key];
        }
      }
    }
  });
};

const getName = () => {
  if (selected.value.length > 0) return selected.value[0].name;
  else return "...";
};

const getFullName = () => {
  if (selected.value.length > 0) return selected.value[0].fullName;
  else return "...";
};

const getCmt = () => {
  if (selected.value.length > 0)
    return selected.value[0].cmtVer ? selected.value[0].cmtVer : "-";
  else return "...";
};

const getLife = () => {
  if (selected.value.length > 0) {
    let d1 =
      selected.value[0].dbeg === "1800-01-01"
        ? "..."
        : date.formatDate(selected.value[0].dbeg, "DD.MM.YYYY");
    let d2 =
      selected.value[0].dend === "3333-12-31"
        ? "..."
        : date.formatDate(selected.value[0].dend, "DD.MM.YYYY");
    return d1 + " - " + d2;
  } else return "...";
};

const fetchData = (clsId) => {
  loading.value = true;
  api
    .post("", {
      method: "cls/loadClsVer",
      params: [clsId],
    })
    .then(
      (response) => {
        rows.value = response.data.result.records;
      },
      (error) => {
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

const getColumns = () => [
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
];

const infoSelected = (row) => {
  let d1 =
    row.dbeg <= "1800-01-01"
      ? "..."
      : date.formatDate(row.dbeg, "DD.MM.YYYY");
  let d2 =
    row.dend >= "3333-12-31"
      ? "..."
      : date.formatDate(row.dend, "DD.MM.YYYY");
  return " " + d1 + " - " + d2;
};

const loadInfo = (clsId) => {
  loading.value = true;
  api
    .post("", {
      method: "cls/loadRecCls",
      params: [clsId],
    })
    .then((response) => {
      console.info("loadRecCls", response.data.result.records[0]);
      cls.value = response.data.result.records[0];
    })
    .catch((error) => {
      console.log(error);
    })
    .finally(() => {
      fetchData(clsId);
      loading.value = false;
    });
};

onMounted(() => {
  cols.value = getColumns();

  clsId.value = parseInt(route.params.cls, 10);
  loadInfo(clsId.value);
});
</script>

<style scoped></style>
