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
        <q-item-section v-if="isChild">
          <div class="row">
          <span class="text-blue q-mt-md-md" > {{ $t("parent") }}: </span>
          <span class="q-mb-lg q-ml-md text-bold"> {{ parentName }} </span>
          </div>
        </q-item-section>

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          autofocus
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- F_FishFamily -->
        <q-select
          :readonly="isChild"
          v-model="form.fvFishFamily"
          :model-value="form.fvFishFamily"
          :label="fmReqLabel('fishFamily')"
          :options="optFvFishFamily"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          @update:model-value="fnSelectFvFishFamily"
        />

        <!-- Description -->
        <q-input :model-value="form['cmt']" v-model="form['cmt']" type="textarea" :label="$t('fldCmt')" />

        <!---->
      </q-card-section>

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
import {api} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["mode", "isChild", "parentName", "data"],

  data() {
    return {
      form: this.data,
      loading: false,
      optFvFishFamily: [],

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

    fnSelectFvFishFamily(v) {
      if (v) {
        this.form.fvFishFamily = v.id
        this.form.pvFishFamily = v["pv"]
      }
    },

    validSave() {
      if (!this.form.name || !this.form.fvFishFamily) return true
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
      this.form.mode = this.mode
      api
        .post('', {
          method: 'data/saveTypesFishProperties',
          params: [this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result["records"][0]);
            //notifySuccess(this.$t("success"));
          })
        .catch(error => {
          err = true
          let msg = error.response
            ? error.response.data.error.message
            : error.message
          notifyError(msg)
        })
        .finally(() => {
          if (!err) this.hide()
        })
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    this.loading = true
    api
      .post('', {
        method: 'data/loadFvFishFamilyForSelect',
        params: ['Factor_FishType'],
      })
      .then(
        (response) => {
          this.optFvFishFamily = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })
  },
}
</script>
