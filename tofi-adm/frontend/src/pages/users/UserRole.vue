<template>
  <div class="q-pa-sm-sm" style="height: calc(100vh - 220px)">
    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      :rows-per-page-options="[0]"
      dense
    >
      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          {{ $t("roles2") }}
        </div>

        <q-space />

        <q-btn
          v-if="hasTarget('adm:usr:gr:usr:sel:role')"
          dense
          icon="edit_note"
          color="secondary"
          class="q-ml-sm"
          @click="fnEdit()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("update") }}
          </q-tooltip>
        </q-btn>
      </template>

      <template #loading>
        <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script setup>
import {getCurrentInstance, onMounted, ref} from "vue";
import {useRoute} from "vue-router";
import {useQuasar} from "quasar";
import {api} from "@/boot/axios";
import UpdaterUserRole from "@/pages/users/UpdaterUserRole.vue";
import {hasTarget} from "../../utils/jsutils.js";

const { proxy } = getCurrentInstance();
const $q = useQuasar();
const route = useRoute();

const user_id = ref(0);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const fnEdit = () => {
  $q.dialog({
    component: UpdaterUserRole,
    componentProps: {
      user: user_id.value,
      dense: true,
    },
  }).onOk(() => {
    fetchData(user_id.value);
  });
};

const fetchData = (user) => {
  loading.value = true;
  api
    .post("", {
      method: "usr/loadUserRoles",
      params: [user],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });
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

onMounted(() => {
  user_id.value = route.params.user;
  cols.value = getColumns();
  fetchData(user_id.value);
});
</script>

<style scoped></style>
