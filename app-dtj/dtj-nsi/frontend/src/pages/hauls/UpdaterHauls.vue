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

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          class="q-ma-md" dense autofocus
        >
        </q-input>
<!--    :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"    -->

        <!-- StartKm -->
        <q-input
          :model-value="form['StartKm']"
          v-model="form['StartKm']"
          class="q-ma-md" dense type="number"
          :label="fmReqLabel('StartKm')"
        />

        <!-- StartPicket -->
        <q-input
          :model-value="form['StartPicket']"
          v-model="form['StartPicket']"
          class="q-ma-md" dense type="number"
          :label="fmReqLabel('StartPicket')"
        />

        <!-- FinishKm -->
        <q-input
          :model-value="form['FinishKm']"
          v-model="form['FinishKm']"
          class="q-ma-md" dense type="number"
          :label="fmReqLabel('FinishKm')"
        />

        <!-- FinishPicket -->
        <q-input
          :model-value="form['FinishPicket']"
          v-model="form['FinishPicket']"
          class="q-ma-md" dense type="number"
          :label="fmReqLabel('FinishPicket')"
        />

        <!-- StageLength -->
        <q-input
          :model-value="form['StageLength']"
          v-model="form['StageLength']"
          class="q-ma-md" dense type="number"
          :label="fmReqLabel('StageLength')"
        />

      </q-card-section>
      <!---->

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
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
import {baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["mode", "data"],

  data() {
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
    fmReqLabel(label) {
      return this.$t(label) + "*";
    },

    validSave() {
      if (!this.form.name || !this.form.StartKm || !this.form.StartPicket ||
        !this.form.FinishKm || !this.form.FinishPicket || !this.form.StageLength) return true
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

      this.$axios
        .post(baseURL, {
          method: "data/saveStage",
          params: [ this.mode, this.form ],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)

            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            notifyError(msg);
          }
        )
        .finally(() => {
          this.hide();
        });
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
