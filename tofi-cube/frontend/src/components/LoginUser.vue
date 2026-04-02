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
import ForgetPsw from "components/ForgetPsw.vue";

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
      this.onCancelClick();

      this.lang = localStorage.getItem("curLang");

      this.$q
        .dialog({
          component: ForgetPsw,
          componentProps: {
            // ...
          },
        })
        .onOk(() => {
          try {
            //console.log("Ok! ForgetPsw");
            //console.log("reg data", r);
            //code to save to DB ....
          } finally {
            setTimeout(() => {
            }, 10);
          }
        })
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
      let err = false
      let params = new URLSearchParams();
      params.append("username", this.form.login);
      params.append("password", this.form.psw);

      api
        .post(authURL + "/login", params, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        })
        .then(
          (res) => {
            const token = res.data.token;
            localStorage.setItem('dtj_token', token);
            api.defaults.headers.common['Authorization'] = 'Bearer ' + token;
            this.$emit("ok", token);
          })
        .catch(error => {
          err = true;
          console.log("ERROR", error.response?.data);
        })
        .finally(() => {
          if (!err) this.hide()
        });
    },

    onCancelClick() {
      this.hide();
    },
  },

};
</script>
