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

        <!-- class -->
        <q-select
          v-model="form.cls" :model-value="form.cls" autofocus
          :label="fmReqLabel('vidWork')" :options="optCls"
          option-label="name" option-value="id" class="q-ma-md"
          dense options-dense map-options
          @update:model-value="fnSelectCls"
        />

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          @blur="onBlurName" class="q-ma-md" dense
        >
        </q-input>

        <!-- objCollections -->
        <q-select
          v-model="form['objCollections']"
          :model-value="form['objCollections']"
          :label="fmReqLabel('source_collections')"
          :options="optCollections"
          dense class="q-ma-md"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectCollections"
          @filter="filterCollections"
        />


        <!-- NumberSource -->
        <q-input
          :model-value="form['NumberSource']"
          v-model="form['NumberSource']"
          class="q-ma-md" dense
          :label="fmReqLabel('Prop_NumberSource')"
            />

        <!-- fvPeriodType -->
        <q-select
          v-model="form.fvPeriodType"
          :model-value="form.fvPeriodType"
          :label="fmReqLabel('Prop_PeriodType')"
          :options="optFvPeriodType"
          dense options-dense
          map-options
          option-label="name"
          option-value="id"
          class="q-ma-md"
          @update:model-value="fnSelectFvPeriodType"
        />

        <!-- Periodicity -->
        <q-input
          :model-value="form['Periodicity']"
          v-model="form['Periodicity']"
          class="q-ma-md" dense type="number"
          :label="fmReqLabel('Prop_Periodicity')"
        />



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
      optCls: [],
      optCollections: [],
      optCollectionsOrg: [],
      optFvPeriodType: [],
      mapCollections: new Map()
    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {

    fnSelectCollections(v) {
      this.form.objCollections = v.id
      this.form.pvCollections = v["pv"]
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

    fnSelectFvPeriodType(v) {
      if (v) {
        this.form.fvPeriodType = v.id
        this.form.pvPeriodType = v["pv"]
      }
    },

    fmReqLabel(label) {
      return this.$t(label) + "*";
    },

    fnSelectCls(val) {
      this.form.cls = val.id
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

    validSave() {
      if (!this.form.name || !this.form.cls || !this.form.objCollections || !this.form["NumberSource"] ||
        !this.form.fvPeriodType || !this.form["Periodicity"]) return true
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

      this.form.fullName = this.form["name"] + " [тк №" + this.form["NumberSource"] +" / "+ this.mapCollections.get(this.form.objCollections)+"]";
      this.$axios
        .post(baseURL, {
          method: "data/saveProcessCharts",
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
    //
    this.loading = true
    api
      .post(baseURL, {
        method: "data/loadClsForSelect",
        params: [ "Typ_Work" ],
      })
      .then(
        (response) => {
          this.optCls = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response)
            msg = this.$t(error.response.data.error.message)
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
          this.optCollections.forEach(item => {
            this.mapCollections.set(item.id, item.name)
          })
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
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadFvPeriodType',
        params: ['Factor_PeriodType'],
      })
      .then(
        (response) => {
          this.optFvPeriodType = response.data.result.records
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



  },
};
</script>
