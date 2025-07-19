<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 800px"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>


        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fmReqLabel('fldName')" autofocus
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fvMethodStatus -->
        <q-select
          v-model="form.fvMethodStatus" :model-value="form.fvMethodStatus"
          :label="fmReqLabel('status')" :options="optFvMethodStatus" dense map-options
          option-label="name" option-value="id" class="q-mb-md"
          @update:model-value="fnSelectFvMethodStatus"
        />

        <!-- MethodApprovalDate -->
        <q-input
          :model-value="form.MethodApprovalDate"
          v-model="form.MethodApprovalDate"
          type="date" class="q-my-lg" clearable
          :label="$t('dateApproval')"
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
import {date} from "quasar";

export default {
  props: ["mode", "data", "mapFV"],

  data() {
    return {
      form: this.data,
      optFvMethodStatus: [],
      loading: false,
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

    fnSelectFvMethodStatus(v) {
      if (v) {
        this.form.fvMethodStatus = v.id
        this.form.pvMethodStatus = v["pv"]
        if (this.mapFV["FV_Approved"]===v.id) {
          this.form.MethodApprovalDate = date.formatDate(Date.now(), "YYYY-MM-DD")
        } else {
          this.form.MethodApprovalDate = null
        }
      }
    },

    validSave() {
      if (!this.form.name || !this.form.fvMethodStatus ||
        (this.form.fvMethodStatus===this.mapFV["FV_Approved"] && !this.form.MethodApprovalDate) ||
        (this.form.fvMethodStatus!==this.mapFV["FV_Approved"] && this.form.MethodApprovalDate) ) return true
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
      this.$axios
        .post(baseURL, {
          method: "data/saveMethodologyProperties",
          params: [this.form],
        })
        .then( (response) => {
            err = false
            this.$emit("ok", response.data.result.records[0]);
            //notifySuccess(this.$t("success"));
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
            err = true
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            notifyError(msg);
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
    //console.info("this.cls", this.cls)
    //
    //
    //
    // fvMethodStatus
    this.loading = true
    api
      .post(baseURL, {
        method: "data/loadFvMethodologyForSelect",
        params: ["Factor_MethodStatus"],
      })
      .then(
        (response) => {
          this.optFvMethodStatus = response.data.result.records
        })
/*
      .then(()=> {
        //console.info("mapFV")
        api
          .post(baseURL, {
            method: "data/mapFvApproved",
            params: [],
          })
          .then(
            (response) => {
              this.mapFV = response.data.result
            })
      })
*/
      .catch(error => {
        let msg = error.message
        if (error.response)
          msg = this.$t(error.response.data.error.message)
        notifyError(msg)
      })
      .finally(() => {
        this.loading = false
      })

  },
};
</script>
