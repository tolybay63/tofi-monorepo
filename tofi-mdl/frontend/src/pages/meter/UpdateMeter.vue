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
            <!-- name -->
            <q-input
              autofocus
              :dense="dense"
              v-model="form.name"
              @blur="onBlurName"
              :label="$t('fldName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />
            <!-- fullName-->
            <q-input
              :dense="dense"
              v-model="form.fullName"
              :label="$t('fldFullName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />

            <!-- cod -->
            <q-input
              :dense="dense"
              v-model="form.cod"
              :label="$t('code')"
              :placeholder="$t('msgCodeGen')"
            />
            <!-- meterStruct -->
            <q-select
              :dense="dense"
              v-model="meterStruct"
              :options="optionsStruct"
              :label="$t('meterStruct')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('meterStruct')"
            />

            <!-- Measure -->
            <q-select
              :dense="dense"
              v-model="measure"
              :options="measures"
              :label="$t('measure')"
              option-value="id"
              option-label="name"
              map-options
              @update:model-value="fnSelMeasure"
            />

            <!-- accessLevel -->
            <q-select
              :dense="dense"
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('accessLevel')"
            />

            <!-- cmt -->
            <q-input
              :dense="dense"
              v-model="form.cmt"
              type="textarea"
              :label="$t('fldCmt')"
            />
          </q-tab-panel>

          <q-tab-panel name="dop">
            <!-- meterDeterm -->
            <q-select
              :dense="dense"
              v-model="meterDeterm"
              :options="optionsDeterm"
              :label="$t('meterDeterm')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('meterDeterm')"
            />
            <!-- distributionLaw -->
            <q-select
              :dense="dense"
              v-model="distributionLaw"
              :options="optionsDistribution"
              :label="$t('distributionLaw')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('distribution')"
            />

            <!-- meterTypeByRate -->
            <q-select
              :dense="dense"
              v-model="meterTypeByRate"
              :options="optionsByRate"
              :label="$t('meterTypeByRate')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('meterTypeByRate')"
            />
            <!-- meterTypeByPeriod -->
            <q-select
              :dense="dense"
              v-model="meterTypeByPeriod"
              :options="optionsByPeriod"
              :label="$t('meterTypeByPeriod')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('meterTypeByPeriod')"
            />
            <!-- meterTypeByMember -->
            <q-select
              :dense="dense"
              v-model="meterTypeByMember"
              :options="optionsByMember"
              :label="$t('meterTypeByMember')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict('meterTypeByMember')"
            />
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
            />

            <q-input
              v-model="form.minVal"
              :dense="dense"
              :label="$t('minVal')"
            />
            <q-input
              v-model="form.maxVal"
              :dense="dense"
              :label="$t('maxVal')"
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
          :disable="validName()"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });

const optionsLevel = ref([]);
const al = ref(props.data.accessLevel);

const optionsStruct = ref([]);
const meterStruct = ref(props.data.meterStruct);

const optionsDeterm = ref([]);
const meterDeterm = ref(props.data.meterDeterm);

const optionsDistribution = ref([]);
const distributionLaw = ref(props.data.distributionLaw);

const optionsByRate = ref([]);
const meterTypeByRate = ref(props.data.meterTypeByRate);

const optionsByPeriod = ref([]);
const meterTypeByPeriod = ref(props.data.meterTypeByPeriod);

const optionsByMember = ref([]);
const meterTypeByMember = ref(props.data.meterTypeByMember);

const optionsBehavior = ref([]);
const meterBehavior = ref(props.data.meterBehavior);

const measures = ref([]);
const measure = ref(props.data.measure);

const tab = ref("main");

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelMeasure = () => {
  form.measure = typeof measure.value === "object" && measure.value !== null ? measure.value.id : measure.value;
};

const fnSelectDict = (dict) => {
  if (dict === "accessLevel")
    form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  else if (dict === "meterStruct")
    form.meterStruct = typeof meterStruct.value === "object" && meterStruct.value !== null ? meterStruct.value.id : meterStruct.value;
  else if (dict === "meterDeterm")
    form.meterDeterm = typeof meterDeterm.value === "object" && meterDeterm.value !== null ? meterDeterm.value.id : meterDeterm.value;
  else if (dict === "distribution")
    form.distributionLaw = typeof distributionLaw.value === "object" && distributionLaw.value !== null ? distributionLaw.value.id : distributionLaw.value;
  else if (dict === "meterTypeByRate")
    form.meterTypeByRate = typeof meterTypeByRate.value === "object" && meterTypeByRate.value !== null ? meterTypeByRate.value.id : meterTypeByRate.value;
  else if (dict === "meterTypeByPeriod")
    form.meterTypeByPeriod = typeof meterTypeByPeriod.value === "object" && meterTypeByPeriod.value !== null ? meterTypeByPeriod.value.id : meterTypeByPeriod.value;
  else if (dict === "meterTypeByMember")
    form.meterTypeByMember = typeof meterTypeByMember.value === "object" && meterTypeByMember.value !== null ? meterTypeByMember.value.id : meterTypeByMember.value;
  else if (dict === "meterBehavior")
    form.meterBehavior = typeof meterBehavior.value === "object" && meterBehavior.value !== null ? meterBehavior.value.id : meterBehavior.value;
};

const validName = () => {
  const currentMeasure = typeof measure.value === "object" && measure.value !== null ? measure.value.id : measure.value;
  return !(
    currentMeasure !== undefined &&
    currentMeasure !== null &&
    currentMeasure > 0 &&
    form.name !== null &&
    form.name !== undefined &&
    form.name.trim().length !== 0
  );
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";

  api
    .post("", {
      method: "meter/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        let msg = error.response?.data?.error?.message || error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
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
      else if (dictName === "FD_MeterStruct")
        optionsStruct.value = response.data.result.records;
      else if (dictName === "FD_MeterDeterm")
        optionsDeterm.value = response.data.result.records;
      else if (dictName === "FD_DistributionLaw")
        optionsDistribution.value = response.data.result.records;
      else if (dictName === "FD_MeterBehavior")
        optionsBehavior.value = response.data.result.records;
      else if (dictName === "FD_MeterType") {
        optionsByRate.value = response.data.result.records;
        optionsByPeriod.value = response.data.result.records;
        optionsByMember.value = response.data.result.records;
      }
    });
};

onMounted(() => {
  getDict("FD_AccessLevel");
  getDict("FD_MeterStruct");
  getDict("FD_MeterDeterm");
  getDict("FD_DistributionLaw");
  getDict("FD_MeterBehavior");
  getDict("FD_MeterType");

  api
    .post("", {
      method: "measure/loadBase",
      params: [{}],
    })
    .then((response) => {
      measures.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
