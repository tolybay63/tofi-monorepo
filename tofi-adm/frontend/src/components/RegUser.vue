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
        <div>{{ $t("registration") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-sm">
        <div class="col">
          <q-card-section>
            <!-- login -->
            <q-input
                v-model="form.login"
                :model-value="form.login"
                autofocus
                type="text"
                :label="$t('login')"
                :rules="[(val) => loginTest(val) || $t('req')]"
                dense
            >
            </q-input>
            <!-- email -->
            <q-input
                dense
                v-model="form.email"
                :model-value="form.email"
                type="email"
                :label="$t('email')"
                :rules="[(val) => emailTest(val) || $t('req')]"
            >
            </q-input>
            <!-- passwd -->
            <q-input
                dense
                v-model="form.passwd"
                :model-value="form.passwd"
                :label="$t('passwd')"
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
            <!-- psw2 -->
            <q-input
                dense
                v-model="form.psw2"
                :model-value="form.psw2"
                :label="$t('confirmation')"
                :type="isPwd ? 'password' : 'text'"
                :rules="[(val) => pswTest(val) || $t('errorPassword')]"
            >
            </q-input>
          </q-card-section>
        </div>
        <!---->
        <div class="col">
          <q-card-section>
            <!-- name -->
            <q-input
                v-model="form.name"
                :model-value="form.name"
                type="text"
                :label="$t('usrName')"
                :rules="[(val) => val.trim().length > 0 || $t('req')]"
                dense
            >
            </q-input>

            <!-- fullname -->
            <q-input
                v-model="form.fullName"
                :model-value="form.fullName"
                type="text"
                :label="$t('usrFullName')"
                :rules="[(val) => val.trim().length > 0 || $t('req')]"
                dense
            >
            </q-input>
            <!--
                        &lt;!&ndash; middlename &ndash;&gt;
                        <q-input
                          v-model="form.middlename"
                          :model-value="form.middlename"
                          type="text"
                          label="Отчество"
                          dense
                        >
                        </q-input>
            -->
            <!-- phone -->
            <q-input
              dense
              clearable
              v-model="form.phone"
              :model-value="form.phone"
              unmasked-value
              :label="$t('phone')"
              prefix="+7"
              mask="### ### ####"
              fill-mask="_"
              @update:model-value="isValid"
            />
          </q-card-section>
        </div>
      </div>
      <!--unmasked-value-->

      <q-card-actions align="right">
        <q-btn
            :loading="loading"
            color="primary"
            icon="how_to_reg"
            :label="$t('registration')"
            @click="onOKClick"
            :disable="disableReg()"
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
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils";
import {api, } from "boot/axios";

export default {
  props: ["lg"],
  emits: ["ok", "hide"],

  data() {
    return {
      form: {
        login: "",
        email: "",
        passwd: "",
        psw2: "",
        name: "",
        fullName: "",
        phone: "",
      },
      lang: this.lg,
      isPwd: ref(true),
      loading: false,
    };
  },

  methods: {
    isValid() {
      return this.form.phone.length === 10
    },

    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
          v
      );
    },

    loginTest(val) {
      return !!val && !!val.trim();
    },

    pswTest(val) {
      return val === this.form.passwd;
    },

    disableReg() {
      return !(
        this.loginTest(this.form.login) &&
        this.loginTest(this.form.name) &&
        this.loginTest(this.form.fullName) &&
        this.emailTest(this.form.email) &&
        this.form.passwd && this.form.passwd === this.form.psw2 &&
        this.isValid()
      )

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
      //const data = JSON.parse(JSON.stringify(this.form));

      this.loading = true;

      api
          .post("", {
            method: "auth/regUser",
            params: [this.form],
          })
          .then(
              () => {
                this.$emit("ok", {res: true});
                this.hide();
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = false;
          });
    },

    onCancelClick() {
      this.hide();
    },
  },
  created() {
    //console.log("appconsts [usersGroupStaff] from RegUser",appconsts["usersGroupStaff"]);
    return {};
  },
};
</script>
