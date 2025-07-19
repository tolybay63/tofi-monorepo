<!-- DimObjItemPropVal -->
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

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>

        <div v-if="pt === 22 || pt === 33 || pt === 46 || pt === 47">
          <q-input
              autofocus dense
              :model-value="form.numberVal"
              v-model="form.numberVal"
              :label="$t('val')" type="number"
          >
          </q-input>
        </div>

        <div v-if="pt === 41 || pt === 42">
          <q-input
              autofocus dense
              :model-value="form['strVal']"
              v-model="form['strVal']"
              :label="$t('val')"
          />
        </div>

        <div v-if="pt === 43 || pt === 44 || pt === 45">
          <q-input
              autofocus dense
              :model-value="form['dateTimeVal']"
              v-model="form['dateTimeVal']"
              :label="$t('val')"
              type="date" stack-label class="q-mt-md"
          />
        </div>

        <div v-if="pt === 48">
          <q-input
              autofocus dense
              :model-value="form['multiStrVal']"
              v-model="form['multiStrVal']"
              :label="$t('val')"
              type="textarea" class="q-mt-lg"
          />
        </div>

        <div v-if="pt === 11 || pt === 55 || pt === 66 || pt === 77">
          <q-select
              v-model="propVal" :model-value="propVal"
              dense options-dense :options="optPropVal"
              :label="$t('val')" class="q-mt-lg"
              option-value="id" option-label="name" map-options
              @update:model-value="fnPropVal"
          />

        </div>

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense color="primary" icon="save" :label="$t('save')"
          @click="onOKClick" :disable="validSave()"
        />
        <q-btn
          dense color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {ref} from "vue";
import {notifyError, notifySuccess} from "src/utils/jsutils";

export default {
  props: ["data", "mode", "dimObjItemProp", "prop", "pt", "kf"],

  data() {
    return {
      form: this.data,

      propVal: this.data.propVal,
      optPropVal: [],

      visible: ref(false),
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnPropVal() {
      this.form.propVal = this.propVal.id
    },

    validSave() {
      if (this.pt === 22 || this.pt === 33 || this.pt === 46 || this.pt === 47) {
        return !this.form.numberVal
      }
      if (this.pt === 41 || this.pt === 42) {
        return !this.form["strVal"]
      }
      if (this.pt === 43 || this.pt === 44 || this.pt === 45) {
        return !this.form["dateTimeVal"]
      }
      if (this.pt === 48) {
        return !this.form["multiStrVal"]
      }
      if (this.pt === 11 || this.pt === 55 || this.pt === 66 || this.pt === 77) {
        return !this.form.propVal
      }
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

      this.visible = ref(true);
      const method = this.mode === "ins" ? "insertDOIpropValue" : "updateDOIpropValue";

      this.form.dimObjItemProp = this.dimObjItemProp;

      if (this.pt===22 || this.pt===23) {
        this.form.numberVal = this.form.numberVal * this.kf
      }

      api
        .post(baseURL, {
          id: this.form.id,
          method: "dimobj/" + method,
          params: [this.form],
        })
        .then(
          () => {
            this.$emit("ok", this.form);
            notifySuccess(this.$t("success"));
          },
          (error) => {
            notifyError(error.response.data.error.message);
          }
        )
        .finally(() => {
          this.hide();
          this.visible = ref(false);
        });
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide();
    },
  },

  created() {
    if (this.pt === 11 || this.pt === 55 || this.pt === 66 || this.pt === 77) {
      this.visible = ref(true);
      api
          .post(baseURL, {
            method: "dimobj/loadOptPropVal",
            params: [this.prop, this.pt, this.dimObjItemProp, this.mode],
          })
          .then((response) => {
            this.optPropVal = response.data.result.records;
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    }
  },
};
</script>
