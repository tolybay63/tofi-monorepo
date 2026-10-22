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
        <!-- factor -->
        <q-select
          :dense="dense"
          v-model="factor"
          :options="factors"
          :label="$t('factor')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelFactor"
          :disable="mode === 'upd'"
        />

        <!-- isReq -->
        <q-toggle
          style="margin-left: 5px; margin-top: 15px"
          :dense="dense"
          v-model="form.isReq"
          :label="$t('isReq')"
        />
        <!-- isUniq -->
        <q-toggle
          style="margin-left: 15px; margin-top: 15px"
          :dense="dense"
          v-model="form.isUniq"
          :label="$t('isUniq')"
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
  lg: [String, Object],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const factors = ref([]);
const factor = ref(props.data.factor);

const fnSelFactor = () => {
  form.factor = typeof factor.value === "object" && factor.value !== null ? factor.value.id : factor.value;
};

const validSave = () => {
  return form.factor === 0;
};

const onOKClick = () => {
  let err = false;
  const method =
    props.mode === "ins"
      ? "insertTypClusterFactor"
      : "updateTypClusterFactor";

  api
    .post("", {
      method: "cluster/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let errMsg = error.response?.data?.error?.message || error.message;
        notifyError(errMsg);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  api
    .post("", {
      method: "cluster/loadFactors",
      params: [form.typ, props.mode],
    })
    .then((response) => {
      factors.value = response.data.result.records;
      factors.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });
});
</script>

<style scoped></style>
