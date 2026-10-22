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
        <div v-if="form.propType === pt_meter || form.propType === pt_rate">


          <TreeSelect
            v-model="form.relTypCharGrProp_measure"
            :label="$t('obj')"
            :options="optPropMeasure"
            class="q-ma-md"
            node-key="key"
            @close="fnSelectPropMeasure"
          />

<!--
          <q-item-label
            :class="form.relTypCharGrProp_measure === 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px"
            class="row"
          >
            {{ $t("propMeasure") }}
            <q-space />
            <q-icon
              name="error"
              v-if="form.relTypCharGrProp_measure === 0"
              color="red-10"
              size="24px"
            />
          </q-item-label>
          <treeselect
            v-scroll
            :options="optPropMeasure"
            v-model="form.relTypCharGrProp_measure"
            :default-expand-level="1"
            :normalizer="normalizerPropMeasure"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @select="fnSelectPropMeasure"
          />
          <q-item-label
            v-if="form.relTypCharGrProp_measure === 0"
            class="text-red-10"
            style="font-size: 0.8em"
          >
            {{ $t("chooseProp") }}
          </q-item-label>
-->

          <TreeSelect
            v-model="form.propVal_measure"
            :label="$t('obj')"
            :options="optMeasure"
            class="q-ma-md"
            node-key="key"
            @close="fnSelectMeasure"
          />

<!--
          <q-item-label class="text-grey-7" style="font-size: 0.8em; margin-top: 10px">
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
  relTypCharGr: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.dta });

const options = ref([]);
const optionsFT = ref([]);
const optPropMeasure = ref([]);
const optMeasure = ref([]);

const loading = ref(false);

const st = ref(props.dta.storageType === undefined ? null : props.dta.storageType);
const ft = ref(props.dta.flatTable === undefined ? null : props.dta.flatTable);

const pt_meter = allConsts.FD_PropType.meter;
const pt_rate = allConsts.FD_PropType.rate;

const fnSelectPropMeasure = (v) => {
  if (v.propType !== allConsts.FD_PropType.measure) {
    form.relTypCharGrProp_measure = null;
  } else {
    form.relTypCharGrProp_measure = v.prop;
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

/*
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
*/

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
    return !(form.storageType > 0 && form.relTypCharGrProp_measure !== 0);
  } else {
    return !(form.storageType > 0 && form.relTypCharGrProp_measure !== 0 && ft.value !== null);
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
      method: "reltyp/updateRelTypCharGrProp",
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
  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_StorageType" }],
    })
    .then((response) => {
      options.value = response.data.result.records;
    });

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

  if (form.propType === pt_meter || form.propType === pt_rate) {
    api
      .post("", {
        method: "reltyp/loadPropMeasure",
        params: [props.relTypCharGr],
      })
      .then((response) => {
        optPropMeasure.value = pack(response.data.result.records, "id");
      });

    let meas = form.relTypCharGrProp_measure == null ? 0 : form.relTypCharGrProp_measure;

    api
      .post("", {
        method: "reltyp/loadMeasure",
        params: [meas],
      })
      .then((response) => {
        optMeasure.value = pack(response.data.result.records, "id");
      });
  }
});
</script>

<style scoped></style>
