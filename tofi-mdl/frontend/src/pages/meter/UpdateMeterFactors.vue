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
      <q-bar class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-select
            v-model="factor"
            @update:model-value="inputValue"
            use-input
            input-debounce="0"
            :label="$t('factor')"
            :options="options"
            option-label="name"
            :dense="dense"
            :options-dense="dense"
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

        <!-- ordDim -->
        <q-input
            :model-value="form.ordDim"
            v-model="form.ordDim"
            :label="$t('measureNumber')"
            type="number"
            input-class="text-right"
            :rules="[(val) => (val > 0 && val < maxDim + 1) || $t('invalidMeasureNumber')]"

        >
        </q-input>

        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
            color="primary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
            :disable="onofOk()"
        />
        <q-btn
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
  props: ["meterId", "data", "dense", "maxDim"],

  data() {
    return {
      form: this.data,
      factor: {id: 0, name: this.$t("notChosen")},
      options: [],
      optionsOrg: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    onofOk() {
      //console.log("onof", this.factor.id, this.form.ordDim)
      return (
          this.factor.id === 0 ||
          !(this.form.ordDim > 0 && this.form.ordDim <= this.maxDim)
      );
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
          this.options = this.optionsOrg;
        });
        return;
      }
      update(() => {
        if (this.optionsOrg.length < 2) return;
        const needle = val.toLowerCase();
        let name = "name";
        this.options = this.optionsOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1;
        });
      });
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

      this.form.meter = this.meterId;
      api
          .post(baseURL, {
            method: "meterfactor/insert",
            params: [{rec: this.form}],
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
    api
        .post(baseURL, {
          method: "meterfactor/factors",
          params: [{meter: this.meterId}],
        })
        .then((response) => {
          this.optionsOrg = response.data.result.records;
          this.options = response.data.result.records;
          this.options.unshift({id: 0, name: this.$t("notChosen")});
          //console.log("options", this.options)
        });

    return {};
  },
};
</script>
