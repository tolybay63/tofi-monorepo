<template>
  <div class="q-pa-sm-sm">
    <q-table
      style="height: 100%; width: 100%"
      color="primary"
      card-class="bg-amber-1"
      table-class="text-grey-8"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      :separator="separator"
      :loading="loading"
      :dense="dense"
      :visible-columns="visibleColumns"
      :rows-per-page-options="[0]"
    >
      <template #header-cell="props">
        <q-th :props="props">
          {{ props.col.label }}
        </q-th>
      </template>

      <template #body-cell="props">
        <q-td :props="props">
          <q-bar class="bg-blue-grey-1" style="font-size: small; height: auto">
            <div
              v-if="props.col.field === 'numberVal'"
              style="font-size: small; height: auto"
            >
              {{ props.value }}
            </div>
            <div
              v-else-if="props.col.field === 'provider'"
              style="font-size: small; height: auto"
            >
              {{ fnProvider(props.value) }}
            </div>
            <div
              v-else-if="props.col.field === 'status'"
              style="font-size: small; height: auto"
            >
              {{ fnStatus(props.value) }}
            </div>
            <div v-else-if="props.col.field === 'cmd'">
              <q-btn
                round size="sm" icon="edit" color="blue" flat dense
                @click="fnEdit(props.row)" class="no-padding no-margin"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("update") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                :disable="props.row.id === undefined"
                round size="sm" icon="delete" color="red" flat dense
                @click="fnDelete(props)" class="no-padding no-margin"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("delete") }}
                </q-tooltip>
              </q-btn>
            </div>
            <div v-else style="font-size: small; height: auto">
              {{ props.value }}
            </div>
          </q-bar>
        </q-td>
      </template>

      <template v-slot:top>
        <div>
          <q-btn
            round size="medium" icon="post_add" style="color: #26a69a"
            flat dense @click="fnAdd()"
          >
            <q-tooltip transition-show="rotate" transition-hide="rotate">
              {{ $t("addRecord") }}
            </q-tooltip>
          </q-btn>
        </div>
        <div>{{ $t("prop") }}:</div>
        <div class="text-blue q-ml-sm" style="font-size: medium">
          {{ propName }}
        </div>
        <q-space />

        <div style="font-size: 11px; margin-top: 2px">
          {{ $t("statusFactor") }}:
        </div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">
          {{ status ? $t("yes") : $t("no") }}
        </div>
        <div style="font-size: 11px; margin-top: 2px">
          {{ $t("providerTyp") }}:
        </div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">
          {{ provider ? $t("yes") : $t("no") }}
        </div>
        <div style="font-size: 11px; margin-top: 2px">{{ $t("measure") }}:</div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">{{ measurename }}</div>
        <div style="font-size: 11px; margin-top: 2px">{{ $t("kfc") }}:</div>
        <div class="text-blue q-ml-sm-sm">{{ kfc }}</div>
      </template>
    </q-table>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError, notifyInfo} from "src/utils/jsutils";
import UpdaterMeterValue from "pages/std/meter/UpdaterMeterValue.vue";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  name: "MeterValue",
  props: [
    "propType",
    "status",
    "provider",
    "isUniq",
    "periodType",
    "propName",
    "measurename",
    "measure",
    "kfc",
    "digit",
  ],

  data: function () {
    return {
      cols: [],
      rows: [],
      rowsOrg: [],
      visibleColumns: ref(["provider", "status"]),
      loading: ref(false),
      separator: ref("cell"),
      dense: true,
      requestParams: {model: getModel.value},

      Status: null,
      Provider: null,
      FD_PeriodType: null,
      fltDt: null,
      fltPt: null,
    };
  },

  methods: {
    fnDelete(props) {
      let rec = props.row;
      //console.info(rec, this.requestParams)
      let method = this.requestParams.isMulti ? "deleteMeterValueMultiProp" : "deleteMeterValue"
      let params = [{ idDataProp: rec.dataProp, id: rec.id, model: getModel.value }]
      if (this.requestParams.isMulti)
        params = [{ dataMultiProp: rec.dataMultiProp, id: rec.id,
          multiPropDim: this.requestParams.multiPropDim,
          dimMultiPropItem: this.requestParams.dimMultiPropItem, model: getModel.value }]

      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteRecord"),
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {

          api
            .post(baseURL, {
              method: "data/"+ method,
              params: params,
            })
            .then(
              (response) => {
                this.loadData(this.requestParams);
              },
              (error) => {
                let msg = "";
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

    hideProvider: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status"];
    },

    hideStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider"];
    },

    hideProviderStatus: function () {
      this.visibleColumns = [];
    },

    showAllCols() {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status"];
    },

    fnProvider(val) {
      //console.info("fnProvider", val)
      if (val === null || val === undefined || val === 0) return null;
      return this.Provider ? this.Provider.get(val) : null;
    },

    fnStatus(val) {
      //console.info("fnStatus", val)
      if (val === null || val === undefined || val === 0) return null;
      return this.Status ? this.Status.get(val) : null;
    },

    fnAdd() {
      console.info("requestParams ins", this.requestParams)
      const lg = { name: this.lang };

      this.$q
        .dialog({
          component: UpdaterMeterValue,
          componentProps: {
            rec: {entityType: 1, measure: this.measure},
            requestParams: this.requestParams,
            lg: lg,
            dense: this.dense,
            mode: "ins",
            model: getModel.value

            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },

    fnEdit(rec) {
      //console.info("EDIT props rec", rec)
      //console.info("requestParams upd", this.requestParams)
      //this.requestParams.dataProp = rec.dataProp
      //this.requestParams.periodType = rec.periodType
      //this.requestParams.fltDt = this.fltDt
      //this.requestParams.fltPt = this.fltPt

      //console.info("requestParams fnEdit posle", this.requestParams)

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterMeterValue,
          componentProps: {
            rec: rec,
            requestParams: this.requestParams,
            lg: lg,
            mode: "upd",
            model: getModel.value
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },

    filterDate(dte) {
      this.fltDt = dte;
      if (dte === null) {
        this.rows = this.rowsOrg.filter((r) => {
          return true;
        });
      } else {
        this.rows = this.rowsOrg.filter((r) => {
          return r.dbeg <= dte && dte <= r.dend;
        });
      }
    },

    filterPeriod(typPeriod) {
      this.fltPt = typPeriod;
      if (typPeriod === 0) {
        this.rows = this.rowsOrg.filter((r) => {
          return true;
        });
      } else {
        this.rows = this.rowsOrg.filter((r) => {
          return r.periodType === typPeriod;
        });
      }
    },

    loadData(params) {
      /////
      params.model = getModel.value
      params.metamodel = getMetaModel.value
      for (let key in params) {
        if (params.hasOwnProperty(key)) {
          this.requestParams[key] = params[key];
        }
      }
      this.requestParams.provider = this.provider;
      this.requestParams.status = this.status;

      console.info("params MeterValue", params)
      console.info("requestParams MeterValue", this.requestParams)
      /////

      if (!this.provider && this.status) this.hideProvider();
      else if (this.provider && !this.status) this.hideStatus();
      else if (!this.provider && !this.status) this.hideProviderStatus();
      else this.showAllCols();

      if (this.periodType > 0) {
        this.loading = ref(true);
        api
          .post(baseURL, {
            method: "data/loadDict",
            params: ["FD_PeriodType"],
          })
          .then((response) => {
            this.FD_PeriodType = new Map();
            response.data.result.records.forEach((it) => {
              this.FD_PeriodType.set(it.id, it.text);
            });
          })
          .finally(() => {
            this.loading = ref(false);
          });
      }

      if (this.requestParams.provider > 0) {
        this.loading = ref(true);
        api
          .post(baseURL, {
            method: "data/loadProvider",
            params: [this.requestParams.prop, getModel.value, getMetaModel.value],
          })
          .then((response) => {
            this.Provider = new Map();
            response.data.result.records.forEach((it) => {
              this.Provider.set(it.id, it.name);
            });
            //console.info("PPPPPP", this.Provider)
          })
          .finally(() => {
            this.loading = ref(false);
          });
      }

      if (this.requestParams.status > 0) {
        this.loading = ref(true);
        api
          .post(baseURL, {
            method: "data/loadStatus",
            params: [this.requestParams.prop],
          })
          .then((response) => {
            this.Status = new Map();
            response.data.result.records.forEach((it) => {
              this.Status.set(it.id, it.name);
            });
            //console.info("Status", this.Status)
          })
          .finally(() => {
            this.loading = ref(false);
          });
      }

      //
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadData",
          params: [params],
        })
        .then(
          (response) => {
            this.rows = response.data.result.store.records;
            this.rowsOrg = response.data.result.store.records;
            console.info("DATA", this.rows)
          },
          (error) => {
            let msg = "";
            if (error.response) msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
        });
    },

    getColumns() {
      return [
        {
          name: "provider",
          label: this.$t("providerTypShort"),
          field: "provider",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "status",
          label: this.$t("statusFactorShort"),
          field: "status",
          sortable: true,
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "numberVal",
          required: true,
          label: this.$t("val"),
          field: "numberVal",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
          format: val => val ? val.toLocaleString(undefined, {minimumFractionDigits: 3, maximumFractionDigits: 3}): ""
        },
        {
          name: "period",
          required: true,
          label: this.$t("period"),
          field: "period",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 10%",
        },
        {
          name: "cmt",
          required: true,
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          sortable: false,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "cmd",
          required: true,
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 5%",
        },
      ];
    },
  },

  mounted() {
    //console.log("MOUNTED RefValue")
  },

  created() {
    //console.log("CREARE MeterValue", this.kfc, this.measurename, this.digit)
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
