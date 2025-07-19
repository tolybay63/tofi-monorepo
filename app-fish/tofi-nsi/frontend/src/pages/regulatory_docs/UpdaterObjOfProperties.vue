<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="loading" color="secondary"/>

      <q-card>
        <q-card-section>

          <q-input
            :model-value="form['RegDocumentsNameDoc']"
            v-model="form['RegDocumentsNameDoc']"
            class="q-my-lg" autofocus
            :label="fnLabelReq('nameDoc')"
          />

          <q-input
            :model-value="form['RegDocumentsNumberDoc']"
            v-model="form['RegDocumentsNumberDoc']"
            class="q-my-lg"
            :label="fnLabelReq('numberDoc')"
          />

          <q-input
            :model-value="form['RegDocumentsAuthorDoc']"
            v-model="form['RegDocumentsAuthorDoc']"
            class="q-my-lg"
            :label="fnLabelReq('authorDoc')"
          />

          <q-input
            :model-value="form['RegDocumentsNumberOrder']"
            v-model="form['RegDocumentsNumberOrder']"
            class="q-my-lg"
            :label="$t('numberOrder')"
          />

          <q-input
            :model-value="form.RegDocumentsDateApproval"
            v-model="form.RegDocumentsDateApproval"
            type="date" class="q-my-lg" clearable
            :label="$t('dateApproval')"
          />

          <q-input
            :model-value="form.RegDocumentsLifeDoc"
            v-model="form.RegDocumentsLifeDoc"
            type="date" class="q-my-lg" clearable
            :label="$t('lifeDoc')"
          />

        </q-card-section>

      </q-card>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          :disable="validSave()"
          @click="onOKClick"
        />
        <q-btn
          dense
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

import {notifyError} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";


export default {
  props: ["data", "mode"],

  data() {

    //console.log("upd data r:", r, this.requestParams)

    return {
      loading: false,
      form: this.data,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnLabelReq(label) {
      return this.$t(label) + "*";
    },

    validSave() {
      return (
        !this.form["RegDocumentsNameDoc"] ||
          !this.form["RegDocumentsNumberDoc"] ||
            !this.form["RegDocumentsAuthorDoc"]
      )
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show()
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide()
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide")
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog
      this.loading = true
      this.form.mode = this.mode
      let err = false
      api
        .post(baseURL, {
          method: "data/saveObjProperties",
          params: [this.form],
        })
        .then((resp) => {
          err = false;
          this.$emit("ok", resp.data.result["records"][0]);
          //notifySuccess(this.$t("success"))
        })
        .catch(error => {
          err = true
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
          if (!err) this.hide()
        });

    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide()
    },

  },

  created() {
  },
}
</script>
