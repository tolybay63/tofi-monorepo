<template>
  <div class="no-padding no-margin">
    <q-table
        style="height: calc(100vh - 220px); width: 100%"
        color="primary"
        card-class="bg-amber-1 text-brown"
        row-key="cod"
        :columns="cols"
        :rows="rows"
        :wrap-cells="true"
        :table-colspan="4"
        table-header-class="text-bold text-white bg-blue-grey-13"
        separator="cell"
        :filter="filter"
        :loading="loading"
        :dense="value"
        selection="single"
        v-model:selected="selected"
        @selection="fnSelection"
        :rows-per-page-options="[0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t("infoFactorVal") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          {{ $t("factorVals") }}
        </div>

        <q-space/>
        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:sel:fv:ins')"
            :dense="value"
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
            icon="edit"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editRow(selected[0], false)"
            v-if="hasTarget('mdl:mn_ds:fac:sel:fv:upd')"
            :dense="value"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            icon="delete"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="removeRow(selected[0])"
            v-if="hasTarget('mdl:mn_ds:fac:sel:fv:del')"
            :dense="value"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:sel:fv:ord')"
            :dense="value"
            icon="swipe_up_alt"
            color="secondary"
            class="q-ml-sm"
            @click="fnUp(true)"
            :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:fac:sel:fv:ord')"
            :dense="value"
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

        <q-space/>

        <q-btn
            :dense="value"
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

        <q-toggle
            style="margin-left: 10px"
            v-model="value"
            :label="$t('isDense')"
        />

        <q-space/>
        <q-input
            :dense="value"
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
  </div>
</template>

<script>
import {defineComponent, ref} from "vue";
import UpdateFactor from "pages/factor/UpdateFactor.vue";
import {exportFile} from "quasar";
import {api, baseURL} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";

const requestParam = {
  factor: 0,
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
    fnUp(up) {
      api
          .post(baseURL, {
            method: "factor/changeOrdFV",
            params: [{rec: this.selected[0], up: up}],
          })
          .then(
              () => {
                //reload...
                requestParam.factor = this.factor1_id;
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

    fnSelection(par) {
      console.log("par", par)
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
      data.parent = this.factor1_id;
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
          "FactorVals_" + this.factor1_id + ".txt",
          content,
          "text/cvs"
      );

      if (status !== true) {
        notifyInfo(this.$t("browserDenied"));
      }
    },

    fetchData(requestProps) {
      this.loading = ref(true);
      const orderBy = requestProps.sortBy;
      const filter = requestProps.filter;
      const factor = requestProps.factor;

      api
          .post(baseURL, {
            id: "1",
            method: "factor/loadFactorVal",
            params: [
              {
                factor: factor,
                orderBy: orderBy,
                filter: filter,
              },
            ],
          })
          .then((response) => {
            //console.log("fvs", response.data.result.records);
            this.rows = response.data.result.records;
            this.maxLen = this.rows.length;
            //
            if (this.selected.length > 0) {
              let curId = this.selected[0].id;
              this.selected = ref([]);
              let index = this.rows.findIndex((row) => row.id === curId);
              this.selected[0] = this.rows[index];
            }
          })
          .catch((error) => {
            console.log(error);
            notifyError(error.message);
          })
          .finally(() => {
            //setTimeout(() => {
            this.loading = ref(false);
            //}, 500)
          });
    },

    requestData(requestProps) {
      if (requestProps.sortBy === null) {
        requestParam.sortBy = null;
      } else {
        if (requestProps.descending)
          requestParam.sortBy = requestParam.sortBy + " desc";
        else
          requestParam.sortBy = requestParam.sortBy + " asc";
      }
      this.fetchData(requestParam);
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
                    (error) => {
                      let msg
                      if (error.response) msg = error.response.data.error.message;
                      else msg = error.message;
                      notifyError(msg);
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
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 25%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          sortable: true,
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

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },
  },

  data: function () {
    //console.log("<<<<<    data: lg", SetLocale.curLang)
    //const cols = this.getColumns()
    //console.log("colss", colss)
    return {
      cols: [],
      rows: [],
      FD_AccessLevel: {},
      filter: "",
      loading: false,
      selected: [],
      factor1_id: null,
      maxLen: 0,
      value: true,
    };
  },

  mounted() {
    this.factor1_id = parseInt(this.$route["params"].factor, 10);

    requestParam.factor = this.factor1_id;
    this.fetchData(requestParam);
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
          })
        })

    this.cols = this.getColumns();
  },

  setup() {}

});
</script>

<style></style>
