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
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          class="q-ma-md" dense autofocus
        >
        </q-input>


        <!-- objDefectsComponent -->
        <q-select
          v-model="form['objDefectsComponent']"
          :model-value="form['objDefectsComponent']"
          :label="fmReqLabel('cmp')"
          :options="optCmp"
          dense class="q-ma-md"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectDefectComponent"
          @filter="filterDefectComponent"
        />

        <!-- fvDefectsCategory -->
        <q-select
          v-model="form['fvDefectsCategory']"
          :model-value="form['fvDefectsCategory']"
          :label="fmReqLabel('DefectsCategory')"
          :options="optCategory"
          dense options-dense map-options
          option-label="name" option-value="id"
          class="q-ma-md"
          @update:model-value="fnSelectFvCategory"
        />

        <!-- DefectsIndex -->
        <q-input
          :model-value="form['DefectsIndex']"
          v-model="form['DefectsIndex']"
          class="q-ma-md" dense
          :label="fmReqLabel('DefectsIndex')"
        />

        <!-- DefectsNote -->
        <q-input
          :model-value="form['DefectsNote']"
          v-model="form['DefectsNote']"
          class="q-ma-md" dense
          :label="fmReqLabel('DefectsNote')"
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
      optCmp: [],
      optCmpOrg: [],
      optCategory: [],


    };
  },

  emits: [
    // REQUIRED
    "ok",
    "hide",
  ],

  methods: {
    fmReqLabel(label) {
      return this.$t(label) + "*";
    },

    fnSelectDefectComponent(v) {
      this.form.objDefectsComponent = v.id
      this.form.pvDefectsComponent = v["pv"]
    },

    filterDefectComponent(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optCmp = this.optCmpOrg
        })
        return
      }
      update(() => {
        if (this.optCmpOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optCmp = this.optCmpOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectFvCategory(v) {
      this.form.fvDefectsCategory = v.id
      this.form.pvDefectsCategory = v["pv"]
    },


    validSave() {
      if (!this.form.name || !this.form.objDefectsComponent || !this.form["fvDefectsCategory"] ||
        !this.form["DefectsIndex"] || !this.form["DefectsNote"]) return true
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
          method: "data/saveDefects",
          params: [this.mode, this.form],
        })
        .then(
          (response) => {
            this.$emit("ok", response.data.result["records"][0])
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
        method: 'data/loadComponentDefect',
        params: ['Typ_Components', "Prop_DefectsComponent"],
      })
      .then(
        (response) => {
          this.optCmp = response.data.result["records"]
          this.optCmpOrg = response.data.result["records"]
        })
      .then(() => {
        api
          .post(baseURL, {
            method: "data/loadFvCategory",
            params: ["Factor_Defects"],
          })
          .then(
            (response) => {
              this.optCategory = response.data.result["records"]
            })
          .catch(error => {
            if (error.response.data.error.message.includes("@")) {
              let msgs = error.response.data.error.message.split("@")
              let m1 = this.$t(`${msgs[0]}`)
              let m2 = (msgs.length > 1) ? " [" + msgs[1] + "]" : ""
              let msg = m1 + m2
              notifyError(msg)
            } else {
              notifyError(this.$t(error.response.data.error.message))
            }
          })
      })
      .catch(error => {
        let msg = error.message
        if (error.response) msg = this.$t(error.response.data.error.message)
        notifyError(msg)
      })
      .finally(() => {
        this.loading = false
      })
    //


  },
};
</script>
