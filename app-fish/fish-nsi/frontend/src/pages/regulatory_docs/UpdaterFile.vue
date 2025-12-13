<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("attach_file") }}</div>
      </q-bar>

      <q-inner-loading :showing="loading" color="secondary"/>

      <q-card>
        <q-card-section>

          <q-input
            autofocus dense
            type="file"
            v-model="file"
            :clearable="true"
            @update:model-value="updFile"
            @clear="clrFile"
            class="q-my-lg"
          />

<!--
          <q-input
            :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
          />
-->

        </q-card-section>

      </q-card>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary" icon="save"
          autofocus
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

import {ref} from "vue";
import {useUserStore} from "stores/user-store";

const store = useUserStore();


export default {
  props: ["obj", "propCod"],

  data() {

    return {
      loading: false,
      form: ref({propCod: this.propCod}),
      file: ref(null),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    clrFile() {
      this.file = ref(null)
      //console.log("FILE(clr)", this.file)
    },

    updFile(val) {
      if (val !== null) {
        this.file = val[0]
        //console.log("fn", this.file.name)
      }
    },

    validSave() {
      return this.file === null
    },


    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show()
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide()
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit("hide")
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog
      this.loading = true
      this.form.isObj = 1
      this.form.objorrelobj = this.obj
      this.form.model = "nsidata"
      this.form.metamodel = "fish"
      let err = false

      //extend(this.form, {filename: this.file.name})
      Object.assign(this.form, {filename: this.file.name})
      let fd = new FormData()
      fd.append("file", this.file)
      fd.append("params", JSON.stringify(this.form))

      this.$axios
        .post("/upload", fd, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        })
        .then(() => {
          err = false;
          this.$emit("ok", {res: true})
          //notifySuccess(this.$t("success"))
        })
        .catch(() => {
          err = true
          //console.log("error=>>>", error.message)
          //console.log("error.response=>>>", error.response)
          //console.log("error.response.data=>>>", error.response.data)
          //setUserStore({})
          //notifyError(this.$t("notLogined"))
        })
        .finally(() => {
          this.loading = false
          if (!err) this.hide()
        });

    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide()
    },

  },

  created() {
  },
}
</script>
