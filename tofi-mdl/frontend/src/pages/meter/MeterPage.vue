<template>
  <q-page class="q-pa-md" style="height: 100px">
    <q-table
      :columns="cols"
      :dense="dense"
      :filter="filter"
      :loading="loading"
      :max="pagesNumber"
      :rows="rows"
      :rows-per-page-options="[25, 0]"
      :table-colspan="4"
      :wrap-cells="true"
      card-class="bg-amber-1"
      class="sticky-header-table"
      color="primary"
      row-key="id"
      selection="single"
      separator="cell"
      style="height: 100%; width: 100%"
      table-header-class="text-bold text-white bg-blue-grey-13"
      @request="requestData"
      v-model:selected="selected"
      v-model:pagination="pagination"
    >
      <template #bottom-row>
        <q-td v-if="selected.length > 0" colspan="100%">
          <span class="text-blue"> {{ $t("selectedRow") }}: </span>
          <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td v-else-if="rows.length > 0" class="text-bold" colspan="100%">
          {{ $t("infoRow") }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" icon="scale" text-color="white"></q-avatar>
          {{ $t("meters") }}
        </div>

        <q-space/>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:ins')"
          :dense="dense"
          :disable="loading"
          color="secondary"
          icon="post_add"
          @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("newRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:upd')"
          :dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-sm"
          color="secondary"
          icon="edit"
          @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:del')"
          :dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-sm"
          color="red"
          icon="delete"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel')"
          :dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-lg"
          color="secondary"
          icon="pan_tool_alt"
          @click="meterChoise"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("chooseRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          :dense="dense"
          class="q-ml-lg"
          color="secondary"
          icon-right="archive"
          no-caps
          @click="exportTable"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("msgToFile") }}
          </q-tooltip>
        </q-btn>

        <q-toggle
          v-model="dense"
          :dense="dense"
          :label="$t('isDense')"
          :model-value="dense"
          style="margin-left: 10px"
        />

        <q-space/>
        <q-input
          v-model="filter"
          :dense="dense"
          :label="$t('txt_filter')"
          :model-value="filter"
          color="primary"
          debounce="300"
        >
          <template v-slot:append>
            <q-icon name="search"/>
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading color="secondary" showing></q-inner-loading>
      </template>
    </q-table>
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api} from "boot/axios";
import {hasTarget, notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import UpdateMeter from "pages/meter/UpdateMeter.vue";
import {exportFile, extend} from "quasar";

const requestParam = {
  page: 1,
  rowsPerPage: 25,
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

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: "",
      loading: false,
      FD_AccessLevel: null,
      FD_MeterStruct: null,

      pagination: {
        sortBy: null,
        descending: false,
        page: 1,
        rowsPerPage: 25,
        rowsNumber: 0,
        filter: "",
      },
      selected: [],
      meter: 0,
      dense: true,
    };
  },


  methods: {
    hasTarget,

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },
    meterChoise() {
      this.$router["push"]({
        name: "meterSelected",
        params: {
          meter: this.selected[0].id,
          meterStruct: this.selected[0].meterStruct,
        },
      });
    },

    editRow(rec, mode) {
      let data = {};
      if (mode === "ins") {
        api
          .post("", {
            method: "meter/newRec",
            params: [{}],
          })
          .then((response) => {
            //console.log("new rec", response.data.result.records)
            data = response.data.result.records[0];
            this.$q
              .dialog({
                component: UpdateMeter,
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
        //data = rec
        for (let key in rec) {
          if (rec.hasOwnProperty(key)) {
            data[key] = rec[key];
          }
        }
        this.$q
          .dialog({
            component: UpdateMeter,
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

    fetchData(requestParam) {
      this.loading = ref(true);
      api
        .post("", {
          id: "1",
          method: "meter/loadMeterPaginate",
          params: [requestParam],
        })
        .then(
          (response) => {
            this.rows = response.data.result.store.records;
            const meta = response.data.result.meta;
            this.pagination.page = meta.page;
            this.pagination.rowsPerPage = meta.limit;
            this.pagination.rowsNumber = meta.total;

            this.selected = [];
            if (this.meter > 0) {
              let index = this.rows.findIndex((row) => row.id === this.meter);
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
          this.loading = ref(false);
          //}, 500)
        });
    },

    requestData(requestProps) {
      extend(true, requestParam, requestProps.pagination)
      requestParam.filter = requestProps.filter;
      extend(true, this.pagination, requestProps.pagination)
      //
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
            .post("", {
              method: "meter/delete",
              params: [{rec: rec}],
            })
            .then(
              () => {
                //console.log("response=>>>", response.data)
                this.rows.splice(index, 1);
                this.selected = [];
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
          name: "meterStruct",
          label: this.$t("meterStruct"),
          field: "meterStruct",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
          format: (val) =>
            this.FD_MeterStruct ? this.FD_MeterStruct.get(val) : null,
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
          style: "width: 40%",
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
        this.$t("meters") + ".txt",
        content,
        "text/cvs"
      );

      if (status !== true) {
        notifyInfo(this.$t("browserDenied"));
      }
    },
  },

  created() {
    console.info("Create")

    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();

    api
      .post("", {
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
      .post("", {
        method: "dict/load",
        params: [{dict: "FD_MeterStruct"}],
      })
      .then((response) => {
        this.FD_MeterStruct = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_MeterStruct.set(it["id"], it["text"]);
        });
      });

    this.fetchData(requestParam);
  },

  mounted() {
    this.meter = parseInt(this.$route["params"].meter, 10);
  },


  setup() {},
});
</script>

<style lang="sass">
.sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 140px)
  /* bg color is important for th; just specify one */
  background-color: #bdbdbd

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  /* this is when the loading indicator appears */


  &.q-table--loading thead tr:last-child th
    /* height of all previous header rows */
    top: 48px

  /* prevent scrolling behind sticky top row on focus */


  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px
</style>

