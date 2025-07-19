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
        <div>Забыли пароль...</div>
      </q-bar>

      <q-card-section>
        <div style="font-size: 16px">
          Пароль будет отправлен по указанной почте...
        </div>

        <!-- email -->
        <q-input
          autofocus
          v-model="form.email"
          :model-value="form.email"
          type="email"
          label="Эл.почта *"
          :rules="[(val) => emailTest(val) || $t('req')]"
        >
        </q-input>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          color="primary"
          icon="save"
          label="Применить"
          @click="onOKClick"
          :disable="!emailTest(form.email)"
          no-caps
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
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

export default {
  props: ["lg"],

  data() {
    return {
      form: { email: "" },
      lang: this.lg,
      loading: false,
    };
  },

  emits: ["ok", "hide"],

  methods: {
    emailTest: function (v) {
      return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
        v
      );
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
      try {
        this.loading = true;
        //code...
      } finally {
        setTimeout(() => {
          this.loading = false
          this.$q
            .dialog({
              title: "Сообщение",
              message: "Пароль отправлен! (имитация)",
              persistent: true,
            })
            .onOk(() => {
              // console.log('OK')
              this.$emit("ok", this.form);
              this.hide();
            });
        }, 3000);
      }
    },

    onCancelClick() {
      this.hide();
    },
  },
  created() {
    return {};
  },
};
</script>
