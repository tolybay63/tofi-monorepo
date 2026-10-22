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
        <!-- cod -->
        <q-input
          :dense="dense"
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />
        <!-- name -->
        <q-input
          :dense="dense"
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          :dense="dense"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- dbeg-->
        <q-input
          :dense="dense"
          type="date"
          stack-label
          v-model="form.dbeg"
          :label="$t('fldDbeg')"
        />
        <!-- dend-->
        <q-input
          :dense="dense"
          type="date"
          stack-label
          v-model="form.dend"
          :label="$t('fldDend')"
        />

        <!-- accessLevel -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectAL"
        />

        <!-- periodNameTml -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="tml"
          :options="optTml"
          :label="$t('periodTml')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectPeriodNameTml"
        />

        <!-- cmt -->
        <q-input
          :dense="dense"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
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
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const optAL = ref([]);
const al = ref(props.data.accessLevel);
const optTml = ref([]);
const tml = ref(props.data.periodNameTml);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelectAL = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const fnSelectPeriodNameTml = () => {
  form.periodNameTml = typeof tml.value === "object" && tml.value !== null ? tml.value.id : tml.value;
};

const validSave = () => {
  if (!form.name) return true;
  if (form.name.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.periodNameTml = typeof tml.value === "object" && tml.value !== null ? tml.value.id : tml.value;

  api
    .post("", {
      id: form.id,
      method: "dimperiod/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response?.data?.error?.message || error.message);
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
      optAL.value = response.data.result.records;
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PeriodNameTml" }],
    })
    .then((response) => {
      optTml.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
