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

      <q-card-section>
        <q-item-section v-if="parentId > 0">
          <div class="row">
          <span class="text-blue q-mt-md-md" > {{ $t("enterprise") }}: </span>
          <span class="q-mb-lg q-ml-md text-bold"> {{ parentName }} </span>
          </div>
        </q-item-section>

        <!-- name -->
        <q-input
          v-model="form.name"
          :model-value="form.name"
          autofocus
          @blur="onBlurName"
          :label="fmReqLabel('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          :model-value="form.fullName"
          :label="fmReqLabel('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cmt -->
        <q-input
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        />
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
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
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "parentId", "parentName"],

  data() {
    return {
      form: this.data,
      loading: false,
    }
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fmReqLabel(label) {
      return this.$t(label) + "*";
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
      return (
        !this.form.name ||
        !this.form.fullName
      )
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

      const method = this.mode === "ins" ? "insertKATO" : "updateKATO";
      if (this.parentId === 0)
        this.form.parent = null

      api
        .post(baseURL, {
          method: "data/" + method,
          params: [this.form],
        })
        .then(
          (response) => {
            //this.$emit("ok", {res: true});
            this.$emit("ok", response.data.result["records"][0]);
            //notifySuccess(this.$t("success"));
          })
        .catch(error => {
          let msg = error.response.data.error.message
            ? error.response.data.error.message
            : error.message
          notifyError(msg)
        })
        .finally(() => {
          this.hide()
        })
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
  },
}
</script>
