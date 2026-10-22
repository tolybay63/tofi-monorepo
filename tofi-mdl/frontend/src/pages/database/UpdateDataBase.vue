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
        <!-- code -->
        <q-input
          v-model="form.cod"
          :label="$t('code')"
          label-color="blue"
          :placeholder="$t('msgCodeGen')"
          dense
        />

        <!-- DataBaseType -->
        <q-select
          autofocus
          v-model="dbType"
          :options="optionsDB"
          :label="$t('dataBaseType')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelect"
        />

        <!-- modelName -->
        <q-input
          class="q-mt-md"
          dense
          v-model="form.modelName"
          :label="$t('modelNameLabel')"
          label-color="blue"
        />

        <!-- name -->
        <q-input
          class="q-mt-md"
          dense
          v-model="form.name"
          @blur="onBlurName"
          :label="$t('fldName')"
          label-color="blue"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fullName-->
        <q-input
          class="q-mt-md"
          dense
          v-model="form.fullName"
          :label="$t('fldFullName')"
          label-color="blue"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- cmt -->
        <q-input
          dense
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
          label-color="blue"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          dense
          :disable="validOk()"
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
const dbType = ref(props.data.dataBaseType === null ? "" : props.data.dataBaseType);
const optionsDB = ref([]);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelect = () => {
  form.dataBaseType = typeof dbType.value === "object" && dbType.value !== null ? dbType.value.id : dbType.value;
};

const validOk = () => {
  return (
    form.dataBaseType === undefined ||
    form.dataBaseType === null ||
    form.modelName === undefined ||
    form.modelName === null ||
    form.name === undefined ||
    form.name === null ||
    form.name === "" ||
    form.fullName === undefined ||
    form.fullName === null ||
    form.fullName === ""
  );
};

const onOKClick = () => {
  let err = false;
  const method = props.mode === "ins" ? "insert" : "update";

  form.permis = typeof dbType.value === "object" && dbType.value !== null ? dbType.value.id : dbType.value;

  api
    .post("", {
      id: form.id,
      method: "database/" + method,
      params: [form],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
        err = false;
      },
      (error) => {
        err = true;
        let msg = error.response?.data?.error?.message || error.message;
        if (msg.includes("i_apps_i_permis")) {
          msg = "Идентификатор сервиса уникален";
        }
        notifyError(msg);
      }
    )
    .finally(() => {
      // dialogRef.value?.hide() is managed internally by onDialogOK / onDialogCancel
    });
};

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DataBaseType" }],
    })
    .then((response) => {
      optionsDB.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
