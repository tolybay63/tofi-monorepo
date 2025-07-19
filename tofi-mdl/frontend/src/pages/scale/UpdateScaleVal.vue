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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" dense>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!--  minVal  -->
        <q-input
            v-model="form.minVal"
            :model-value="form.minVal"
            :label="$t('minValGr')"
            type="number"
            dense autofocus
        />

        <!-- isMinValOpen -->
        <q-toggle
            class="q-mt-md"
            :model-value="form.isMinValOpen"
            v-model="form.isMinValOpen"
            :label="$t('isMinValOpen')"
            dense
        />

        <!--  maxVal  -->
        <q-input
            class="q-mt-md"
            v-model="form.maxVal"
            :model-value="form.maxVal"
            :label="$t('maxValGr')"
            type="number"
            dense
        />

        <!-- isMaxValOpen -->
        <q-toggle
            class="q-mt-md"
            :model-value="form.isMaxValOpen"
            v-model="form.isMaxValOpen"
            :label="$t('isMaxValOpen')"
            dense
        />

        <!-- cmt -->
        <q-input
            class="q-mt-md"
            :model-value="form.cmt"
            v-model="form.cmt"
            dense type="textarea"
            :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save" dense
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
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
      loading: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    validSave() {
      return (this.form.minVal && this.form.maxVal);
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
      this.loading = ref(true)
      let err = false
      const method = this.mode === "ins" ? "insertScaleVal" : "updateScaleVal";

      api
          .post(baseURL, {
            id: this.form.id,
            method: "scale/" + method,
            params: [ this.form ],
          })
          .then(
              (response) => {
                err = false
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true
                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message)
                notifyError(msg);
              }
          )
          .finally(() => {
            if (!err) this.hide()
            this.loading= ref(false)
          });
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
