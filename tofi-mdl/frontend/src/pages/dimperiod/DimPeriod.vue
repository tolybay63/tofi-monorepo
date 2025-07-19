<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :dense="dense"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :filter="filter"
      :loading="loading"
      @update:pagination="updPagination"
      :rows-per-page-options="[20, 25, 0]"
      :max="pagesNumber"
      @request="requestData"
      selection="single"
      :v-model:pagination="pagination"
      v-model:selected="selected"
      @update:selected="updSelection"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td
          v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="format_shapes"></q-avatar>
          {{ $t("dimsPeriod") }}
        </div>

        <q-space/>
        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmper:ins')"
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
          v-if="hasTarget('mdl:mn_dp:dmper:upd')"
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
          v-if="hasTarget('mdl:mn_dp:dmper:del')"
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
          v-if="hasTarget('mdl:mn_dp:dmper:sel')"
          :dense="dense"
          icon="pan_tool_alt"
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="dpChoise"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-toggle
          style="margin-left: 10px"
          :dense="dense"
          v-model="dense"
          :model-value="dense"
          :label="$t('isDense')"
        />

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmper:view')"
          dense
          icon="visibility"
          color="secondary"
          class="q-ml-sm"
          @click="fnView()"
          :disable="loading || selected.length === 0"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("view") }}
          </q-tooltip>
        </q-btn>

        <q-space/>
        <q-input
          :dense="dense"
          debounce="300"
          color="primary"
          :model-value="filter"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search"/>
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
import {defineComponent, ref} from "vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";

import {date, extend} from "quasar";
import UpdaterDimPeriod from "pages/dimperiod/UpdaterDimPeriod.vue";
import DimPeriodView from "pages/dimperiod/DimPeriodView.vue";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

export default defineComponent({
  name: "DimPeriod",

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: "",
      loading: false,
      FD_AccessLevel: null,

      pagination: ref({
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 25,
        rowsNumber: 0,
      }),
      selected: [],
      dense: true,
      dp: 0
    };
  },

  methods: {
    hasTarget,
    fnView() {
      this.$q
        .dialog({
          component: DimPeriodView,
          componentProps: {
            dimperiod: this.selected[0].id,
            dense: this.dense,
            lg: this.lg,
            cmdDate: true,
            // ...
          },
        })
        .onOk(() => {
        });
    },

    dpChoise() {
      this.$router["push"]({
        name: "DimPeriodItem",
        params: {
          dimperiod: this.selected[0].id,
          name: this.selected[0].name,
        },
      });
    },

    updSelection() {
    },

    editRow(rec, mode) {
      let data = {};
      if (mode === "ins") {
        api
          .post(baseURL, {
            method: "dimperiod/newRec",
            params: [{}],
          })
          .then((response) => {
            //console.log("new rec", response.data.result.records)
            data = response.data.result.records[0];
            this.$q
              .dialog({
                component: UpdaterDimPeriod,
                componentProps: {
                  data: data,
                  mode: mode,
                  dense: this.dense,
                  // ...
                },
              })
              .onOk((r) => {
                //console.log("Ok! updated", r);
                this.rows.push(r);
                this.selected = [];
                this.selected.push(r);
              });
          });
      } else {
        data = extend({}, rec)
        this.$q
          .dialog({
            component: UpdaterDimPeriod,
            componentProps: {
              data: data,
              mode: mode,
              dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                rec[key] = r[key];
              }
            }
          });
      }
    },

    pagesNumber: function () {
      return 1;
    },

    fetchData(requestProps) {
      this.loading = ref(true)
      this.selected = []
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
          id: "1",
          method: "dimperiod/loadDimPeriodPaginate",
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
            //console.info("rows", this.rows)
          },
          (error) => {

            let msg
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            else msg = error.message;
            notifyError(msg);
          }
        )
        .then(() => {
          if (this.dp > 0) {
            let index = this.rows.findIndex((row) => row.id === this.dp)
            this.selected.push(this.rows[index])
          }
        })
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
              method: "dimperiod/delete",
              params: [{rec: rec}],
            })
            .then(
              () => {
                //console.log("response=>>>", response.data)
                this.rows.splice(index, 1);
                this.selected = ref([]);
                notifySuccess(this.$t("success"));
              })
            .catch(() => {
                notifyInfo(this.$t("hasChild"));
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
          //sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          //sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "dbeg",
          label: this.$t("fldDbeg"),
          field: "dbeg",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
            val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "dend",
          label: this.$t("fldDend"),
          field: "dend",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
          format: (val) =>
            val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY"),
        },

        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
          format: (val) =>
            this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
      ];
    },

    infoSelected(row) {
      return " " + row.name;
    },
  },

  created() {
    console.info("created")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();

    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        this.FD_AccessLevel = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_AccessLevel.set(it["id"], it["text"]);
        })
      })
      .catch(error => {
        console.log(error.message);
        notifyError(this.$t(error.message));
      })
  },

  mounted() {
    console.info("mounted")
    this.dp = parseInt(this.$route["params"].dimperiod, 10);
    this.fetchData(requestParam);
  },

  setup() {
  }

});
</script>

<style></style>
