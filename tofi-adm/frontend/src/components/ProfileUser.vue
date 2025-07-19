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
        <div>{{ $t("myProfile") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-md">
        <div class="col">
          <q-card-section>
            <!-- email -->
            <q-input
                v-model="form.email"
                :model-value="form.email"
                type="email"
                :label="$t('email')"
                autofocus
                :rules="[(val) => emailTest(val) || $t('req')]"
                @change="onChangeEmil"
            >
            </q-input>
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
                :rules="[(val) => val.length > 9 || $t('req')]"
                @change="onChangePhone"
            />

            <q-toggle
                v-model="chPsw"
                :label="$t('changePsw')"
                :model-value="chPsw"
                @click="onToogle"
            />

            <!--
            <q-input
              dense
              v-model="form.passwdold"
              :model-value="form.passwdold"
              label="Старый пароль *"
              autofocus
              :type="isPwd ? 'password' : 'text'"
              :rules="[
                (val) => (!!chPsw && !!val && !!val.trim()) || $t('req'),
              ]"
              :disable="chPsw !== true"
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
              :rules="[
                (val) => (!!chPsw && !!val && !!val.trim()) || $t('req'),
              ]"
              @change="onChangePsw"
              :disable="chPsw !== true"
            ></q-input>

            &lt;!&ndash; psw2 &ndash;&gt;
            <q-input
              dense
              v-model="form.passwd2"
              :model-value="form.passwd2"
              label="Подтверждение *"
              :type="isPwd ? 'password' : 'text'"
              :rules="[
                (val) => (!!chPsw && pswTest(val)) || $t('errorPassword'),
              ]"
              :disable="chPsw !== true"
            ></q-input>
-->
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
                :rules="[(val) => textTest(val) || $t('req')]"
                @change="onChangeNm"
                @blur="onBlur"
            >
            </q-input>

            <!-- fullName -->
            <q-input
                v-model="form.fullName"
                :model-value="form.fullName"
                type="text"
                :label="$t('usrFullName')"
                :rules="[(val) => textTest(val) || $t('req')]"
                @change="onChangeFnm"
            >
            </q-input>
          </q-card-section>
        </div>
      </div>

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
import {api, authURL, baseURL} from "boot/axios.js";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils.js";
import UpdaterPsw from "components/UpdaterPsw.vue";
import {useUserStore} from "stores/user-store.js";

export default {
  props: ["lg", "userId"],

  data() {
    return {
      form: {},
      form2: {},
      lang: this.lg,
      loading: ref(false),
      chPsw: ref(false),
      newForm: {},
    };
  },

  emits: ["ok", "hide"],

  methods: {
    onBlur() {
      if (this.form.name) {
        this.form.name = this.form.name.trim();
        if (
            !this.form.fullName ||
            (this.form.fullName && this.form.fullName.trim() === "")
        ) {
          this.form.fullName = this.form.name;
        }
      }
    },

    validSave() {
      return !(
          this.chPsw ||
          !(
              this.isChanged() &&
              this.form.phone &&
              this.form.phone.length > 9 &&
              this.emailTest(this.form.email) &&
              this.textTest(this.form.name) &&
              this.textTest(this.form.fullName)
          )
      );
    },

    isChanged() {
      return !(
          this.form.email !== this.form2.email ||
          this.form.name !== this.form2.name ||
          this.form.fullName !== this.form2.fullName ||
          this.form.phone !== this.form2.phone
      );
    },

    onChangeEmil() {
      this.newForm.email = this.form.email;
    },
    onChangePhone() {
      this.newForm.phone = this.form.phone;
    },
    onChangeNm() {
      this.newForm.name = this.form.name;
    },
    onChangeFnm() {
      this.newForm.fullName = this.form.fullName;
    },

    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
          v
      );
    },

    textTest(val) {
      return !!val && !!val.trim();
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

    onOKClick() {
      const store = useUserStore();
      const {setUserStore, setUserName} = store;

      this.newForm.id = this.userId;
      //console.log("new Form", this.newForm);

      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "auth/saveProfile",
            params: [this.newForm],
          })
          .then(
              (response) => {
                setUserName(this.form.fullName);
                if (this.chPsw) {
                  api
                      .post(authURL + "/logout", {
                        params: {},
                      })
                      .then((response) => {
                        setUserStore({});
                        this.$router.push("/");
                      });

                  this.$q
                      .dialog({
                        component: UpdaterPsw,
                        componentProps: {
                          id: this.newForm.id,
                          // ...
                        },
                      })
                      .onOk((r) => {
                        if (r.res) {
                          console.log("Ok! Psw changed!");
                          this.hide();
                          //location.reload()
                        }
                      });
                } else {
                  setUserName(this.form.fullName);
                  this.hide();
                  //location.reload()
                }
              },
              (error) => {
                notifyError(error.messages);
              }
          )
          .finally(() => {
            this.loading = ref(false);
          });
    },

    mounted() {
    },

    onCancelClick() {
      this.hide();
    },
  },
  created() {
    this.loading = ref(true);
    api
        .post(baseURL, {
          id: "1",
          method: "auth/loadProfile",
          params: [this.userId],
        })
        .then(
            (response) => {
              this.form = response.data.result.records[0];
              Object.assign(this.form2, this.form);
            },
            (error) => {
              notifyError(error.messages);
            }
        )
        .finally(() => {
          this.loading = ref(false);
        });

    return {};
  },
};
</script>
