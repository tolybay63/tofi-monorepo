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

        <!-- cod -->
        <q-input
          :dense="dense"
          v-model="form.cod"
          :model-value="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <!-- name -->
        <q-input
            :dense="dense"
            :model-value="form.name"
            v-model="form.name"
            autofocus
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fullName-->
        <q-input
            :dense="dense"
            :model-value="form.fullName"
            v-model="form.fullName"
            :label="$t('fldFullName')"
            :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!--reltyp-->
        <q-select
          dense options-dense
          v-model="form.relTyp"
          :model-value="form.relTyp"
          :options="optRelTyp"
          :label="$t('reltyp')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectRelTyp"
        />

        <!--relcls-->
        <q-select
          dense options-dense
          v-model="form.relCls"
          :model-value="form.relCls"
          :options="optRelCls"
          :label="$t('relcls')"
          option-value="id"
          option-label="name"
          map-options clearable @clear="fnClearRelCls"
          @update:model-value="fnSelectRelCls"
        />


        <!--accessLevel-->
        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="al"
            :options="optionsAL"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="al"
            @update:model-value="fnSelect"
        />

        <!-- cmt -->
        <q-input
            :dense="dense"
            :model-value="form.cmt"
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
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
import {notifyError, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";

export default {

  props: ["data", "mode", "lg", "dense"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      optRelTyp: [],
      optRelCls: [{id: null, name: "Все классы"}],

      al: this.data.accessLevel,
      optionsAL: [],
      loading: ref(false)
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

/*    normalizerRelCls(node) {
      return {
        id: node.relcls,
        label: node.name,
      };
    },*/

    fnSelectRelTyp(val) {
      this.form.relTyp = val.id;
      this.loadRelCls(val.id)
    },

    loadRelCls(relTyp) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "reltyp/loadRelClsForSelect",
          params: [relTyp],
        })
        .then((response) => {
          this.optRelCls = response.data.result.records
        })
        .then(()=> {
          this.optRelCls.push({id: null, name: "Все классы"})
        })
        .then(()=> {
          console.info("optRelCls", this.optRelCls)
        })
        .catch(error=> {
          notifyError(error.response.data.error.message)
        })
        .finally(() => {
          this.loading = ref(false);
        })
    },

    fnClearRelCls() {
      this.form.relCls = null;
    },

    fnSelectRelCls(val) {
      if (val)
        this.form.relCls = val.id;
      else
        this.form.relCls = null
    },


    fnSelect() {
      //console.log("select", this.form.accessLevel, this.al)
      this.form.accessLevel = this.al.id;
    },

    validSave() {
      return !(
          this.form.name !== null &&
          this.form.name !== undefined &&
          this.form.fullName !== null &&
          this.form.fullName !== undefined
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

      const method =
          this.mode === "ins" ? "insertRelTypCharGr" : "updateRelTypCharGr";

      api
          .post(baseURL, {
            method: "reltyp/" + method,
            params: [this.form],
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
    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          //console.log("FD_AccessLevel", response.data.result.records)
          this.optionsAL = response.data.result.records;
        })
        .finally(() => {
          this.loading = ref(false);
        });


    this.loading = ref(true);
    api
    .post(baseURL, {
      method: "reltyp/loadRelTypForSelect",//"dimMultiProp/loadRelClsForSelect",
      params: [],
    })
    .then((response) => {
          this.optRelTyp = response.data.result.records
        })
      .catch(error=> {
        notifyError(error.response.data.error.message)
      })
    .finally(() => {
      this.loading = ref(false);
    })

    if (this.mode === "upd") {
      this.loadRelCls(this.data.relTyp)
    }


  },
};
</script>
