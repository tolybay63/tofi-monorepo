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
      <q-bar class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="st"
          :options="options"
          :label="$t('storageType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelect"
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
import { ref, reactive, onMounted } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const options = ref([]);
const st = ref(props.data.storageType === undefined ? null : props.data.storageType);

const fnSelect = () => {
  form.storageType = st.value?.id || st.value;
};

const validSave = () => {
  return !(form.storageType > 0);
};

const onOKClick = () => {
  form.storageType = typeof st.value === "object" && st.value !== null ? st.value.id : st.value;

  api
    .post("", {
      method: "reltyp/updateRelTypCharGrMultiProp",
      params: [form],
    })
    .then(() => {
      onDialogOK({ res: true });
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    });
};

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_StorageType" }],
    })
    .then((response) => {
      options.value = response.data.result.records;
      options.value.splice(1, 1);
    });
});
</script>
