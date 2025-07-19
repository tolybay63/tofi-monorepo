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
            dense
            v-model="form.passwdold"
            :model-value="form.passwdold"
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
        ></q-input>

        <!-- psw2 -->
        <q-input
            dense
            v-model="form.passwd2"
            :model-value="form.passwd2"
            label="Подтверждение *"
            :type="isPwd ? 'password' : 'text'"
            :rules="[(val) => pswTest(val) || $t('errorPassword')]"
        ></q-input>
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
import {api, baseURL} from "boot/axios.js";
import {ref} from "vue";
import {notifyError} from "src/utils/jsutils.js";

export default {
  props: ["lg", "id"],

  data() {
    return {
      form: {
        passwdold: "",
        passwd: "",
        passwd2: "",
      },
      lang: this.lg,
      loading: ref(false),

      isPwd: ref(true),
    };
  },

  emits: ["ok", "hide"],

  methods: {
    validSave() {
      return (
          this.form.passwdold === "" ||
          this.form.passwd.trim() === "" ||
          (this.form.passwd.trim() !== "" &&
              this.form.passwd.trim() !== this.form.passwd2.trim())
      );
    },

    pswTest(val) {
      return val === this.form.passwd;
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
      this.form.id = this.id;
      //console.log("new Form", this.newForm);

      let err = false;
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "auth/savePsw",
            params: [this.form],
          })
          .then(
              (response) => {
                //console.log("response saveInfo", response.data)
                this.$emit("ok", {res: true});
                this.hide();
              },
              (error) => {
                err = true;
                let msg = error.response.data.error.message
                    ? error.response.data.error.message
                    : error.message;
                msg = this.$t(msg);
                notifyError(msg);
              }
          )
          .finally(() => {
            this.loading = ref(false);
            if (!err) location.reload();
          });
    },

    mounted() {
    },

    onCancelClick() {
      this.hide();
    },
  },
  created() {
  },
};
</script>
