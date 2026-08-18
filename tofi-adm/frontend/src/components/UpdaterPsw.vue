<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-down"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("changePsw") }}</div>
      </q-bar>

      <q-card-section>
        <q-input
          v-if="!force"
          dense
          v-model="form.passwdold"
          label="Старый пароль *"
          autofocus
          :type="isPwd ? 'password' : 'text'"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
          <template v-slot:append>
            <q-icon
              dense
              :name="isPwd ? 'visibility_off' : 'visibility'"
              class="cursor-pointer"
              @click="isPwd = !isPwd"
            />
          </template>
        </q-input>

        <q-input
          dense
          v-model="form.passwd"
          label="Новый пароль *"
          :type="isPwd ? 'password' : 'text'"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
          <template v-slot:append>
            <q-icon
              dense
              :name="isPwd ? 'visibility_off' : 'visibility'"
              class="cursor-pointer"
              @click="isPwd = !isPwd"
            />
          </template>
        </q-input>

        <q-input
          dense
          v-model="form.passwd2"
          label="Подтверждение *"
          :type="isPwd ? 'password' : 'text'"
          :rules="[(val) => pswTest(val) || $t('errorPassword')]"
        >
        </q-input>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
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
import { ref, reactive } from "vue";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios.js";

const props = defineProps({
  login: String,
  force: Boolean
});

const emit = defineEmits(["ok", "hide"]);
const $q = useQuasar();

const dialogRef = ref(null);
const loading = ref(false);
const isPwd = ref(true);

const form = reactive({
  passwdold: "",
  passwd: "",
  passwd2: "",
});

const validSave = () => {
  const oldPswValid = props.force ? false : (form.passwdold === "");
  return (
    oldPswValid ||
    form.passwd.trim() === "" ||
    (form.passwd.trim() !== "" &&
      form.passwd.trim() !== form.passwd2.trim())
  );
};

const pswTest = (val) => {
  return val === form.passwd;
};

const show = () => {
  dialogRef.value?.show();
};

const hide = () => {
  dialogRef.value?.hide();
};

const onDialogHide = () => {
  emit("hide");
};

const onOKClick = () => {
  loading.value = true;
  let err = false;

  const requestPromise = props.force
    ? api.post('?method=auth/forceChangePsw', {
      method: "auth/forceChangePsw",
      params: [props.login, form.passwd]
    })
    : api.post('', { method: "auth/savePsw", params: [form] });

  requestPromise
    .then(() => {
      $q.notify({
        type: 'positive',
        message: 'Пароль успешно изменен! Вход в систему...',
        position: 'top',
        timeout: 2000
      });

      emit("ok", { res: true });
    })
    .catch((error) => {
      err = true;
      console.error(error);
    })
    .finally(() => {
      loading.value = false;

      if (!err)
        emit("hide");

      if (!props.force) {
        setTimeout(() => {
          location.reload();
        }, 1500);
      }
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
