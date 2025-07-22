<template>
  <div class="q-pa-sm-sm">
    <q-splitter
      v-model="splitterModel"
      class="no-padding no-margin"
      horizontal
      style="height: calc(100vh - 250px); width: 100%"
      separator-class="bg-red"
      :model-value="splitterModel"
    >
      <template v-slot:before>
        <q-table
          style="height: 100%; width: 100%"
          color="primary"
          card-class="bg-amber-1"
          table-class="text-grey-8"
          row-key="id"
          :columns="colsQ"
          :rows="rowsQ"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          :separator="separator"
          :loading="loading" dense
          :visible-columns="visibleColumns"
          :rows-per-page-options="[0]"
          selection="single"
          v-model:selected="selectedQ"
          @update:selected="updSelectionQ"
        >
          <template #header-cell="props">
            <q-th :props="props">
              {{ props.col.label }}
            </q-th>
          </template>

          <template #body-cell="props">
            <q-td :props="props">
              <q-bar
                class="bg-blue-grey-1"
                style="font-size: small; height: auto"
              >
                <div
                  v-if="props.col.field === 'provider'"
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
                  v-else-if="props.col.field === 'strVal'"
                  style="font-size: small; height: auto"
                >
                  {{ props.value }}
                </div>

                <div
                  v-else-if="props.col.field === 'cmd'"
                  class="no-padding no-margin row"
                >

                      <q-btn
                        round size="sm" icon="edit" color="blue" flat dense
                        @click="fnEdit(props.row)"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("update") }}
                        </q-tooltip>
                      </q-btn>

                      <q-btn
                        :disable="props.row.id === undefined"
                        round size="sm" icon="delete" color="red" flat dense
                        @click="fnDelete(props)"
                      >
                        <q-tooltip
                          transition-show="rotate"
                          transition-hide="rotate"
                        >
                          {{ $t("deleteRecord") }}
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
            <div>{{ $t("complexProp") }}:</div>
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
            <div style="font-size: 11px; margin-top: 2px">
              {{ $t("isUniq") }}:
            </div>
            <div class="text-blue q-ml-sm-sm q-mr-sm">
              {{ requestParams.isUniq ? $t("yes") : $t("no") }}
            </div>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <items-value
          :status="status"
          :provider="provider"
          :propComplex="requestParams.id"
          ref="itemsValue"
        />
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError, notifyInfo} from "src/utils/jsutils";
import allConsts from "pages/all-consts";
import UpdaterComplexValue from "pages/std/complex/UpdaterComplexValue.vue";
import ItemsValue from "pages/std/complex/ItemsValue.vue";
import {useParamsStore} from "stores/params-store";
import {extend} from "quasar";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  name: "ComplexValue",
  components: { ItemsValue },

  props: ["status", "provider", "propName"],

  data: function () {
    return {
      splitterModel: ref(40),

      colsQ: [],
      rowsQ: [],
      rowsOrg: [],
      visibleColumns: ref(["provider", "status"]),
      loading: ref(false),
      separator: ref("cell"),
      selectedQ: ref([]),
      requestParams: {},

      Status: null,
      Provider: null,

      fltDt: null,
      fltPt: null,
      FD_AttribValType: null,
      //PropVal: null,
    };
  },

  methods: {
    updSelectionQ(par) {
      //console.info("updSelectionQ", par[0])
      //console.info("selectedQ", this.selectedQ)

      if (par.length > 0) {
        this.requestParams.idDataComplex = par[0].id;
        this.requestParams.isComplexItem = 1;
      } else {
        this.requestParams.idDataComplex = 0;
        this.requestParams.isComplexItem = 0;
      }

      this.$refs.itemsValue.loadData(this.requestParams);
    },

    fnProvider(val) {
      //console.info("fnProvider", val)
      if (this.Provider === null || this.Provider === undefined) return null;
      if (val === null || val === undefined || val === 0) return null;
      return this.Provider.get(val);
    },

    fnStatus(val) {
      //console.info("fnStatus", val)
      if (this.Status === null || this.Status === undefined) return null;
      if (val === null || val === undefined || val === 0) return null;
      return this.Status.get(val);
    },

    getValType() {
      if (this.FD_AttribValType === null) return null;
      return this.FD_AttribValType.get(this.requestParams.attribValType);
    },

    fnDelete(props) {
      let rec = props.row;
      //console.info(rec, this.requestParams)
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message: this.$t("deleteRecord") + `</br><span>Значение: ${rec.strVal}</span>` +
            "</br><span class='text-red'>(Удаляются вложенные элементы)</span>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          api
            .post(baseURL, {
              method: "data/deleteComplexValue",
              params: [rec.id, getModel.value ],
            })
            .then(
              () => {
                this.loadData(this.requestParams);
                this.$refs.itemsValue.loadData(this.requestParams);
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
      this.colsQ[2].headerStyle = "font-size: 1.3em; width=40%";
    },

    hideStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider"];
      this.colsQ[2].headerStyle = "font-size: 1.3em; width=40%";
    },

    hideProviderStatus: function () {
      this.visibleColumns = [];
      this.colsQ[2].headerStyle = "font-size: 1.3em; width=60%";
    },

    showAll: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status"];
    },

    fnAdd() {
      //console.info("requestParams Add", this.requestParams)
      const lg = { name: this.lang };

      this.$q
        .dialog({
          component: UpdaterComplexValue,
          componentProps: {
            rec: {},
            requestParams: this.requestParams,
            lg: lg,
            mode: "ins",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadData(this.requestParams);
          }
        })
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
          component: UpdaterComplexValue,
          componentProps: {
            rec: rec,
            requestParams: this.requestParams,
            lg: lg,
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            try {
              this.loadData(this.requestParams);
            } finally {
              this.$refs.itemsValue.loadData(this.requestParams);
            }
          }
        });
    },

    filterDate(dte) {
      this.fltDt = dte;
      if (dte === null) {
        this.rowsQ = this.rowsOrg.filter((r) => {
          return true;
        });
      } else {
        this.rowsQ = this.rowsOrg.filter((r) => {
          return r.dbeg <= dte && dte <= r.dend;
        });
      }
    },

    filterPeriod(typPeriod) {
      this.fltPt = typPeriod;
      if (typPeriod === 0) {
        this.rowsQ = this.rowsOrg.filter((r) => {
          return true;
        });
      } else {
        this.rowsQ = this.rowsOrg.filter((r) => {
          return r.periodType === typPeriod;
        });
      }
    },

    getColumnsQ() {
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
          name: "strVal",
          required: true,
          label: this.$t("val"),
          field: "strVal",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 15%",
        },
        {
          name: "period",
          required: true,
          label: this.$t("period"),
          field: "period",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "cmt",
          required: true,
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          sortable: false,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "cmd",
          required: true,
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 8%",
        },
      ];
    },

    loadData(params) {
      if (!params.dependPeriod) params.periodType = 0;
      params.propType = allConsts.FD_PropType.complex;
      params.model = getModel.value
      params.metamodel = getMetaModel.value
      console.info("params", params);

      /////
      for (let key in params) {
        if (params.hasOwnProperty(key)) {
          this.requestParams[key] = params[key];
        }
      }

      this.requestParams.provider = this.provider;
      this.requestParams.status = this.status;

      if (!this.provider && this.status) {
        this.hideProvider();
      } else if (this.provider && !this.status) {
        this.hideStatus();
      } else if (!this.provider && !this.status) {
        this.hideProviderStatus();
      } else {
        this.showAll();
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
          method: "data/loadDict",
          params: ["FD_AttribValType"],
        })
        .then((response) => {
          this.FD_AttribValType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_AttribValType.set(it.id, it.text);
          });
        })
        .finally(() => {
          this.loading = ref(false);
        });

      //
      this.loading = ref(true);
      params.isComplexItem = 0;
      api
        .post(baseURL, {
          method: "data/loadData",
          params: [params],
        })
        .then(
          (response) => {
            this.rowsQ = response.data.result.store.records;
            this.rowsOrg = response.data.result.store.records;
            //console.info("this.rowsQ", this.rowsQ);
            let ppp = {}
            extend(true, ppp, this.requestParams)
            ppp.isComplexItem = 1
            ppp.idDataComplex = 0
            this.$refs.itemsValue.loadData(ppp)
          })
        .catch(error=> {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        })
        .finally(() => {
          this.loading = ref(false);
        });
      //
    },
  },

  mounted() {
    //console.log("MOUNTED RefValue")
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.colsQ = this.getColumnsQ();
  },

  setup() {
    return {};
  },
};
</script>

<style scoped></style>
