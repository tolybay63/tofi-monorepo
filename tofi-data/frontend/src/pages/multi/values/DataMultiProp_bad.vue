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
          :loading="loading"
          :dense="dense"
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
                  v-if="props.col.field === 'propMatrixCond'"
                  style="font-size: small; height: auto"
                >
                  {{ fnCond(props.value) }}
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
                  v-else-if="props.col.field === 'periodType'"
                  style="font-size: small; height: auto"
                >
                  {{ fnPeriodType(props.value) }}
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
                    class="no-padding no-margin"
                    :disable="fnDisableAdd()"
                  >
                    <q-tooltip
                      transition-show="rotate"
                      transition-hide="rotate"
                    >
                      {{ $t("update") }}
                    </q-tooltip>
                  </q-btn>

                  <q-btn
                    :disable="props.row.id === undefined"
                    round
                    size="sm"
                    icon="delete"
                    color="red"
                    flat
                    dense
                    @click="fnDelete(props)"
                    class="no-padding no-margin"
                  >
                    <q-tooltip
                      transition-show="rotate"
                      transition-hide="rotate"
                    >
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
                round
                size="medium"
                icon="post_add"
                style="color: #26a69a"
                flat
                dense
                @click="fnAdd()"
                :disable="fnDisableAdd()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("addRecord") }}
                </q-tooltip>
              </q-btn>
            </div>
            <div>{{ $t("propMatrix") }}:</div>
            <div class="text-blue q-ml-sm" style="font-size: medium">
              {{ reqParams.propName }}
            </div>
            <q-space />

            <div style="font-size: 11px; margin-top: 2px">
              {{ $t("statusFactor") }}:
            </div>
            <div class="text-blue q-ml-sm-sm q-mr-sm">
              {{ reqParams.statusFactor > 0 ? $t("yes") : $t("no") }}
            </div>
            <div style="font-size: 11px; margin-top: 2px">
              {{ $t("providerTyp") }}:
            </div>
            <div class="text-blue q-ml-sm-sm q-mr-sm">
              {{ reqParams.providerTyp > 0 ? $t("yes") : $t("no") }}
            </div>
            <div style="font-size: 11px; margin-top: 2px">
              {{ $t("isUniq") }}:
            </div>
            <div class="text-blue q-ml-sm-sm q-mr-sm">
              {{ reqParams.isUniq ? $t("yes") : $t("no") }}
            </div>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <data-prop-matrix-val
          :lstEntityType="lstEntityType"
          ref="DataPropMatrixVal"
        />
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError, notifyInfo} from "src/utils/jsutils";
//import UpdaterDataPropMatrix from "pages/matrix/values/UpdaterDataPropMatrix";
//import DataPropMatrixVal from "pages/matrix/values/DataPropMatrixVal";
import allConsts from "pages/all-consts";

export default {
  name: "DataPropMatrix",
  components: { DataPropMatrixVal },
  props: ["reqParams", "mapPeriodType", "lstEntityType"],

  data: function () {
    //console.info("mapPeriodType", this.mapPeriodType)
    //console.info("reqParams", this.reqParams)

    return {
      splitterModel: ref(40),
      colsQ: [],
      rowsQ: [],
      rowsOrg: [],
      visibleColumns: ref(["provider", "status"]),
      loading: ref(false),
      separator: ref("cell"),
      selectedQ: ref([]),
      dense: true,
      idDataMatrix: 0,
      hasCond: false,
      Status: null,
      Provider: null,

      fltDt: null,
      fltPt: null,
    };
  },

  methods: {
    fnCond(val) {
      let v = JSON.parse(val);
      if (v.length === 0) return this.$t("no");
      let arr = [];
      v.forEach((it) => {
        let t = "";
        if (it.type === allConsts.FD_PropType.factor) t = this.$t("factor");
        else if (it.type === allConsts.FD_PropType.typ) t = this.$t("obj");
        else if (it.type === allConsts.FD_PropType.reltyp)
          t = this.$t("relobj");
        arr.push(it.name + " (" + t + ")");
      });
      return arr.join("; ");
    },

    fnDisableAdd() {
      let b =
        !this.reqParams.dependPeriod &&
        !this.hasCond &&
        this.reqParams.statusFactor === 0 &&
        this.reqParams.providerTyp === 0;
      //console.info("fnDisableAdd", b)
      return b;
    },

    fnProvider(val) {
      //console.info("fnProvider", val)
      if (val === null || val === undefined || val === 0) return null;
      return !this.Provider ? null : this.Provider.get(val);
    },

    fnStatus(val) {
      //console.info("fnStatus", val)
      if (val === null || val === undefined || val === 0) return null;
      return !this.Status ? null : this.Status.get(val);
    },

    fnPeriodType(val) {
      //console.info("fnPeriodType", val)
      if (val === null || val === undefined || val === 0)
        return this.$t("notDepend");
      return !this.mapPeriodType ? null : this.mapPeriodType.get(val);
    },

    updSelectionQ(par) {
      //console.info("updSelectionQ", par)
      //console.info("this.reqParams", this.reqParams)

      let param = {};
      if (par.length > 0) {
        this.$refs.DataPropMatrixVal.addDisabled = false;
        param = Object.assign(par[0], this.reqParams);
      } else {
        this.$refs.DataPropMatrixVal.addDisabled = true;
        param = Object.assign({ id: 0 }, this.reqParams);
      }
      this.$refs.DataPropMatrixVal.loadData(param);
    },

    fnDelete(props) {
      let rec = props.row;
      //console.info(rec, this.requestParams)
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
              method: "matrix/deleteDataPropMatrix",
              params: [rec.id],
            })
            .then(
              (response) => {
                this.loadDataMultiProp();
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

    fnAdd() {
      const lg = { name: this.lang };

      this.$q
        .dialog({
          component: UpdaterDataPropMatrix,
          componentProps: {
            rec: {},
            requestParams: this.reqParams,
            lg: lg,
            dense: this.dense,
            mode: "ins",

            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadDataMultiProp();
          }
        });
    },

    fnEdit(rec) {
      //console.info("Edit rec", rec)
      //console.info("Edit reqParam", this.reqParams)

      const lg = { name: this.lang };
      this.$q
        .dialog({
          component: UpdaterDataPropMatrix,
          componentProps: {
            rec: rec,
            requestParams: this.reqParams,
            lg: lg,
            dense: this.dense,
            mode: "upd",
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadDataMultiProp();
          }
        });
    },

    loadDataMultiProp() {
      //console.info("loadDataMultiProp reqParams", this.reqParams)
      if (this.reqParams.providerTyp === 0 && this.reqParams.statusFactor > 0) {
        this.hideProvider();
      } else if (
        this.reqParams.providerTyp > 0 &&
        this.reqParams.statusFactor === 0
      ) {
        this.hideStatus();
      } else if (
        this.reqParams.providerTyp === 0 &&
        this.reqParams.statusFactor === 0
      ) {
        this.hideProviderStatus();
      } else {
        this.showAll();
      }

      if (this.reqParams.providerTyp > 0) {
        this.loading = ref(true);
        api
          .post(baseURL, {
            method: "matrix/loadProvider",
            params: [this.reqParams.propMatrix],
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

      if (this.reqParams.statusFactor > 0) {
        this.loading = ref(true);
        api
          .post(baseURL, {
            method: "matrix/loadStatus",
            params: [this.reqParams.propMatrix],
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
      api
        .post(baseURL, {
          method: "matrix/loadDataMultiProp",
          params: [this.reqParams],
        })
        .then(
          (response) => {
            //setTimeout(()=> {
            this.rowsQ = response.data.result.records;
            this.rowsOrg = response.data.result.records;
            if (this.rowsQ !== null && this.rowsQ.size > 0)
              this.hasCond = this.rowsQ.propMatrixCond.length > 2;

            let param = Object.assign({ id: 0 }, this.reqParams);
            this.$refs.DataPropMatrixVal.loadData(param);
            //},500)
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
      //
    },

    hideProvider: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["status"];
      this.colsQ[0].headerStyle = "font-size: 1.3em; width=50%";
    },

    hideStatus: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider"];
      this.colsQ[0].headerStyle = "font-size: 1.3em; width=50%";
    },

    hideProviderStatus: function () {
      this.visibleColumns = [];
      this.colsQ[0].headerStyle = "font-size: 1.3em; width=50%";
      this.colsQ[3].headerStyle = "font-size: 1.3em; width=40%";
    },

    showAll: function () {
      this.visibleColumns = [];
      this.visibleColumns = ["provider", "status"];
    },

    getColumns() {
      return [
        {
          name: "propMatrixCond",
          required: true,
          label: this.$t("propMatrixCond"),
          field: "propMatrixCond",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 30%",
        },
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
          name: "periodType",
          required: true,
          label: this.$t("periodType"),
          field: "periodType",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 20%",
        },
        {
          name: "cmd",
          required: true,
          field: "cmd",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "width: 10%",
        },
      ];
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.colsQ = this.getColumns();
  },
};
</script>

<style scoped></style>
