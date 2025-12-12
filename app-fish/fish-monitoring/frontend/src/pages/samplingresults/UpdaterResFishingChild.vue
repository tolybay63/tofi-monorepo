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
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- TypeOfFish -->
        <q-select
          v-model="form.objFishTyp"
          :model-value="form.objFishTyp"
          :label="fmReqLabel('typeOfFish', null)"
          :options="optTypeOfFish"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          use-input
          clearable
          @update:model-value="fnSelectTypeOfFish"
          @filter="filterTypeOfFish"
        />

        <!-- FishAge -->
        <q-input
          :model-value="form.FishAge"
          v-model="form.FishAge"
          :label="fmReqLabel('FishAge', 'Prop_FishAge')"
          type="number"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- FishWeight -->
        <q-input
          :model-value="form.FishWeight"
          v-model="form.FishWeight"
          type="number"
          :label="fmReqLabel('FishWeight', 'Prop_FishWeight')"
          :rules="[(val) => !!val || $t('req')]"
        />

        <!-- FishLength -->
        <q-input
          :model-value="form.FishLength"
          v-model="form.FishLength"
          type="number"
          :label="fmReqLabel('FishLength', 'Prop_FishLength')"
          :rules="[(val) => !!val || $t('req')]"
        />

        <!-- F_FishGender -->
        <q-select
          v-model="form.fvFishGender"
          :model-value="form.fvFishGender"
          :label="fmReqLabel('FishGender', null)"
          :options="optFvFishGender"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          @update:model-value="fnSelectFvFishGender"
        />
      </q-card-section>
      <!---->

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import { api, baseURL } from 'boot/axios'
import { notifyError, notifySuccess } from 'src/utils/jsutils'

export default {
  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      loading: false,
      optTypeOfFish: [],
      optTypeOfFishOrg: [],
      mapMeasure: new Map(),
      optFvFishGender: [],
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {
    fmReqLabel(label, prop) {
      if (prop && this.mapMeasure[prop] !== undefined) {
        let o = this.mapMeasure[prop]
        return this.$t(label) + ', ' + o['name'] + '*'
      } else return this.$t(label) + '*'
    },

    fnSelectFvFishGender(v) {
      if (v) {
        this.form.fvFishGender = v.id
        this.form.pvFishGender = v["pv"]
      }
    },

    fnSelectTypeOfFish(v) {
      if (v) {
        this.form.objFishTyp = v.id
        this.form.pvFishTyp = v["pv"]
      }
    },

    filterTypeOfFish(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optTypeOfFish = this.optTypeOfFishOrg
        })
        return
      }
      update(() => {
        if (this.optTypeOfFishOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optTypeOfFish = this.optTypeOfFishOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    validSave() {
      if (
        !this.form.objFishTyp ||
        !this.form.FishAge ||
        !this.form.fvFishGender ||
        !this.form.FishWeight ||
        !this.form.FishLength
      )
        return true
    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog.show()
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog.hide()
    },

    onDialogHide() {
      // required to be emitted
      // when QDialog emits "hide" event
      this.$emit('hide')
    },

    onOKClick() {
      // on OK, it is REQUIRED to
      // emit "ok" event (with optional payload)
      // before hiding the QDialog

      let err = false
      this.form.mode = this.mode
      this.$axios
        .post(baseURL, {
          method: 'data/saveResultSamplingPropertiesChild',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result.records[0])
            notifySuccess(this.$t('success'))
          },
          (error) => {
            err = true
            let msg = error.message
            if (error.response) msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .finally(() => {
          if (!err) this.hide()
        })
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide()
    },
  },

  created() {
    console.info("Created data", this.data)

    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadTypeOfFishForRes',
        params: [this.form.parent, /*'Cls_FishTypes',*/ 'Prop_FishTyp'],
        //params: ['Cls_FishTypes', 'Prop_FishTyp'],
      })
      .then((response) => {
        this.optTypeOfFish = response.data.result.records
        this.optTypeOfFishOrg = response.data.result.records
        console.info("Fish", this.optTypeOfFish)
      })
      .then(() => {
        api
          .post(baseURL, {
            method: 'data/measureInfo',
            params: [],
          })
          .then((response) => {
            this.mapMeasure = response.data.result
          })
      })
      .then(() => {
        api
          .post(baseURL, {
            method: 'data/loadFvFishFamilyForSelect',
            params: ['Factor_FishGender'],
          })
          .then((response) => {
            this.optFvFishGender = response.data.result.records
          })
      })
      .catch((error) => {
        let msg = error.message
        if (error.response) msg = this.$t(error.response.data.error.message)
        notifyError(msg)
      })
      .finally(() => {
        this.loading = false
      })
  },
}
</script>
