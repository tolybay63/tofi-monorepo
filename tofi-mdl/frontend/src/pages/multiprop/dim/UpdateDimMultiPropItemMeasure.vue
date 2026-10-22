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
        <!-- measure -->
        <q-select
          v-model="measure"
          :options="measures"
          :label="$t('measure')"
          option-value="id"
          option-label="name"
          map-options
          dense
          @update:model-value="fnSelMeasure"
        />

        <!-- format -->
        <q-select
          class="q-my-lg"
          v-model="vf"
          :options="optVF"
          :label="$t('visualFormat')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectVF"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const visible = ref(false);

const vf = ref(props.data.visualFormat);
const optVF = ref([]);
const measures = ref([]);
const measure = ref(props.data.measure);

const fnSelMeasure = () => {
  form.measure = typeof measure.value === "object" && measure.value !== null ? measure.value.id : measure.value;
};

const fnSelectVF = () => {
  form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;
};

const validSave = () => {
  return !form.measure;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insDimMultiPropItemMeasure" : "updDimMultiPropItemMeasure";

  form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;
  form.measure = typeof measure.value === "object" && measure.value !== null ? measure.value.id : measure.value;

  api
    .post("", {
      method: "dimMultiProp/" + method,
      params: [form],
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

onMounted(() => {
  visible.value = true;
  api
    .post("", {
      method: "measure/loadBase",
      params: [{}],
    })
    .then((response) => {
      measures.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_VisualFormat" }],
    })
    .then((response) => {
      optVF.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
