<template>
  <q-dialog
      ref="dialog"
      @hide="onDialogHide"
      persistent
      autofocus
      transition-show="slide-up"
      transition-hide="slide-down"
      style="width: 800px"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card-section>
        <q-select
            v-model="factor"
            map-options
            :model-value="factor"
            use-input
            input-debounce="0"
            :label="$t('factor')"
            :options="optionsFactor"
            option-value="id"
            option-label="name"
            :dense="dense"
            :options-dense="dense"
            clearable
            @clear="fnClearFactor"
            @update:model-value="inputValueFactor"
            @filter="filterFnFactor"
            :disable="factorDisable"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-select
            v-model="typ"
            :model-value="typ"
            use-input
            map-options
            input-debounce="0"
            :label="$t('typ')"
            :options="optionsTyp"
            option-value="id"
            option-label="name"
            :dense="dense"
            :options-dense="dense"
            @update:model-value="inputValueTyp"
            @filter="filterFnTyp"
            clearable
            @clear="fnClearTyp"
            :disable="typDisable"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-select
            v-model="relTyp"
            :model-value="relTyp"
            map-options
            use-input
            input-debounce="0"
            :label="$t('reltyp')"
            :options="optionsRelTyp"
            option-value="id"
            option-label="name"
            :dense="dense"
            :options-dense="dense"
            :disable="relTypDisable"
            @update:model-value="inputValueRelTyp"
            @filter="filterFnRelTyp"
            clearable
            @clear="fnClearRelTyp"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-toggle
            style="margin-left: 10px; margin-top: 10px"
            :dense="dense"
            model-value="form.isReq"
            v-model="form.isReq"
            :label="$t('isReq')"
        />
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
  props: ["data", "mode", "multiProp", "lg", "dense"],

  data() {
    //console.log("data 2", this.data)

    return {
      form: this.data,
      visible: ref(false),

      factor: this.data.factor,
      optionsFactor: [],
      optionsFactorOrg: [],

      typ: this.data.typ,
      optionsTyp: [],
      optionsTypOrg: [],

      relTyp: this.data.relTyp,
      optionsRelTyp: [],
      optionsRelTypOrg: [],

      factorDisable: false,
      typDisable: false,
      relTypDisable: false,
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fnClearFactor() {
      this.form.factor = null;
      this.typDisable = false;
      this.relTypDisable = false;
    },

    fnClearTyp() {
      this.form.typ = null;
      this.factorDisable = false;
      this.relTypDisable = false;
    },

    fnClearRelTyp() {
      this.form.relTyp = null;
      this.factorDisable = false;
      this.typDisable = false;
    },

    inputValueFactor(val) {
      if (val) {
        this.factor.id = val.id;
        this.form.factor = val.id;

        this.typDisable = true;
        this.relTypDisable = true;
      }
    },

    inputValueTyp(val) {
      if (val) {
        this.typ.id = val.id;
        this.form.typ = val.id;

        this.factorDisable = true;
        this.relTypDisable = true;
      }
    },

    inputValueRelTyp(val) {
      if (val) {
        this.relTyp.id = val.id;
        this.form.relTyp = val.id;

        this.typDisable = true;
        this.factorDisable = true;
      }
    },

    filterFnFactor(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsFactor = this.optionsFactorOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsFactorOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsFactor = this.optionsFactorOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    filterFnTyp(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsTyp = this.optionsTypOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsTypOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsTyp = this.optionsTypOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    filterFnRelTyp(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optionsRelTyp = this.optionsRelTypOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsRelTypOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optionsRelTyp = this.optionsRelTypOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },

    validSave() {
      return !(
          (
              this.form.factor !== null ||
              this.form.typ !== null ||
              this.form.relTyp !== null
          ) /*&& this.mode !=='upd'*/
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
      this.form.multiProp = this.multiProp;
      //console.log("form", this.form);
      const method =
          this.mode === "ins" ? "insertMultiPropCond" : "updateMultiPropCond";

      let err = false;
      api
          .post(baseURL, {
            method: "multiProp/" + method,
            params: [this.form],
          })
          .then(
              () => {
                err = false;
                this.$emit("ok", {res: true});
                notifySuccess(this.$t("success"));
              },
              (error) => {
                err = true;
                //console.log("error.response.data=>>>", error.response.data.error.message)
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

    loadFactors() {
      this.visible = ref(true);
      api
          .post(baseURL, {
            method: "multiProp/loadFactorsForSelect",
            params: [this.multiProp, this.mode],
          })
          .then((response) => {
            this.optionsFactorOrg = response.data.result.records;
            this.optionsFactor = response.data.result.records;
          })
          .finally(() => {
            if (this.mode === "upd") {
              this.factorDisable = true;
              this.typDisable = true;
              this.relTypDisable = true;
            }
            this.visible = ref(false);
          });
    },

    loadTyps() {
      this.visible = ref(true);
      api
          .post(baseURL, {
            method: "multiProp/loadTypsForSelect",
            params: [this.multiProp, this.mode],
          })
          .then((response) => {
            this.optionsTypOrg = response.data.result.records;
            this.optionsTyp = response.data.result.records;
          })
          .finally(() => {
            if (this.mode === "upd") {
              this.factorDisable = true;
              this.typDisable = true;
              this.relTypDisable = true;
            }
            this.visible = ref(false);
          });
    },

    loadRelTyps() {
      this.visible = ref(true);
      api
          .post(baseURL, {
            method: "multiProp/loadRelTypsForSelect",
            params: [this.multiProp, this.mode],
          })
          .then((response) => {
            this.optionsRelTypOrg = response.data.result.records;
            this.optionsRelTyp = response.data.result.records;
          })
          .finally(() => {
            if (this.mode === "upd") {
              this.factorDisable = true;
              this.typDisable = true;
              this.relTypDisable = true;
            }
            this.visible = ref(false);
          });
    },


  },
  created() {
    if (this.mode === "ins") {
      this.loadFactors();
      this.loadTyps();
      this.loadRelTyps();
    } else if (this.mode === "upd") {
      if (this.form.factor != null) {
        this.loadFactors();
      } else if (this.form.typ != null) {
        this.loadTyps();
      } else if (this.form.relTyp != null) {
        this.loadRelTyps();
      } else notifyError("Error :-(");
    }
  }
};
</script>
