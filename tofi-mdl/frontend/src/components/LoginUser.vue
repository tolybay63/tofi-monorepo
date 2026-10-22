<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-down"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("logIn") }}</div>

        <q-inner-loading :showing="loading" color="secondary"/>

      </q-bar>
      <q-form @submit="onOKClick">
        <q-card-section>
          <q-input
            v-model="form.login"
            :label="$t('login')"
            :model-value="form.login"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            autofocus
            type="text"
            @keyup.enter.stop="loginTest() ? onfocus(form.psw) : onOKClick"
          >
          </q-input>

          <q-input
            v-model="form.psw"
            :label="$t('passwd')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            :type="isPwd ? 'password' : 'text'"
            @keyup.enter.stop="loginTest() ? null : onOKClick"
          >
            <template v-slot:append>
              <q-icon
                :name="isPwd ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="isPwd = !isPwd"
              />
            </template>
          </q-input>

          <q-space></q-space>
        </q-card-section>

        <div class="text-right">
          <q-chip
            :disable="forgetDisable()"
            clickable color="white"
            dense
            flat
            text-color="blue"
            @click="forgetPsw"
          >
            {{ $t("forgotPsw") }}
          </q-chip>
        </div>
        <q-card-actions align="right">
          <q-btn
            :disable="!(loginTest())"
            :label="$t('logIn')"
            :loading="loading"
            color="primary"
            icon="login"
            type="submit"
          >
            <template #loading>
              <q-spinner-hourglass color="white"/>
            </template>
          </q-btn>
          <q-btn
            :label="$t('cancel')"
            color="primary"
            icon="cancel"
            @click="onCancelClick"
          />
        </q-card-actions>
      </q-form>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useQuasar } from "quasar";
import { api, authURL } from "@/boot/axios.js";
import UpdaterPsw from "@/components/UpdaterPsw.vue";
import { useUserStore } from "@/stores/user-store.js";
import axios from "axios";

const $q = useQuasar();
const emit = defineEmits(["ok", "hide"]);

const dialog = ref(null);

const form = reactive({
  login: "",
  email: "",
  psw: "",
  psw2: ""
});

const isPwd = ref(true);
const loading = ref(false);

const forgetDisable = () => {
  return form.login === "" || form.login === "sysadmin";
};

const forgetPsw = () => {
  loading.value = true;
  let err = false;

  axios
    .post(api.defaults.baseURL, {
      method: "psw/forgetPasswd",
      params: [
        { login: form.login }
      ]
    })
    .then(() => {
      $q.notify({
        type: 'positive',
        message: 'Ссылка для подтверждения отправлена на почту',
        position: 'top',
      });
    })
    .catch((error) => {
      err = true;
      console.error(error);
      $q.notify({
        type: 'negative',
        message: 'Ошибка при отправке запроса',
        position: 'top',
      });
    })
    .finally(() => {
      loading.value = false;
      if (!err) hide();
    });
};

const loginTest = () => {
  return form.login && form.login.trim() && form.psw && form.psw.trim();
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
  loading.value = true;
  let err = false;

  const params = new URLSearchParams();
  params.append('password', form.psw);
  params.append('username', form.login + ":::admin-quasar");

  axios
    .post(authURL + "/login", params, {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded"
      },
      withCredentials: true
    })
    .then((res) => {
      const token = res.data?.result?.token || res.data?.token || res.data?.result?.data?.token;

      const forcePasswordChange = res.data?.result?.forcePasswordChange ??
        res.data?.forcePasswordChange ??
        res.data?.result?.force_change ??
        res.data?.force_change ??
        res.data?.result?.data?.force_change;

      if (token) {
        const userStore = useUserStore();
        userStore.setUserStore(token);
        api.defaults.headers.common['Authorization'] = 'Bearer ' + token;

        if (forcePasswordChange === true || forcePasswordChange === 1 || forcePasswordChange === 'true') {
          hide();

          $q.dialog({
            component: UpdaterPsw,
            componentProps: {
              login: form.login,
              force: true
            }
          }).onOk(() => {
            emit("ok", token);
          });

          return;
        }

        hide();
        emit("ok", token);
      }
    })
    .catch((error) => {
      err = true;
      console.error("ERROR", error);
    })
    .finally(() => {
      loading.value = false;
      if (!err) hide();
    });
};

const onCancelClick = () => {
  hide();
};

// Экспортируем методы наружу для работы с диалоговыми плагинами Quasar
defineExpose({
  show,
  hide
});
</script>
