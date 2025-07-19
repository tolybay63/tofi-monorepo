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
        <div v-if="form.propType === pt_meter || form.propType === pt_rate">
          <q-item-label
              :class="form.relTypCharGrProp_measure===0 ? 'text-red-10' : 'text-grey-7'"
              style="font-size: 0.8em; margin-top: 10px" class="row">
                {{ $t("propMeasure") }}
            <q-space></q-space>
            <q-icon name="error" v-if="form.relTypCharGrProp_measure===0" color="red-10" size="24px"></q-icon>
          </q-item-label>
          <treeselect
              v-scroll
              :options="optPropMeasure"
              v-model="form.relTypCharGrProp_measure"
              :default-expand-level="1"
              :normalizer="normalizerPropMeasure"
              :placeholder="$t('select')"
              :noChildrenText="$t('noChilds')"
              :noResultsText="$t('noResult')"
              :noOptionsText="$t('noResult')"
              @select="fnSelectPropMeasure"
          />
          <q-item-label v-if="form.relTypCharGrProp_measure===0"
                        class="text-red-10" style="font-size: 0.8em"
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
  props: ["dta", "relTypCharGr", "lg", "dense"],

  data() {
    let data = {};
    extend(true, data, this.dta);

    return {
      form: data,
      lang: this.lg,
      options: [],
      optionsFT: [],
      st: data.storageType === undefined ? null : data.storageType,
      ft: data.flatTable === undefined ? null : data.flatTable,
      optPropMeasure: [],
      optMeasure: [],
      pt_meter: allConsts.FD_PropType.meter,
      pt_rate: allConsts.FD_PropType.rate
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectPropMeasure(v) {

      if (v.propType !== allConsts.FD_PropType.measure) {
        v = null;
        this.form.relTypCharGrProp_measure = null;
        //notifyError("Выберите свойство типа [Типа ед.измерения]");
      } else {
        this.form.relTypCharGrProp_measure = v.prop;
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
      this.form.storageType = this.st.id;
      if (this.form.storageType !== allConsts.FD_StorageType.flat)
        this.form.flatTable = null;
    },

    fnSelectFT() {
      this.form.flatTable = this.ft.id;
    },

    validSave() {
      if (this.form.storageType !== allConsts.FD_StorageType.flat)
        return !(this.form.storageType > 0 && this.form.relTypCharGrProp_measure !== 0);
      else return !(this.form.storageType > 0 && this.form.relTypCharGrProp_measure !== 0 && this.ft !== null);
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

      this.form.storageType = typeof this.st === "object" ? this.st.id : this.st;

      if (this.form.storageType !== allConsts.FD_StorageType.flat)
        this.form.flatTable = null;
      else this.form.flatTable = this.ft.id;

      api
          .post(baseURL, {
            method: "reltyp/updateRelTypCharGrProp",
            params: [this.form],
          })
          .then(
              () => {
                this.$emit("ok", {res: true});
                //notifySuccess(this.$t("success"))
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
          params: [{dict: "FD_StorageType"}],
        })
        .then((response) => {
          this.options = response.data.result.records;
        });

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

    if (
        this.form.propType === allConsts.FD_PropType.meter ||
        this.form.propType === allConsts.FD_PropType.rate
    ) {
      api
          .post(baseURL, {
            method: "reltyp/loadPropMeasure",
            params: [this.relTypCharGr],
          })
          .then((response) => {
            this.optPropMeasure = pack(response.data.result.records, "id");
          });

      let meas =
          this.form.relTypCharGrProp_measure == null
              ? 0
              : this.form.relTypCharGrProp_measure;

      api
          .post(baseURL, {
            method: "reltyp/loadMeasure",
            params: [meas],
          })
          .then((response) => {
            this.optMeasure = pack(response.data.result.records, "id");
          });
    }

  },
};
</script>
