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
        <!-- title -->
        <q-input
            :model-value="form.title"
            v-model="form.title"
            autofocus dense
            :label="$t('titleDop')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            :loading="loading"
            color="primary"
            icon="save" dense
            :label="$t('save')"
            @click="onOKClick"
            :disable="validName()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";

export default {
  props: ["data", "mode"],

  data() {
    return {
      form: this.data,
      loading: ref(false)
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {


    validName() {
      if (!this.form.title) return true;
      else if (this.form.title.trim().length === 0) return true;
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

      const method = this.mode === "ins" ? "insertMoreCols" : "updateMoreCols";

      api
          .post(baseURL, {
            id: this.form.id,
            method: "dimMultiProp/" + method,
            params: [this.form],
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
          })
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
