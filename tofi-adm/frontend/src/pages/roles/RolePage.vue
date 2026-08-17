<template>
  <q-page class="q-pa-md" style="height: calc(100vh - 140px);">
    <q-table
      class="sticky-header-table"
      style="width: 100%"
      color="primary"
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
      :dense="dense"
      :rows-per-page-options="[25, 0]"
      :max="pagesNumber"
      @request="requestData"
      selection="single"
      v-model:pagination="pagination"
      v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
          {{ $t("infoRole") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="manage_accounts" />
          {{ $t("roles2") }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('adm:role:ins')"
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
          v-if="hasTarget('adm:role:upd')"
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
          v-if="hasTarget('adm:role:sel')"
          :dense="dense"
          icon="pan_tool_alt"
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="fnChoose"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-space />
        <q-input
          :dense="dense"
          debounce="300"
          color="primary"
          :model-value="filter"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </q-page>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError, notifyInfo, notifySuccess } from "../../utils/jsutils";
import UpdateRole from "@/pages/roles/UpdaterRole.vue";

const { proxy } = getCurrentInstance();
const $q = useQuasar();
const route = useRoute();
const router = useRouter();

const cols = ref([]);
const rows = ref([]);
const filter = ref("");
const loading = ref(false);
const maxLen = ref(0);
const role_id = ref(0);

let pagination = reactive({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
});

const selected = ref([]);
const dense = ref(true);

const requestParam = {
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 25%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 60%",
  },
];

const fnChoose = () => {
  router.push({
    name: "RoleSelected",
    params: {
      role: selected.value[0].id,
    },
  });
};

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
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
          method: "role/delete",
          params: [{ rec: rec }],
        })
        .then(() => {
          rows.value.splice(index, 1);
          selected.value = [];
          notifySuccess(proxy?.$t("success"));
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
    });
};

const editRow = (rec, mode) => {
  let data = {
    id: 0,
    name: "",
    fullName: "",
    cmt: null,
  };
  if (mode === "upd") {
    data = {
      id: rec.id,
      name: rec.name,
      fullName: rec.fullName,
      cmt: rec.cmt,
    };
  }

  $q.dialog({
    component: UpdateRole,
    componentProps: {
      data: data,
      mode: mode,
    },
  })
    .onOk((r) => {
      if (mode === "ins") {
        rows.value.push(r);
        selected.value = [];
        selected.value.push(r);
      } else {
        for (let key in r) {
          if (r.hasOwnProperty(key)) {
            rec[key] = r[key];
          }
        }
      }
    });
};

const fetchData = (requestProps) => {
  loading.value = true;

  const page = requestProps.page;
  const rowsPerPage = requestProps.rowsPerPage;
  const orderBy = requestProps.sortBy;
  const filterVal = requestProps.filter;

  api
    .post("", {
      method: "role/loadRolePaginate",
      params: [
        {
          page: page,
          limit: rowsPerPage,
          orderBy: orderBy,
          filter: filterVal,
        },
      ],
    })
    .then(
      (response) => {
        rows.value = response.data.result.store.records;
        const meta = response.data.result.meta;
        pagination.page = meta.page;
        pagination.rowsPerPage = meta.limit === meta.total ? 0 : meta.limit;
        pagination.rowsNumber = meta.total;
        maxLen.value = rows.value.length;

        selected.value = [];
        if (role_id.value > 0) {
          let index = rows.value.findIndex((row) => row.id === role_id.value);
          if (index !== -1) {
            selected.value[0] = rows.value[index];
          }
        }
      },
      (error) => {
        router.push("/");
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message);

        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

const pagesNumber = () => 1;

const requestData = (requestProps) => {
  const sb = requestProps.pagination.sortBy;
  const des = requestProps.pagination.descending;

  if (sb === null) {
    requestParam.sortBy = null;
  } else {
    if (des === true) requestParam.sortBy = sb + " desc";
    else requestParam.sortBy = sb;
  }
  requestParam.descending = requestProps.pagination.descending;
  requestParam.filter = requestProps.filter;
  requestParam.page = requestProps.pagination.page;
  requestParam.rowsPerPage = requestProps.pagination.rowsPerPage;
  requestParam.rowsNumber = requestProps.pagination.rowsNumber;

  pagination.sortBy = requestProps.pagination.sortBy;
  pagination.descending = requestProps.pagination.descending;

  fetchData(requestParam);
};

const infoSelected = (row) => " " + row.name;

// Инициализация при создании компонента
role_id.value = parseInt(route.params.role, 10) || 0;
cols.value = getColumns();
fetchData(requestParam);
</script>

<style lang="sass">
.sticky-header-table
  height: calc(100vh - 140px)

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    top: 48px

  tbody
    scroll-margin-top: 48px
</style>
