<!-- DimObjItemProp (lt > 4) -->

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
          :dense="dense"
          :options-dense="dense"
          v-model="prop"
          :options="optProp"
          :label="$t('prop')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectProp"
        />

        <!-- propStatus -->
        <q-select
          :disable="!form.statusFactor"
          :dense="dense"
          :options-dense="dense"
          v-model="s"
          :options="optS"
          :label="$t('statusFactor')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectSt"
        />
        <!-- propStatusMissing -->
        <q-select
          :disable="!form.statusFactor"
          :dense="dense"
          :options-dense="dense"
          v-model="sMis"
          :options="optSmis"
          :label="$t('statusMissing')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectStMis"
        />

        <!-- propProvider -->
        <q-select
          :disable="!form.providerTyp"
          :dense="dense"
          :options-dense="dense"
          v-model="q"
          :options="optQ"
          :label="$t('providerTyp')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectQ"
        />
        <!-- propProviderMissing -->
        <q-select
          :disable="!form.providerTyp"
          :dense="dense"
          :options-dense="dense"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  doi: [Number, String],
  doiType: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);

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

const visible = ref(false);

const loadPropStatus = (propVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadPropStatus",
      params: [propVal],
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

const loadPropProvider = (propVal) => {
  visible.value = true;
  api
    .post("", {
      method: "dimobjitem/loadPropProvider",
      params: [propVal],
    })
    .then((response) => {
      optQ.value = response.data.result.records;
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

  if (selectedProp?.rtm > 0) {
    form.relTypMember = selectedProp.rtm;
  }

  if (selectedProp?.statusFactor) {
    loadPropStatus(form.prop);
  } else {
    optS.value = [];
  }
  if (selectedProp?.providerTyp) {
    loadPropProvider(form.prop);
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
  return false;
};

const onOKClick = () => {
  visible.value = true;

  const method = props.mode === "ins" ? "insertDOIprop" : "updateDOIprop";
  form.propStatusMissing = typeof sMis.value === "object" && sMis.value !== null ? sMis.value.id : sMis.value;
  form.propProviderMissing = typeof qMis.value === "object" && qMis.value !== null ? qMis.value.id : qMis.value;
  form.dimObjItem = props.doi;

  api
    .post("", {
      id: form.id,
      method: "dimobjitem/" + method,
      params: [form],
    })
    .then(
      () => {
        onDialogOK({ res: true });
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
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
