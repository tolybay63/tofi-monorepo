<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
      style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- name -->
        <q-input
            :model-value="form.name" v-model="form.name"
            dense autofocus @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            :model-value="form.fullName" v-model="form.fullName"
            dense :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>


        <div class="row full-width" >
          <q-avatar :style="fnStyle()" size="28px" class="q-mr-md q-mt-md" />
          <q-input
              :label="$t('scaleColor')"
              v-model="form['scaleValColor']"
              :model-value="form['scaleValColor']"
          >
            <template v-slot:append>
              <q-icon name="colorize" class="cursor-pointer">
                <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                  <q-color v-model="form['scaleValColor']"  :model-value="form['scaleValColor']"/>
                </q-popup-proxy>
              </q-icon>
            </template>
          </q-input>
        </div>

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save" dense
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
            icon="cancel" dense
            :label="$t('cancel')"
            @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {ref} from "vue";
import {extend} from "quasar";

export default {
  props: ["data"],

  data() {
    let dta= {}
    extend(true, dta, this.data)

    return {
      form: dta,
      loading: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnStyle() {
      return "background: " + this.form["scaleValColor"]

    },

    onBlurName() {
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
      return this.form.name === "" || this.form.fullName === "";
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide();
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide");
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog
      try {
        this.$emit("ok", this.form);
      } finally {
        this.hide();
      }
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
  },

};
</script>
