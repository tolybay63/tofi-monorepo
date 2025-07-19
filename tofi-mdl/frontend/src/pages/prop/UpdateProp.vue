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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card>
        <q-tabs v-model="tab" class="text-teal">
          <q-tab name="main" :label="$t('mainProp')"/>
          <q-tab name="dop" :label="$t('dopProp')"/>
        </q-tabs>

        <q-separator/>

        <q-tab-panels
          v-model="tab"
          animated
          swipeable
          vertical
          transition-prev="jump-up"
          transition-next="jump-up"
        >
          <q-tab-panel name="main">
            <div class="row">
              <div class="text-blue q-mr-sm" style="font-size: small">
                {{ $t("parent") }}:
              </div>
              <div>{{ parentName }}</div>
            </div>

            <!-- propType -->
            <q-select
              :disable="mode === 'upd'"
              dense
              v-model="propType"
              :model-value="propType"
              :options="optionsPropType"
              :label="$t('propType')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('propType')"
            />

            <q-input
              autofocus
              dense
              :model-value="form.name"
              v-model="form.name"
              @blur="onBlurName"
              :label="$t('fldName')"
              :error-message="$t('min3word')"
              :error="!isValid(form.name)"
            >
            </q-input>

            <!-- fullName-->
            <q-input
              dense
              :model-value="form.fullName"
              v-model="form.fullName"
              :label="$t('fldFullName')"
              :error-message="$t('min3word')"
              :error="!isValid(form.fullName)"
            >
            </q-input>

            <!-- Cod -->
            <q-input
              dense
              v-model="form.cod"
              :model-value="form.cod"
              :label="$t('code')"
              :placeholder="$t('msgCodeGen')"
            />

            <!-- AccessLevel -->
            <q-select
              dense
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              :model-value="al"
              @update:model-value="fnSelectDict('accessLevel')"
              :disable="
                (mode === 'upd' && form.parent !== undefined) || isComplexProp
              "
            />

            <q-select
              v-model="status"
              map-options
              :model-value="status"
              use-input
              input-debounce="0"
              :label="$t('statusFactor')"
              :options="optionsStatus"
              option-value="id"
              option-label="name"
              dense
              options-dense
              clearable
              @clear="fnClearStatus"
              @update:model-value="inputValueStatus"
              @filter="filterFnStatus"
              :disable="
                (mode === 'upd' && form.parent !== undefined) || isComplexProp
              "
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
              v-model="provider"
              :model-value="provider"
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
              clearable
              @clear="fnClearProvider"
              :disable="
                (mode === 'upd' && form.parent !== undefined) || isComplexProp
              "
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t("noResults") }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>
          </q-tab-panel>

          <q-tab-panel name="dop">
            <div v-if="isAttr()">
              <q-select
                v-model="attrib"
                :model-value="attrib"
                map-options
                use-input
                input-debounce="0"
                :label="$t('attribute')"
                :options="optionsAttrib"
                option-value="id"
                option-label="name"
                dense options-dense
                :disable="mode === 'upd'"
                @update:model-value="inputValueAttrib"
                @filter="filterFnAttrib"
                :rules="[(val) => !!val || $t('req')]"
              >
                <template v-slot:no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      {{ $t("noResults") }}
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>

            <div v-if="isFactor()">
              <q-select
                v-model="factor"
                :model-value="factor"
                map-options
                use-input
                input-debounce="0"
                :label="$t('factor')"
                :options="optionsStatus"
                option-value="id"
                option-label="name"
                dense options-dense
                :disable="mode === 'upd'"
                @update:model-value="inputValueFactor"
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
            </div>

            <div v-if="isMeter()">
              <q-select
                v-model="meter"
                :model-value="meter"
                map-options
                use-input
                input-debounce="0"
                :label="$t('meter')"
                :options="optionsMeter"
                option-value="id"
                option-label="name"
                dense options-dense
                :disable="mode === 'upd'"
                @update:model-value="inputValueMeter"
                @filter="filterFnMeter"
              >
                <template v-slot:no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      {{ $t("noResults") }}
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>

            <div v-if="isRate()">
              <q-select
                v-model="meter"
                :model-value="meter"
                map-options
                use-input
                input-debounce="0"
                :label="$t('meter')"
                :options="optionsMeter"
                option-value="id"
                option-label="name"
                dense options-dense
                :disable="mode === 'upd'"
                @update:model-value="inputValueMeter"
                @filter="filterFnMeter"
              >
                <template v-slot:no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      {{ $t("noResults") }}
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>

              <div>
                <q-item-label class="text-grey-7" style="font-size: 0.8em">{{
                    $t("rate")
                  }}
                </q-item-label>
                <treeselect
                  :options="optRate"
                  max-height="800"
                  v-model="rate"
                  :normalizer="normalizer"
                  :placeholder="$t('select')"
                  :noChildrenText="$t('noChilds')"
                  :noResultsText="$t('noResult')"
                  :noOptionsText="$t('noResult')"
                  @close="fnCloseRate"
                  :disabled="mode === 'upd' && form.parent !== undefined"
                />
              </div>
            </div>

            <div v-if="isTyp()">
              <q-select
                v-model="typ"
                :model-value="typ"
                map-options
                use-input
                input-debounce="0"
                :label="$t('typ')"
                :options="optionsProvider"
                option-value="id"
                option-label="name"
                dense options-dense
                :disable="mode === 'upd'"
                @update:model-value="inputValueTyp"
                @filter="filterFnTyp"
              >
                <template v-slot:no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      {{ $t("noResults") }}
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>

            </div>

            <div v-if="isRel()">
              <q-select
                v-model="relTyp"
                :model-value="relTyp"
                map-options
                use-input
                input-debounce="0"
                :label="$t('reltyp')"
                :options="optionsRelTyp"
                option-value="id"
                option-label="name"
                :dense="dense"
                :options-dense="dense"
                :disable="mode === 'upd'"
                @update:model-value="inputValueRelTyp"
                @filter="filterFnRelTyp"
              >
                <template v-slot:no-option>
                  <q-item>
                    <q-item-section class="text-grey">
                      {{ $t("noResults") }}
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>

            <div>
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
                :model-value="form['isDependValueOnPeriod']"
                v-model="form['isDependValueOnPeriod']"
                :label="$t('dependValueOnPeriod')"
                @update:model-value="fnPeriodDepend()"
                :disable="
                  (mode === 'upd' && form.parent !== undefined) || isComplexProp
                "
              />
            </div>

            <div>
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
                :model-value="form.isDependNameOnPeriod"
                v-model="form.isDependNameOnPeriod"
                :label="$t('dependNameOnPeriod')"
                :disable="
                  !this.form['isDependValueOnPeriod'] ||
                  (mode === 'upd' && form.parent !== undefined) ||
                  isComplexProp
                "
              />
            </div>

            <div
              v-if="isFactor() || isAttr() || isTyp() || isRel() || isComplex()"
            >
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
                model-value="form.isUniq"
                v-model="form.isUniq"
                :label="$t('isUniq')"
              />
            </div>
            <div v-if="isFactor() || isTyp() || isRel()">
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
                model-value="form.allItem"
                v-model="form.allItem"
                :label="$t('allItem')"
              />

              <!-- visualFormat -->
              <q-select
                style="margin-top: 10px"
                :dense="dense"
                v-model="vf"
                :options="optionsFV"
                :label="$t('visualFormat')"
                option-value="id"
                option-label="text"
                map-options
                :model-value="vf"
                @update:model-value="fnSelectDict('visualFormat')"
              />
            </div>

            <div v-if="isMeter() || isRate()">
              <!-- meterBehavior -->
              <q-select
                :dense="dense"
                v-model="meterBehavior"
                :model-value="meterBehavior"
                :options="optionsBehavior"
                :label="$t('meterBehavior')"
                option-value="id"
                option-label="text"
                map-options
                @update:model-value="fnSelectDict('meterBehavior')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />

              <div>
                <q-item-label class="text-grey-7" style="font-size: 0.8em">{{
                    $t("measure")
                  }}
                </q-item-label>
                <treeselect
                  :options="measures"
                  max-height="800"
                  v-model="measure"
                  :normalizer="normalizer"
                  :placeholder="$t('select')"
                  :noChildrenText="$t('noChilds')"
                  :noResultsText="$t('noResult')"
                  :noOptionsText="$t('noResult')"
                  @close="fnCloseMeasure"
                  :disabled="mode === 'upd' && form.parent !== undefined"
                  @select="fnSelMeasure"
                />
              </div>

              <q-input
                v-model="form.minVal"
                :model-value="form.minVal"
                type="number"
                :dense="dense"
                :label="$t('minVal')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />
              <q-input
                v-model="form.maxVal"
                :model-value="form.maxVal"
                type="number"
                :dense="dense"
                :label="$t('maxVal')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />

              <q-input
                v-model="form.digit"
                :model-value="form.digit"
                type="number"
                :dense="dense"
                :label="$t('digit')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />
            </div>

            <!-- cmt -->
            <q-input
              :dense="dense"
              :model-value="form.cmt"
              v-model="form.cmt"
              type="textarea"
              :label="$t('fldCmt')"
            >
            </q-input>

            <q-input
              :dense="dense"
              :model-value="form.propTag"
              v-model="form.propTag"
              :label="$t('propTag')"
            />
          </q-tab-panel>
        </q-tab-panels>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          :dense="dense"
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
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import {ref} from "vue";
import allConsts from "pages/all-consts";

export default {
  components: {treeselect},
  props: ["rec", "mode", "lg", "isComplexProp"],

  data() {
    console.info("UpdateProp", this.rec)
    return {
      dense: true,  //???????????????????????????


      form: this.rec,
      visible: ref(false),
      parentName: null,

      optionsLevel: [],
      al: this.rec.accessLevel,

      optionsFV: [],
      vf: this.rec.visualFormat,

      optionsPropType: [],
      propType: this.rec.propType,

      status: this.rec.statusFactor,
      optionsStatus: [],
      optionsStatusOrg: [],

      provider: this.rec.providerTyp,
      optionsProvider: [],
      optionsProviderOrg: [],

      factor: this.rec.factor,

      meter: this.rec.meter,
      measureBase: null,
      optionsMeter: [],
      optionsMeterOrg: [],
      meterStruct: 0,

      rate: this.rec.meterRate,
      optRate: [],

      typ: this.rec.typ,

      relTyp: this.rec.relTyp,
      optionsRelTyp: [],
      optionsRelTypOrg: [],

      optionsBehavior: [],
      meterBehavior: this.rec.meterBehavior,

      attrib: this.rec.attrib,
      optionsAttrib: [],
      optionsAttribOrg: [],

      measures: [],
      measure: this.rec.measure,

      tab: ref("main"),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    inputValueAttrib(val) {
      if (val) {
        this.attrib.id = val.id;
        this.form.attrib = val.id;
      }
    },

    fnPeriodDepend() {
      if (!this.form["isDependValueOnPeriod"]) {
        this.form.isDependNameOnPeriod = false;
      }
    },

    fnCloseMeasure(v) {
      this.form.measure = v;
    },

    fnCloseRate(v) {
      this.form.meterRate = v;
    },

    normalizer(node) {
      return {
        id: node.id,
        label: node.name,
      };
    },

    fnClearStatus() {
      this.form.statusFactor = null;
    },

    fnClearProvider() {
      this.form.providerTyp = null;
    },

    inputValueFactor(val) {
      if (val) {
        this.factor.id = val.id;
        this.form.factor = val.id;
      }
    },

    loadAttrib() {
      this.visible = ref(true);
      api
        .post(baseURL, {
          method: "attrib/loadForSelect",
          params: [],
        })
        .then((response) => {
          this.optionsAttribOrg = response.data.result.records;
          this.optionsAttrib = response.data.result.records;
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    loadRelTyp() {
      this.visible = ref(true);
      api
        .post(baseURL, {
          method: "reltyp/loadRelTypForSelect",
          params: [],
        })
        .then((response) => {
          this.optionsRelTypOrg = response.data.result.records;
          this.optionsRelTyp = response.data.result.records;
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    loadMeter() {
      this.visible = ref(true);
      api
        .post(baseURL, {
          method: "meter/loadForSelect",
          params: [],
        })
        .then((response) => {
          this.optionsMeterOrg = response.data.result.records;
          this.optionsMeter = response.data.result.records;
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    loadMeterRate(meter) {
      //console.log("loadMeterRate", meter, this.propType.id, this.rec.propType, this.meterStruct)
      if (this.rec.propType === allConsts.FD_PropType.rate) {
        let act = "meterrate/loadHardMR";
        if (this.meterStruct === allConsts.FD_MeterStruct.soft)
          act = "meterrate/loadSoftMR";
        api
          .post(baseURL, {
            method: act,
            params: [meter],
          })
          .then((response) => {
            this.optRate = pack(response.data.result.records, "ord");
            //console.log("optCls", this.optCls)
          });
      }
    },

    inputValueMeter(val) {
      if (val) {
        //console.log("METER", val)
        //console.log("Val.MEASURE", val.measure)
        this.meter.id = val.id;
        this.form.meter = val.id;
        this.meterStruct = val.meterStruct;
        this.measureBase = val.measure;
        let params = {};
        if (this.measureBase) params.measureBase = this.measureBase;
        //console.log("MEASURE", this.measure)

        this.loadMeterRate(val.id);

        api
          .post(baseURL, {
            method: "measure/load",
            params: [params],
          })
          .then((response) => {
            this.measures = pack(response.data.result.records, "ord");
            this.measure = this.measures[0];
          })
          .finally(() => {
            this.form.measure = val.measure;
            //this.measure = val.measure
            //console.log("MEASURE", this.measure)
          });
      }
    },

    filterFnMeter(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsMeter = this.optionsMeterOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsMeterOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsMeter = this.optionsMeterOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    filterFnAttrib(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsAttrib = this.optionsAttribOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsAttribOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsAttrib = this.optionsAttribOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    inputValueStatus(val) {
      if (val) {
        this.status.id = val.id;
        this.form.statusFactor = val.id;
      }
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

    inputValueTyp(val) {
      if (val) {
        this.typ.id = val.id;
        this.form.typ = val.id;
      }
    },


    filterFnTyp(val, update) {
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

    inputValueRelTyp(val) {
      if (val) {
        this.relTyp.id = val.id;
        this.form.relTyp = val.id;
      }
    },

    filterFnRelTyp(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsRelTyp = this.optionsRelTypOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsRelTypOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsRelTyp = this.optionsRelTypOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    inputValueProvider(val) {
      if (val) {
        this.provider.id = val.id;
        this.form.providerTyp = val.id;
      }
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

    onBlurName() {
      if (this.form.name) {
        this.form.name = this.form.name.trim();
        if (
          !this.form.fullName ||
          (this.form.fullName && this.form.fullName.trim() === "")
        ) {
          this.form.fullName = this.form.name;
        }
      }
    },

    fnSelMeasure() {
      this.form.measure = this.measure.id;
    },

    fnSelectDict(dict) {
      if (dict === "accessLevel") this.form.accessLevel = this.al.id;
      else if (dict === "visualFormat") this.form.visualFormat = this.vf.id;
      else if (dict === "meterBehavior")
        this.form.meterBehavior = this.meterBehavior.id;
      else if (dict === "propType") {
        //console.log("dict", this.form)
        this.form.propType = this.propType.id;
        if (this.form.propType === allConsts.FD_PropType.factor) {
          this.form.meter = null;
          this.form.meterRate = null;
          this.form.attrib = null;
          this.form.typ = null;
          this.form.relTyp = null;
          this.form.measure = null;
          //this.form.complex = null
        } else if (this.form.propType === allConsts.FD_PropType.meter) {
          this.form.factor = null;
          this.form.meterRate = null;
          this.form.attrib = null;
          this.form.typ = null;
          this.form.relTyp = null;
          this.form.measure = null;
          //this.form.complex = null
          this.form.isUniq = null;
          this.form.allItem = null;
          if (this.mode === "ins") {
            this.form.meterBehavior = allConsts.FD_MeterBehavior.neutral;
            this.meterBehavior = allConsts.FD_MeterBehavior.neutral;
          }
          this.loadMeter();
        } else if (this.form.propType === allConsts.FD_PropType.rate) {
          this.form.factor = null;
          this.form.meter = null;
          this.form.attrib = null;
          this.form.typ = null;
          this.form.relTyp = null;
          this.form.measure = null;
          //this.form.complex = null
          this.form.isUniq = null;
          this.form.allItem = null;
          this.visible = ref(true);
          this.loadMeter();
        } else if (this.form.propType === allConsts.FD_PropType.attr) {
          this.form.factor = null;
          this.form.meter = null;
          this.form.meterRate = null;
          this.form.typ = null;
          this.form.relTyp = null;
          this.form.measure = null;
          //this.form.complex = null
          this.loadAttrib();
        } else if (this.form.propType === allConsts.FD_PropType.typ) {
          this.form.factor = null;
          this.form.meter = null;
          this.form.meterRate = null;
          this.form.attrib = null;
          this.form.relTyp = null;
          this.form.measure = null;
          //this.form.complex = null
        } else if (this.form.propType === allConsts.FD_PropType.reltyp) {
          this.form.factor = null;
          this.form.meter = null;
          this.form.meterRate = null;
          this.form.attrib = null;
          this.form.typ = null;
          this.form.measure = null;
          //this.form.complex = null

          this.loadRelTyp();
        } else if (this.form.propType === allConsts.FD_PropType.measure) {
          this.form.factor = null;
          this.form.meter = null;
          this.form.meterRate = null;
          this.form.attrib = null;
          this.form.typ = null;
          this.form.reltyp = null;
          //this.form.complex = null
        } else if (this.form.propType === allConsts.FD_PropType.complex) {
          this.form.factor = null;
          this.form.meter = null;
          this.form.meterRate = null;
          this.form.attrib = null;
          this.form.typ = null;
          this.form.reltyp = null;
          this.form.measure = null;
        }
      }
    },

    isValid(v) {
      return v && v.length > 2
    },

    validSave() {
      return !(
        this.form.name !== null &&
        this.form.name !== undefined &&
        this.form.name.length > 2 &&
        this.form.fullName !== null &&
        this.form.fullName !== undefined &&
        this.form.fullName.length > 2 &&
        ((this.form.propType === allConsts.FD_PropType.factor &&
            this.form.factor > 0) ||
          (this.form.propType === allConsts.FD_PropType.typ &&
            this.form.typ > 0) ||
          (this.form.propType === allConsts.FD_PropType.reltyp &&
            this.form.relTyp > 0) ||
          (this.form.propType === allConsts.FD_PropType.meter &&
            this.form.meter > 0) ||
          (this.form.propType === allConsts.FD_PropType.rate &&
            this.form.meterRate > 0) ||
          (this.form.propType === allConsts.FD_PropType.attr &&
            this.form.attrib > 0) ||
          this.form.propType === allConsts.FD_PropType.measure ||
          this.form.propType === allConsts.FD_PropType.complex)
      );
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

      const method = this.mode === "ins" ? "insert" : "update";

      let err = false;
      api
        .post(baseURL, {
          method: "prop/" + method,
          params: [{rec: this.form}],
        })
        .then(
          (response) => {
            err = false;
            this.$emit("ok", response.data.result.records[0]);
            //this.$emit("ok", {res: true});

            notifySuccess(this.$t("success"));
          })
        .catch(error=> {
          err = true;
          console.log(error.message)
          if (error.response.data.error.message.includes("@")) {
            let msgs = error.response.data.error.message.split("@")
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
            let msg = m1 + m2
            notifyError(msg)
            if (msgs[0] === "NotChangeStructComplexProp") {
              this.hide();
            }
          } else {
            notifyError(error.response.data.error.message)
          }
        })
        .finally(() => {
          if (!err) this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    getDict(dictName) {
      api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: dictName}],
        })
        .then(response => {
          if (dictName === "FD_AccessLevel")
            this.optionsLevel = response.data.result.records;
          else if (dictName === "FD_PropType") {
            this.optionsPropType = response.data.result.records;
            if (this.isComplexProp) {
              this.optionsPropType = this.optionsPropType.filter((r) => {
                return r.id !== allConsts.FD_PropType.complex;
              });
            }
          } else if (dictName === "FD_MeterBehavior")
            this.optionsBehavior = response.data.result.records;
          else if (dictName === "FD_VisualFormat")
            this.optionsFV = response.data.result.records;
        })
        .catch(error=> {
          notifyError(this.$t(error.response.data.error.message))
        });
    },

    isFactor() {
      return this.form.propType === allConsts.FD_PropType.factor;
    },

    isAttr() {
      return this.form.propType === allConsts.FD_PropType.attr;
    },

    isMeter() {
      return this.form.propType === allConsts.FD_PropType.meter;
    },

    isRate() {
      return this.form.propType === allConsts.FD_PropType.rate;
    },

    isTyp() {
      return this.form.propType === allConsts.FD_PropType.typ;
    },

    isRel() {
      return this.form.propType === allConsts.FD_PropType.reltyp;
    },

    isComplex() {
      return this.form.propType === allConsts.FD_PropType.complex;
    },
  },

  created() {
    console.info("UpdateProp Created")
    this.getDict("FD_AccessLevel");
    this.getDict("FD_PropType");
    this.getDict("FD_MeterBehavior");
    this.getDict("FD_VisualFormat");

    api
      .post(baseURL, {
        method: "prop/getParentName",
        params: [this.rec.propGr, !this.rec.parent ? 0 : this.rec.parent],
      })
      .then((response) => {
        this.parentName = response.data.result;
      });

    api
      .post(baseURL, {
        method: "factor/loadForSelect",
        params: [],
      })
      .then((response) => {
        this.optionsStatusOrg = response.data.result.records;
        this.optionsStatus = response.data.result.records;
        //this.optionsStatus.unshift({id: 0, name: this.$t('notChosen')})
      });

    this.visible = ref(true);
    api
      .post(baseURL, {
        method: "typ/loadTypForSelect",
        params: [{}],
      })
      .then((response) => {
        this.optionsProviderOrg = response.data.result.records;
        this.optionsProvider = response.data.result.records;
        //this.optionsProvider.unshift({id: 0, name: this.$t('notChosen')})
      })
      .finally(() => {
        this.visible = ref(false)
      });
    if (this.mode === "upd") {
      if (this.form.propType === allConsts.FD_PropType.attr) {
        this.loadAttrib();
      }
      if (this.form.propType === allConsts.FD_PropType.reltyp) {
        this.loadRelTyp();
      }

      if (
        this.form.propType === allConsts.FD_PropType.meter ||
        this.form.propType === allConsts.FD_PropType.rate
      ) {
        api
          .post(baseURL, {
            method: "meter/loadForSelect",
            params: [],
          })
          .then((response) => {
            this.optionsMeterOrg = response.data.result.records;
            this.optionsMeter = response.data.result.records;
            //
            let flt = this.optionsMeterOrg.filter(
              (r) => r.id === this.rec.meter
            );
            this.meterStruct = flt[0].meterStruct;
          })
          .finally(() => {
            if (this.form.propType === allConsts.FD_PropType.rate) {
              this.loadMeterRate(this.rec.meter);
            }
            api
              .post(baseURL, {
                method: "measure/load",
                params: [{meter: this.form.meter}],
              })
              .then((response) => {
                this.measures = pack(response.data.result.records, "ord");
              });
          });
      }
    }
  },

  computed: {
    /*
        isValid() {
          let vm = this;
          return vm.form.name && vm.form.name.length > 2
        },
    */
  }

};
</script>
