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
        <q-input
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
          dense
        />
        <!-- name -->
        <q-input
          v-model="form.name"
          dense
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          dense
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- AccessLevel -->
        <q-select
          v-model="al"
          :options="optionsLevel"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelect"
        />
        <!-- AttribValType -->
        <q-select
          v-model="valType"
          :options="optionsValType"
          :label="$t('attribValType')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectValType"
          :rules="[(val) => !!val || $t('req')]"
        />

        <q-toggle
          style="margin-left: 10px; margin-top: 10px"
          dense
          v-model="form.isMultiLang"
          :label="$t('multiLang').replaceAll('-', '')"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
          dense
          type="textarea"
          :label="$t('fldCmt')"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const optionsLevel = ref([]);
const optionsValType = ref([]);

const al = ref(props.data?.accessLevel === 0 ? 1 : props.data?.accessLevel);
const valType = ref(props.data?.attribValType === 0 ? 1 : props.data?.attribValType);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelect = () => {
  form.accessLevel = al.value?.id || al.value;
};

const fnSelectValType = () => {
  form.attribValType = valType.value?.id || valType.value;
};

const validSave = () => {
  return !form.name || !form.fullName || !form.attribValType;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;

  api
    .post("", {
      id: form.id,
      method: "attrib/" + method,
      params: [{ rec: form }],
    })
    .then((response) => {
      onDialogOK(response.data.result.records[0]);
      notifySuccess(proxy?.$t("success"));
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    });
};

onMounted(() => {
  api
    .post("", { method: "dict/load", params: [{ dict: "FD_AccessLevel" }] })
    .then((response) => {
      optionsLevel.value = response.data.result.records;
    });

  api
    .post("", { method: "dict/load", params: [{ dict: "FD_AttribValType" }] })
    .then((response) => {
      optionsValType.value = response.data.result.records;
    });
});
</script>
