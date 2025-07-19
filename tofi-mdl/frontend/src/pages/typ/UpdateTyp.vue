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
      <!-- cod -->
      <q-card-section>
        <q-input
            :dense="dense"
            v-model="myData.cod"
            :model-value="myData.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
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
        <!-- dbeg-->
        <q-input
            :dense="dense"
            type="date"
            stack-label
            :model-value="myData.dbeg"
            v-model="myData.dbeg"
            :label="$t('fldDbeg')"
        >
        </q-input>
        <!-- dend-->
        <q-input
            :dense="dense"
            type="date"
            stack-label
            :model-value="myData.dend"
            v-model="myData.dend"
            :label="$t('fldDend')"
        >
        </q-input>

        <q-toggle
            style="margin-left: 10px"
            :dense="dense"
            v-model="myData.isOpenness"
            :model-value="myData.isOpenness"
            :label="$t('isOpenness')"
        />

        <!-- Parent -->
        <q-select
            :dense="dense"
            v-model="parentTyp"
            :options="parentTyps"
            :label="$t('parentTyp')"
            option-value="id"
            option-label="name"
            map-options
            :model-value="parentTyp"
            @update:model-value="fnSelParentTyp"
        />

        <!-- accessLevel -->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="al"
            :options="optAL"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="al"
            @update:model-value="fnSelectAL()"
        />

        <!-- typCategory -->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="typCateg"
            :options="optCateg"
            :label="$t('typCategory')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="typCateg"
            @update:model-value="fnSelectCateg()"
        />

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
  props: ["form", "mode", "lg", "action", "dense"],

  data() {
    return {
      myData: this.form,
      lang: this.lg,
      act: this.action,
      optAL: [],
      al: this.form.accessLevel,
      optCateg: [],
      typCateg: this.form.typCategory,

      parentTyps: [],
      parentTyp: this.form.parent,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnSelParentTyp() {
      this.myData.parent = this.parentTyp.id;
    },

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

    fnSelectAL() {
      //console.log("select", this.al)
      this.myData.accessLevel = this.al.id;
    },

    fnSelectCateg() {
      this.myData.typCategory = this.typCateg.id;
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

      let err = false
      const method = this.mode === "ins" ? "insert" : "update";
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
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.optAL = response.data.result.records;
        });
    //
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_TypCategory"}],
        })
        .then((response) => {
          this.optCateg = response.data.result.records;
        });
    //
    api
        .post(baseURL, {
          method: "typ/loadTypParent",
          params: [{}],
        })
        .then((response) => {
          this.parentTyps = response.data.result.records;
        });

    return {};
  },
};
</script>
