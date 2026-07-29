<template>
  <q-dialog
    ref="dialog"
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
          :model-value="form.passwd"
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
          :model-value="form.passwd2"
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

<script>
import { api } from "../boot/axios.js";

export default {
  props: {
    login: String,
    force: Boolean
  },

  data() {
    return {
      form: {
        passwdold: "",
        passwd: "",
        passwd2: "",
      },
      loading: false,
      isPwd: true, // Сделали обычным булевым флагом для Options API
    };
  },

  emits: ["ok", "hide"],

  methods: {
    validSave() {
      // Если смена принудительная, старый пароль проверять не нужно
      const oldPswValid = this.force ? false : (this.form.passwdold === "");
      return (
        oldPswValid ||
        this.form.passwd.trim() === "" ||
        (this.form.passwd.trim() !== "" &&
          this.form.passwd.trim() !== this.form.passwd2.trim())
      );
    },

    pswTest(val) {
      return val === this.form.passwd;
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

    onOKClick() {
      this.loading = true;
      let err = false

      // Если это принудительный перехват (force), шлем именованный объект на новый метод бэка
      // Если обычная смена из профиля — шлем стандартный savePsw
      const requestPromise = this.force
        ? api.post('?method=auth/forceChangePsw', {
          method: "auth/forceChangePsw",
          params: [this.login, this.form.passwd]
        })
        : api.post('', { method: "auth/savePsw", params: [this.form] });

      requestPromise
        .then(() => {
          // Зелёное уведомление успеха
          this.$q.notify({
            type: 'positive',
            message: 'Пароль успешно изменен! Вход в систему...',
            position: 'top',
            timeout: 2000
          });

          // Генерируем событие OK для LoginUser.vue
          this.$emit("ok", { res: true });
        })
        .catch((error) => {
          err = true
          console.error(error);
        })
        .finally(() => {
          this.loading = false;

          // Закрываем диалог смены пароля
          if (!err)
            this.$emit("hide");

          // КРИТИЧЕСКИЙ ФИКС: Перезагружаем вкладку приложения ТОЛЬКО
          // если пользователь менял пароль у себя в профиле самостоятельно.
          // При force-входе релоад делать НЕЛЬЗЯ, иначе сотрется оперативная память Pinia!
          if (!this.force) {
            setTimeout(() => {
              location.reload();
            }, 1500);
          }
        });
    },

    onCancelClick() {
      this.hide();
    },
  }
};
</script>
