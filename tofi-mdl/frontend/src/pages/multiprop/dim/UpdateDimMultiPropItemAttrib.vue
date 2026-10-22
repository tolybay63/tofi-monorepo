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
        <!-- AttribValType -->
        <q-select
          v-model="avt"
          :options="optAVT"
          :label="$t('attribValType')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectValType"
          :rules="[(val) => !!val || $t('req')]"
        />

        <!-- format -->
        <q-input
          dense
          v-model="form.format"
          :label="$t('formatValue')"
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
const avt = ref(props.data.attribValType === 0 ? 1 : props.data.attribValType);
const optAVT = ref([]);

const fnSelectValType = () => {
  form.attribValType = typeof avt.value === "object" && avt.value !== null ? avt.value.id : avt.value;
};

const validSave = () => {
  return !form.attribValType;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insDimMultiPropItemAttrib" : "updDimMultiPropItemAttrib";

  form.attribValType = typeof avt.value === "object" && avt.value !== null ? avt.value.id : avt.value;

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
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AttribValType" }],
    })
    .then((response) => {
      optAVT.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
