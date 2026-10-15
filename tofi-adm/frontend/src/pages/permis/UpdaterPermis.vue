<template>
  <q-dialog
    ref="dialog"
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
          {{ $t("parent") }}: {{ parentName }}
        </q-item-section>

        <!-- name -->
        <q-input
          :model-value="form.text"
          v-model="form.text"
          autofocus
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- id -->
        <q-input
          v-model="form.id"
          :model-value="form.id"
          :label="$t('target')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          :disable="mode === 'upd'"
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
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {getCurrentInstance, reactive, ref} from "vue";
import {api} from "@/boot/axios";
import {notifyError, notifySuccess} from "../../utils/jsutils";

const props = defineProps({
  mode: String,
  isChild: Boolean,
  parentName: String,
  data: Object
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();

const dialog = ref(null);
const form = reactive({ ...props.data });

const validName = () => {
  return !!(form.text === "" || form.id === "");
};

const show = () => {
  dialog.value?.show();
};

const hide = () => {
  dialog.value?.hide();
};

const onDialogHide = () => {
  emit("hide");
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";

  api
    .post("", {
      method: "permis/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        emit("ok", response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message);
        notifyError(msg);
      }
    )
    .finally(() => {
      hide();
    });
};

const onCancelClick = () => {
  hide();
};

defineExpose({
  show,
  hide
});
</script>
