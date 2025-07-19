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
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="perm_contact_calendar"></q-avatar>
          {{ $t("roles") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:role:ins')"
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
            v-if="hasTarget('mdl:mn_ds:role:upd')"
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
            v-if="hasTarget('mdl:mn_ds:role:del')"
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

        <q-toggle
            style="margin-left: 10px"
            :dense="dense"
            v-model="dense"
            :model-value="dense"
            :label="$t('isDense')"
        />

        <q-space/>
        <q-input
            :dense="dense"
            debounce="300"
            color="primary"
            :model-value="filter.value"
            v-model="filter"
            :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import UpdateRole from "pages/role/UpdateRole.vue";

import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default defineComponent({
  methods: {
    hasTarget,
    editRow(rec, mode) {
      let data = {
        id: 0,
        cod: "",
        accessLevel: 0,
        name: "",
        fullName: "",
        roleTag: null,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          cod: rec.cod,
          accessLevel: rec.accessLevel,
          name: rec.name,
          fullName: rec.fullName,
          roleTag: rec.roleTag,
          cmt: rec.cmt,
        };
      }
      //const upd = {isIns: ins};
      const lg = {name: this.lang};

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateRole,
            componentProps: {
              form: data,
              mode: mode,
              lg: lg,
              action: "role",
              dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
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
          })
          .onCancel(() => {
            //console.log('Cancel!')
          });
    },

    pagesNumber: function () {
      return 1;
    },

    fetchData(requestProps) {
      this.loading = ref(true);

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
                this.selected = ref([]);
              },
              (error) => {

                let msg
                if (error.response)
                  msg = this.$t(error.response.data.error.message);
                else msg = error.message;
                notifyError(msg);
              }
          )
          .finally(() => {
            //setTimeout(() => {
            this.loading = ref(false);
            //}, 500)
          });
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

    updPagination() {
      return (this.pagination.sortBy = requestParam.sortBy);
    },

    removeRow(rec) {
      //console.log("Delete Row:", JSON.stringify(rec))
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
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
            let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "role/delete",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    (error) => {
                      let msg
                      if (error.response) msg = error.response.data.error.message;
                      else msg = error.message;
                      notifyError(msg);
                      //notifyInfo(this.$t('hasValue'))
                    }
                );
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 5%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
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
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
          format: (val) => this.FD_AccessLevel.get(val),
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 40%",
        },
      ];
    },
  },

  data: function () {
    return {
      cols: [],
      rows: [],
      FD_AccessLevel: null,
      filter: ref(""),
      loading: ref(false),
      pagination: ref({
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 15,
        rowsNumber: 0,
      }),
      selected: ref([]),
      dense: true,
    };
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.FD_AccessLevel = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_AccessLevel.set(it["id"], it["text"]);
          });
        });

    this.cols = this.getColumns();

    this.fetchData(requestParam);
  },

  setup() {
    return {
      infoSelected(row) {
        return " " + row.cod + " - " + row.name;
      },
    }
  },

});
</script>

<style></style>
