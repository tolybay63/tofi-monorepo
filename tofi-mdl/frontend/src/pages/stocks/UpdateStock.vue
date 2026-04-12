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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" dense>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- code -->
        <q-input
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')"
          label-color="blue"
          :placeholder="$t('msgCodeGen')"
          dense
        />

        <!-- name -->
        <q-input
          class="q-mt-md"
          dense autofocus
          :model-value="form.name"
          v-model="form.name"
          @blur="onBlurName"
          :label="$t('fldName')"
          label-color="blue"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fullName-->
        <q-input
          class="q-mt-md"
          dense
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          label-color="blue"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- sourceStockType -->
        <q-select
          class="q-mt-md"
          dense
          options-dense
          v-model="ssType"
          :model-value="ssType"
          :options="optSST"
          :label="$t('stockType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectSST()"
          style="margin-bottom: 5px"
        />

        <!-- cmt -->
        <q-input
          class="q-mt-md"
          dense
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
          label-color="blue"
        />
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          dense
          :disable="validOk()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
          dense
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  name: "UpdateStock",
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      ssType: 1,
      optSST: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
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

    fnSelectSST() {
      //console.log("select", this.al)
      this.form.sourceStockType = this.ssType.id;
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog["show"]();
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog["hide"]();
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
      let err = false;
      const method = this.mode === "ins" ? "insert" : "update";
      this.form.sourceStockType = typeof this.ssType === "object" ? this.ssType.id : this.ssType;

      api
        .post('', {
          id: this.form.id,
          method: "stock/" + method,
          params: [this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
            notifySuccess(this.$t("success"));
            err = false;
          },
          (error) => {
            err = true;
            let msg = error.response.data.error.message;
            notifyError(msg);
          }
        )
        .finally(() => {
          if (!err) this.hide();
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },

    validOk() {
      return (
        this.form.name === undefined ||
        this.form.name === null ||
        this.form.name === "" ||
        this.form.fullName === undefined ||
        this.form.fullName === null ||
        this.form.fullName === ""
      );
    },
  },

  created() {
    api
      .post('', {
        method: "dict/load",
        params: [{dict: "FD_SourceStockType"}],
      })
      .then((response) => {
        this.optSST = response.data.result.records;
      });
    //
  },

};
</script>

<style scoped></style>
