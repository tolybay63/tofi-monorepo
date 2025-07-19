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
        <!-- factor -->
        <q-select
            :dense="dense"
            v-model="factor"
            :options="factors"
            :label="$t('factor')"
            option-value="id"
            option-label="name"
            map-options
            :model-value="factor"
            @update:model-value="fnSelFactor"
            :disable="mode === 'upd'"
        />

        <!-- isReq -->
        <q-toggle
            style="margin-left: 5px; margin-top: 15px"
            :dense="dense"
            v-model="form.isReq"
            :model-value="form.isReq"
            :label="$t('isReq')"
        />
        <!--        <q-space/>-->
        <!-- isUniq -->
        <q-toggle
            style="margin-left: 15px; margin-top: 15px"
            :dense="dense"
            v-model="form.isUniq"
            :model-value="form.isUniq"
            :label="$t('isUniq')"
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
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "lg", "dense"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      factors: [],
      factor: this.data.factor,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnSelFactor() {
      this.form.factor = this.factor.id;
    },
    validSave() {
      return this.form.factor === 0;
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
      const method =
          this.mode === "ins"
              ? "insertTypClusterFactor"
              : "updateTypClusterFactor";
      api
          .post(baseURL, {
            method: "typ/" + method,
            params: [{rec: this.form}],
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
    api
        .post(baseURL, {
          method: "typ/loadFactors",
          params: [this.form.typ, this.mode],
        })
        .then((response) => {
          //this.measures = pack(response.data.result.records)
          this.factors = response.data.result.records;
          this.factors.unshift({id: 0, name: this.$t("notChosen")});
        });

    return {};
  },
};
</script>
