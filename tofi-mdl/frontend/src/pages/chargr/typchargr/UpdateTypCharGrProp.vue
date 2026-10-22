<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <div v-if="form.propType === propTypeMeter || form.propType === propTypeRate">

          <TreeSelect
            v-model="form.typCharGrProp_measure"
            :label="$t('propMeasure')"
            :options="optPropMeasure"
            class="q-ma-md"
            node-key="key"
            @close="fnSelectPropMeasure"
          />


<!--
          <q-item-label
            class="text-grey-7"
            style="font-size: 0.8em; margin-top: 10px"
          >
            {{ $t("propMeasure") }}
          </q-item-label>
          <treeselect
            v-scroll
            :options="optPropMeasure"
            v-model="form.typCharGrProp_measure"
            :default-expand-level="1"
            :normalizer="normalizerPropMeasure"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @select="fnSelectPropMeasure"
          />
-->


          <TreeSelect
            v-model="form.typCharGrProp_measure"
            :label="$t('propMeasure')"
            :options="optPropMeasure"
            class="q-ma-md"
            node-key="key"
            @close="fnSelectPropMeasure"
          />


          <TreeSelect
            v-model="form.propVal_measure"
            :label="$t('measure')"
            :options="optMeasure"
            class="q-ma-md"
            node-key="key"
            @close="fnSelectMeasure"
          />



<!--
          <q-item-label
            v-if="form.typCharGrProp_measure === 0"
            class="text-red"
            style="font-size: 0.8em"
          >
            {{ $t("chooseProp") }}
          </q-item-label>

          <q-item-label
            class="text-grey-7"
            style="font-size: 0.8em; margin-top: 10px"
          >
            {{ $t("measure") }}
          </q-item-label>
          <treeselect
            v-scroll
            :options="optMeasure"
            v-model="form.propVal_measure"
            :default-expand-level="1"
            :normalizer="normalizerMeasure"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @select="fnSelectMeasure"
          />
-->

        </div>

        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="st"
          :options="options"
          :label="$t('storageType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelect"
        />

        <q-select
          v-if="form.storageType === 2"
          :dense="dense"
          :options-dense="dense"
          v-model="ft"
          :options="optionsFT"
          :label="$t('flatTable')"
          option-value="id"
          option-label="nameTable"
          map-options
          @update:model-value="fnSelectFT"
        />
      </q-card-section>

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
import { notifyError, pack } from "@/utils/jsutils";
import allConsts from "@/pages/all-consts";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  dta: {
    type: Object,
    default: () => ({}),
  },
  typCharGr: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.dta });
const lang = ref(props.lg);

const options = ref([]);
const optionsFT = ref([]);
const optPropMeasure = ref([]);
const optMeasure = ref([]);

const loading = ref(false);

const st = ref(props.dta.storageType === undefined ? null : props.dta.storageType);
const ft = ref(props.dta.flatTable === undefined ? null : props.dta.flatTable);

const propTypeMeter = allConsts.FD_PropType.meter;
const propTypeRate = allConsts.FD_PropType.rate;

const fnSelectPropMeasure = (v) => {
  if (v.propType !== allConsts.FD_PropType.measure) {
    form.typCharGrProp_measure = null;
  } else {
    form.typCharGrProp_measure = v.prop;
    api
      .post("", {
        method: "typ/loadMeasure",
        params: [v.prop],
      })
      .then((response) => {
        optMeasure.value = pack(response.data.result.records, "id");
      });
  }
};

const fnSelectMeasure = (v) => {
  form.propVal_measure = v.propval;
};

const normalizerPropMeasure = (node) => {
  return {
    id: node.prop,
    label: node.name,
  };
};

const normalizerMeasure = (node) => {
  return {
    id: node.propval,
    label: node.name,
  };
};

const fnSelect = () => {
  form.storageType = st.value?.id || st.value;
  if (form.storageType !== allConsts.FD_StorageType.flat) {
    form.flatTable = null;
    ft.value = null;
  }
};

const fnSelectFT = () => {
  form.flatTable = ft.value?.id || ft.value;
};

const validSave = () => {
  if (form.storageType !== allConsts.FD_StorageType.flat) {
    return !(form.storageType > 0 && form.typCharGrProp_measure !== 0);
  } else {
    return !(form.storageType > 0 && form.typCharGrProp_measure !== 0 && ft.value !== null);
  }
};

const onOKClick = () => {
  form.storageType = typeof st.value === "object" && st.value !== null ? st.value.id : st.value;

  if (form.storageType !== allConsts.FD_StorageType.flat) {
    form.flatTable = null;
  } else {
    form.flatTable = typeof ft.value === "object" && ft.value !== null ? ft.value.id : ft.value;
  }

  api
    .post("", {
      method: "typ/updateTypCharGrProp",
      params: [form],
    })
    .then(() => {
      onDialogOK({ res: true });
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    });
};

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_StorageType" }],
    })
    .then((response) => {
      options.value = response.data.result.records;
    });

  if (form.propType === propTypeMeter || form.propType === propTypeRate) {
    api
      .post("", {
        method: "typ/loadPropMeasure",
        params: [props.typCharGr],
      })
      .then((response) => {
        optPropMeasure.value = pack(response.data.result.records, "id");
      });

    let meas = form.typCharGrProp_measure == null ? 0 : form.typCharGrProp_measure;

    api
      .post("", {
        method: "typ/loadMeasure",
        params: [meas],
      })
      .then((response) => {
        optMeasure.value = pack(response.data.result.records, "id");
      });
  }

  loading.value = true;
  api
    .post("", {
      method: "flatTable/loadTables",
      params: [{}],
    })
    .then((response) => {
      optionsFT.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.response ? error.response.data.error.message : error.message;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
