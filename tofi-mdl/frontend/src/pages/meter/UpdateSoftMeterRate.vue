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
      <q-bar class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-item-section v-if="isChild">
          Родитель: {{ parentName }}
        </q-item-section>

        <!-- name -->
        <q-input
            :model-value="form.name"
            v-model="form.name"
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
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cod -->
        <q-input
            v-model="form.cod"
            :model-value="form.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
        />
        <!-- accessLevel -->
        <q-select
            v-model="al"
            :options="options"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="al"
            @update:model-value="fnSelect()"
        />

        <!-- cmt -->
        <q-input
            :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea"
            label="Комментарий"
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
            :disable="validName()"
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
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["isChild", "parentName", "data"],

  data() {
    return {
      form: this.data,
      options: [],
      al: this.data.accessLevel === 0 ? 1 : this.data.accessLevel,
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
      //console.log("select", this.al)
      this.form.accessLevel = this.al.id;
    },

    validName() {
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

      //delete this.form.accessLevel_text
      const method = "updateSoftMR";
      this.form.accessLevel =
          typeof this.al === "object" ? this.al.id : this.al;

      api
          .post(baseURL, {
            method: "meterrate/" + method,
            params: [{rec: this.form}],
          })
          .then(
              () => {
                this.$emit("ok", {res: true});
                notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
                let msg = error.response.data.error.message
                    ? error.response.data.error.message
                    : error.message;
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
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.options = response.data.result.records;
        });

    return {};
  },
};
</script>
