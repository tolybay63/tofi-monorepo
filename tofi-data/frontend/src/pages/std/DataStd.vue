<template>
  <q-page class="q-pa-sm">
    <q-breadcrumbs class="text-blue">
      <q-breadcrumbs-el
        icon="arrow_back"
        :label="$t('ownersStd')"
        to="/std"
      />
      <q-breadcrumbs-el class="text-grey" :label="$t('dataStd')" />
    </q-breadcrumbs>

    <q-banner inline-actions class="bg-orange-1 q-mb-sm">
      <div style="font-size: 1.2em">{{ ownerInfo() }}</div>
      <template v-slot:action>
        <div class="text-blue q-pr-lg q-mt-sm">{{ $t("txt_filter") }}:</div>
        <q-input
          v-model="fltDt"
          :model-value="fltDt"
          type="date"
          dense
          style="width: 140px; margin-right: 20px"
          clearable
          stack-label
          :label="$t('date')"
          @update:model-value="fnDt"
        />
        <q-select
          v-model="fltPt"
          :model-value="fltPt"
          dense
          options-dense
          :options="optionsPeriod"
          :label="$t('periodType')"
          option-value="id"
          option-label="text"
          map-options
          style="width: 200px"
          :disable="!propsOfProp.dependPeriod"
          @update:model-value="fnPeriod"
        />
      </template>
    </q-banner>

    <div class="q-pa-md-md">
      <q-splitter
        v-model="splitterModel"
        class="no-padding no-margin"
        style="height: 100%; width: 100%"
        separator-class="bg-red"
        before-class="overflow-hidden q-mr-sm"
        after-class="overflow-hidden q-ml-sm"
      >
        <template v-slot:before>
          <props-of-owner
            ref="childProp"
            @updateSelect="onUpdateSelect"
            class="no-scroll"
          />
        </template>

        <template v-slot:after>
          <div style="height: 100%; width: 100%">
            <empty-page v-if="currentNode == null"/>
            <div v-else-if="isMeter()">
              <meter-value
                :propType="propsOfProp.propType"
                :isUniq="propsOfProp.isUniq"
                :periodType="propsOfProp.periodType"
                :status="propsOfProp.status"
                :provider="propsOfProp.provider"
                :propName="currentNode.name"
                :measurename="propsOfProp.measureName"
                :kfc="propsOfProp.kfc"
                ref="MeterValue"
              />
            </div>
            <div v-else-if="isRef()">
              <ref-value
                :propType="propsOfProp.propType"
                :isUniq="propsOfProp.isUniq"
                :periodType="propsOfProp.periodType"
                :status="propsOfProp.status"
                :provider="propsOfProp.provider"
                :propName="currentNode.name"
                ref="RefValue"
              />
            </div>
            <div v-else-if="isAttr()">
              <attrib-value
                :status="propsOfProp.status"
                :provider="propsOfProp.provider"
                :propName="currentNode.name"
                ref="AttribValue"
              />
            </div>
            <div v-else-if="isComplex()">
              <complex-value
                :status="propsOfProp.status"
                :provider="propsOfProp.provider"
                :propName="currentNode.name"
                ref="ComplexValue"
              />
            </div>

            <empty-page v-else></empty-page>
          </div>
        </template>
      </q-splitter>
    </div>
  </q-page>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import PropsOfOwner from "pages/std/PropsOfOwner.vue";
import {notifyError} from "src/utils/jsutils";
import EmptyPage from "pages/main/EmptyPage.vue";
import allConsts from "pages/all-consts";
import AttribValue from "pages/std/attr/AttribValue.vue";
import ErrorPage from "pages/main/ErrorPage.vue";
import MeterValue from "pages/std/meter/MeterValue.vue";
import RefValue from "pages/std/ref/RefValue.vue";
import {storeToRefs} from "pinia";
import ComplexValue from "pages/std/complex/ComplexValue.vue";
import {useParamsStore} from "stores/params-store";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);


export default {
  components: {
    ComplexValue,
    RefValue,
    MeterValue,
    ErrorPage,
    AttribValue,
    EmptyPage,
    PropsOfOwner,
  },

  name: "StdInputPage",

  data() {
    return {
      owner: null,
      isObj: true,
      splitterModel: ref(40),
      fltDt: null, //this.today(),
      fltPt: null,

      loading: ref(false),
      FD_PeriodType: [],
      optionsPeriod: [],
      recOwn: {},
      typORrel: null,
      currentNode: null,
      propsOfProp: { proptype: 0, dependperiod: 0, dependperiodname: 0 },
      requestParam: { dt: this.today() },
    };
  },

  methods: {
    loadPropsOfProp(prop) {
      api
        .post(baseURL, {
          method: "data/propsOfProp",
          params: [prop],
        })
        .then(
          (response) => {
            this.propsOfProp = response.data.result.props.records[0]
            this.requestParam.prop = prop
            this.requestParam.propType = this.propsOfProp.propType
            this.requestParam.isObj = this.isObj

            if (this.isMeter()) {
              //meter
              this.requestParam.minVal = this.propsOfProp.minVal
              this.requestParam.maxVal = this.propsOfProp.maxVal
              this.requestParam.kfc = this.propsOfProp.kfc
              this.requestParam.isUniq = 1
            } else {
              this.requestParam.isUniq = this.propsOfProp.isUniq
            }

            if (this.isAttr()) {
              //attr
              this.requestParam.attribValType = this.propsOfProp.attribValType;
              this.requestParam.maskReg = this.propsOfProp.maskReg;
              this.requestParam.format = this.propsOfProp.format;
              this.requestParam.entityType = this.propsOfProp.entityType;
              this.requestParam.periodTypeAttr =
                this.propsOfProp.periodTypeAttr;
              this.requestParam.fileExt = this.propsOfProp.fileExt;
            }

            if (this.isRef()) {
              this.requestParam.visualFormat = this.propsOfProp.visualFormat;
            }

            this.requestParam.statusFactor =
              this.propsOfProp.statusFactor === undefined
                ? 0
                : this.propsOfProp.statusFactor;
            this.requestParam.providerTyp =
              this.propsOfProp.providerTyp === undefined
                ? 0
                : this.propsOfProp.providerTyp;

            this.requestParam.dependPeriod = this.propsOfProp.dependPeriod;
            if (this.requestParam.dependPeriod) {
              this.loadPeriod(prop);
              //console.log("fltPt", this.fltPt);
              this.requestParam.periodType = this.propsOfProp.periodType;;
            } else {
              this.optionsPeriod = [];
              this.optionsPeriod.push({ id: 0, text: this.$t("notDepend") });
              this.fltPt = 0;
              this.requestParam.periodType = this.fltPt;
            }

            //console.info("DataStd:this.requestParam", this.requestParam)
          },
          (error) => {
            let msg = error.message;
            if (error.response) {
              msg = error.response.data.error.message;
              if (msg.startsWith("notStatus")) {
                msg = this.$t("notStatus") + ":" + msg.substring(10);
              }
              if (msg.startsWith("notProvider")) {
                msg = this.$t("notProvider") + ":" + msg.substring(12);
              }
            }
            notifyError(msg);
          }
        )
        .finally(() => {
          this.requestParam.isMulti = false
          //console.info("FINNALLY propsOfProp", this.propsOfProp)
          console.info("FINNALLY requestParam", this.requestParam)
          if (this.isMeter()) {
            this.$refs.MeterValue.loadData(this.requestParam);
          } else if (this.isRef()) {
            this.$refs.RefValue.loadData(this.requestParam);
          } else if (this.isAttr()) {
            this.$refs.AttribValue.loadData(this.requestParam);
          } else if (this.isComplex()) {
            this.$refs.ComplexValue.loadData(this.requestParam);
            //

          }

          //....
        });
    },

    onUpdateSelect(data) {
      //console.info("data.sel", data.selected)

      this.currentNode = data.selected !== undefined ? data.selected : null;
      let prop = this.currentNode == null ? 0 : this.currentNode.prop;
      this.fltPt = 0;
      if (prop > 0) {
        console.info("onUpdateSelect", this.currentNode)
        this.loadPropsOfProp(prop);
      }
      //
    },

    loadPeriod(prop) {
      api
        .post(baseURL, {
          method: "data/propPeriodType",
          params: [prop, false],
        })
        .then((response) => {
          this.optionsPeriod = [];
          this.FD_PeriodType.forEach((r) => {
            if (response.data.result.includes(r.id)) {
              this.optionsPeriod.push(r);
            }
          });
          this.optionsPeriod.push({ id: 0, text: this.$t("notChosen") });
          this.fltPt = 0;
        })
        .finally(() => {});
    },

    fnDt(val) {
      this.requestParam.dt = val
      if (val) this.fltPt = 0;

      if (this.isMeter()) {
        this.$refs.MeterValue.filterDate(val);
      } else if (this.isRef()) {
        this.$refs.RefValue.filterDate(val);
      } else if (this.isAttr()) {
        this.$refs.AttribValue.filterDate(val);
      } else if (this.isComplex()) {
        this.$refs.ComplexValue.filterDate(val);
      }
    },

    fnPeriod(val) {
      if (val) this.fltDt = null;
      this.requestParam.periodType = val.id
      if (this.isMeter()) {
        this.$refs.MeterValue.filterPeriod(val.id);
      } else if (this.isRef()) {
        this.$refs.RefValue.filterPeriod(val.id);
      } else if (this.isAttr()) {
        this.$refs.AttribValue.filterPeriod(val.id);
      } else if (this.isComplex()) {
        this.$refs.ComplexValue.filterPeriod(val);
      }
    },

    isAttr() {
      return this.propsOfProp.propType === allConsts.FD_PropType.attr
    },

    isMeter() {
      return (
        this.propsOfProp.propType === allConsts.FD_PropType.meter ||
        this.propsOfProp.propType === allConsts.FD_PropType.rate
      );
    },

    isRef() {
      return (
        this.propsOfProp.propType === allConsts.FD_PropType.typ ||
        this.propsOfProp.propType === allConsts.FD_PropType.factor ||
        this.propsOfProp.propType === allConsts.FD_PropType.reltyp ||
        this.propsOfProp.propType === allConsts.FD_PropType.measure
      );
    },

    isComplex() {
      return this.propsOfProp.propType === allConsts.FD_PropType.complex;
    },

    ownerInfo() {
      let d1 = this.recOwn.dbeg <= "1800-01-01" ? "..." : this.recOwn.dbeg;
      let d2 = this.recOwn.dend >= "3333-12-31" ? "..." : this.recOwn.dend;
      return (
        this.recOwn.cod +
        " - " +
        this.recOwn.name +
        ", [" +
        d1 +
        " - " +
        d2 +
        "]"
      );
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
  },

  mounted() {
    console.log("mounted OwnerPage", this.$route.params)

    this.owner = this.$route.params.owner;
    this.isObj = this.$route.params.isObj;
    this.typORrel = this.$route.params.typORrel;
    this.requestParam.owner = parseInt(this.owner);
    this.requestParam.objorrelobj = parseInt(this.owner);
    this.requestParam.isObj = this.isObj ? 1 : 0;

    console.log("mounted requestParam", this.requestParam)
    console.log("owner", this.owner, this.isObj)

    this.loading = ref(true)
    api
      .post(baseURL, {
        method: "data/loadDict",
        params: ["FD_PeriodType"],
      })
      .then((response) => {
        this.FD_PeriodType = response.data.result.records
      })
      .catch(error=> {
        notifyError(this.$t(error.response.data.error.message))
      })
      .finally(() => {
        this.loading = ref(false)
      });

    this.loading = ref(true)
    api
      .post(baseURL, {
        method: "data/loadRecObjOrRelObj",
        params: [this.owner, this.isObj, getModel.value, getMetaModel.value],
      })
      .then((response) => {
        this.recOwn = response.data.result.records[0]
      })
      .then(()=> {
        this.$refs.childProp.loadDataProps(this.typORrel, this.isObj);
      })
      .finally(() => {
        this.loading = ref(false);
      });

    //this.$refs.childProp.loadDataProps(this.typORrel, this.isObj);
  },

  created() {
    return {};
  },
};
</script>

<style scoped></style>
