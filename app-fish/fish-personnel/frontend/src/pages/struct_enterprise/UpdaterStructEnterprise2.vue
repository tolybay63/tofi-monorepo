<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    style="width: 600px"
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
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
          {{ $t("parentObj") }}: {{ parentName }}
        </q-item-section>

        <!-- name -->
        <q-input
          v-model="form.name"
          :label="fnLabel('fldName', true)"
          :model-value="form.name"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          autofocus
          @blur="onBlurName"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          :label="fnLabel('fldFullName', true)"
          :model-value="form.fullName"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- clsName -->
        <q-select
          v-model="form.cls"
          :label="fnLabel('cls', true)"
          :model-value="form.cls"
          :options="optCls"
          map-options
          option-label="name"
          option-value="id"
          @update:model-value="fnSelect()"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
          :label="fnLabel('fldCmt', false)"
          :model-value="form.cmt"
          type="textarea"
        >
        </q-input>
        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validName()"
          :label="$t('save')"
          color="primary"
          icon="save"
          @click="onOKClick"
        />
        <q-btn
          :label="$t('cancel')"
          color="primary"
          icon="cancel"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["mode", "isChild", "parentName", "data"],

  data() {
    return {
      form: this.data,
      optCls: [],
    };
  },
  //al: this.form.accessLevel===0 ? 1 : this.form.accessLevel
  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnLabel(txt, req) {
      if (req)
        return this.$t(txt) + "*";
      else
        return this.$t(txt);
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

    fnSelect(v) {
      //console.log("select Cls", v)
    },


    validName() {
      return (
        !this.form.name ||
        !this.form.fullName ||
        !this.form.cls
      );
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

      this.form.cls =
        typeof this.form.cls === "object" ? this.form.cls.id : this.form.cls

      const method = this.mode === "ins" ? "insertEnterprise" : "updateEnterprise";
      api
        .post("", {
          method: "data/" + method,
          params: [this.form],
        })
        .then(
          () => {
            this.$emit("ok", {res: true});
            notifySuccess(this.$t("success"));
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
            let msg = error.response.data.error.message
              ? error.response.data.error.message
              : error.message;
            notifyError(msg);
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
    let flag = "parents"
    if (this.isChild) {
      flag = "childs"
    }
    api
      .post("", {
        method: "data/loadCls",
        params: ["Typ_Enterprise", flag],
      })
      .then((response) => {
        this.optCls = response.data.result.records;
      })
      .then(() => {
        if (this.mode==="ins" && flag === "parents") {
          this.form.cls = this.optCls[0].id;
        }
      })
      .then(() => {
        //console.info("data", this.data)
      })

  },
};
</script>
