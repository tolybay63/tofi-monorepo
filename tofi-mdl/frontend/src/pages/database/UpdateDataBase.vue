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

        <!-- DataBaseType -->
        <q-select
            autofocus
            v-model="dbType"
            :options="optionsDB"
            :label="$t('dataBaseType')"
            option-value="id"
            option-label="text"
            map-options
            dense
            options-dense
            :model-value="dbType"
            @update:model-value="fnSelect()"
        />

        <!-- modelName -->
        <q-input
            class="q-mt-md"
            dense
            :model-value="form.modelName"
            v-model="form.modelName"
            :label="$t('modelNameLabel')"
            label-color="blue"
        />

        <!-- name -->
        <q-input
            class="q-mt-md"
            dense
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

        <!-- cmt -->
        <q-input
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  name: "UpdateDataBase",
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      dbType: this.data.dataBaseType === null ? "" : this.data.dataBaseType,
      optionsDB: [],
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

    fnSelect() {
      this.form.dataBaseType = this.dbType.id;
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
      let err = false;
      const method = this.mode === "ins" ? "insert" : "update";
      this.form.permis =
          typeof this.dbType === "object" ? this.dbType.id : this.dbType;
      api
          .post(baseURL, {
            id: this.form.id,
            method: "database/" + method,
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
                if (msg.includes("i_apps_i_permis"))
                  msg = "Идентификатор сервиса уникален";

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
          this.form.dataBaseType === undefined ||
          this.form.dataBaseType === null ||
          this.form.modelName === undefined ||
          this.form.modelName === null ||
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
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_DataBaseType"}],
        })
        .then((response) => {
          this.optionsDB = response.data.result.records;
        });

  },
};
</script>

<style scoped></style>
