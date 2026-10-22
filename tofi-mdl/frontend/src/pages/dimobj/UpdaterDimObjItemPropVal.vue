<!-- DimObjItemProp (lt < 5) -->
<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <!--  prop -->
        <q-select
          dense
          options-dense
          v-model="prop"
          option-value="id"
          :options="optProp"
          :label="$t('prop')"
          option-label="name"
          map-options
          @update:model-value="fnSelectProp"
        />

        <!-- propStatus -->
        <q-select
          :disable="sDisable"
          dense
          options-dense
          v-model="s"
          option-value="id"
          :options="optS"
          :label="$t('statusFactor')"
          option-label="name"
          map-options
          @update:model-value="fnSelectSt"
        />
        <!-- propStatusMissing -->
        <q-select
          :disable="sDisable"
          dense
          options-dense
          v-model="sMis"
          option-value="id"
          option-label="text"
          :options="optSmis"
          :label="$t('statusMissing')"
          map-options
          @update:model-value="fnSelectStMis"
        />

        <!-- propProvider -->
        <q-select
          :disable="qDisable"
          dense
          options-dense
          v-model="q"
          :options="optQ"
          :label="$t('providerTyp')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectQ"
          @click="fnClickQ"
        />
        <!-- propProviderMissing -->
        <q-select
          :disable="qDisable"
          dense
          options-dense
          v-model="qMis"
          :options="optQmis"
          :label="$t('providerMissing')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectQMis"
        />
      </q-card-section>

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
import { notifyError, notifyInfo, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  doi: [Number, String],
  linkType: [Number, String],
  doiType: [Number, String],
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });

const optProp = ref([]);
const prop = ref(props.data.prop);

const optS = ref([]);
const s = ref(props.data.propStatus);

const optSmis = ref([]);
const sMis = ref(props.data.propStatusMissing);

const optQ = ref([]);
const q = ref(props.data.propProvider);

const optQmis = ref([]);
const qMis = ref(props.data.propProviderMissing);

const sDisable = ref(true);
const qDisable = ref(true);
const visible = ref(false);

const fnClickQ = () => {
  notifyInfo("Запрос к другой базе!");
};

const loadPropStatus = (doiVal, propVal, modeVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadPropStatus",
      params: [doiVal, propVal, modeVal],
    })
    .then((response) => {
      optS.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const fnSelectProp = () => {
  const selectedProp = prop.value;
  form.prop = typeof selectedProp === "object" && selectedProp !== null ? selectedProp.id : selectedProp;

  sDisable.value = !selectedProp?.statusfactor;
  qDisable.value = !selectedProp?.providertyp;

  if (!sDisable.value) {
    loadPropStatus(props.doi, form.prop, props.mode);
  } else {
    optS.value = [];
  }
  if (!qDisable.value) {
    // provider logic if needed
  } else {
    optQ.value = [];
  }
};

const fnSelectSt = () => {
  form.propStatus = s.value?.id || s.value;
};

const fnSelectStMis = () => {
  form.propStatusMissing = sMis.value?.id || sMis.value;
};

const fnSelectQ = () => {
  form.propProvider = q.value?.id || q.value;
};

const fnSelectQMis = () => {
  form.propProviderMissing = qMis.value?.id || qMis.value;
};

const validSave = () => {
  return !(
    (form.prop && !sDisable.value && form.propStatus && form.propStatusMissing) ||
    (form.prop && sDisable.value)
  );
};

const onOKClick = () => {
  visible.value = true;

  const method = props.mode === "ins" ? "insertDOIprop" : "updateDOIprop";
  form.propStatusMissing = typeof sMis.value === "object" && sMis.value !== null ? sMis.value.id : sMis.value;
  form.propProviderMissing = typeof qMis.value === "object" && qMis.value !== null ? qMis.value.id : qMis.value;
  form.dimObjItem = props.doi;

  api
    .post("", {
      method: "dimobjitem/" + method,
      params: [form],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response?.data?.error?.message || error.message);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

onMounted(() => {
  visible.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PropStatusMissing" }],
    })
    .then((response) => {
      optSmis.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PropProviderMissing" }],
    })
    .then((response) => {
      optQmis.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadOptProp",
      params: [props.doi],
    })
    .then((response) => {
      optProp.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });

  if (props.mode === "upd") {
    loadPropStatus(props.doi, form.prop, props.mode);
  }
});
</script>

<style scoped></style>
