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
        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cmt -->
        <q-input
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
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
import { ref, reactive, getCurrentInstance } from "vue";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "../../utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({ id: 0, name: "", fullName: "", cmt: null })
  },
  mode: {
    type: String,
    default: "ins"
  }
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();
const $q = useQuasar();

const dialog = ref(null);

// Безопасная инициализация формы с защитой от undefined
const form = reactive({
  id: props.data?.id || 0,
  name: props.data?.name || "",
  fullName: props.data?.fullName || "",
  cmt: props.data?.cmt || null
});

const options = ref([]);

const onBlurName = () => {
  if (form.name) {
    if (form.fullName === "")
      form.fullName = form.name.trim();
  }
};

const validName = () => {
  if (!form.name) return true;
  else if (form.name.trim().length === 0) return true;
  return false;
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
      id: form.id,
      method: "role/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        emit("ok", response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response.data.error.message);
      }
    )
    .finally(() => {
      hide();
    });
};

const onCancelClick = () => {
  hide();
};

// Экспортируем методы для Quasar Dialog plugin
defineExpose({
  show,
  hide
});
</script>
