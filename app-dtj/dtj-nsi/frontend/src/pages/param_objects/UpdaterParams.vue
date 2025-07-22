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
          :model-value="form.name"
          v-model="form.name" autofocus dense
          :label="fnReqLabel('fldName')"
          class="q-ma-md"
        />


        <!-- meaParamsMeasure -->
        <q-select
          v-model="form.meaParamsMeasure"
          :model-value="form.meaParamsMeasure"
          :label="fnReqLabel('Prop_ParamsMeasure')"
          :options="optMeaParamsMeasure"
          dense options-dense
          map-options
          option-label="name"
          option-value="id"
          class="q-ma-md"
          @update:model-value="fnSelectMeaParamsMeasure"
        />

        <!-- objCollections -->
        <q-select
          v-model="form['objCollections']"
          :model-value="form['objCollections']"
          :label="fnLabel('source_collections')"
          :options="optCollections"
          dense class="q-ma-md"
          map-options clearable
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectCollections"
          @filter="filterCollections"
        />

        <!-- ParamsDescription -->
        <q-input
          :model-value="form['ParamsDescription']"
          v-model="form['ParamsDescription']"
          class="q-ma-md"
          :label="fnLabel('description')"
        >
        </q-input>

      </q-card-section>
      <!---->

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel" dense
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";

export default {
  props: ["mode", "data"],

  data() {
    return {
      loading: false,
      form: this.data,
      optCollections: [],
      optCollectionsOrg: [],
      optMeaParamsMeasure: [],

      optCls: [],
    }
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnReqLabel(label) {
      return this.$t(label) + "*";
    },

    fnLabel(label) {
      return this.$t(label);
    },

    fnSelectMeaParamsMeasure(v) {
      if (v) {
        this.form.meaParamsMeasure = v.id
        this.form.pvParamsMeasure = v["pv"]
      } else {
        this.form.meaParamsMeasure = null
        this.form.pvParamsMeasure = null
      }
    },

    fnSelectCollections(v) {
      if (v) {
        this.form.objCollections = v.id
        this.form.pvCollections = v["pv"]
      } else {
        this.form.objCollections = null
        this.form.pvCollections = null
      }
    },

    filterCollections(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optCollections = this.optCollectionsOrg
        })
        return
      }
      update(() => {
        if (this.optCollectionsOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optCollections = this.optCollectionsOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    validSave() {
      if (!this.form.name || !this.form.meaParamsMeasure) return true
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

      this.$axios
        .post(baseURL, {
          method: "data/saveParams",
          params: [ this.mode, this.form ],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result.records[0]);
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)

            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
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
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadMeasure',
        params: ['Prop_ParamsMeasure'],
      })
      .then(
        (response) => {
          this.optMeaParamsMeasure = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })

    //
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadCollections',
        params: ['Cls_Collections', 'Prop_Collections'],
      })
      .then(
        (response) => {
          this.optCollections = response.data.result["records"]
          this.optCollectionsOrg = response.data.result["records"]
        })
      .catch(error => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })
    //

  },
};
</script>
