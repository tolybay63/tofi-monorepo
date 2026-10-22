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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" dense>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- CubeSDimType -->
        <q-select
          autofocus
          v-model="form.cubeSDimType"
          :options="optCDT"
          :label="$t('cubeSDimType')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          class="q-mb-md"
          :disable="mode === 'upd'"
          @update:model-value="fnSelectCDT"
        />

        <!-- dimPeriod -->
        <div v-if="form.cubeSDimType === dimTypePeriod">
          <q-select
            autofocus
            v-model="form.dimPeriod"
            :options="optDimPeriod"
            :label="$t('dimPeriod')"
            option-value="id"
            option-label="name"
            map-options
            dense
            options-dense
            class="q-mb-md"
            @update:model-value="fnSelectPeriod"
          />
        </div>

        <!-- DimPropType -->
        <div v-if="form.cubeSDimType === 2">
          <q-select
            v-model="dpt"
            :options="optDPT"
            :label="$t('dimPropType')"
            option-value="id"
            option-label="text"
            map-options
            dense
            options-dense
            class="q-mb-md"
            :disable="mode === 'upd'"
            @update:model-value="fnSelectDPT"
          />
        </div>

        <div v-if="form.cubeSDimType === dimTypeProp">

          <TreeSelect
            :disabled="mode==='upd'"
            v-model="form['dimProp']"
            :label="$t('dimProp')"
            :options="optDimProp"
            class="q-ma-md"
            node-key="key"
            @select="fnSelectDimProp"
          />

<!--
          <q-item-label
            :class="form.dimProp === 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px"
            class="row"
          >
            {{ $t("dimProp") }}
            <q-space />
            <q-icon
              name="error"
              v-if="form.dimProp === 0"
              color="red-10"
              size="24px"
            />
          </q-item-label>

          <treeselect
            :options="optDimProp"
            v-model="form.dimProp"
            :normalizer="normalizerDimProp"
            :placeholder="$t('select')"
            :noResultsText="$t('noResult')"
            :noChildrenText="$t('noChilds')"
            :noOptionsText="$t('noResult')"
            :default-expand-level="1"
            autofocus
            :disabled="mode === 'upd'"
            @select="fnSelectDimProp"
          />

          <q-item-label
            v-if="form.dimProp === 0"
            class="text-red-10"
            style="font-size: 0.8em"
          >
            {{ $t("chooseDimProp") }}
          </q-item-label>
-->

        </div>

        <div v-if="form.cubeSDimType === dimTypeObj">

          <TreeSelect
            :disabled="mode==='upd'"
            v-model="form['dimObj']"
            :label="$t('dimObj')"
            :options="optDimObj"
            class="q-ma-md"
            node-key="key"
            @select="fnSelectDimObj"
          />


<!--
          <q-item-label
            :class="form.dimObj === 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px"
            class="row"
          >
            {{ $t("dimObj") }}
            <q-space />
            <q-icon
              name="error"
              v-if="form.dimObj === 0"
              color="red-10"
              size="24px"
            />
          </q-item-label>

          <treeselect
            :options="optDimObj"
            v-model="form.dimObj"
            :normalizer="normalizerDimObj"
            :placeholder="$t('select')"
            :noResultsText="$t('noResult')"
            :noChildrenText="$t('noChilds')"
            :noOptionsText="$t('noResult')"
            :default-expand-level="1"
            autofocus
            :disabled="mode === 'upd'"
            @select="fnSelectDimObj"
          />
          <q-item-label
            v-if="form.dimObj === 0"
            class="text-red-10"
            style="font-size: 0.8em"
          >
            {{ $t("chooseDimObj") }}
          </q-item-label>
-->


        </div>

        <!-- name -->
        <q-input
          v-model="form.name"
          dense
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <q-input
          v-model="form.dOrg"
          :label="$t('dOrg')"
          dense
          type="date"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          dense
          :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onDialogCancel"
          dense
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
import allConsts from "@/pages/all-consts.js";

import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  typeProp: [Number, String],
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const optCDT = ref([]);
const optDimPeriod = ref([]);
const optDimProp = ref([]);
const optDimObj = ref([]);
const dpt = ref(props.data.dimPropType ? props.data.dimPropType : undefined);
const optDPT = ref([]);
const loading = ref(false);

const dimTypePeriod = allConsts.FD_CubeSDimType.period;
const dimTypeProp = allConsts.FD_CubeSDimType.prop;
const dimTypeObj = allConsts.FD_CubeSDimType.obj;

const loadDimPropForSelect = (cubeS, dimPropType, modeVal) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimPropForSelect",
      params: [cubeS, dimPropType, modeVal],
    })
    .then((response) => {
      optDimProp.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnSelectDPT = (v) => {
  dpt.value = v;
  form.dimPropType = v?.id || v;
  loadDimPropForSelect(form.cubeS, form.dimPropType, props.mode);
};

const fnSelectCDT = (v) => {
  form.cubeSDimType = v?.id || v;
};

const fnSelectPeriod = (v) => {
  form.dimPeriod = v?.id || v;
  form.name = v?.name;
};

const fnSelectDimProp = (v) => {
  form.name = v.name;
};

const normalizerDimProp = (node) => {
  return {
    id: node.dimProp,
    label: node.name,
  };
};

const fnSelectDimObj = (v) => {
  form.name = v.name;
};

const normalizerDimObj = (node) => {
  return {
    id: node.dimObj,
    label: node.name,
  };
};

const validSave = () => {
  return (
    form.name === "" ||
    !form.cubeSDimType ||
    (form.cubeSDimType === dimTypeProp && !form.dimProp) ||
    (form.cubeSDimType === dimTypeObj && form.dimObj)
  );
};

const onOKClick = () => {
  let err = false;
  form.mode = props.mode;
  api
    .post("", {
      method: "cubes/saveCubeDim",
      params: [form],
    })
    .then((response) => {
      onDialogOK(response.data.result.records[0]);
      notifySuccess(proxy?.$t("success"));
    })
    .catch((error) => {
      err = true;
      notifyError(proxy?.$t(error.response.data.error.message));
    })
    .finally(() => {
      // onDialogOK implicitly handles closing, but keeping consistent check if needed
    });
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_CubeSDimType" }],
    })
    .then((response) => {
      optCDT.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimPropType" }],
    })
    .then((response) => {
      let notProp1 = 0;
      let notProp2 = 0;
      if (props.typeProp === 1) {
        notProp1 = 3;
        notProp2 = 4;
      }
      if (props.typeProp === 3 || props.typeProp === 4) {
        notProp1 = 1;
        notProp2 = 0;
      }

      optDPT.value = response.data.result.records.filter((it) => {
        return it["id"] !== notProp1 && it["id"] !== notProp2;
      });
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimPeriod",
      params: [],
    })
    .then((response) => {
      optDimPeriod.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimObj",
      params: [],
    })
    .then((response) => {
      optDimObj.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      loading.value = false;
    });

  if (form.dimPropType > 0) {
    loadDimPropForSelect(form.cubeS, form.dimPropType, props.mode);
  }
});
</script>

<style scoped></style>
