<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-down"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("logIn") }}</div>
      </q-bar>
      <q-form @submit="onOKClick">
        <q-card-section>
          <!-- login -->
          <q-input
            v-model="form.login"
            :model-value="form.login"
            autofocus
            type="text"
            :label="$t('login')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            @keyup.enter.stop="loginTest() ? onfocus(form.psw) : onOKClick"
          >
          </q-input>
          <!-- email -->
          <!--
          <q-input
            v-model="form.email"
            :model-value="form.email"
            type="email"
            label="Эл.почта *"
            :rules="[val => emailTest(val) || $t('req')]"
          >
          </q-input>
  -->
          <!-- psw -->
          <q-input
            v-model="form.psw"
            :model-value="form.psw"
            :label="$t('passwd')"
            :type="isPwd ? 'password' : 'text'"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
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
            clickable
            flat
            text-color="blue"
            dense
            color="white"
            @click="forgetPsw"
          >
            {{ $t("forgotPsw") }}
          </q-chip>
        </div>
        <q-card-actions align="right">
          <q-btn
            :loading="loading"
            color="primary"
            icon="login"
            :label="$t('logIn')"
            type="submit"
            :disable="!(loginTest())"
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
      </q-form>
    </q-card>
  </q-dialog>
</template>
<script>
import {ref} from "vue";
import {api, authURL} from "boot/axios.js";
import axios from "axios";
import UpdaterPsw from "components/UpdaterPsw.vue";
import {useUserStore} from "stores/user-store.js";

export default {
  props: [],

  data() {
    return {
      form: {login: "", email: "", psw: "", psw2: ""},
      isPwd: ref(true),
      loading: false,
    };
  },

  emits: ["ok", "hide"],

  methods: {
    forgetPsw() {
      this.loading = true;
      let err = false;

      axios
        .post(api.defaults.baseURL, {
          method: "psw/forgetPasswd", // Полный путь через айтем
          params: [
            { login: this.form.login } // Оборачиваем мапу в массив [ ] для маппера JAndCode!
          ]
        })
        .then(() => {
          this.$q.notify({
            type: 'positive',
            message: 'Ссылка для подтверждения отправлена на почту',
            position: 'top',
          });
        })
        .catch((error) => {
          err = true;
          console.error(error);
          this.$q.notify({
            type: 'negative',
            message: 'Ошибка при отправке запроса',
            position: 'top',
          });
        })
        .finally(() => {
          this.loading = false;
          if (!err) this.hide();
        });

    },

    /*
        emailTest: function (v) {
          return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
            v
          );
        },
    */

    loginTest() {
      return this.form.login && this.form.login.trim() && this.form.psw && this.form.psw.trim();
    },

    show() {
      this.$refs.dialog["show"]();
    },

    hide() {
      this.$refs.dialog["hide"]();
    },

    onDialogHide() {
      this.$emit("hide");
    },

    onOKClick: function () {
      this.loading = true
      let err = false;

      const params = new URLSearchParams();
      params.append('password', this.form.psw);
      params.append('username', this.form.login + ":::admin-quasar");

      axios
        .post(authURL + "/login", params, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded"
          },
          withCredentials: true
        })
        .then((res) => {
          // 1. Извлекаем токен из любого возможного формата ответа бэка
          const token = res.data?.result?.token || res.data?.token || res.data?.result?.data?.token;

          // 2. Извлекаем флаг принудительной смены пароля
          const forcePasswordChange = res.data?.result?.forcePasswordChange ??
            res.data?.forcePasswordChange ??
            res.data?.result?.force_change ??
            res.data?.force_change ??
            res.data?.result?.data?.force_change;

          if (token) {
            // Инициализируем хранилище и Axios
            const userStore = useUserStore();
            userStore.setUserStore(token);
            api.defaults.headers.common['Authorization'] = 'Bearer ' + token;
            // ПЕРЕХВАТ: Если флаг равен true или 1
            if (forcePasswordChange === true || forcePasswordChange === 1 || forcePasswordChange === 'true') {
              // Закрываем окно логина перед вызовом смены пароля
              if (typeof this.hide === 'function') this.hide();
              else this.$emit("hide");

              this.$q.dialog({
                component: UpdaterPsw,
                componentProps: {
                  login: this.form.login, // Наш строковый логин
                  force: true
                }
              }).onOk(() => {
                // Защита: передаем и токен, и объект ответа, чтобы подошло любому родителю
                this.$emit("ok", token);
              });

              return;
            }

            // ОБЫЧНЫЙ УСПЕШНЫЙ ВХОД (Для sysadmin без force_change)
            // Сначала принудительно гасим модальное окно, чтобы оно не висло
            if (typeof this.hide === 'function') this.hide();
            else this.$emit("hide");

            // Отправляем события в обоих форматах (для старой и новой архитектуры)
            this.$emit("ok", token);
          }
        })
        .catch((error) => {
          err = true;
          console.error("ERROR", error);
        })
        .finally(()=> {
          this.loading = false;
          if (!err) this.hide();
        })
    },

    onCancelClick() {
      this.hide();
    },
  },

};
</script>
