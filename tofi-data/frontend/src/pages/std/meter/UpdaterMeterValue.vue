<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar class="text-white bg-primary" v-if="form.id > 0">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>
      <q-bar class="text-white bg-primary" v-else>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-inner-loading :showing="visible" color="secondary" />

      <q-card>
        <q-card-section>
          <q-bar class="bg-white no-padding no-margin">
            <q-input
              v-if="reqParams.dependPeriod"
              v-model="dt"
              stack-label
              :model-value="dt"
              type="date"
              dense
              style="width: 100px; margin-right: 20px"
              :disable="form.isComplex === true"
              :label="$t('date')"
              @update:model-value="fnDt"
            />

            <q-select
              v-if="reqParams.dependPeriod"
              v-model="form.periodType"
              :model-value="form.periodType"
              dense
              options-dense
              :options="optionsPeriod"
              :disable="form.isComplex === true /*mode==='upd'*/"
              :label="$t('periodType')"
              option-value="id"
              option-label="text"
              map-options
              style="width: 200px"
              @update:model-value="fnPeriod"
            />
          </q-bar>

          <q-select
            v-if="requestParams.providerTyp > 0"
            class="q-pt-md"
            v-model="form.provider"
            :model-value="form.provider"
            use-input
            map-options
            input-debounce="0"
            :label="$t('providerTyp')"
            :options="optionsProvider"
            option-value="id"
            option-label="name"
            dense options-dense
            :disable="mode === 'upd'"
            @update:model-value="inputValueProvider"
            @filter="filterFnProvider"
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <q-select
            v-if="requestParams.statusFactor > 0"
            class="q-pt-md"
            v-model="form.status"
            map-options
            :model-value="form.status"
            use-input
            input-debounce="0"
            :label="$t('statusFactor')"
            :options="optionsStatus"
            option-value="id"
            option-label="name"
            dense options-dense
            :disable="mode === 'upd'"
            @update:model-value="inputValueStatus"
            @filter="filterFnStatus"
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <q-input
            autofocus
            dense
            :model-value="form.numberVal"
            v-model="form.numberVal"
            :label="$t('val')"
            type="number"
            :rules="[(val) => validValue(val) || msg]"
          >
          </q-input>

          <q-input
            dense
            type="date"
            stack-label
            :disable="form.isComplex === true /*reqParams.periodType > 0*/"
            :model-value="form.dbeg"
            v-model="form.dbeg"
            :label="$t('fldDbeg')"
          >
          </q-input>

          <q-input
            dense
            type="date"
            tack-label
            :disable="form.isComplex === true /*reqParams.periodType > 0*/"
            :model-value="form.dend"
            v-model="form.dend"
            :label="$t('fldDend')"
          >
          </q-input>

          <!-- cmt -->
          <q-input
            dense
            :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
          >
          </q-input>
        </q-card-section>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          dense
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError, notifyInfo, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
import allConsts from "pages/all-consts";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);


export default {
  props: ["rec", "requestParams", "mode", "lg"],

  data() {
    console.log("upd data", this.rec, this.requestParams)
    let r = Object.assign({}, this.rec);
    r.minVal= this.requestParams.minVal
    r.maxVal= this.requestParams.maxVal
    r.digit= this.requestParams.digit
    //console.log("upd data r", r)

    return {
      form: r,
      reqParams: this.requestParams,
      visible: ref(false),
      periodTypeName: this.$t("notDepend"),
      //dt: this.today(),
      dt: r.dbeg,

      //provider: null,
      optionsProvider: [],
      optionsProviderOrg: [],

      //status: null,
      optionsStatus: [],
      optionsStatusOrg: [],

      //period: null,
      optionsPeriod: [],
      FD_PeriodType: null,
      msg: "",
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    validValue(val) {
      let b = true;
      let v = val * this.reqParams.kfc;
      if (this.reqParams.minVal !== undefined) b = v >= this.reqParams.minVal;
      if (this.reqParams.maxVal !== undefined) b = b && v <= this.reqParams.maxVal;

      return b;
    },

    fnDt(val) {
      this.getPeriodInfo(val, this.form.periodType);
    },

    fnPeriod(val) {
      console.info("fnPeriod", val)
      this.getPeriodInfo(this.dt, val.id);
    },

    inputValueProvider(val) {
      //console.info("val", val)
      //console.info("form.provider", this.form.provider)
      this.form.provider = val.id;
    },

    inputValueStatus(val) {
      //console.info("val", val)
      //console.info("form.status", this.form.status)
      this.form.status = val.id;
    },

    filterFnStatus(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsStatus = this.optionsStatusOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsStatusOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsStatus = this.optionsStatusOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    filterFnProvider(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsProvider = this.optionsProviderOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsProviderOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsProvider = this.optionsProviderOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    today() {
      let d = new Date();
      let currDate = d.getDate();
      let currMonth = d.getMonth() + 1;
      let currYear = d.getFullYear();
      return (
        currYear +
        "-" +
        (currMonth < 10 ? "0" + currMonth : currMonth) +
        "-" +
        (currDate < 10 ? "0" + currDate : currDate)
      );
    },

    validSave() {
      //console.info((this.form.numberVal==="" ||this.form.numberVal===undefined) || (this.form.provider===null || this.form.provider==="" ||this.form.provider===undefined))
      let b = true;
      if ( !this.reqParams.provider && !this.reqParams.status ) {
        b = this.form.numberVal === "" || this.form.numberVal === undefined;
      } else if (!this.reqParams.provider && this.reqParams.status) {
        b =
          this.form.numberVal === "" ||
          this.form.numberVal === undefined ||
          this.form.statusFactor === null ||
          this.form.statusFactor === "" ||
          this.form.statusFactor === undefined;
      } else if ( this.reqParams.provider && !this.reqParams.status ) {
        b =
          this.form.numberVal === "" ||
          this.form.numberVal === undefined ||
          this.form.providerTyp === null ||
          this.form.providerTyp === "" ||
          this.form.providerTyp === undefined;
      } else if ( this.reqParams.provider && this.reqParams.status ) {
        b =
          this.form.numberVal === "" ||
          this.form.numberVal === undefined ||
          this.form.providerTyp === null ||
          this.form.providerTyp === "" ||
          this.form.providerTyp === undefined ||
          this.form.statusFactor === null ||
          this.form.statusFactor === "" ||
          this.form.statusFactor === undefined;
      }
      //console.info("b", b)
      b = b || !this.validValue(this.form.numberVal);
      return b;
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide");
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      const method =
        this.mode === "ins" ? "insertMeterValue" : "updateMeterValue";
      this.visible = true;

      this.form.mode = this.mode;
      this.form.isObj = this.reqParams.isObj==="false" ? 0 : 1;
      this.form.model = this.reqParams.model
      this.form.metamodel = this.reqParams.metamodel
      this.form.owner = this.reqParams.owner
      this.form.objorrelobj = this.reqParams.owner
      if (this.reqParams.isMulti) {
        this.form.entityType = allConsts.FD_MultiValEntityType.meter
        this.form.isMulti = this.reqParams.isMulti
        this.form.multiProp = this.reqParams.multiProp
        this.form.multiPropDim = this.reqParams.multiPropDim
        this.form.dimMultiPropItem = this.reqParams.dimMultiPropItem
      } else {
        this.form.prop = this.reqParams.prop
      }

      //console.info("this.form+req Ok", this.form)

      let err = false;
      api
        .post(baseURL, {
          method: "data/" + method,
          params: [this.form],
        })
        .then(
          (response) => {
            err = false;
            //console.info("response FlatTable Ins", response.data.result.records[0])
            this.$emit("ok", { res: true });
            notifySuccess(this.$t("success"));
          },
          (error) => {
            err = true;
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.visible = ref(false);
          if (!err) this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    getPeriodInfo(dt, periodType) {
      api
        .post(baseURL, {
          method: "data/getPeriodInfo",
          params: [dt, periodType],
        })
        .then(
          (response) => {
            //console.info("getPeriodInfo periodType", periodType)
            this.form.dbeg = response.data.result.dbeg;
            this.form.dend = response.data.result.dend;
            this.form.periodType = periodType;
            this.periodTypeName = response.data.result.periodTypeName;
            this.form.periodTypeName = response.data.result.periodTypeName;
            //console.info("getPeriodInfo", this.form)
          },
          (error) => {
            let msg = error.message;
            if (error.response.data.error.message)
              msg = error.response.data.error.message
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
        });
    },
  },
  created() {
    console.info("UpdateMeter Created", this.reqParams)
    let b1 =
      this.reqParams.minVal !== undefined ? this.reqParams.minVal : "...";
    let b2 =
      this.reqParams.maxVal !== undefined ? this.reqParams.maxVal : "...";
    this.msg = this.$t("val") + ": [" + b1 + "; " + b2 + "]";

    api
      .post(baseURL, {
        method: "data/loadDict",
        params: ["FD_PeriodType"],
      })
      .then((response) => {
        this.FD_PeriodType = response.data.result.records;
      });

    if (this.reqParams.provider > 0) {
      api
        .post(baseURL, {
          method: "data/loadProvider",
          params: [this.reqParams.prop, getModel.value, getMetaModel.value],
        })
        .then((response) => {
          this.optionsProviderOrg = response.data.result.records;
          this.optionsProvider = response.data.result.records;
          if (this.optionsProvider.length === 0) {
            notifyInfo(this.$t("notProvider"));
          } else {
            if (this.form.provider === undefined) {
              this.form.provider = this.optionsProvider[0].id;
            }
          }
        });
    }
    //
    if (this.reqParams.status > 0) {
      api
        .post(baseURL, {
          method: "data/loadStatus",
          params: [this.reqParams.prop],
        })
        .then((response) => {
          this.optionsStatusOrg = response.data.result.records;
          this.optionsStatus = response.data.result.records;
          if (this.optionsStatus.length === 0) {
            notifyInfo(this.$t("notStatus"));
          } else if (this.form.status === undefined) {
            this.form.status = this.optionsStatus[0].id;
          }
        });
    }
    //
    if (this.reqParams.dependPeriod) {
      let prop = this.reqParams.isMulti ? this.reqParams.multiProp : this.reqParams.prop
      api
        .post(baseURL, {
          method: "data/propPeriodType",
          params: [prop, this.reqParams.isMulti],
        })
        .then((response) => {
          this.optionsPeriod = [];
          this.FD_PeriodType.forEach((r) => {
            if (response.data.result.includes(r.id)) {
              this.optionsPeriod.push(r);
            }
            if (
              this.form.periodType === undefined &&
              this.form.dt === undefined
            ) {
              this.dt = this.today();
              this.form.periodType = this.optionsPeriod[0].id;
              this.getPeriodInfo(this.dt, this.form.periodType);
            }
          });

          console.info("this.FD_PeriodType", this.FD_PeriodType)
          console.info("this.optionsPeriod", this.optionsPeriod)
          console.info("this.form", this.form)
        })
        .finally(() => {})
    } else {
      if (this.form.dbeg === "1800-01-01") {
        this.form.dbeg = null;
      }
      if (this.form.dend === "3333-12-31") {
        this.form.dend = null;
      }
    }

    return {};
  },
};
</script>
