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

        <!-- card-->
        <q-input
            dense
            type="number"
            :model-value="form.card"
            v-model="form.card"
            :label="$t('fldCard')"
            :rules="[(val) => val >= 0 || $t('msgCard')]"
        >
        </q-input>

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
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,

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

    validSave() {
      return !this.form.name ||
          !this.form.fullName ||
          this.form.card < 0
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

      let err = false;

      api
          .post(baseURL, {
            id: this.form.id,
            method: "relcls/updateRelClsMember",
            params: [this.form],
          })
          .then(
              (response) => {
                err = false;
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                //console.log("error.response.data=>>>", error.response.data.error.message)
                err = true;
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
          method: "dict/load",
          params: [{dict: "FD_MemberType"}],
        })
        .then((response) => {
          this.optMT = response.data.result.records;
          this.optMT.splice(2, 2);
        });
    //
    api
        .post(baseURL, {
          method: "role/loadRoles",
          params: [{}],
        })
        .then((response) => {
          //this.measures = pack(response.data.result.records)
          this.relroles = response.data.result.records;
          this.relroles.unshift({id: 0, name: this.$t("notChosen")});
        });

    api
        .post(baseURL, {
          method: "typ/loadTypForSelect",
          params: [{}],
        })
        .then((response) => {
          this.optTyp = response.data.result.records;
          this.optTyp.unshift({id: 0, name: this.$t("notChosen")});
        });

    api
        .post(baseURL, {
          method: "reltyp/loadRelTypForSelect",
          params: [],
        })
        .then((response) => {
          this.optRT = response.data.result.records;
          this.optRT.unshift({id: 0, name: this.$t("notChosen")});
        });

    return {};
  },
};
</script>
