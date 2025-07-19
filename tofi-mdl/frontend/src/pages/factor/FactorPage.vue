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
        @request="requestData"
        @update:pagination="updPagination"
        :rows-per-page-options="[20, 25, 0]"

        selection="single"
        v-model:selected="selected"
        v-model:pagination="pagination"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoFactor") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="account_tree"></q-avatar>
          {{ $t("factors") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ins')"
            :dense="dense"
            icon="post_add"
            color="secondary"
            :disable="loading"
            @click="editRow(null, true)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:upd')"
            :dense="dense"
            icon="edit"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0], false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:del')"
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
            v-if="hasTarget('mdl:mn_ds:fac:sel')"
            :dense="dense"
            icon="pan_tool_alt"
            color="secondary"
            class="q-ml-lg"
            :disable="loading || selected.length === 0"
            @click="factorVal"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            :dense="dense"
            class="q-ml-lg"
            icon-right="archive"
            color="secondary"
            no-caps
            @click="exportTable"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("msgToFile") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ord')"
            :dense="dense"
            icon="swipe_up_alt"
            color="secondary"
            class="q-ml-lg"
            @click="fnUp(true)"
            :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:ord')"
            :dense="dense"
            icon="swipe_down_alt"
            color="secondary"
            class="q-ml-sm"
            @click="fnUp(false)"
            :disable="onoffDown()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("down") }}
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
import UpdateFactor from "pages/factor/UpdateFactor.vue";
import {exportFile} from "quasar";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: "id",
};

function wrapCsvValue(val, formatFn) {
  let formatted = formatFn !== void 0 ? formatFn(val) : val;
  formatted =
      formatted === void 0 || formatted === null ? "" : String(formatted);

  formatted = formatted.split('"').join('""');
  /**
   * Excel accepts \n and \r in strings, but some other CSV parsers do not
   * Uncomment the next two lines to escape new lines
   */
  // .split('\n').join('\\n')
  // .split('\r').join('\\r')
  return `${formatted}`;
}

export default defineComponent({
  methods: {
    hasTarget,
    fnUp(up) {
      api
          .post(baseURL, {
            method: "factor/changeOrdF",
            params: [{rec: this.selected[0], up: up}],
          })
          .then(
              () => {
                //reload...
                //requestParam.factor = this.factor1_id;
                this.fetchData(requestParam);
              },
              (error) => {
                let msg = error.response.data.error.message
                    ? error.response.data.error.message
                    : error.message;
                notifyError(msg);
              }
          );
    },

    onoffUp() {
      //console.log("selected[0]", this.selected[0])

      if (this.selected[0] === undefined) return true;
      else {
        return this.indexOf(this.selected[0].id) <= 0;
      }
    },

    onoffDown() {
      if (this.selected[0] === undefined) return true;
      else {
        return this.indexOf(this.selected[0].id) >= this.maxLen - 1;
      }
    },

    indexOf: function (id) {
      let rez = -1;
      this.rows.forEach((row, index) => {
        //console.log(row)
        if (row.id === id) {
          rez = index;
        }
      });
      return rez;
    },

    factorVal() {
      this.$router["push"]({
        name: "factorvalmain",
        params: {
          factor: this.selected[0].id,
        },
      });
    },

    editRow(rec, ins) {
      let data = {
        id: 0,
        cod: "",
        accessLevel: 1,
        name: "",
        fullName: "",
        cmt: null,
      };
      if (!ins) {
        data = {
          id: rec.id,
          cod: rec.cod,
          accessLevel: rec.accessLevel,
          name: rec.name,
          fullName: rec.fullName,
          cmt: rec.cmt,
        };
      }
      const upd = {isIns: ins};
      const lg = {name: this.lang};

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateFactor,
            componentProps: {
              form: data,
              upd: upd,
              lg: lg,
              action: "factor",
              dense: this.dense,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (ins) {
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

    },

    fetchData(requestProps) {
      //console.info("fetchData In this.factorId", this.factorId)
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
            method: "factor/loadFactorPaginate",
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
                if (this.factorId > 0) {
                  let index = this.rows.findIndex((row) => row.id === this.factorId);
                  this.selected.push(this.rows[index]);
                }
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
            this.loading = false
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
                  method: "factor/delete",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.rows.splice(index, 1);
                      this.selected = ref([]);
                      notifySuccess(this.$t("success"));
                    },
                    () => {
                      /*
                                            let msg = "";
                                            if (error.response) msg = error.response.data.error.message;
                                            else msg = error.message;
                                            notifyError(msg)
                            */
                      notifyInfo(this.$t("hasValue"));
                    }
                );
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          })
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
          style: "width: 10%",
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
          format: (val) =>
              this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 35%",
        },
      ];
    },

    exportTable() {
      // naive encoding to csv format
      //const cont = [this.cols.map(col => col.label)].concat().join("\t")
      //console.info("cols", cont)

      const content = [this.cols.map((col) => wrapCsvValue(col.label))]
          .concat(
              this.rows.map((row) =>
                  this.cols
                      .map((col) =>
                          wrapCsvValue(
                              typeof col.field === "function"
                                  ? col.field(row)
                                  : row[col.field === void 0 ? col.name : col.field],
                              col.format
                          )
                      )
                      .join("\t")
              )
          )
          .join("\r\n");

      const status = exportFile(
          this.$t("factors") + ".txt",
          content,
          "text/cvs"
      );

      if (status !== true) {
        notifyInfo(this.$t("browserDenied"));
      }
    },

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },

  },

  data: function () {
    return {
      cols: [],
      rows: [],
      FD_AccessLevel: null,
      filter: "",
      loading: false,
      maxLen: 0,
      pagination: {
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 25,
        rowsNumber: 0,
      },
      selected: [],
      dense: true,
      factorId: 0,
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

  mounted() {
    this.factorId = parseInt(this.$route["params"].factor, 10);
    //this.fetchData(requestParam);
  },

  setup() {}

});
</script>

<style></style>
