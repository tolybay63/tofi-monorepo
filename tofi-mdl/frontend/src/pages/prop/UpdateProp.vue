<template>
  <q-dialog
    ref="dialogRef"
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

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card>
        <q-tabs v-model="tab" class="text-teal">
          <q-tab name="main" :label="$t('mainProp')" />
          <q-tab name="dop" :label="$t('dopProp')" />
        </q-tabs>

        <q-separator />

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
              v-model="form.name"
              @blur="onBlurName"
              :label="$t('fldName')"
              :error-message="$t('min3word')"
              :error="!isValid(form.name)"
            />

            <!-- fullName-->
            <q-input
              dense
              v-model="form.fullName"
              :label="$t('fldFullName')"
              :error-message="$t('min3word')"
              :error="!isValid(form.fullName)"
            />

            <!-- Cod -->
            <q-input
              dense
              v-model="form.cod"
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
              @update:model-value="fnSelectDict('accessLevel')"
              :disable="
                (mode === 'upd' && form.parent !== undefined) || isComplexProp
              "
            />

            <q-select
              v-model="status"
              map-options
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
              use-input
              map-options
              input-debounce="0"
              :label="$t('providerTyp')"
              :options="optionsProvider"
              option-value="id"
              option-label="name"
              dense
              options-dense
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
                map-options
                use-input
                input-debounce="0"
                :label="$t('attribute')"
                :options="optionsAttrib"
                option-value="id"
                option-label="name"
                dense
                options-dense
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
                map-options
                use-input
                input-debounce="0"
                :label="$t('factor')"
                :options="optionsStatus"
                option-value="id"
                option-label="name"
                dense
                options-dense
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
                map-options
                use-input
                input-debounce="0"
                :label="$t('meter')"
                :options="optionsMeter"
                option-value="id"
                option-label="name"
                dense
                options-dense
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
                map-options
                use-input
                input-debounce="0"
                :label="$t('meter')"
                :options="optionsMeter"
                option-value="id"
                option-label="name"
                dense
                options-dense
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

                <TreeSelect
                  v-model="rate"
                  :label="$t('rate')"
                  :options="optRate"
                  class="q-ma-md"
                  node-key="key"
                  @close="fnCloseRate"
                  :disabled="mode === 'upd' && form.parent !== undefined"
                />


<!--
                <q-item-label class="text-grey-7" style="font-size: 0.8em">
                  {{ $t("rate") }}
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
-->

              </div>
            </div>

            <div v-if="isTyp()">
              <q-select
                v-model="typ"
                map-options
                use-input
                input-debounce="0"
                :label="$t('typ')"
                :options="optionsProvider"
                option-value="id"
                option-label="name"
                dense
                options-dense
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
                v-model="form['isDependValueOnPeriod']"
                :label="$t('dependValueOnPeriod')"
                @update:model-value="fnPeriodDepend"
                :disable="
                  (mode === 'upd' && form.parent !== undefined) || isComplexProp
                "
              />
            </div>

            <div>
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
                v-model="form.isDependNameOnPeriod"
                :label="$t('dependNameOnPeriod')"
                :disable="
                  !form['isDependValueOnPeriod'] ||
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
                v-model="form.isUniq"
                :label="$t('isUniq')"
              />
            </div>
            <div v-if="isFactor() || isTyp() || isRel()">
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
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
                @update:model-value="fnSelectDict('visualFormat')"
              />
            </div>

            <div v-if="isMeter() || isRate()">
              <!-- meterBehavior -->
              <q-select
                :dense="dense"
                v-model="meterBehavior"
                :options="optionsBehavior"
                :label="$t('meterBehavior')"
                option-value="id"
                option-label="text"
                map-options
                @update:model-value="fnSelectDict('meterBehavior')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />

              <div>
                <TreeSelect
                  v-model="measure"
                  :label="$t('measure')"
                  :options="measures"
                  class="q-my-md"
                  node-key="key"
                  :disabled="mode === 'upd' && form.parent !== undefined"
                  @close="fnCloseMeasure"
                  @select="fnSelMeasure"
                />


<!--
                <q-item-label class="text-grey-7" style="font-size: 0.8em">
                  {{ $t("measure") }}
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
-->

              </div>

              <q-input
                v-model="form.minVal"
                type="number"
                :dense="dense"
                :label="$t('minVal')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />
              <q-input
                v-model="form.maxVal"
                type="number"
                :dense="dense"
                :label="$t('maxVal')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />

              <q-input
                v-model="form.digit"
                type="number"
                :dense="dense"
                :label="$t('digit')"
                :disable="mode === 'upd' && form.parent !== undefined"
              />
            </div>

            <!-- cmt -->
            <q-input
              :dense="dense"
              v-model="form.cmt"
              type="textarea"
              :label="$t('fldCmt')"
            />

            <q-input
              :dense="dense"
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
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess, pack } from "@/utils/jsutils";
import allConsts from "@/pages/all-consts";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  rec: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: String,
  isComplexProp: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const dense = ref(true);
const form = reactive({ ...props.rec });
const visible = ref(false);
const parentName = ref(null);

const optionsLevel = ref([]);
const al = ref(props.rec.accessLevel);

const optionsFV = ref([]);
const vf = ref(props.rec.visualFormat);

const optionsPropType = ref([]);
const propType = ref(props.rec.propType);

const status = ref(props.rec.statusFactor);
const optionsStatus = ref([]);
const optionsStatusOrg = ref([]);

const provider = ref(props.rec.providerTyp);
const optionsProvider = ref([]);
const optionsProviderOrg = ref([]);

const factor = ref(props.rec.factor);

const meter = ref(props.rec.meter);
const measureBase = ref(null);
const optionsMeter = ref([]);
const optionsMeterOrg = ref([]);
const meterStruct = ref(0);

const rate = ref(props.rec.meterRate);
const optRate = ref([]);

const typ = ref(props.rec.typ);

const relTyp = ref(props.rec.relTyp);
const optionsRelTyp = ref([]);
const optionsRelTypOrg = ref([]);

const optionsBehavior = ref([]);
const meterBehavior = ref(props.rec.meterBehavior);

const attrib = ref(props.rec.attrib);
const optionsAttrib = ref([]);
const optionsAttribOrg = ref([]);

const measures = ref([]);
const measure = ref(props.rec.measure);

const tab = ref("main");

const inputValueAttrib = (val) => {
  if (val) {
    attrib.value = { ...attrib.value, id: val.id };
    form.attrib = val.id;
  }
};

const fnPeriodDepend = () => {
  if (!form["isDependValueOnPeriod"]) {
    form.isDependNameOnPeriod = false;
  }
};

const fnCloseMeasure = (v) => {
  form.measure = v;
};

const fnCloseRate = (v) => {
  form.meterRate = v;
};

const normalizer = (node) => {
  return {
    id: node.id,
    label: node.name,
  };
};

const fnClearStatus = () => {
  form.statusFactor = null;
};

const fnClearProvider = () => {
  form.providerTyp = null;
};

const inputValueFactor = (val) => {
  if (val) {
    factor.value = { ...factor.value, id: val.id };
    form.factor = val.id;
  }
};

const loadAttrib = () => {
  visible.value = true;
  api
    .post("", {
      method: "attrib/loadForSelect",
      params: [],
    })
    .then((response) => {
      optionsAttribOrg.value = response.data.result.records;
      optionsAttrib.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadRelTyp = () => {
  visible.value = true;
  api
    .post("", {
      method: "reltyp/loadRelTypForSelect",
      params: [],
    })
    .then((response) => {
      optionsRelTypOrg.value = response.data.result.records;
      optionsRelTyp.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadMeter = () => {
  visible.value = true;
  api
    .post("", {
      method: "meter/loadForSelect",
      params: [],
    })
    .then((response) => {
      optionsMeterOrg.value = response.data.result.records;
      optionsMeter.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });
};

const loadMeterRate = (meterVal) => {
  if (props.rec.propType === allConsts.FD_PropType.rate) {
    let act = "meterrate/loadHardMR";
    if (meterStruct.value === allConsts.FD_MeterStruct.soft)
      act = "meterrate/loadSoftMR";
    api
      .post("", {
        method: act,
        params: [meterVal],
      })
      .then((response) => {
        optRate.value = pack(response.data.result.records, "ord");
      });
  }
};

const inputValueMeter = (val) => {
  if (val) {
    meter.value = { ...meter.value, id: val.id };
    form.meter = val.id;
    meterStruct.value = val.meterStruct;
    measureBase.value = val.measure;
    let params = {};
    if (measureBase.value) params.measureBase = measureBase.value;

    loadMeterRate(val.id);

    api
      .post("", {
        method: "measure/load",
        params: [params],
      })
      .then((response) => {
        measures.value = pack(response.data.result.records, "ord");
        measure.value = measures.value[0];
      })
      .finally(() => {
        form.measure = val.measure;
      });
  }
};

const filterFnMeter = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsMeter.value = optionsMeterOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsMeterOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsMeter.value = optionsMeterOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const filterFnAttrib = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsAttrib.value = optionsAttribOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsAttribOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsAttrib.value = optionsAttribOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const inputValueStatus = (val) => {
  if (val) {
    status.value = { ...status.value, id: val.id };
    form.statusFactor = val.id;
  }
};

const filterFnStatus = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsStatus.value = optionsStatusOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsStatusOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsStatus.value = optionsStatusOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const inputValueTyp = (val) => {
  if (val) {
    typ.value = { ...typ.value, id: val.id };
    form.typ = val.id;
  }
};

const filterFnTyp = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsProvider.value = optionsProviderOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsProviderOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsProvider.value = optionsProviderOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const inputValueRelTyp = (val) => {
  if (val) {
    relTyp.value = { ...relTyp.value, id: val.id };
    form.relTyp = val.id;
  }
};

const filterFnRelTyp = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsRelTyp.value = optionsRelTypOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsRelTypOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsRelTyp.value = optionsRelTypOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const inputValueProvider = (val) => {
  if (val) {
    provider.value = { ...provider.value, id: val.id };
    form.providerTyp = val.id;
  }
};

const filterFnProvider = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsProvider.value = optionsProviderOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsProviderOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsProvider.value = optionsProviderOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelMeasure = () => {
  form.measure = measure.value.id;
};

const fnSelectDict = (dict) => {
  if (dict === "accessLevel")
    form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  else if (dict === "visualFormat")
    form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;
  else if (dict === "meterBehavior")
    form.meterBehavior = typeof meterBehavior.value === "object" && meterBehavior.value !== null ? meterBehavior.value.id : meterBehavior.value;
  else if (dict === "propType") {
    form.propType = typeof propType.value === "object" && propType.value !== null ? propType.value.id : propType.value;
    if (form.propType === allConsts.FD_PropType.factor) {
      form.meter = null;
      form.meterRate = null;
      form.attrib = null;
      form.typ = null;
      form.relTyp = null;
      form.measure = null;
    } else if (form.propType === allConsts.FD_PropType.meter) {
      form.factor = null;
      form.meterRate = null;
      form.attrib = null;
      form.typ = null;
      form.relTyp = null;
      form.measure = null;
      form.isUniq = null;
      form.allItem = null;
      if (props.mode === "ins") {
        form.meterBehavior = allConsts.FD_MeterBehavior.neutral;
        meterBehavior.value = allConsts.FD_MeterBehavior.neutral;
      }
      loadMeter();
    } else if (form.propType === allConsts.FD_PropType.rate) {
      form.factor = null;
      form.meter = null;
      form.attrib = null;
      form.typ = null;
      form.relTyp = null;
      form.measure = null;
      form.isUniq = null;
      form.allItem = null;
      visible.value = true;
      loadMeter();
    } else if (form.propType === allConsts.FD_PropType.attr) {
      form.factor = null;
      form.meter = null;
      form.meterRate = null;
      form.typ = null;
      form.relTyp = null;
      form.measure = null;
      loadAttrib();
    } else if (form.propType === allConsts.FD_PropType.typ) {
      form.factor = null;
      form.meter = null;
      form.meterRate = null;
      form.attrib = null;
      form.relTyp = null;
      form.measure = null;
    } else if (form.propType === allConsts.FD_PropType.reltyp) {
      form.factor = null;
      form.meter = null;
      form.meterRate = null;
      form.attrib = null;
      form.typ = null;
      form.measure = null;
      loadRelTyp();
    } else if (form.propType === allConsts.FD_PropType.measure) {
      form.factor = null;
      form.meter = null;
      form.meterRate = null;
      form.attrib = null;
      form.typ = null;
      form.relTyp = null;
    } else if (form.propType === allConsts.FD_PropType.complex) {
      form.factor = null;
      form.meter = null;
      form.meterRate = null;
      form.attrib = null;
      form.typ = null;
      form.relTyp = null;
      form.measure = null;
    }
  }
};

const isValid = (v) => {
  return v && v.length > 2;
};

const validSave = () => {
  return !(
    form.name !== null &&
    form.name !== undefined &&
    form.name.length > 2 &&
    form.fullName !== null &&
    form.fullName !== undefined &&
    form.fullName.length > 2 &&
    ((form.propType === allConsts.FD_PropType.factor &&
        form.factor > 0) ||
      (form.propType === allConsts.FD_PropType.typ &&
        form.typ > 0) ||
      (form.propType === allConsts.FD_PropType.reltyp &&
        form.relTyp > 0) ||
      (form.propType === allConsts.FD_PropType.meter &&
        form.meter > 0) ||
      (form.propType === allConsts.FD_PropType.rate &&
        form.meterRate > 0) ||
      (form.propType === allConsts.FD_PropType.attr &&
        form.attrib > 0) ||
      form.propType === allConsts.FD_PropType.measure ||
      form.propType === allConsts.FD_PropType.complex)
  );
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";

  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;
  form.meterBehavior = typeof meterBehavior.value === "object" && meterBehavior.value !== null ? meterBehavior.value.id : meterBehavior.value;
  form.propType = typeof propType.value === "object" && propType.value !== null ? propType.value.id : propType.value;

  let err = false;
  api
    .post("", {
      method: "prop/" + method,
      params: [{ rec: form }],
    })
    .then((response) => {
      err = false;
      onDialogOK(response.data.result.records[0]);
      notifySuccess(proxy?.$t("success") || "success");
    })
    .catch((error) => {
      err = true;
      let errMessage = error.response?.data?.error?.message || error.message;
      if (errMessage.includes("@")) {
        let msgs = errMessage.split("@");
        let m1 = proxy?.$t(`${msgs[0]}`) || msgs[0];
        let m2 = msgs.length > 1 ? " [" + msgs[1] + "]" : "";
        let msg = m1 + m2;
        notifyError(msg);
        if (msgs[0] === "NotChangeStructComplexProp") {
          onDialogCancel();
        }
      } else {
        notifyError(errMessage);
      }
    })
    .finally(() => {
      if (!err) {
        // dialogRef handles close via onDialogOK/onDialogCancel
      }
    });
};

const getDict = (dictName) => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: dictName }],
    })
    .then((response) => {
      if (dictName === "FD_AccessLevel")
        optionsLevel.value = response.data.result.records;
      else if (dictName === "FD_PropType") {
        optionsPropType.value = response.data.result.records;
        if (props.isComplexProp) {
          optionsPropType.value = optionsPropType.value.filter((r) => {
            return r.id !== allConsts.FD_PropType.complex;
          });
        }
      } else if (dictName === "FD_MeterBehavior")
        optionsBehavior.value = response.data.result.records;
      else if (dictName === "FD_VisualFormat")
        optionsFV.value = response.data.result.records;
    })
    .catch((error) => {
      let errMsg = error.response?.data?.error?.message || error.message;
      notifyError(proxy?.$t(errMsg) || errMsg);
    });
};

const isFactor = () => form.propType === allConsts.FD_PropType.factor;
const isAttr = () => form.propType === allConsts.FD_PropType.attr;
const isMeter = () => form.propType === allConsts.FD_PropType.meter;
const isRate = () => form.propType === allConsts.FD_PropType.rate;
const isTyp = () => form.propType === allConsts.FD_PropType.typ;
const isRel = () => form.propType === allConsts.FD_PropType.reltyp;
const isComplex = () => form.propType === allConsts.FD_PropType.complex;

onMounted(() => {
  getDict("FD_AccessLevel");
  getDict("FD_PropType");
  getDict("FD_MeterBehavior");
  getDict("FD_VisualFormat");

  api
    .post("", {
      method: "prop/getParentName",
      params: [props.rec.propGr, !props.rec.parent ? 0 : props.rec.parent],
    })
    .then((response) => {
      parentName.value = response.data.result;
    });

  api
    .post("", {
      method: "factor/loadForSelect",
      params: [],
    })
    .then((response) => {
      optionsStatusOrg.value = response.data.result.records;
      optionsStatus.value = response.data.result.records;
    });

  visible.value = true;
  api
    .post("", {
      method: "typ/loadTypForSelect",
      params: [],
    })
    .then((response) => {
      optionsProviderOrg.value = response.data.result.records;
      optionsProvider.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  if (props.mode === "upd") {
    if (form.propType === allConsts.FD_PropType.attr) {
      loadAttrib();
    }
    if (form.propType === allConsts.FD_PropType.reltyp) {
      loadRelTyp();
    }

    if (
      form.propType === allConsts.FD_PropType.meter ||
      form.propType === allConsts.FD_PropType.rate
    ) {
      api
        .post("", {
          method: "meter/loadForSelect",
          params: [],
        })
        .then((response) => {
          optionsMeterOrg.value = response.data.result.records;
          optionsMeter.value = response.data.result.records;

          let flt = optionsMeterOrg.value.filter(
            (r) => r.id === props.rec.meter
          );
          if (flt.length > 0) meterStruct.value = flt[0].meterStruct;
        })
        .finally(() => {
          if (form.propType === allConsts.FD_PropType.rate) {
            loadMeterRate(props.rec.meter);
          }
          api
            .post("", {
              method: "measure/load",
              params: [{ meter: form.meter }],
            })
            .then((response) => {
              measures.value = pack(response.data.result.records, "ord");
            });
        });
    }
  }
});
</script>

<style scoped></style>
