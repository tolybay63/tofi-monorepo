<template>
  <div class="q-pa-md" style="height: 100%">
    <q-splitter
        v-model="splitterModel"
        :model-value ="splitterModel"
        class="no-padding no-margin"
        horizontal
        style="height: calc(100vh - 150px); width: 100%"
        separator-class="bg-red"
    >
      <template v-slot:before>
        <q-table
            style="height: 100%; width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            dense
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :filter="filter"
            :loading="loading"
            @update:pagination="updPagination"
            :rows-per-page-options="[20, 25, 0]"
            @request="requestData"
            selection="single"
            @update:selected="updSelection"
            v-model:selected="selected"
            v-model:pagination="pagination"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td
                v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
              {{ $t("infoAttrib") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="format_shapes"></q-avatar>
              {{ $t("attributs") }}
            </div>

            <q-space/>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:attr:ins')"
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
                v-if="hasTarget('mdl:mn_ds:attr:upd')"
                dense icon="edit"
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
                v-if="hasTarget('mdl:mn_ds:attr:del')"
                dense icon="delete"
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
                dense class="q-ml-lg"
                icon-right="archive"
                color="secondary"
                no-caps
                @click="exportTable"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("msgToFile") }}
              </q-tooltip>
            </q-btn>

            <q-toggle
                style="margin-left: 10px"
                v-model="dense"
                :model-value="dense"
                :label="$t('isDense')" dense
            />

            <q-space/>
            <q-input
                dense
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
      </template>

      <template v-slot:after>
        <div
            v-if="selected.length > 0 && visVals.includes(selected[0].attribValType)"
        >
          <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
            {{ $t("formatValue") }}
          </q-bar>

          <div>
            <q-table
                style="height: 100%; width: 100%"
                color="primary"
                card-class="bg-amber-1 text-brown"
                row-key="id"
                dense
                :columns="vals.cols"
                :rows="vals.rows"
                table-header-class="text-bold text-white bg-blue-grey-13"
                separator="cell"
                :loading="loading2.value"
                selection="single"
                v-model:selected="selected2"
                :rows-per-page-options="[0]"
            >
              <template v-slot:top>
                <div style="align-self: center">
                  <q-btn
                      v-if="hasTarget('mdl:mn_ds:attr:val:ins')"
                      dense
                      icon="post_add"
                      color="secondary"
                      :disable="loading || vals.rows.length === 1"
                      @click="editRowChar(selected[0].attribValType, null, 'ins')"
                  >
                    <q-tooltip
                        transition-show="rotate"
                        transition-hide="rotate"
                    >
                      {{ $t("newRecord") }}
                    </q-tooltip>
                  </q-btn>
                  <q-btn
                      v-if="hasTarget('mdl:mn_ds:attr:val:upd')"
                      dense
                      icon="edit"
                      color="secondary"
                      class="q-ml-sm"
                      :disable="loading2 || selected2.length === 0"
                      @click="
                      editRowChar(
                        selected[0].attribValType,
                        selected2[0],
                        'upd'
                      )
                    "
                  >
                    <q-tooltip
                        transition-show="rotate"
                        transition-hide="rotate"
                    >
                      {{ $t("editRecord") }}
                    </q-tooltip>
                  </q-btn>
                  <q-btn
                      v-if="hasTarget('mdl:mn_ds:attr:val:del')"
                      dense
                      icon="delete"
                      color="secondary"
                      class="q-ml-sm"
                      :disable="loading2 || selected2.length === 0"
                      @click="removeRowChar(selected2[0])"
                  >
                    <q-tooltip
                        transition-show="rotate"
                        transition-hide="rotate"
                    >
                      {{ $t("deletingRecord") }}
                    </q-tooltip>
                  </q-btn>
                </div>
              </template>
            </q-table>
          </div>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {defineComponent, ref} from "vue";
import {exportFile} from "quasar";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdateAttrib from "pages/attrib/UpdateAttrib.vue";
import UpdateAttribChar from "pages/attrib/UpdateAttribChar.vue";

const requestParam = {
  page: 1,
  rowsPerPage: 20,
  rowsNumber: 0,
  filter: "",
  descending: false,
  sortBy: null,
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
    updSelection(par) {
      if (par[0] === undefined) return;
      //console.info("updSel", par, par[0])
      this.selected2 = [];
      const attrib = par[0].id;
      const attribValType = par[0].attribValType;
      //
      if (this.visVals.includes(attribValType)) {
        this.loading2 = ref(true);
        api
            .post(baseURL, {
              method: "attrib/loadAttribChar",
              params: [{attrib: attrib}],
            })
            .then((response) => {
              this.vals.rows = response.data.result.records;
              this.vals.cols = this.getValsColumns(attribValType);
            })
            .catch((error) => {
              //console.log(error)
              let msg = error.message;
              if (error.response)
                msg = this.$t(error.response.data.error.message);

              notifyError(msg);
            })
            .finally(() => {
              this.loading2 = ref(false);
            });
      }
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        cod: "",
        name: "",
        fullName: "",
        accessLevel: 1,
        attribValType: 1,
        isMultiLang: false,
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          cod: rec.cod,
          name: rec.name,
          fullName: rec.fullName,
          accessLevel: rec.accessLevel,
          attribValType: rec.attribValType,
          isMultiLang: rec.isMultiLang,
          cmt: rec.cmt,
        };
      }

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateAttrib,
            componentProps: {
              data: data,
              mode: mode,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (mode === "ins") {
              this.rows.push(r);
              this.selected = [];
              this.selected.push(r);
              this.updSelection(this.selected);
            } else {
              for (let key in r) {
                if (r.hasOwnProperty(key)) {
                  rec[key] = r[key];
                }
              }
            }
          });
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
                  method: "attrib/delete",
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
          });
    },

    editRowChar(avt, rec, mode) {
      let data = {
        id: 0,
        maskReg: "",
        format: "",
        fileExt: "",
        entityType: 1,
        periodType: 11,
        //entityType_text: "",
        //periodType_text: "",
      };
      if (mode === "upd") {
        data = {
          id: rec.id,
          maskReg: rec.maskReg,
          format: rec.format,
          fileExt: rec.fileExt,
          entityType: rec.entityType,
          periodType: rec.periodType,
          //entityType_text: rec.entityType_text,
          //periodType_text: rec.periodType_text,
        };
      }

      //console.log("data",data)

      this.$q
          .dialog({
            component: UpdateAttribChar,
            componentProps: {
              avt: avt,
              data: data,
              mode: mode,
              attrib: this.selected[0].id,
              // ...
            },
          })
          .onOk((r) => {
            //console.log("Ok! updated", r);
            if (mode === "ins") {
              this.vals.rows.push(r);
              this.selected2 = [];
              this.selected2.push(r);
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

    removeRowChar(rec) {
      //console.log("Delete Row:", JSON.stringify(rec))
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message: "Удалить запись?",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            let index = this.vals.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "attrib/deleteChar",
                  params: [{id: rec.id}],
                })
                .then(
                    () => {
                      //console.log("response=>>>", response.data)
                      this.vals.rows.splice(index, 1);
                      this.selected2 = ref([]);
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
          });
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
          this.$t("attributs") + ".txt",
          content,
          "text/cvs"
      );

      if (status !== true) {
        notifyInfo(this.$t("browserDenied"));
      }
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
            method: "attrib/loadAttribPaginate",
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
                //console.log("FD_AL", this.FD_AccessLevel)
                this.rows = response.data.result.store.records;
                const meta = response.data.result.meta;
                this.pagination.page = meta.page;
                this.pagination.rowsPerPage = meta.limit;
                this.pagination.rowsNumber = meta.total;
                this.selected = ref([]);
              },
              (error) => {

                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

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

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; bold",
          style: "width: 8%",
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
          name: "attribValType",
          label: this.$t("attribValType"),
          field: "attribValType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
          format: (val) => this.FD_AttribValType.get(val),
        },
        {
          name: "isMultiLang",
          label: this.$t("multiLang"),
          field: "isMultiLang",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 5%",
          format: (val) => (val ? this.$t("yes") : this.$t("no")),
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

    getValsColumns(avt) {
      if (avt === 2)
        return [
          {
            name: "maskReg",
            label: this.$t("maskVal"),
            field: "maskReg",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em",
          },
        ];
      else if (avt === 3 || avt === 4 || avt === 5)
        return [
          {
            name: "format",
            label: this.$t("formatVal"),
            field: "format",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em",
          },
        ];
      else if (avt === 7)
        return [
          {
            name: "fileExt",
            label: this.$t("fileExt"),
            field: "fileExt",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em",
          },
        ];
      else if (avt === 8)
        return [
          {
            name: "entityType",
            label: this.$t("entityType"),
            field: "entityType",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em",
            format: (val) => this.FD_EntityType.get(val),
          },
        ];
      else if (avt === 9)
        return [
          {
            name: "periodType",
            label: this.$t("periodType"),
            field: "periodType",
            align: "left",
            classes: "bg-blue-grey-1",
            headerStyle: "font-size: 1.2em",
            format: (val) => this.FD_PeriodType.get(val),
          },
        ];
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
      FD_AttribValType: null,
      FD_EntityType: null,
      FD_PeriodType: null,

      filter: "",
      loading: false,
      dense: true,

      pagination: {
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 15,
        rowsNumber: 0,
      },
      selected: [],
      splitterModel: 60,
      visVals: [2,3,4,5,7,8,9],

/*      visVals: [allConsts.FD_AttribValType.mask,
        allConsts.FD_AttribValType.dt,
        allConsts.FD_AttribValType.dt,
        allConsts.FD_AttribValType.tm,
        allConsts.FD_AttribValType.file,
        allConsts.FD_AttribValType.entity,
        allConsts.FD_AttribValType.period],*/
      //
      loading2: false,
      selected2: [],
      vals: {
        rows: [],
        cols: [],
      },
    };
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();
    //
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

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AttribValType"}],
        })
        .then((response) => {
          this.FD_AttribValType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_AttribValType.set(it["id"], it["text"]);
          });
        });

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_EntityType"}],
        })
        .then((response) => {
          this.FD_EntityType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_EntityType.set(it["id"], it["text"]);
          });
        });

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_PeriodType"}],
        })
        .then((response) => {
          this.FD_PeriodType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_PeriodType.set(["id"], it["text"]);
          });
        });

    this.fetchData(requestParam);
  },

  setup() {
  },
});
</script>

<style></style>
