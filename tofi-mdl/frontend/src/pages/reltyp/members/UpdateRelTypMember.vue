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
            :dense="dense"
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
            :dense="dense"
            :model-value="form.fullName"
            v-model="form.fullName"
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- card-->
        <q-input
            :dense="dense"
            type="number"
            :model-value="form.card"
            v-model="form.card"
            :label="$t('fldCard')"
            :rules="[(val) => val >= 0 || $t('msgCard')]"
        >
        </q-input>

        <!-- memberTyp -->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="mt"
            :options="optMT"
            :label="$t('memberType')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="form.memberType"
            :disable="mode === 'upd'"
            @update:model-value="fnSelectMT()"
        />

        <!-- typ -->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="typ"
            :options="optTyp"
            :label="$t('typ')"
            option-value="id"
            option-label="name"
            map-options
            :model-value="typ"
            @update:model-value="fnSelectTyp()"
            :disable="typ_disabled"
        />

        <!-- relTypMemb -->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="relTypMemb"
            :options="optRT"
            :label="$t('reltyp')"
            option-value="id"
            option-label="name"
            map-options
            :model-value="relTypMemb"
            @update:model-value="fnSelectRT()"
            :disable="!typ_disabled"
        />

        <!-- role -->
        <q-select
            :dense="dense"
            v-model="relrole"
            :options="relroles"
            :label="$t('role2')"
            option-value="id"
            option-label="name"
            map-options
            :model-value="relrole"
            @update:model-value="fnSelRole"
        />

        <!-- cmt -->
        <q-input
            :dense="dense"
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
import allConsts from "pages/all-consts.js";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "lg", "dense"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      optMT: [],
      mt: this.data.memberType,
      relroles: [],
      relrole: this.data.role,
      optTyp: [],
      typ: this.data.typ,
      optRT: [],
      relTypMemb: this.data.relTypMemb,
      //
      typ_disabled: false
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectMT() {
      this.form.memberType = this.mt.id;
      if (this.mt.id===allConsts.FD_MemberType.typ) {
        this.relTypMemb = null
        this.form.relTypMemb = null
        this.typ_disabled = false
      } else {
        this.typ = null
        this.form.typ = null
        this.typ_disabled = true
      }
    },

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

    fnSelRole() {
      this.form.role = this.relrole.id;
    },

    fnSelectTyp() {
      this.form.typ = this.typ.id;
      this.form.relTypMemb = null;
    },

    fnSelectRT() {
      this.form.typ = null;
      this.form.relTypMemb = this.relTypMemb.id;
    },

    validSave() {
      return !this.form.name ||
          !this.form.fullName ||
          this.form.card < 0 ||
          (this.form.memberType === 1 && this.form.typ === 0) ||
          (this.form.memberType === 2 && this.form.relTypMemb === 0);
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
      const method =
          this.mode === "ins" ? "insertRelTypMember" : "updateRelTypMember";

      api
          .post(baseURL, {
            id: this.form.id,
            method: "reltyp/" + method,
            params: [{rec: this.form}],
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

    if (this.mode === "ins") {
      this.typ_disabled = false
    } else {
      this.typ_disabled = this.form.memberType !== allConsts.FD_MemberType.typ;
    }

  },
};
</script>
