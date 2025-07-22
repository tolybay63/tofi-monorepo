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
      :loading="loading"
      dense separator="cell"
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
              v-if="props.col.field === 'val'"
              style="font-size: small; height: auto"
            >
              {{ fnVal(props) }}
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

            <div
              v-else-if="props.col.field === 'cmd'"
              class="no-padding no-margin"
            >


                  <q-btn
                    round size="sm" icon="edit" color="blue" flat dense
                    @click="fnEdit(props.row)" class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("update") }}
                    </q-tooltip>
                  </q-btn>

<!--                  <q-btn
                    :disable="props.row.id === undefined"
                    round size="sm" icon="delete" color="red" flat dense
                    @click="fnDelete(props)" class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate" transition-hide="rotate"
                    >
                      {{ $t("deleteRecord") }}
                    </q-tooltip>
                  </q-btn>-->


            </div>

            <div v-else style="font-size: small; height: auto">
              {{ props.value }}
            </div>
          </q-bar>
        </q-td>
      </template>

      <template v-slot:top>
        <div class="q-mr-sm">{{ $t("itemsComplexProp") }}</div>
      </template>
    </q-table>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {date} from "quasar";
import {notifyError, notifyInfo} from "src/utils/jsutils";
import allConsts from "pages/all-consts";
import UpdaterMeterValue from "pages/std/meter/UpdaterMeterValue.vue";
import SelectPropItems from "pages/std/complex/SelectPropItems.vue";
import UpdaterAttribValue from "pages/std/attr/UpdaterAttribValue.vue";
import UpdaterRefValue from "pages/std/ref/UpdaterRefValue.vue";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);


export default {
  name: "RefValue",
  props: ["propComplex", "status", "provider"],

  data: function () {
    return {
      cols: [],
      rows: [],
      rowsOrg: [],
      visibleColumns: ref(["provider", "status"]),
      loading: ref(false),
      requestParams: {},
      //addDisabled: true,
      Status: null,
      Provider: null,
      FD_PeriodType: null,
      PropValEnt: null,
      PropValRef: null,
      fltDt: null,
      fltPt: null,
    };
  },

  methods: {
    fnVal(props) {
      //console.info("Mea", props);
      if (
        props.row.propType === allConsts.FD_PropType.meter ||
        props.row.propType === allConsts.FD_PropType.rate
      )
        return props.value;
      else if (props.row.propType === allConsts.FD_PropType.attr) {
        if (props.row.attribValType === allConsts.FD_AttribValType.entity) {

          console.info("this.PropValEnt", this.PropValEnt)
          console.info("this.PropValRef", this.PropValRef)
          console.info("props.value", props.value)

          //return props.value

          if (props.value==0 || this.PropValEnt===undefined || Object.keys(this.PropValEnt).length===0)
            return ""
          else {
            let r = this.PropValEnt[parseInt(props.value)];
            return r.cod + " - " + r.name;
          }

        } else if (props.row.attribValType === allConsts.FD_AttribValType.dt) {
          return date.formatDate(props.value, "DD.MM.YYYY");
        } else if (
          props.row.attribValType === allConsts.FD_AttribValType.dttm
        ) {
          return date.formatDate(props.value, "DD.MM.YYYY hh:mm");
        } else if (props.row.attribValType === allConsts.FD_AttribValType.tm) {
          return date.formatDate(props.value, "hh:mm");
        } else {
          return props.value; //?????
        }
      } else if (props.row.propType === allConsts.FD_PropType.factor) {
        //console.info("props.row", props.row)
        let r = this.PropValRef["f_" + props.value];
        if (r === undefined) return "";
        else return r.cod + " - " + r.name;
      } else if (props.row.propType === allConsts.FD_PropType.typ) {
        let r = this.PropValRef["o_" + props.row.obj];
        if (r === undefined) return "";
        else return r.cod + " - " + r.name;
      } else if (props.row.propType === allConsts.FD_PropType.reltyp) {
        let r = this.PropValRef["r_" + props.row.relobj];
        if (r === undefined) return "";
        else return r.cod + " - " + r.name;
      } else if (props.row.propType === allConsts.FD_PropType.measure) {
        let r = this.PropValRef["m_" + props.value];
        if (r === undefined) return "";
        else return r.cod + " - " + r.name;
      }
    },

    fnSelect() {
      //console.info("requestParams", this.requestParams)
      const lg = { name: this.lang };

      this.$q
        .dialog({
          component: SelectPropItems,
          componentProps: {
            propComplex: this.requestParams.prop,
            lg: lg,
            // ...
          },
        })
        .onOk((r) => {
          console.log("Item Props Of Prop", r);
          if (
            r.propType === allConsts.FD_PropType.meter ||
            r.propType === allConsts.FD_PropType.rate
          ) {
            this.fnAddMeter(r);
          } else if (r.propType === allConsts.FD_PropType.attr) {
            this.fnAddAttrib(r);
          } else if (
            r.propType === allConsts.FD_PropType.factor ||
            r.propType === allConsts.FD_PropType.typ ||
            r.propType === allConsts.FD_PropType.reltyp ||
            r.propType === allConsts.FD_PropType.measure
          ) {
            this.fnAddRef(r);
          }
        });
    },

    fnEdit(row) {
      //row.prop
      api
        .post(baseURL, {
          method: "data/propsOfProp",
          params: [row.prop],
        })
        .then(
          (response) => {
            let reqParam = response.data.result.props.records[0];
            reqParam.prop = row.prop;
            //
            if (
              row.propType === allConsts.FD_PropType.meter ||
              row.propType === allConsts.FD_PropType.rate
            ) {
              this.fnEditMeter(row, reqParam);
            } else if (row.propType === allConsts.FD_PropType.attr) {
              this.fnEditAttrib(row, reqParam);
            } else if (
              row.propType === allConsts.FD_PropType.factor ||
              row.propType === allConsts.FD_PropType.typ ||
              row.propType === allConsts.FD_PropType.reltyp ||
              row.propType === allConsts.FD_PropType.measure
            ) {
              this.fnEditRef(row, reqParam);
            }
          },
          (error) => {
            let msg = error.message;
            if (error.response.data.error.message)
              msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
        });
    },

    fnAddRef(r) {
      r.parent = this.requestParams.idDataComplex;
      r.providerTyp = this.requestParams.providerTyp;
      r.statusFactor = this.requestParams.statusFactor;
      r.isObj = this.requestParams.isObj;
      r.owner = this.requestParams.owner;
      r.model = getModel.value;

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterRefValue,
          componentProps: {
            rec: { parent: this.requestParams.idDataComplex },
            requestParams: r,
            lg: lg,

            mode: "ins",
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },

    fnEditRef(row, reqParam) {
      reqParam.parent = this.requestParams.idDataComplex;
      reqParam.providerTyp = this.requestParams.providerTyp;
      reqParam.statusFactor = this.requestParams.statusFactor;
      reqParam.isObj = this.requestParams.isObj;
      reqParam.owner = this.requestParams.owner;
      reqParam.model = getModel.value;
      row.parent = this.requestParams.idDataComplex;
      row.isComplex = true;

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterRefValue,
          componentProps: {
            rec: row,
            requestParams: reqParam, //this.requestParams,
            lg: lg,
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },

    //////
    fnEditAttrib(row, reqParam) {
      console.info("fnEdit props row", row);
      console.info("reqParam fnEdit", reqParam);

      if (reqParam.attribValType === allConsts.FD_AttribValType.entity) {
        row.propVal = parseInt(row.val);
      } else if (
        reqParam.attribValType === allConsts.FD_AttribValType.str ||
        reqParam.attribValType === allConsts.FD_AttribValType.mask ||
        reqParam.attribValType === allConsts.FD_AttribValType.num ||
        reqParam.attribValType === allConsts.FD_AttribValType.integ ||
        reqParam.attribValType === allConsts.FD_AttribValType.multistr
      ) {
        row.strVal = row.val;
      } else if (
        reqParam.attribValType === allConsts.FD_AttribValType.str ||
        reqParam.attribValType === allConsts.FD_AttribValType.dttm ||
        reqParam.attribValType === allConsts.FD_AttribValType.dt ||
        reqParam.attribValType === allConsts.FD_AttribValType.tm
      ) {
        row.dateTimeVal = row.val;
      }
      reqParam.model = getModel.value;
      row.parent = this.requestParams.idDataComplex;
      row.isComplex = true;

      console.info("row2", row);

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterAttribValue,
          componentProps: {
            rec: row,
            requestParams: reqParam,
            lg: lg,
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },

    fnAddAttrib(r) {
      const lg = { name: this.lang };
      r.parent = this.requestParams.idDataComplex;
      r.providerTyp = this.requestParams.providerTyp;
      r.statusFactor = this.requestParams.statusFactor;
      r.isObj = this.requestParams.isObj;
      r.owner = this.requestParams.owner;
      r.model = getModel.value;

      this.$q
        .dialog({
          component: UpdaterAttribValue,
          componentProps: {
            rec: {},
            requestParams: r,
            lg: lg,
            mode: "ins",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },
    //////
    fnAddMeter(r) {
      const lg = { name: this.lang };
      r.parent = this.requestParams.idDataComplex;
      r.providerTyp = this.requestParams.providerTyp;
      r.statusFactor = this.requestParams.statusFactor;
      r.isObj = this.requestParams.isObj;
      r.owner = this.requestParams.owner;
      r.model = getModel.value;

      this.$q
        .dialog({
          component: UpdaterMeterValue,
          componentProps: {
            rec: {},
            requestParams: r,
            lg: lg,
            mode: "ins",
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },

    fnEditMeter(row, reqParam) {
      row.numberVal = parseFloat(row.val);
      row.parent = this.requestParams.idDataComplex;
      row.isComplex = true;
      reqParam.providerTyp = this.requestParams.providerTyp;
      reqParam.statusFactor = this.requestParams.statusFactor;
      reqParam.model = getModel.value;

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterMeterValue,
          componentProps: {
            rec: row,
            requestParams: reqParam, //this.requestParams,
            lg: lg,
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        });
    },
    //////
/*
    fnDelete(props) {
      let rec = props.row;
      //console.info(rec, this.requestParams)
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecordValue") +
            '<div style="color: plum">(' +
            rec.propName +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post(baseURL, {
              method: "data/deleteItemValue",
              params: [{ /!*idDataProp: rec.dataProp, *!/ id: rec.id }],
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
*/

    hideProvider: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "period"];
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
      return this.Provider.get(val);
    },

    fnStatus(val) {
      //console.info("fnStatus", val)
      if (val === null || val === undefined || val === 0) return null;
      return this.Status.get(val);
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
      for (let key in params) {
        if (params.hasOwnProperty(key)) {
          this.requestParams[key] = params[key];
        }
      }
      //
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

      if (this.requestParams.providerTyp > 0) {
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
          })
          .finally(() => {
            this.loading = ref(false);
          });
      }

      if (this.requestParams.statusFactor > 0) {
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
          })
          .finally(() => {
            this.loading = ref(false);
          });
      }

      //
      this.loading = ref(true);
      params.isComplexItem = 1;
      api
        .post(baseURL, {
          method: "data/loadData",
          params: [params],
        })
        .then(
          (response) => {
            this.PropValEnt = response.data.result.mapPVent;
            this.PropValRef = response.data.result.mapPVref;
            this.rows = response.data.result.store.records;
            this.rowsOrg = response.data.result.store.records;

            //console.info("params", params);
            console.info("rows", this.rows);
            console.info("PropValEnt", this.PropValEnt);
            console.info("PropValRef", this.PropValRef);

          })
        .catch(error => {
          let msg = "";
          if (error.response) msg = error.response.data.error.message;
          else msg = error.message;
          notifyError(msg);
        })
        .finally(() => {
          this.loading = ref(false);
        });
    },

    getColumns() {
      return [
        {
          name: "propName",
          required: true,
          label: this.$t("prop"),
          field: "propName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "provider",
          label: this.$t("providerTypShort"),
          field: "provider",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "status",
          label: this.$t("statusFactorShort"),
          field: "status",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "val",
          required: true,
          label: this.$t("val"),
          field: "val",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 15%",
        },
        {
          name: "period",
          required: true,
          label: this.$t("period"),
          field: "period",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 10%",
        },
        {
          name: "cmt",
          required: true,
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "cmd",
          required: true,
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 3%",
        },
      ];
    },

    reload() {
      this.lang = localStorage.getItem("curLang");
      this.lang = this.lang === "en-US" ? "en" : this.lang;
      this.cols = this.getColumns();

      if (!this.provider && this.status) this.hideProvider();
      else if (this.provider && !this.status) this.hideStatus();
      else if (!this.provider && !this.status) this.hideProviderStatus();
      else this.showAllCols();
    },
  },

  mounted() {
    //console.log("MOUNTED RefValue")
  },

  created() {
    //console.log("CREARE RefValue")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.cols = this.getColumns();

    if (!this.provider && this.status) this.hideProvider();
    else if (this.provider && !this.status) this.hideStatus();
    else if (!this.provider && !this.status) this.hideProviderStatus();
    else this.showAllCols();
  },

  setup() {
    return {};
  },
};
</script>

<style scoped></style>
