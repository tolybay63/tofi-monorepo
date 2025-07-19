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
      <q-bar v-if="isIns.isIns" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="!isIns.isIns" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-input
            :dense="dense"
            v-model="myData.cod"
            :model-value="myData.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
        />

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
        />

        <!-- name -->
        <q-input
            :dense="dense"
            :model-value="myData.name"
            v-model="myData.name"
            autofocus
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
            :dense="dense"
            :model-value="myData.fullName"
            v-model="myData.fullName"
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- cmt -->
        <q-input
            :dense="dense"
            :model-value="myData.cmt"
            v-model="myData.cmt"
            type="textarea"
            :label="$t('fldCmt')"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            :dense="dense"
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validName()"
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
  props: ["form", "upd", "lg", "action", "dense"],

  data() {
    return {
      myData: this.form,
      isIns: this.upd,
      lang: this.lg,
      act: this.action,
      options: [],
      al: this.form.accessLevel === undefined ? 1 : this.form.accessLevel,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    onBlurName() {
      if (this.myData.name) {
        this.myData.name = this.myData.name.trim();
        if (
            !this.myData.fullName ||
            (this.myData.fullName && this.myData.fullName.trim() === "")
        ) {
          this.myData.fullName = this.myData.name;
        }
      }
    },

    fnSelect() {
      //console.log("select", this.al)
      this.myData.accessLevel = this.al.id;
    },

    validName() {
      if (!this.myData.name) return true;
      else if (this.myData.name.trim().length === 0) return true;
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

      //delete this.myData.accessLevel_text
      //console.log("al:", this.al)
      //console.log("this.myData:", this.myData)

      const method = this.isIns.isIns ? "insert" : "update";
      this.myData.accessLevel =
          typeof this.al === "object" ? this.al.id : this.al;
      api
          .post(baseURL, {
            id: this.myData.id,
            method: this.act + "/" + method,
            params: [{rec: this.myData}],
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
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          //console.log("FD_AccessLevel", response.data.result.records)
          this.options = response.data.result.records;
        });

    return {};
  },
};
</script>
