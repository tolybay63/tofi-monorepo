<template>
  <q-page class="q-pa-sm">
    <q-breadcrumbs class="text-blue">
      <q-breadcrumbs-el
        icon="arrow_back"
        :label="$t('ownersMulti')"
        to="/multi"
      />
      <q-breadcrumbs-el class="text-grey" :label="$t('dataMulti')" />
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
          <multi-props-of-owner
            ref="childProp"
            @updateSelect="onUpdateSelect"
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
                :measure="propsOfProp.measure"
                :kfc="propsOfProp.kfc"
                :digit="propsOfProp.digit"
                ref="MeterValue"
              />
            </div>

          </div>
        </template>
      </q-splitter>
    </div>
  </q-page>
</template>

<script>
import MultiPropsOfOwner from "pages/multi/MultiPropsOfOwner.vue";
import EmptyPage from "pages/main/EmptyPage.vue";
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
import allConsts from "pages/all-consts";
import MeterValue from "pages/std/meter/MeterValue.vue";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  name: "MultiInputPage",
  components: {MeterValue, MultiPropsOfOwner, EmptyPage/*, DataMultiProp*/ },

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
//

      mapPeriodType: null,
      FD_AttribValType: [],
      optionsAttribValType: [],
      entityType: [],
    };
  },

  methods: {
    loadPropsOfMulti(dmp, dmpi) {
      api
        .post(baseURL, {
          method: "data/propsOfMultiProp",
          params: [dmp, dmpi],
        })
        .then(
          (response) => {
            this.propsOfProp = response.data.result
            this.requestParam.multiProp = this.propsOfProp.multiProp
            this.requestParam.multiPropDim = this.propsOfProp.multiPropDim
            this.requestParam.dimMultiPropItem = this.propsOfProp.dimMultiPropItem
            this.requestParam.isObj = this.isObj

            if (this.isMeter()) {
              //meter
              this.requestParam.minVal = this.propsOfProp.minVal
              this.requestParam.maxVal = this.propsOfProp.maxVal
              this.requestParam.digit = this.propsOfProp.digit
              this.requestParam.kfc = this.propsOfProp.kfc
              this.requestParam.isUniq = 1
              this.requestParam.entityType = allConsts.FD_MultiValEntityType.meter
              this.requestParam.measure = this.propsOfProp.measure
            } else {
              this.requestParam.isUniq = this.propsOfProp.isUniq
            }

            this.requestParam.dependPeriod = this.propsOfProp.dependPeriod;
            if (this.requestParam.dependPeriod) {
              this.loadPeriod(this.propsOfProp.multiProp);
              this.requestParam.periodType = this.propsOfProp.periodType;
            } else {
              this.optionsPeriod = [];
              this.optionsPeriod.push({ id: 0, text: this.$t("notDepend") });
              this.fltPt = 0;
              this.requestParam.periodType = this.fltPt;
            }

/*
            this.propsOfProp = response.data.result.props.records[0];
            this.entityType = response.data.result.entityType.records;
            this.entityType.forEach((it) => {
              it.name = this.$t(it.name);
            });

            this.requestParam.multiProp = multiProp;
            this.requestParam.propName = this.propsOfProp.name;
            this.requestParam.isUniq = this.propsOfProp.isUniq;
            this.requestParam.visualFormat = this.propsOfProp.visualFormat;

            if (this.propsOfProp.isMeter) {
              //meter
              this.requestParam.measure = this.propsOfProp.measure;
              this.requestParam.minVal = this.propsOfProp.minVal;
              this.requestParam.maxVal = this.propsOfProp.maxVal;
              this.requestParam.kfc = this.propsOfProp.kfc;
              this.requestParam.digit = this.propsOfProp.digit;
            }

            if (this.propsOfProp.isAttr) {
              //attr
              this.requestParam.format = this.propsOfProp.format;
              this.loadAttribValType(multiProp);
            }

            this.requestParam.statusFactor =
              this.propsOfProp.statusFactor === undefined
                ? 0
                : this.propsOfProp.statusFactor;
            this.requestParam.providerTyp =
              this.propsOfProp.providerTyp === undefined
                ? 0
                : this.propsOfProp.providerTyp;
            this.requestParam.dependPeriod =
              this.propsOfProp.isDependValueOnPeriod;
            this.propsOfProp.dependPeriod =
              this.propsOfProp.isDependValueOnPeriod;
            if (this.propsOfProp.periodType !== undefined) {
              this.loadPeriod(multiProp);
              this.requestParam.periodType = this.propsOfProp.periodType;
            } else {
              this.optionsPeriod = [];
              this.optionsPeriod.push({ id: 0, text: this.$t("notDepend") });
              this.fltPt = 0;
              this.requestParam.periodType = this.fltPt;
            }
*/
          })
        .catch(error=> {
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
        })
        .finally(() => {
          this.requestParam.isMulti = true
          //console.info("FINNALLY requestParam", this.requestParam)
          //console.info("FINNALLY propsOfProp", this.propsOfProp)
          if (this.isMeter()) {
            this.$refs.MeterValue.loadData(this.requestParam);
          }/* else if (this.isRef()) {
            this.$refs.RefValue.loadData(this.requestParam);
          } else if (this.isAttr()) {
            this.$refs.AttribValue.loadData(this.requestParam);
          } else if (this.isComplex()) {
            this.$refs.ComplexValue.loadData(this.requestParam);
          }*/

          //....
        });
    },

    loadPeriod(prop) {
      api
        .post(baseURL, {
          method: "data/propPeriodType",
          params: [prop, true],
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
          //console.info("this.optionsPeriod", this.optionsPeriod)
        })
        .finally(() => {});
    },

    loadAttribValType(multiProp) {
      api
        .post(baseURL, {
          method: "matrix/propAttribValType",
          params: [multiProp],
        })
        .then((response) => {
          this.optionsAttribValType = [];
          this.FD_AttribValType.forEach((r) => {
            if (response.data.result.includes(r.id)) {
              this.optionsAttribValType.push(r);
              // format, mask not known!
            }
          });
        })
        .finally(() => {});
    },

    onUpdateSelect(data) {
      this.currentNode = data.selected !== undefined ? data.selected : null;

      //console.info("currentNode", this.currentNode)

      let multiProp =
        this.currentNode == null ? 0 : this.currentNode.multiProp;
      this.fltPt = 0;
      //console.info("this.currentNode", this.currentNode)
      if (this.currentNode) {
        this.loadPropsOfMulti(this.currentNode.dimMultiProp, this.currentNode.dimMultiPropItem);
      }
      //
    },

    isMeter() {
      return this.propsOfProp.multiEntityType===allConsts.FD_MultiValEntityType.meter;
    },

    isAttrib() {
      return this.propsOfProp.multiEntityType===allConsts.FD_MultiValEntityType.attr_str ||
              this.propsOfProp.multiEntityType===allConsts.FD_MultiValEntityType.attr_mask ||
                this.propsOfProp.multiEntityType===allConsts.FD_MultiValEntityType.attr_date;
    },


    fnDt(val) {
      //this.requestParam.dt = val
      if (val) this.fltPt = null;
    },

    fnPeriod(val) {
      if (val) this.fltDt = null;
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
    //console.log("mounted DataMulti", this.$route.params)

    this.owner = this.$route.params.owner;
    this.isObj = this.$route.params.isObj;
    this.typORrel = this.$route.params.typORrel;
    this.requestParam.owner = parseInt(this.owner);
    this.requestParam.isObj = this.isObj ? 1 : 0;

    if (this.isAttrib()) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "data/loadDict",
          params: ["FD_AttribValType"],
        })
        .then((response) => {
          this.FD_AttribValType = response.data.result.records;
        })
        .finally(() => {
          this.loading = ref(false);
        });
    }

    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "data/loadDict",
        params: ["FD_PeriodType"],
      })
      .then((response) => {
        this.FD_PeriodType = response.data.result.records;

        //console.info("this.FD_PeriodType", this.FD_PeriodType)
      })
      .finally(() => {
        this.loading = ref(false);
      });

    this.loading = ref(true);

    api
      .post(baseURL, {
        method: "data/loadRecObjOrRelObj",
        params: [this.owner, this.isObj, getModel.value, getMetaModel.value],
      })
      .then((response) => {
        this.recOwn = response.data.result.records[0];
      })
      .finally(() => {
        this.loading = ref(false);
      });

    this.$refs.childProp.loadDataProps(this.typORrel, this.isObj);
  },

  created() {
    return {};
  },
};
</script>

<style scoped></style>
