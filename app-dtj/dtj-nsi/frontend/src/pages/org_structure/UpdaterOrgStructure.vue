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

      <q-card-section>
        <div class="row q-ma-md">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fnReqLabel('fldName')"
          class="q-ma-md" dense autofocus
        >
        </q-input>

        <!-- StartKm -->
        <q-input
          :model-value="form['StartKm']"
          v-model="form['StartKm']"
          class="q-ma-md" dense type="number"
          :label="fnLabel('StartKm_')"
        />

        <!-- FinishKm -->
        <q-input
          :model-value="form['FinishKm']"
          v-model="form['FinishKm']"
          class="q-ma-md" dense type="number"
          :label="fnLabel('FinishKm_')"
        />

        <!-- StageLength -->
        <q-input
          :model-value="form['StageLength']"
          v-model="form['StageLength']"
          class="q-ma-md" dense type="number"
          :label="fnLabel('StageLength')"
        />

        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            dense color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
            :loading="loading"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
        <q-btn
            dense color="primary"
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
import {extend} from "quasar";

export default {

  props: [
    "data",
    "mode",
    "parentName",
  ],

  data() {
    return {
      form: extend({}, this.data),
      loading: false,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnReqLabel(label) {
      return this.$t(label) + "*";
    },

    fnLabel(label) {
      return this.$t(label);
    },

    validSave() {
      if (!this.form.name) return true;
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
      this.loading = true
      let err = false
      this.$axios
          .post(baseURL, {
            id: this.form.id,
            method: "data/saveOrgStructure",
            params: [this.mode, this.form],
          })
          .then(
              (response) => {
                this.$emit("ok", response.data.result["records"][0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            this.loading = false
            if (!err) this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    this.loading = true

    if (this.form.parent) {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/getCls',
          params: [this.form.parent],
        })
        .then(
          (response) => {
            this.form.cls = response.data.result
          })
        .catch(error => {
            let msg = error.message
            if (error.response) msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .finally(() => {
          this.loading = false
        })

    }



  }

}
</script>
