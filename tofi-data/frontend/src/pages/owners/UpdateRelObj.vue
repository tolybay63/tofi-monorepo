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

        <q-item-label class="q-ma-md">
          <span class="text-blue">{{ $t('relcls') }}:</span> {{ name }}
        </q-item-label>

      <!-- cod -->
      <q-card-section>
        <q-input
          dense autofocus
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
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          dense
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- dbeg-->
        <q-input
          dense
          type="date"
          stack-label
          :model-value="form.dbeg"
          v-model="form.dbeg"
          :label="$t('fldDbeg')"
        />
        <!-- dend-->
        <q-input
          dense
          type="date"
          stack-label
          :model-value="form.dend"
          v-model="form.dend"
          :label="$t('fldDend')"
        />

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
        />
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
import { api, baseURL } from "boot/axios";
import { notifyError, notifySuccess } from "src/utils/jsutils";
import {useParamsStore} from "stores/params-store";
import {storeToRefs} from "pinia";
const storeParams = useParamsStore();
const { getModel, getMetaModel } = storeToRefs(storeParams);

export default {
  props: ["data", "mode", "relCls", "name"],

  data() {
    return {
      form: this.data,
      optAL: [],
      al: this.data.accessLevel,
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
        this.form.fullName = this.form.name.trim();
      }
    },

    fnSelectAL() {
      //console.log("select", this.al)
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

      let err = false;
      this.form.accessLevel =
        typeof this.al === "object" ? this.al.id : this.al
      this.form.relCls = this.relCls
      this.form.isObj = false
      this.form.mode = this.mode
      this.form.model = getModel.value
      this.form.metamodel = getMetaModel.value
      api
        .post(baseURL, {
          method: "data/createOwn",
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
        method: "data/loadDict",
        params: ["FD_AccessLevel"],
      })
      .then((response) => {
        this.optAL = response.data.result.records;
      })
      .catch((error)=> {
        console.error(error.message);
        notifyError(this.$t(error.message))
      })
    //

    return {};
  },
};
</script>
