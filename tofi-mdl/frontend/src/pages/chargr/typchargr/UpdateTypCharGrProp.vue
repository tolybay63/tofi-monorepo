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
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <div
            v-if="
            form.propType === propTypeMeter || form.propType === propTypeRate
          "
        >
          <q-item-label
              class="text-grey-7"
              style="font-size: 0.8em; margin-top: 10px"
          >{{ $t("propMeasure") }}
          </q-item-label
          >
          <treeselect
              v-scroll
              :options="optPropMeasure"
              v-model="form.typCharGrProp_measure"
              :default-expand-level="1"
              :normalizer="normalizerPropMeasure"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
              defaultExpandLevel="1"
              @select="fnSelectPropMeasure"
          />
          <q-item-label v-if="form.typCharGrProp_measure===0"
                        class="text-red" style="font-size: 0.8em"
          >
            {{ $t("chooseProp") }}
          </q-item-label>

          <q-item-label
              class="text-grey-7"
              style="font-size: 0.8em; margin-top: 10px"
          >{{ $t("measure") }}
          </q-item-label
          >
          <treeselect
              v-scroll
              :options="optMeasure"
              v-model="form.propVal_measure"
              :default-expand-level="1"
              :normalizer="normalizerMeasure"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
              @select="fnSelectMeasure"
          />
        </div>

        <q-select
            :dense="dense"
            :options-dense="dense"
            v-model="st"
            :options="options"
            :label="$t('storageType')"
            option-value="id"
            option-label="text"
            map-options
            :model-value="st"
            @update:model-value="fnSelect()"
        />

        <q-select
            v-if="form.storageType === 2"
            :dense="dense"
            :options-dense="dense"
            v-model="ft"
            :model-value="ft"
            :options="optionsFT"
            :label="$t('flatTable')"
            option-value="id"
            option-label="nameTable"
            map-options
            @update:model-value="fnSelectFT()"
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
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {api, baseURL} from "boot/axios";
import {notifyError, pack} from "src/utils/jsutils";
import allConsts from "pages/all-consts";
import {ref} from "vue";
import {extend} from "quasar";

export default {
  components: {treeselect},
  props: ["dta", "typCharGr", "lg", "dense"],

  data() {
    let data = {};
    extend(true, data, this.dta);
    //console.log("dta", this.dta)
    //console.log("data", data)

    return {
      form: data,
      lang: this.lg,
      options: [],
      optionsFT: [],
      st: data.storageType === undefined ? null : data.storageType,
      ft: data.flatTable === undefined ? null : data.flatTable,
      optPropMeasure: [],
      optMeasure: [],
      propTypeMeter: allConsts.FD_PropType.meter,
      propTypeRate: allConsts.FD_PropType.rate,
      //

    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectPropMeasure(v) {
      //console.log("fnSelectPropMeasure", v)

      if (v.propType !== allConsts.FD_PropType.measure) {
        this.form.typCharGrProp_measure = null;
      } else {
        this.form.typCharGrProp_measure = v.prop;
        api
            .post(baseURL, {
              method: "typ/loadMeasure",
              params: [v.prop],
            })
            .then((response) => {
              this.optMeasure = pack(response.data.result.records, "id");
            });
      }
    },

    fnSelectMeasure(v) {
      //console.log("fnSelectMeasure", v)
      this.form.propVal_measure = v["propval"];
    },

    normalizerPropMeasure(node) {
      return {
        id: node.prop,
        label: node.name,
      };
    },

    normalizerMeasure(node) {
      return {
        id: node["propval"],
        label: node.name,
      };
    },

    fnSelect() {
      //console.log("select", this.st)
      this.form.storageType = this.st.id;
      if (this.form.storageType !== allConsts.FD_StorageType.flat)
        this.form.flatTable = null;
    },

    fnSelectFT() {
      this.form.flatTable = this.ft.id;
      //console.log("selectFT", this.form.flatTable)
    },

    validSave() {
      if (this.form.storageType !== allConsts.FD_StorageType.flat)
        return !(this.form.storageType > 0 && this.form.typCharGrProp_measure !== 0);
      else return !(this.form.storageType > 0 && this.form.typCharGrProp_measure !== 0 && this.ft !== null);
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

      this.form.storageType = typeof this.st === "object" ? this.st.id : this.st;

      if (this.form.storageType !== allConsts.FD_StorageType.flat)
        this.form.flatTable = null;
      else this.form.flatTable = this.ft.id;

      //console.info("Ok", this.form)

      api
          .post(baseURL, {
            method: "typ/updateTypCharGrProp",
            params: [this.form],
          })
          .then(
              () => {
                err = false;
                this.$emit("ok", {res: true});
                //notifySuccess(this.$t("success"))
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
          method: "dict/load",
          params: [{dict: "FD_StorageType"}],
        })
        .then((response) => {
          this.options = response.data.result.records;
        });

    if (
        this.form.propType === allConsts.FD_PropType.meter ||
        this.form.propType === allConsts.FD_PropType.rate
    ) {
      api
          .post(baseURL, {
            method: "typ/loadPropMeasure",
            params: [this.typCharGr],
          })
          .then((response) => {
            this.optPropMeasure = pack(response.data.result.records, "id");
          });

      let meas =
          this.form.typCharGrProp_measure == null
              ? 0
              : this.form.typCharGrProp_measure;
      api
          .post(baseURL, {
            method: "typ/loadMeasure",
            params: [meas],
          })
          .then((response) => {
            this.optMeasure = pack(response.data.result.records, "id");
          });
    }

    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "flatTable/loadTables",
          params: [{}],
        })
        .then((response) => {
          this.optionsFT = response.data.result.records;
        })
        .catch((error) => {
          let msg
          if (error.response) msg = error.response.data.error.message;
          else msg = error.message;
          notifyError(msg);
          //
        })
        .finally(() => {
          this.loading = ref(false);
        });
  },
};
</script>
