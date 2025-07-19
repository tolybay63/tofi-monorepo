<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"

  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>

        <!-- measure -->
        <q-item-label
          class="text-grey-7"
          style="font-size: 0.8em; margin-top: 10px"
        >{{ $t("measure") }}
        </q-item-label>

        <treeselect
          v-model="form.measure"
          :options="optMeasure"
          :default-expand-level="1"
          :noChildrenText="$t('noChilds')"
          :noOptionsText="$t('noResult')"
          :noResultsText="$t('noResult')"
          :normalizer="normalizerMeasure"
          :placeholder="$t('select')"
          @select="fnSelectMeasure"
        />

        <!-- minVal -->
        <q-input
          v-model="form.minVal"
          :label="$t('minVal')"
          :model-value="form.minVal"
          type="number" dense
        />

        <!-- maxVal -->
        <q-input
          v-model="form.maxVal"
          :label="$t('maxVal')"
          :model-value="form.maxVal"
          type="number" dense
        />

        <!-- digit  -->
        <q-input
          v-model="form.digit"
          :disable="mode === 'upd' && form.parent !== undefined"
          :label="$t('digit')"
          :model-value="form.digit"
          clearable
          type="number" dense
          :rules="[ (val) => (!val || val >= 0) || 'Количество знаков после запятой должно быть не отрицательной']"
        />

        <!---->
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('save')"
          color="primary"
          dense
          icon="save"
          @click="onOKClick"
        />
        <q-btn
          :label="$t('cancel')"
          color="primary"
          dense
          icon="cancel"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError, notifySuccess, pack} from "src/utils/jsutils";
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";

export default {
  components: {treeselect},
  props: ["data", "mode", "lg"],

  data() {
    return {
      form: this.data,
      lang: this.lg,
      meter: this.data.meter,
      optMeasure: [],
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    normalizerMeasure(node) {
      return {
        id: node.id,
        label: node.name,
      };
    },

    fnSelectMeasure(v) {
      this.form.measure = v.id;
    },

    validSave() {
      return !(this.form.measure && (!this.form.digit || this.form.digit >= 0));
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

      const method = this.mode === "ins" ? "insDimMultiPropItemMeter" : "updDimMultiPropItemMeter";

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
    api
      .post(baseURL, {
        method: "dimMultiProp/loadMeasure",
        params: [],
      })
      .then((response) => {
        this.optMeasure = pack(response.data.result.records, "id");
      });
  },
};
</script>
