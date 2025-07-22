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
      <q-bar class="text-white bg-primary" v-if="form.dataPropVal > 0">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>
      <q-bar class="text-white bg-primary" v-else>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-inner-loading :showing="visible" color="secondary" />

      <q-card>
        <q-card-section>
          <div class="text-blue" style="font-size: 11px">
            {{ $t("val") }}:
            <span class="text-orange" style="font-size: 13px">{{
              valueName
            }}</span>
            <q-space />
            <q-toggle
              :model-value="add"
              v-model="add"
              :label="$t('addRecord')"
              v-if="form.dataPropVal > 0"
              @update:model-value="selectAdd"
            />
          </div>

          <q-bar class="bg-white no-padding no-margin">
            <q-input
              v-if="reqParams.periodType > 0"
              v-model="dt"
              stack-label
              :model-value="dt"
              type="date"
              dense
              style="width: 100px; margin-right: 20px"
              :disable="reqParams.parent > 0"
              :label="$t('date')"
              @update:model-value="fnDt"
            />

            <q-select
              v-if="reqParams.periodType > 0"
              v-model="form.periodType"
              :model-value="form.periodType"
              dense
              options-dense
              :options="optionsPeriod"
              :disable="reqParams.parent > 0 || disablePT"
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

          <!--
          todo :disable="reqParams.periodType > 0"
          -->
          <q-input
            dense
            type="date"
            stack-label
            :model-value="form.dbeg"
            v-model="form.dbeg"
            :disable="reqParams.parent > 0 /*reqParams.periodType > 0*/"
            :label="$t('fldDbeg')"
          >
          </q-input>

          <q-input
            dense
            type="date"
            stack-label
            :model-value="form.dend"
            v-model="form.dend"
            :disable="reqParams.parent > 0 /*reqParams.periodType > 0*/"
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
          dense color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          dense color="primary"
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
import {notifyError} from "src/utils/jsutils";
import {ref} from "vue";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);



export default {
  props: [
    "dataProp",
    "rec",
    "requestParams",
    "lg",
    "valueName",
    "providerName",
    "statusName",
  ],

  data() {
    console.log("upd data RefValue requestParams", this.requestParams);
    console.log("upd data RefValue rec", this.rec);

    return {
      form: this.rec,
      reqParams: this.requestParams,
      visible: ref(false),
      periodTypeName: this.$t("notDepend"),
      //dt: this.today(),
      dt: this.rec.dbeg,

      //provider: null,
      optionsProvider: [],
      optionsProviderOrg: [],

      //status: null,
      optionsStatus: [],
      optionsStatusOrg: [],

      //period: null,
      optionsPeriod: [],
      FD_PeriodType: null,
      add: ref(false),
      disablePT: false,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    selectAdd(val) {
      console.info("selectAdd", val)
      if (val) {
        this.disablePT = false;
      } else {
        if (this.requestParams.dependPeriod) {
          this.disablePT = true;
          this.form.periodType = this.oldPeriodType;
          this.getPeriodInfo(this.dt, this.form.periodType);
        }
      }
    },

    fnDt(val) {
      this.getPeriodInfo(val, this.form.periodType);
    },

    fnPeriod(val) {
      this.getPeriodInfo(this.dt, val.id);
    },

    inputValueStatus(val) {},

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

    inputValueProvider(val) {},

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
      return false;
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

      if (this.add) {
        this.form.dataProp = null;
        this.form.dataPropVal = null;
      }
      console.info("add, form", this.add, this.form, typeof this.form.status);
      try {
        this.form.status =
          typeof this.form.status === "object"
            ? this.form.status.id
            : this.form.status;
        this.form.provider =
          typeof this.form.provider === "object"
            ? this.form.provider.id
            : this.form.provider;

        this.$emit("ok", { add: this.add, rec: this.form });
      } finally {
        this.hide();
      }
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
            this.form.dbeg = response.data.result.dbeg;
            this.form.dend = response.data.result.dend;
            this.form.periodType = periodType;
            this.periodTypeName = response.data.result.periodTypeName;
          },
          (error) => {
            let msg = error.message;
            //if (error.response.data.error.message)
            //msg = error.response.data.error.message
            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
        });
    },
  },
  created() {
    if (this.form.periodType > 0)
      this.getPeriodInfo(this.dt, this.form.periodType);

    api
      .post(baseURL, {
        method: "data/loadDict",
        params: ["FD_PeriodType"],
      })
      .then((response) => {
        this.FD_PeriodType = response.data.result.records;
      });

    if (this.reqParams.providerTyp > 0) {
      api
        .post(baseURL, {
          method: "data/loadProvider",
          params: [this.reqParams.prop, getModel.value, getMetaModel.value],
        })
        .then((response) => {
          this.optionsProviderOrg = response.data.result.records;
          this.optionsProvider = response.data.result.records;
          if (this.form.provider === undefined) {
            this.form.provider = this.optionsProvider[0].id;
          }
        });
    }
    //
    if (this.reqParams.statusFactor > 0) {
      api
        .post(baseURL, {
          method: "data/loadStatus",
          params: [this.reqParams.prop],
        })
        .then((response) => {
          this.optionsStatusOrg = response.data.result.records;
          this.optionsStatus = response.data.result.records;
          if (this.form.status === undefined) {
            this.form.status = this.optionsStatus[0].id;
          }
        });
    }
    //
    if (this.reqParams.periodType > 0) {
      api
        .post(baseURL, {
          method: "data/propPeriodType",
          params: [this.reqParams.prop],
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
            if (this.rec.dataPropVal > 0) {
              this.disablePT = true;
              this.oldPeriodType = this.form.periodType;
            }
          });
        })
        .finally(() => {});
    }

    return {};
  },
};
</script>
