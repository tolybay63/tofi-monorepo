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
          Родитель: {{ parentName }}
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
          option-label="text"
          map-options
          @update:model-value="fnSelect"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
          type="textarea"
          label="Комментарий"
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

const validName = () => {
  if (!form.name) return true;
  if (form.name.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertHardMR" : "updateHardMR";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;

  api
    .post("", {
      method: "meterrate/" + method,
      params: [{ rec: form }],
    })
    .then(
      () => {
        onDialogOK({ res: true });
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
