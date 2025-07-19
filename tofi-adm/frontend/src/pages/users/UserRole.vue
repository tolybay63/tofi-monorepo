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

<script>

import {api, baseURL} from "boot/axios";
import UpdaterUserRole from "pages/users/UpdaterUserRole.vue";
import {hasTarget} from "src/utils/jsutils.js";

export default {
  name: "UserRole",

  data() {
    return {
      user_id: 0,
      cols: [],
      rows: [],

      loading: false,

    };
  },

  methods: {
    hasTarget,
    fnEdit() {
      this.$q
        .dialog({
          component: UpdaterUserRole,
          componentProps: {
            user: this.user_id, // userName: this.userName,
            lg: this.lang,
            dense: true,
          },
        })
        .onOk(() => {
          //if (data.res) {
          this.fetchData(this.user_id);
          //}
        });
    },

    fetchData(user) {
      this.loading = true;
      api
        .post(baseURL, {
          method: "usr/loadUserRoles",
          params: [user],
        })
        .then((response) => {
          this.rows = response.data.result.records;
        })
        .finally(() => {
          this.loading = false;
        });
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 25%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 60%",
        },
      ];
    },
  },

  mounted() {
    this.user_id = this.$route["params"].user;
    this.fetchData(this.user_id);
  },

  computed: {},

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
  },

  setup() {
    return {};
  },
};
</script>

<style scoped></style>
