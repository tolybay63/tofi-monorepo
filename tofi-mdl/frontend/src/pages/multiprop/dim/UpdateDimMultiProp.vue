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

      <q-card-section>
        <q-input
            v-model="form.cod"
            :model-value="form.cod"
            :label="$t('code')" dense
            :placeholder="$t('msgCodeGen')"
        />

        <q-select
            v-model="al"
            :options="options"
            options-dense dense
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="al"
            @update:model-value="fnSelect()"
        />

        <!-- name -->
        <q-input
            :model-value="form.name"
            v-model="form.name"
            autofocus dense
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            :model-value="form.fullName"
            v-model="form.fullName"
            :label="$t('fldFullName')" dense
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <q-select
          v-model="dmpt"
          :options="optDMPT"
          :label="$t('dimMultiPropType')"
          option-value="id"
          option-label="text"
          dense options-dense
          map-options
          :model-value="dmpt"
          @update:model-value="fnSelectDMPT()"
        />

        <!-- cmt -->
        <q-input
            :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea" dense
            :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save"
            :label="$t('save')" dense
            @click="onOKClick"
            :disable="validName()"
        />
        <q-btn
            color="primary"
            icon="cancel"
            :label="$t('cancel')" dense
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
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: extend({}, this.data),
      lang: this.lg,
      options: [],
      al: this.data.accessLevel === 0 ? 1 : this.data.accessLevel,
      dmpt: this.data.dimMultiPropType === 0 ? 1 : this.data.dimMultiPropType,
      optDMPT: [],
      optProp: [],
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
      this.form.accessLevel = this.al.id;
    },

    fnSelectDMPT() {
      this.form.dimMultiPropType = this.dmpt.id;
    },

    validName() {
      if (!this.form.name || !this.form.fullName ||
      !this.form.accessLevel || !this.form.dimMultiPropType) return true;

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

      this.form.dimMultiPropType =
        typeof this.dmpt === "object" ? this.dmpt.id : this.dmpt;

      api
          .post(baseURL, {
            id: this.form.id,
            method: "dimMultiProp/" + method,
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
          this.options = response.data.result.records;
        });

    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_DimMultiPropType"}],
      })
      .then((response) => {
        this.optDMPT = response.data.result.records;
      });

    api
        .post(baseURL, {
          method: "dimMultiProp/loadPropForMultiPropItem",
          params: [],
        })
        .then((response) => {
          this.optProp = response.data.result.records
        })
  },
};
</script>
