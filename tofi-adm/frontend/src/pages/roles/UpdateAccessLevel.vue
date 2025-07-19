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
      <q-bar class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="loading" color="secondary" />

      <q-card-section>
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="al"
          :options="options"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          :model-value="al"
          @update:model-value="fnSelect()"
          clearable
        />

        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
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

export default {
  props: ["data", "lg", "dense"],

  data() {
    return {
      form: JSON.parse(JSON.stringify(this.data)),
      lang: this.lg,
      options: [],
      al: this.data.accessLevel === undefined ? null : this.data.accessLevel,
      loading: false
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnSelect() {
      this.form.accessLevel = this.al ? this.al.id : null;
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

      this.$emit("ok", this.form);
      this.hide();
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    this.loading = true
    api
      .post(baseURL, {
        method: "dict/loadDictAsStore",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        this.options = response.data.result.records;
      })
      .finally(()=> {
        this.loading = false
      })
  },
};
</script>
