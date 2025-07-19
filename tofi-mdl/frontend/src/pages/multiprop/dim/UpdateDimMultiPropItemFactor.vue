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

      <q-inner-loading :showing="visible" color="secondary"/>

      <q-card-section>

        <!-- factor -->
        <q-select
            v-model="factor"
            :model-value="factor"
            @update:model-value="inputValue"
            use-input
            input-debounce="0"
            :label="$t('factor')"
            :options="optFactor"
            option-label="name"
            dense options-dense
            @filter="filterFn"
            autofocus
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <!-- format -->
        <q-select
            v-model="vf"
            :model-value="vf"
            :options="optVF"
            :label="$t('visualFormat')"
            option-value="id"
            option-label="text"
            map-options
            dense options-dense
            @update:model-value="fnSelectVF"
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
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess} from "src/utils/jsutils";
import {ref} from "vue";

export default {
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      visible: ref(false),

      vf: this.data.visualFormat,
      optVF: [],

      factor: {id: 0, name: this.$t("notChosen")},
      optFactor: [],
      optFactorOrg: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectVF() {
      this.form.visualFormat = this.vf.id
    },

    inputValue(val) {
      if (val) {
        this.factor.id = val.id;
        this.form.factor = val.id;
      }
    },

    filterFn(val, update) {
      if (val === null || val === "") {
        update(() => {
          this.optFactor = this.optFactorOrg
        });
        return;
      }
      update(() => {
        if (this.optFactorOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.optFactor = this.optFactorOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
    },


    validSave() {
      return !this.form.factor;
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

      const method = this.mode === "ins" ? "insDimMultiPropItemFactor" : "updDimMultiPropItemFactor";

      this.form.visualFormat =
        typeof this.vf === "object" ? this.vf.id : this.vf;
      api
          .post(baseURL, {
            method: "dimMultiProp/" + method,
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

    this.visible = ref(true)
    api
      .post(baseURL, {
        method: "dimMultiProp/loadFactors",
        params: [],
      })
      .then((response) => {
        this.optFactor = response.data.result.records;
        this.optFactorOrg = response.data.result.records;
        this.optFactor.unshift({id: 0, name: this.$t("notChosen")});
      })
        .finally(()=> {
          this.visible = ref(false)
        });

    this.visible = ref(true)
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_VisualFormat"}],
        })
        .then((response) => {
          this.optVF = response.data.result.records;
        })
        .finally(()=> {
          this.visible = ref(false)
        });


  },
};
</script>
