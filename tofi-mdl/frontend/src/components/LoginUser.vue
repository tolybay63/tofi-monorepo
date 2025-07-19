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
import {notifyError} from "src/utils/jsutils.js";

export default {
  props: ["lg"],

  data() {
    return {
      form: {login: "", email: "", psw: "", psw2: ""},
      lang: this.lg,
      isPwd: ref(true),
      loading: ref(false),
    };
  },

  emits: ["ok", "hide"],

  methods: {
    forgetPsw() {
      this.onCancelClick();

      this.lang = localStorage.getItem("curLang");
      const lg = {name: this.lang};

      this.$q
        .dialog({
          component: ForgetPsw,
          componentProps: {
            lg: lg,
            // ...
          },
        })
        .onOk((r) => {
          try {
            console.log("Ok! ForgetPsw");
            console.log("reg data", r);
            //code to save to DB ....
          } finally {
            setTimeout(() => {
            }, 10);
          }
        })
        .onCancel(() => {
          console.log("Cancel!");
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
      this.$refs.dialog.show();
    },

    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      this.$emit("hide");
    },

    /*
params: {username: this.form.login, password: this.form.psw},
    * */
    onOKClick: function () {
      this.loading = ref(true);
      let err = false
      let fd = new FormData()
      fd.append("username", this.form.login);
      fd.append("password", this.form.psw);
      api
        .post(authURL + "/login", fd,{
          responseType: "arraybuffer",
          headers: {
            "Content-Type": "multipart/form-data",
          },
        })
        .then(
          () => {
            //const JSESSIONID = Cookies.get("JSESSIONID");
            //console.info(JSESSIONID, JSESSIONID);
            //console.log("loginUser: response", response);
            this.$emit("ok", {res: true});
          },
          (error) => {
            //console.log("loginUser: error", error.message)
            //console.log("loginUser: error.response", error.response)
            err = true
            let msg
            if (error.response) msg = this.$t("invalidLoginPasswd");
            else msg = this.$t("networkError");

            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = ref(false);
          if (!err) this.hide()

        });
    },

    onCancelClick() {
      this.hide();
    },
  },
  setup() {
  },
};
</script>
