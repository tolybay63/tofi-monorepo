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
        <q-input
            v-model="form.cod"
            :model-value="form.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
            dense
        />
        <!-- name -->
        <q-input
            :model-value="form.name"
            v-model="form.name"
            dense
            autofocus
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            :model-value="form.fullName"
            v-model="form.fullName"
            dense
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- AccessLevel -->
        <q-select
            v-model="al"
            :options="optionsLevel"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            dense options-dense
            :model-value="al"
            @update:model-value="fnSelect()"
        />
        <!-- AttribValType -->
        <q-select
            v-model="valType"
            :options="optionsValType"
            :label="$t('attribValType')"
            option-value="id"
            option-label="text"
            map-options
            dense options-dense
            :model-value="valType"
            @update:model-value="fnSelectValType()"
            :rules="[(val) => !!val || $t('req')]"
        />

        <q-toggle
            style="margin-left: 10px; margin-top: 10px"
            dense
            v-model="form.isMultiLang"
            :model-value="form.isMultiLang"
            :label="$t('multiLang').replaceAll('-', '')"
        />

        <!-- cmt -->
        <q-input
            :model-value="form.cmt"
            v-model="form.cmt"
            dense
            type="textarea"
            :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            dense
            :disable="validSave()"
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
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      optionsLevel: [],
      optionsValType: [],
      al: this.data.accessLevel === 0 ? 1 : this.data.accessLevel,
      valType: this.data.attribValType === 0 ? 1 : this.data.attribValType,
    };
  },
  //al: this.form.accessLevel===0 ? 1 : this.form.accessLevel
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
      this.form.accessLevel = this.al.id;
    },
    fnSelectValType() {
      //console.log("select", this.al)
      this.form.attribValType = this.valType.id;
    },

    validSave() {
      return this.form.name === "" || this.form.fullName === "" || !this.form.attribValType;
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

      const method = this.mode === "ins" ? "insert" : "update";
      this.form.accessLevel =
          typeof this.al === "object" ? this.al.id : this.al;
      api
          .post(baseURL, {
            id: this.form.id,
            method: "attrib/" + method,
            params: [{rec: this.form}],
          })
          .then(
              (response) => {
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
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
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optionsLevel = response.data.result.records;
        });

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AttribValType"}],
        })
        .then((response) => {
          //console.log("FD_AttribValType", response.data.result.records)
          this.optionsValType = response.data.result.records;
        });

  },
};
</script>
