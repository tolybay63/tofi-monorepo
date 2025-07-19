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

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <!-- cod -->
        <q-input
          dense
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />
        <!-- name -->
        <q-input
          dense
          :model-value="form.name"
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          dense
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- accessLevel -->
        <q-select
          dense options-dense
          v-model="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          :model-value="al"
          @update:model-value="fnSelectAL()"
        />
        <!-- cmt -->
        <q-input
          dense
          :model-value="form.cmt"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
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
import {ref} from "vue";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      optAL: [],
      al: this.data.accessLevel,
      visible: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
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

    fnSelectAL() {
      this.form.accessLevel = this.al.id;
    },

    validSave() {
      if (!this.form.name) return true;
      else if (this.form.name.trim().length === 0) return true;
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
      this.visible = ref(true);

      const method = this.mode === "ins" ? "insert" : "update";
      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al;

      api
        .post(baseURL, {
          id: this.form.id,
          method: "dimobj/" + method,
          params: [{ rec: this.form }],
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
          this.visible = ref(false);
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },
  created() {
    this.visible = ref(true);
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{ dict: "FD_AccessLevel" }],
      })
      .then((response) => {
        this.optAL = response.data.result.records;
      })
      .finally(() => {
        this.visible = ref(false);
      });
    //
  },
};
</script>
