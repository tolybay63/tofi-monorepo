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

        <!-- class -->
        <q-select
          v-model="form.cls" :model-value="form.cls" autofocus
          :label="fmReqLabel('nameCls')" :options="optCls" dense map-options
          option-label="name" option-value="id" class="q-mb-lg"
          @update:model-value="fnSelectCls"
        />

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          autofocus
          :label="fmReqLabel('fldName')"
          @blur="onBlurName"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cmt -->
        <q-input
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        />

      </q-card-section>
      <!---->

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
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
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["mode", "data"],

  data() {
    return {
      form: this.data,
      optCls: [],
      loading: false
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

    fnSelectCls(val) {
      this.form.cls = val.id
    },

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

    validSave() {
      if (!this.form.name || !this.form.cls) return true
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

      let method = this.mode === "ins" ? "insertObj" : "updateObj";

      this.$axios
        .post(baseURL, {
          method: "data/" + method,
          params: [ this.form ],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
            //notifySuccess(this.$t("success"));
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
    //console.info("this.cls", this.cls)
    //
    this.loading = true
    api
      .post(baseURL, {
        method: "data/loadClsForSelect",
        params: [ "Typ_FishGear" ],
      })
      .then(
        (response) => {
          this.optCls = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response)
            msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })

  },
};
</script>
