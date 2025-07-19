<template>
  <q-page class="q-pa-md" style="height: 100px">
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
      :filter="filter"
      :loading="loading"
      :dense="dense"
      @update:pagination="updPagination"
      :rows-per-page-options="[20, 25, 0]"
      :max="pagesNumber"
      @request="requestData"
      selection="single"
      v-model:pagination="pagination"
      v-model:selected="selected"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
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
          v-if="hasTarget('adm:role:del')"
          :dense="dense"
          icon="delete"
          color="secondary"
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

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdateRole from "pages/roles/UpdaterRole.vue";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default {
  name: "RolePage",

  data() {
    return {
      cols: [],
      rows: [],
      filter: "",
      loading: false,
      maxLen: 0,
      role_id: 0,

      pagination: {
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 15,
        rowsNumber: 0,
      },
      selected: [],
      dense: true,
    };
  },

  methods: {
    hasTarget,
    fnChoose() {
      this.$router.push({
        name: "RoleSelected",
        params: {
          role: this.selected[0].id,
        },
      });
    },

    removeRow(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.name +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "role/delete",
              params: [{ rec: rec }],
            })
            .then(
              () => {
                this.rows.splice(index, 1);
                this.selected = ref([]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                let msg = error.message;
                if (error.response) msg = error.response.data.error.message;

                notifyError(msg);
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    editRow(rec, mode) {
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

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdateRole,
          componentProps: {
            data: data,
            mode: mode,
            lg: lg,
            // ...
          },
        })
        .onOk((r) => {
          if (mode === "ins") {
            this.rows.push(r);
            this.selected = [];
            this.selected.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                rec[key] = r[key];
              }
            }
          }
        });
    },

    fetchData(requestProps) {
      this.loading = true;

      const page =
        requestProps.rowsPerPage === undefined ? 1 : requestProps.page;
      const rowsPerPage =
        requestProps.rowsPerPage === 0
          ? this.pagination.rowsNumber
          : requestProps.rowsPerPage;
      const orderBy = requestProps.sortBy;
      const filter = requestProps.filter;
      //
      api
        .post(baseURL, {
          method: "role/loadRolePaginate",
          params: [
            {
              page: page,
              limit: rowsPerPage,
              orderBy: orderBy,
              filter: filter,
            },
          ],
        })
        .then(
          (response) => {
            this.rows = response.data.result.store.records;
            const meta = response.data.result.meta;
            this.pagination.page = meta.page;
            this.pagination.rowsPerPage = meta.limit;
            this.pagination.rowsNumber = meta.total;
            this.maxLen = this.rows.length;
            //
            this.selected = ref([]);
            if (this.role_id > 0) {
              let index = this.rows.findIndex((row) => row.id === this.role_id);
              this.selected[0] = this.rows[index];
            }
          },
          (error) => {
            this.$router.push("/");
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          }
        )
        .finally(() => {
          //setTimeout(() => {
          this.loading = false;
          //}, 500)
        });
    },

    pagesNumber: function () {
      return 1;
    },

    updPagination(newPagination) {
      //console.info(newPagination)
      return (this.pagination.sortBy = requestParam.sortBy);
    },

    requestData(requestProps) {
      const sb = requestProps.pagination.sortBy;
      const des = requestProps.pagination.descending;
      //debugger
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

      this.pagination.sortBy = requestProps.pagination.sortBy;
      this.pagination.descending = requestProps.pagination.descending;
      //
      this.fetchData(requestParam);
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
    this.role_id = parseInt(this.$route.params.role, 10);
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    this.cols = this.getColumns();

    this.fetchData(requestParam);
  },

  computed: {},

  setup() {
    return {
      infoSelected(row) {
        return " " + row.name;
      },
    };
  },
};
</script>

<style scoped></style>
