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
          :label="fmReqLabel('clsObj')" :options="optCls"
          option-label="name" option-value="id"
          class="q-ma-md" :readonly="mode === 'upd'"
          dense options-dense map-options
          @update:model-value="fnSelectCls"
        />

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          class="q-ma-md"
          :label="fmReqLabel('fldName')"
        >
        </q-input>

      </q-card-section>
      <!---->

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
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
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["mode", "data"],

  data() {
    return {
      loading: false,
      form: this.data,
      optCls: [],
    }
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

      this.$axios
        .post(baseURL, {
          method: "data/saveComponents",
          params: [ this.mode, this.form ],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
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
    //
    this.loading = true
    api
      .post(baseURL, {
        method: "data/loadClsForSelect",
        params: [ "Typ_Components" ],
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
