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
        <!--  minVal  -->
        <q-input
          v-model="form.minVal"
          :label="$t('minValGr')"
          type="number"
          dense autofocus
        />

        <!-- isMinValOpen -->
        <q-toggle
          class="q-mt-md"
          v-model="form.isMinValOpen"
          :label="$t('isMinValOpen')"
          dense
        />

        <!--  maxVal  -->
        <q-input
          class="q-mt-md"
          v-model="form.maxVal"
          :label="$t('maxValGr')"
          type="number"
          dense
        />

        <!-- isMaxValOpen -->
        <q-toggle
          class="q-mt-md"
          v-model="form.isMaxValOpen"
          :label="$t('isMaxValOpen')"
          dense
        />

        <!-- cmt -->
        <q-input
          class="q-mt-md"
          v-model="form.cmt"
          dense type="textarea"
          :label="$t('fldCmt')"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>
        <q-btn
          color="primary"
          icon="cancel" dense
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

const validSave = () => {
  return Boolean(form.minVal && form.maxVal);
};

const onOKClick = () => {
  loading.value = true;
  let err = false;
  const method = props.mode === "ins" ? "insertScaleVal" : "updateScaleVal";

  api
    .post("", {
      id: form.id,
      method: "scale/" + method,
      params: [form],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
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
