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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <div class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- factor -->
        <q-btn @click="selectFV()" class="q-mt-md full-width">
          Значение фасторов
        </q-btn>

        <!-- name -->
        <q-input
          class="q-mt-md"
          :dense="dense"
          :model-value="form.name"
          v-model="form.name"
          @blur="onBlurName"
          :label="$t('fldName')"
          :disable="fvs === ''"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          :dense="dense"
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :disable="fvs === ''"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cmt -->
        <q-input
          :dense="dense"
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          :dense="dense"
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import SelectFV from "pages/dimprop/SelectFV.vue";
import {extend} from "quasar";

export default {
  props: [
    "data",
    "mode",
    "dimPropName",
    "parentName",
    "dimprop",
    "lg",
    "dense",
  ],

  data() {
    return {
      form: extend({}, this.data),
      visible: false,
      lang: this.lg,
      fvs: "",
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    selectFV() {
      let dimPropItem = 0;
      if (this.mode === "upd") dimPropItem = this.form.id;

      this.$q
        .dialog({
          component: SelectFV,
          componentProps: {
            dimPropItem: dimPropItem,
            lg: this.lang,
            dense: true,
          },
        })
        .onOk((data) => {
          //console.info("fvs", data)
          //console.info("dimPropName", this.dimPropName)
          this.fvs = data.fvs;
          this.form.fvs = data.fvs;
          this.form.name = this.dimPropName + " (" + data.nms + ")";
          this.form.fullName = this.dimPropName + " (" + data.nms + ")";
        });
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
      if (!this.form.name) return true;
      else if (this.form.name.trim().length === 0) return true;
      return false;
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

      this.visible = true
      const method = this.mode === "ins" ? "insertDPI" : "updateDPI";

      api
        .post(baseURL, {
          id: this.form.id,
          method: "dimprop/" + method,
          params: [this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
            //this.$emit("ok", { res: true });
            notifySuccess(this.$t("success"));
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.hide()
          this.visible = false
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    console.info("UpdaterDimPropItemFactor");
  },
};
</script>
