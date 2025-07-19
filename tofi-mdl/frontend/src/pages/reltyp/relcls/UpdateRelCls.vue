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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card>

        <q-card-section>

          <!-- Cod -->
          <q-input
              style="margin-bottom: 10px"
              :dense="dense"
              v-model="form.cod"
              :model-value="form.cod"
              :label="$t('code')"
              :placeholder="$t('msgCodeGen')"
          />

          <!-- name -->
          <q-input
              autofocus
              :dense="dense"
              :model-value="form.name"
              v-model="form.name"
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

          <!-- isOpenness -->
          <q-toggle
              style="margin-left: 5px; margin-top: 5px; margin-bottom: 10px "
              :dense="dense"
              v-model="form.isOpenness"
              :model-value="form.isOpenness"
              :label="$t('isOpenness')"
          />

          <!-- accessLevel -->
          <q-select
              :dense="dense"
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              :model-value="al"
              @update:model-value="fnSelectAL()"
          />

          <!-- database -->
          <q-select
              :dense="dense"
              v-model="db"
              :options="optionsDB"
              :label="$t('modelNameLabel')"
              option-value="id"
              option-label="name"
              map-options
              :model-value="db"
              @update:model-value="fnSelectDB()"
          />


          <!-- cmt -->
          <q-input
              :dense="dense"
              :model-value="form.cmt"
              v-model="form.cmt"
              type="textarea"
              :label="$t('fldCmt')"
          />
        </q-card-section>


      </q-card>

      <q-card-actions align="right">
        <q-btn
            :loading="loading"
            :dense="dense"
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white"/>
          </template>
        </q-btn>

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
import {notifyError, notifySuccess,} from "src/utils/jsutils";
import {ref} from "vue";


export default {
  props: ["data", "mode", "typ", "lg", "dense"],

  data() {
    //console.log("data", this.data)
    return {
      cols: [],
      rows: [],
      form: this.data,
      loading: ref(false),
      separator: ref("cell"),
      optionsLevel: [],
      al: this.data.accessLevel,

      optionsDB: [],
      db: this.data.dataBase,

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

    fnSelectDB() {
      this.form.dataBase = this.db.id;
    },

    validSave() {
      return !(
          this.form.name &&
          this.form.fullName &&
          this.form.dataBase !== null
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

      let ids = [];
      this.rows.forEach((row) => {
        const {children} = row;
        children.forEach((it) => {
          if (it.checked === 1) ids.push(it.id.toString());
        });
      });
      const method = this.mode === "ins" ? "insert" : "update";
      let err = false;
      api
          .post(baseURL, {
            method: "relcls/" + method,
            params: [this.form],
          })
          .then(
              (response) => {
                err = false;
                //this.$emit("ok", response.data.result.records[0]);
                this.$emit("ok", response.data.result.records[0]);
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true;
                //console.log("error.response.data=>>>", error.response.data.error.message)
                let msg = error.message;
                if (error.response) {
                  msg = this.$t(error.response.data.error.message);
                }
                notifyError(msg);
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
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optionsLevel = response.data.result.records;
        });

    api
        .post(baseURL, {
          method: "database/loadDbForSelect",
          params: [],
        })
        .then((response) => {
          this.optionsDB = response.data.result.records;
        });

  },
};
</script>
