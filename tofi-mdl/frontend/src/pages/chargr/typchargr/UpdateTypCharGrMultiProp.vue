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
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="st"
            :options="options"
            :label="$t('storageType')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="st"
            @update:model-value="fnSelect()"
        />
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
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["data", "lg", "dense"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      options: [],
      st: this.data.storageType === undefined ? null : this.data.storageType,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnSelect() {
      this.form.storageType = this.st.id;
    },

    validSave() {
      return !(this.form.storageType > 0);
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

      this.form.storageType = typeof this.st === "object" ? this.st.id : this.st;
      api
          .post(baseURL, {
            method: "typ/updateTypCharGrMultiProp",
            params: [this.form],
          })
          .then(
              () => {
                this.$emit("ok", {res: true});
                //notifySuccess(this.$t("success"))
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
          params: [{dict: "FD_StorageType"}],
        })
        .then((response) => {
          this.options = response.data.result.records;
          this.options.splice(1, 1);
        });

    return {};
  },
};
</script>
