<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="min-width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="loading" color="secondary"/>

      <q-card-section>

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fnReqLabel('fldName')"
          class="q-ma-md" dense autofocus
        >
        </q-input>

        <!-- DocumentNumber -->
        <q-input
          :model-value="form['DocumentNumber']"
          v-model="form['DocumentNumber']"
          class="q-ma-md" dense
          :label="fnReqLabel('Prop_DocumentNumber')"
        />

        <!-- DocumentApprovalDate -->
        <q-input
          :model-value="form['DocumentApprovalDate']"
          v-model="form['DocumentApprovalDate']"
          class="q-ma-md" dense type="date"
          :label="fnReqLabel('Prop_DocumentApprovalDate')"
        />

        <!-- DocumentAuthor -->
        <q-input
          :model-value="form['DocumentAuthor']"
          v-model="form['DocumentAuthor']"
          class="q-ma-md" dense
          :label="fnReqLabel('Prop_DocumentAuthor')"
        />

        <!-- DocumentStartDate -->
        <q-input
          :model-value="form['DocumentStartDate']"
          v-model="form['DocumentStartDate']"
          class="q-ma-md" dense type="date" clearable
          :label="fnLabel('Prop_DocumentStartDate')"
          @clear="form['DocumentStartDate']=null"
        />

        <!-- DocumentEndDate -->
        <q-input
          :model-value="form['DocumentEndDate']"
          v-model="form['DocumentEndDate']"
          class="q-ma-md" dense type="date" clearable
          :label="fnLabel('Prop_DocumentEndDate')"
          @clear="form['DocumentEndDate']=null"
        />

                                                                                        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            dense color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
            :loading="loading"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>

        <q-btn
            dense color="primary"
            icon="cancel"
            :label="$t('cancel')"
            @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>

/*import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";*/
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import {extend} from "quasar";

export default {
  /*components: {treeselect},*/
  props: ["data","mode"],

  data() {
    return {
      form: extend({}, this.data),
      loading: false,
      //optDepartment: [],

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnReqLabel(label) {
      return this.$t(label) + "*";
    },

    fnLabel(label) {
      return this.$t(label);
    },

    validSave() {
      if (!this.form.name || !this.form["DocumentNumber"] || !this.form["DocumentApprovalDate"] || !this.form["DocumentAuthor"] )
        return true;
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
      this.loading = false
      let err = false
      api
          .post(baseURL, {
            id: this.form.id,
            method: "data/saveSourceCollections",
            params: [this.mode, this.form],
          })
          .then(
              (response) => {
                this.$emit("ok", response.data.result["records"][0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true
                //console.log("error.response.data=>>>", error.response.data.error.message)
                notifyError(error.response.data.error.message);
              }
          )
          .finally(() => {
            this.loading = false
            if (!err) this.hide();
          });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
  }

}
</script>
