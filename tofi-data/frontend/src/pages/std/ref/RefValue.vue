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
              v-if="props.col.field === 'propVal'"
              style="font-size: small; height: auto"
            >
              {{ fnPropVal(props.row) }}
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
                round
                size="sm"
                icon="edit"
                color="blue"
                flat
                dense
                @click="fnEdit(props.row)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("update") }}
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
        <div v-if="rowsOrg.length === 0">
          <q-btn
            round
            size="medium"
            icon="post_add"
            style="color: #26a69a"
            flat
            dense
            @click="fnAdd()"
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
        <div style="font-size: 11px; margin-top: 2px">{{ $t("isUniq") }}:</div>
        <div class="text-blue q-ml-sm-sm q-mr-sm">
          {{ isUniq ? $t("yes") : $t("no") }}
        </div>
      </template>
    </q-table>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";
import UpdaterRefValue from "pages/std/ref/UpdaterRefValue.vue";
import allConsts from "pages/all-consts";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  name: "RefValue",
  props: ["propType", "status", "provider", "isUniq", "periodType", "propName"],

  data: function () {
    return {
      cols: [],
      rows: [],
      rowsOrg: [],
      visibleColumns: ref(["provider", "status", "period"]),
      loading: ref(false),
      separator: ref("cell"),
      dense: true,
      requestParams: {},

      mapObjRelObj: new Map(),
      mapFacMea: new Map(),
      mapStatus: new Map(),
      mapProvider: new Map(),

      FD_PeriodType: null,
      fltDt: null,
      fltPt: null,
    };
  },

  methods: {
    hideProvider: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status", "period"];
    },

    hideStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "period"];
    },

    hideProviderStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["period"];
    },

    showAllCols() {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status", "period"];
    },

    fnProvider(val) {
      if (val === undefined || val === 0) return null;
      return this.Provider.get(val);
    },

    fnStatus(val) {
      if (val === undefined || val === 0) return null;
      return this.Status.get(val);
    },

    fnPropVal(row) {
      let val = 0
      if (this.propType===allConsts.FD_PropType.factor || this.propType === allConsts.FD_PropType.measure) {
        if (this.propType === allConsts.FD_PropType.factor) {
          val = row.factorVal ? row.factorVal : 0
          console.info("val fv", val)
        } else if (this.propType === allConsts.FD_PropType.measure) {
          val = row.measure ? row.measure : 0
          console.info("val mea", val)
        }
        console.info("this.mapFacMea", val, this.mapFacMea)

        console.info("this.mapFacMea.get(val)", val, this.mapFacMea.get(val))

        if (this.mapFacMea.get(val)===undefined) return ""

        if (allConsts.FD_VisualFormat.cod === this.requestParams.visualFormat)
          return this.mapFacMea.get(val).cod;
        else if (allConsts.FD_VisualFormat.short === this.requestParams.visualFormat)
          return this.mapFacMea.get(val).name ? this.mapFacMea.get(val).name : "";
        else if (allConsts.FD_VisualFormat.full === this.requestParams.visualFormat)
          return this.mapFacMea.get(val).fullName;
        else if (allConsts.FD_VisualFormat.cod_short === this.requestParams.visualFormat)
          return this.mapFacMea.get(val).cod + " - " + this.mapFacMea.get(val).name;
        else if (allConsts.FD_VisualFormat.cod_full === this.requestParams.visualFormat)
          return this.mapFacMea.get(val).cod + " - " + this.mapFacMea.get(val).fullName;
        return this.mapFacMea.get(val).name;
      }

      if (this.propType===allConsts.FD_PropType.typ || this.propType===allConsts.FD_PropType.reltyp) {
        if (this.propType === allConsts.FD_PropType.typ)
          val = row.obj ? row.obj : 0
        else if (this.propType === allConsts.FD_PropType.reltyp)
          val = row.relobj ? row.relobj : 0

        if (this.mapObjRelObj.get(val)===undefined) return ""

        if (allConsts.FD_VisualFormat.cod === this.requestParams.visualFormat)
          return this.mapObjRelObj.get(val).cod;
        else if (allConsts.FD_VisualFormat.short === this.requestParams.visualFormat)
          return this.mapObjRelObj.get(val).name;
        else if (allConsts.FD_VisualFormat.full === this.requestParams.visualFormat)
          return this.mapObjRelObj.get(val).fullName;
        else if (allConsts.FD_VisualFormat.cod_short === this.requestParams.visualFormat)
          return this.mapObjRelObj.get(val).cod + " - " + this.mapObjRelObj.get(val).name;
        else if (allConsts.FD_VisualFormat.cod_full === this.requestParams.visualFormat)
          return this.mapObjRelObj.get(val).cod + " - " + this.mapObjRelObj.get(val).fullName;
        return this.mapObjRelObj.get(val).name;
      }
    },

    fnAdd() {
      console.info("requestParams", this.requestParams)
      const lg = { name: this.lang };

      this.$q
        .dialog({
          component: UpdaterRefValue,
          componentProps: {
            rec: {},
            requestParams: this.requestParams,
            lg: lg,
            dense: this.dense,
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

    fnEdit(rec) {
      //console.info("EDIT props rec", rec)
      this.requestParams.dataProp = rec.dataProp;
      this.requestParams.periodType = rec.periodType;
      this.requestParams.fltDt = this.fltDt;
      this.requestParams.fltPt = this.fltPt;

      //console.info("requestParams fnEdit", this.requestParams)

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterRefValue,
          componentProps: {
            rec: rec,
            requestParams: this.requestParams,
            lg: lg,
            dense: this.dense,
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
      if (this.requestParams.isObj==="true")
        this.requestParams.isObj = 1
      else
        this.requestParams.isObj = 0
      this.requestParams.provider = this.provider;
      this.requestParams.status = this.status;

      console.info("!!!requestParams RefValue!!!", this.requestParams)
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
      this.loading = ref(true);
      console.info("this.requestParams", this.requestParams)
      //
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadData",
          params: [params],
        })
        .then(
          (response) => {
            if ("storePV_TR" in response.data.result) {
              response.data.result.storePV_TR.records.forEach((it) => {
                this.mapObjRelObj.set(it.id, it);
              })
              this.mapObjRelObj.set(0, { id: 0, name: "", cod: "", fullName: "" });
              console.info("mapObjRelObj", this.mapObjRelObj);
            }

            if ("storePV_FacORMea" in response.data.result) {
              response.data.result.storePV_FacORMea.records.forEach((it) => {
                this.mapFacMea.set(it.id, it);
              })
              this.mapFacMea.set(0, { id: 0, name: "", cod: "", fullName: "" });
              console.info("mapFacMea", this.mapFacMea);
            }

            if ("store_Status" in response.data.result) {
              response.data.result.store_Status.records.forEach((it) => {
                this.mapStatus.set(it.id, it);
              })
            }
            if ("store_Provider" in response.data.result) {
              response.data.result.store_Provider.records.forEach((it) => {
                this.mapProvider.set(it.id, it);
              })
            }

            this.rows = response.data.result.store.records
            this.rowsOrg = response.data.result.store.records
            console.info("this.Data Ref", response.data.result)
          })
        .then((response)=> {
        })
        .catch(error => {
            let msg = "";
            if (error.response) msg = error.response.data.error.message;
            else msg = error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
        });



/*
      api
        .post(baseURL, {
          method: "data/loadPropVal", // load PropVal, Status, Provider
          params: [this.requestParams.prop, this.requestParams.propType, getModel.value, getMetaModel.value],
        })
        .then(
          (response) => {
            this.PropVal = new Map();
            response.data.result.store.records.forEach((it) => {
              this.PropVal.set(it.id, it);
            })
            console.info("this.PropVal", this.PropVal)

            this.PropVal.set(0, { id: 0, name: "" });
            //
            if ("status" in response.data.result) {
              this.Status = new Map();
              response.data.result.status.records.forEach((it) => {
                this.Status.set(it.id, it.name);
              })

              console.info("this.Status", this.Status)
            }
            if ("provider" in response.data.result) {
              this.Provider = new Map();
              response.data.result.provider.records.forEach((it) => {
                this.Provider.set(it.id, it.name);
              })

              console.info("this.Provider", this.Provider)
            }
            console.info("this.Provider", this.Provider)
            //
            this.loading = ref(true);
            api
              .post(baseURL, {
                method: "data/loadData",
                params: [params],
              })
              .then(
                (response) => {
                  this.rows = response.data.result.store.records
                  this.rowsOrg = response.data.result.store.records

                  console.info("this.rows Ref", this.rows)
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
            ///////////
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
*/
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
          headerStyle: "font-size: 1.3em; width: 22%",
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
          name: "propVal",
          required: true,
          label: this.$t("val"),
          field: "propVal",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 25%",
        },
        {
          name: "period",
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
          headerStyle: "width: 3%",
        },
      ];
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
  },

  setup() {
    return {};
  },
};
</script>

<style scoped></style>
