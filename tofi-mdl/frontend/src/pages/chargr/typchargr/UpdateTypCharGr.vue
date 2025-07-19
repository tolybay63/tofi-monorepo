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


        <!-- typ -->
        <q-select
            :disabled="mode === 'upd'"
            :dense="dense"
            :options-dense="dense"
            v-model="typ"
            :model-value="typ"
            :options="optTyp"
            :label="$t('typ')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectTyp()"
        />

        <!-- Clust FV-->
        <q-item-label
            :class="form.factorVal===0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px" class="row"
        >{{ $t("factorVal") }}
          <q-space/>
          <q-icon name="error" v-if="form.factorVal===0" color="red-10" size="24px"></q-icon>
        </q-item-label
        >
        <treeselect
            :disabled="mode === 'upd'"
            :options="optionsCFV"
            v-model="form.factorVal"
            :default-expand-level="1"
            max-heigth="600"
            :normalizer="normalizer"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @select="fnSelectCFV"
        />
        <q-item-label v-if="form.factorVal===0"
                      class="text-red-10" style="font-size: 0.8em"
        >
          {{ $t('chooseFV') }}
        </q-item-label>

        <!-- cod -->
        <q-input
            :dense="dense"
            v-model="form.cod"
            :model-value="form.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
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
            @update:model-value="fnSelectAL"
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
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";

export default {
  components: {treeselect},

  props: ["data", "mode", "lg", "dense"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      al: this.data.accessLevel,
      optionsAL: [],
      optionsCFV: [],
      optTyp: [],
      typ: this.data.typ,

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnSelectTyp() {
      this.form.typ = this.typ.id
      this.optionsCFV = []
      this.form.factorVal = null;
      this.loadclustFactorVal(this.form.typ, this.mode)
    },

    fnSelectCFV(v) {
      this.form.factorVal = v.key
    },

    normalizer(node) {
      return {
        id: node.key,
        label: node.name,
      };
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

    fnSelectAL() {
      //console.log("select", this.form.accessLevel, this.al);
      this.form.accessLevel = this.al.id;
    },

    validSave() {
      return !(
          this.form.name && this.form.fullName && this.form.typ && this.form.factorVal && this.form.factorVal !== 0)
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

      this.form.accessLevel =
          typeof this.al === "object" ? this.al.id : this.al;
      this.form.typ =
          typeof this.typ === "object" ? this.typ.id : this.typ;

      const method =
          this.mode === "ins" ? "insertTypCharGr" : "updateTypCharGr";

      api
          .post(baseURL, {
            method: "typ/" + method,
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

    loadclustFactorVal(typ, mode) {
      api
          .post(baseURL, {
            method: "typ/loadTypClustFactorVal",
            params: [typ, mode],
          })
          .then((response) => {
            this.optionsCFV = pack(response.data.result.records, "ord");
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
          this.optionsAL = response.data.result.records;
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


    if (this.mode === 'upd') {
      this.loadclustFactorVal(this.form.typ, this.mode)
    }

  },
};
</script>
