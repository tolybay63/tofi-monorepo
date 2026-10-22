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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-item-section v-if="isChild">
          {{ $t("baseMeasure") }}: {{ parentName }}
        </q-item-section>

        <!-- name -->
        <q-input
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- cod -->
        <q-input
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />
        <!-- accessLevel -->
        <q-select
          v-model="al"
          :options="options"
          :label="$t('accessLevel')"
          option-value="id"
          :option-label="dictText()"
          map-options
          @update:model-value="fnSelect"
        />

        <!-- kFromBase -->
        <q-input
          v-if="isChild"
          v-model="form.kFromBase"
          :label="$t('kfcMeasure')"
          type="number"
          input-class="text-right"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
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
          :disable="validName()"
        />
        <q-btn
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
  mode: String,
  isChild: Boolean,
  parentName: String,
  data: {
    type: Object,
    default: () => ({}),
  },
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const options = ref([]);
const al = ref(props.data.accessLevel === 0 ? 1 : props.data.accessLevel);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelect = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const dictText = () => {
  return "text";
};

const validName = () => {
  return (
    !form.name ||
    !form.fullName ||
    parseFloat(form.kFromBase) <= 0
  );
};

const onOKClick = () => {
  delete form["accessLevel_text"];
  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;

  api
    .post("", {
      method: "measure/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
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
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      options.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
