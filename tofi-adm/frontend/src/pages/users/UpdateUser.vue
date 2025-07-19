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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <div class="row q-col-gutter-y-sm">
        <div class="col">
          <q-card-section>
            <!-- login
            :disable="true"
            -->
            <q-input
              :disable="mode === 'upd' && form.login === 'sysadmin'"
              autofocus
              dense
              v-model="form.login"
              :model-value="form.login"
              type="text"
              :label="$t('login')"
              :rules="[(val) => loginTest(val) || $t('req')]"
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
              v-show="mode === 'ins'"
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
              v-show="mode === 'ins'"
              dense
              v-model="form.psw2"
              :model-value="form.psw2"
              :label="$t('confirmation')"
              :type="isPwd ? 'password' : 'text'"
              :rules="[(val) => pswTest(val) || $t('errorPassword')]"
            >
            </q-input>

            <!-- AccessLevel -->
            <q-select
              :disable="mode === 'upd' && form.login === 'sysadmin'"
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              dense
              options-dense
              :model-value="al"
              @update:model-value="fnSelect()"
            />
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
              :rules="[(val) => !!val || $t('req')]"
              @blur="onBlurName"
              dense
            >
            </q-input>

            <!-- fullname -->
            <q-input
              v-model="form.fullName"
              :model-value="form.fullName"
              type="text"
              :label="$t('usrFullName')"
              :rules="[(val) => !!val || $t('req')]"
              dense
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
            />

            <q-toggle
              :disable="mode === 'upd' && form.login === 'sysadmin'"
              class="q-mt-lg"
              dense
              :model-value="form.locked"
              v-model="form.locked"
              :label="$t('locked')"
            />
          </q-card-section>
        </div>
      </div>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="how_to_reg"
          :label="$t('save')"
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
import {api, baseURL} from "boot/axios";

export default {
  props: ["rec", "mode", "lg", "dense"],
  emits: ["ok", "hide"],

  data() {
    return {
      form: this.rec,
      lang: this.lg,
      isPwd: ref(true),
      loading: false,
      optionsLevel: [],
      al: this.rec.accessLevel === 0 ? 1 : this.rec.accessLevel,
    };
  },

  methods: {
    fnSelect() {
      this.form.accessLevel = this.al.id;
    },

    onBlurName() {
      this.form.name = this.form.name ? this.form.name.trim() : null;
      if (this.form.name) {
        if (this.form.fullName == null || this.form.fullName === "")
          this.form.fullName = this.form.name.trim();
      }
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
      if (this.mode === "ins")
        return !(
          this.loginTest(this.form.login) && this.emailTest(this.form.email) &&
          this.loginTest(this.form.name) && this.loginTest(this.form.fullName) &&
          this.form.passwd && this.form.passwd === this.form.psw2
        );
      else {
        return !(this.loginTest(this.form.login) && this.emailTest(this.form.email) &&
          this.loginTest(this.form.name) && this.loginTest(this.form.fullName)
        );
      }
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
      //const data = JSON.parse(JSON.stringify(this.form));

      this.loading = true;
      let method = this.mode === "ins" ? "insert" : "update";

      api
        .post(baseURL, {
          method: "usr/" + method,
          params: [this.form],
        })
        .then(
          () => {
            this.$emit("ok", {res: true});
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
          this.hide();
        });
    },

    onCancelClick() {
      this.hide();
    },
  },
  created() {

    this.loading = true
    api
      .post(baseURL, {
        method: "dict/loadDictAsStore",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        //console.log("FD_AccessLevel", response.data.result.records)
        this.optionsLevel = response.data.result.records;
      })
      .finally(() => {
        this.loading = false
      })
  },
};
</script>
