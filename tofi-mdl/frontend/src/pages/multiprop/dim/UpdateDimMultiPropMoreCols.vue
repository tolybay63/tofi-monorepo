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

      <q-card-section>
        <!-- title -->
        <q-input
          v-model="form.title"
          autofocus
          dense
          :label="$t('titleDop')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="save"
          dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validName()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>
        <q-btn
          color="primary"
          icon="cancel"
          dense
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from "vue";
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
const loading = ref(false);

const validName = () => {
  if (!form.title) return true;
  if (form.title.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  loading.value = true;
  const method = props.mode === "ins" ? "insertMoreCols" : "updateMoreCols";

  api
    .post("", {
      id: form.id,
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
      loading.value = false;
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};
</script>

<style scoped></style>
