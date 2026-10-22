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
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          class="q-mb-md"
          @update:model-value="fnSelectAL"
        />

        <!-- CubeSType -->
        <q-select
          v-model="ct"
          :options="optCT"
          :label="$t('cubesType')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          class="q-mb-md"
          @update:model-value="fnSelectCT"
        />

        <q-input
          v-model="form.dOrg"
          :label="$t('dOrg')"
          dense
          type="date"
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
const al = ref(props.data.accessLevel === 0 ? 1 : props.data.accessLevel);
const optAL = ref([]);
const ct = ref(props.data.cubeSType === 0 ? 1 : props.data.cubeSType);
const optCT = ref([]);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelectAL = () => {
  form.accessLevel = al.value?.id || al.value;
};

const fnSelectCT = () => {
  form.cubeSType = ct.value?.id || ct.value;
};

const validSave = () => {
  return (
    form.name === "" ||
    form.fullName === "" ||
    !form.accessLevel ||
    !form.cubeSType
  );
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertCube" : "updateCube";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.cubeSType = typeof ct.value === "object" && ct.value !== null ? ct.value.id : ct.value;

  api
    .post("", {
      id: form.id,
      method: "cubes/" + method,
      params: [form],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response?.data?.error?.message || error.message);
      }
    );
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
      params: [{ dict: "FD_CubeSType" }],
    })
    .then((response) => {
      optCT.value = response.data.result.records;
    })
    .catch((error) => {
      console.error(error.message);
      if (error.response?.data?.error) {
        notifyError(proxy?.$t(error.response.data.error.message));
      } else {
        notifyError(error.message);
      }
    });
});
</script>
