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
            :dense="dense"
            :model-value="myData.name"
            v-model="myData.name"
            autofocus
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            :dense="dense"
            :model-value="myData.fullName"
            v-model="myData.fullName"
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- dbeg-->
        <q-input
            :dense="dense"
            type="date"
            stack-label
            :model-value="myData.dbeg"
            v-model="myData.dbeg"
            :label="$t('fldDbeg')"
            clearable
        >
        </q-input>
        <!-- dend-->
        <q-input
            :dense="dense"
            type="date"
            stack-label
            :model-value="myData.dend"
            v-model="myData.dend"
            :label="$t('fldDend')"
            clearable
        >
        </q-input>

        <!-- cmtVer -->
        <q-input
            :dense="dense"
            :model-value="myData.cmtVer"
            v-model="myData.cmtVer"
            type="textarea"
            :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            :dense="dense"
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validName()"
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
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["form", "mode", "lg", "dense"],

  data() {
    return {
      myData: this.form,
      lang: this.lg,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    onBlurName() {
      if (this.myData.name) {
        this.myData.name = this.myData.name.trim();
        if (
            !this.myData.fullName ||
            (this.myData.fullName && this.myData.fullName.trim() === "")
        ) {
          this.myData.fullName = this.myData.name;
        }
      }
    },

    validName() {
      if (!this.myData.name) return true;
      else if (this.myData.name.trim().length === 0) return true;
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

      let err = false
      const method = this.mode === "ins" ? "insertClsVer" : "updateClsVer";
      api
          .post(baseURL, {
            id: this.myData.id,
            method: "typ/" + method,
            params: [this.myData],
          })
          .then(
              (response) => {
                err = false
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
                err = true
                notifyError(error.response.data.error.message);
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
  },
  created() {
    return {};
  },
};
</script>
