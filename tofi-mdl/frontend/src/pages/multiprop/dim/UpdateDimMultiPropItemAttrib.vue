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

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card-section>

        <!-- AttribValType -->
        <q-select
          v-model="avt"
          :model-value="avt"
          :options="optAVT"
          :label="$t('attribValType')"
          option-value="id"
          option-label="text"
          map-options
          dense options-dense
          @update:model-value="fnSelectValType"
          :rules="[(val) => !!val || $t('req')]"
        />

        <!-- format -->
        <q-input
            dense
            :model-value="form.format"
            v-model="form.format"
            :label="$t('formatValue')"
        />
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            dense
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
        />
        <q-btn
            dense
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
import {ref} from "vue";

export default {
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      visible: ref(false),
      avt: this.data.attribValType === 0 ? 1 : this.data.attribValType,
      optAVT: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectValType() {
      this.form.attribValType = this.avt.id
    },

    validSave() {
      return !this.form.attribValType;

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

      const method = this.mode === "ins" ? "insDimMultiPropItemAttrib" : "updDimMultiPropItemAttrib";

      this.form.attribValType =
        typeof this.avt === "object" ? this.avt.id : this.avt;
      api
          .post(baseURL, {
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
        params: [{dict: "FD_AttribValType"}],
      })
      .then((response) => {
        //console.log("FD_AttribValType", response.data.result.records)
        this.optAVT = response.data.result.records;
      });

  },
};
</script>
