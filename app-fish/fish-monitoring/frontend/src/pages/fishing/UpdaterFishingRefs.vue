<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="min-width: 40%">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- class -->
        <q-select
          v-model="form.cls"
          :disable="mode==='upd'"
          :label="fmReqLabel('fishingType')"
          :model-value="form.cls"
          :options="optCls"
          autofocus
          class="q-ma-md"
          dense
          map-options
          option-label="name"
          option-value="id"
          @update:model-value="fnSelectCls"
        />
        <!-- StartDate -->
        <q-input
          v-model="form.StartDate"
          :model-value="form.StartDate"
          :label="fmReqLabel('StartDate')"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md" dense
          @update:model-value="fnSelectDte"
        />
        <!-- objFishLocation -->
        <q-select
          :disable="mode==='upd'"
          v-model="objFishLocation"
          :model-value="objFishLocation"
          :label="fmReqLabel('FishLocation')"
          :options="optFishLocation"
          class="q-ma-md"
          color="blue"
          dense
          map-options
          option-label="name"
          option-value="id"
          options-dense
          options-selected-class="text-blue"
          @update:model-value="fnSelectFishLocation"
        />
        <!-- AreaOfTon -->
        <q-input
          v-model="form['AreaOfTon']"
          :label="fmReqLabel('AreaOfTon')"
          type="number"
          class="q-ma-md"
          dense
        />
        <!-- objFishGear -->
        <q-select
          :disable="mode==='upd'"
          v-model="objFishGear"
          :model-value="objFishGear"
          :label="fmReqLabel('FishGear')"
          :options="optFishGear"
          class="q-ma-md"
          color="blue"
          dense
          map-options
          option-label="name"
          option-value="id"
          options-dense
          options-selected-class="text-blue"
          @update:model-value="fnSelectFishGear"
        />
        <!-- objFishManager -->
<!--        :disable="mode==='upd'"-->
        <q-select

          v-model="objFishManager"
          :model-value="objFishManager"
          :label="fmReqLabel('FishManager')"
          :options="optFishManager"
          class="q-ma-md"
          color="blue"
          dense
          map-options
          option-label="name"
          option-value="id"
          options-dense
          options-selected-class="text-blue"
          @update:model-value="fnSelectFishManager"
        />

        <!-- FishParticipants -->

        <q-select
          v-model="FishParticipants"
          :model-value="FishParticipants"
          :label="fmReqLabel('FishParticipants')"
          :options="optFishParticipants"
          class="q-ma-md"
          options-dense
          dense map-options
          multiple use-chips
          option-label="name"
          option-value="id"
          options-selected-class="text-blue"
        />

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
        <q-btn :label="$t('cancel')" color="primary" dense icon="cancel" @click="onCancelClick"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>


import {api} from 'boot/axios'
import {notifySuccess} from 'src/utils/jsutils'

export default {

  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      optCls: [],

      objFishLocation: null,
      optFishLocation: [],

      objFishGear: null,
      optFishGear: [],

      objFishManager: null,
      optFishManager: [],

      FishParticipants: [],
      optFishParticipants: [],

      loading: false,

    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {

    fmReqLabel(label) {
      return this.$t(label) + '*'
    },

    fnSelectCls(val) {
      this.form.cls = val.id
    },

    fnSelectDte(val) {
      //this.form.StartDate = val
    },

    fnSelectFishLocation(v) {
      if (v) {
        this.form.objFishLocation = v.id
        this.form.pvFishLocation = v.pv
      }
    },

    fnSelectFishGear(v) {
      if (v) {
        this.form.objFishGear = v.id
        this.form.pvFishGear = v.pv
      }
    },


    fnSelectFishManager(v) {
      if (v) {
        this.form.objFishManager = v.id
        this.form.pvFishManager = v.pv
      }
    },


    validSave() {
      //console.info("form", this.form)

      if (!this.form.cls || !this.form.AreaOfTon ||
        !this.objFishLocation || !this.objFishGear || !this.objFishManager || this.FishParticipants.length === 0)
        return true
      else
        return false

    },

    // following method is REQUIRED
    // (don't change its name --> "show")
    show() {
      this.$refs.dialog["show"]()
    },

    // following method is REQUIRED
    // (don't change its name --> "hide")
    hide() {
      this.$refs.dialog["hide"]()
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

      //console.info("FishParticipants", this.FishParticipants)
      this.form.FishParticipants = []
      this.FishParticipants.forEach(it => {
        this.form.FishParticipants.push(it.id)
      })

      api
        .post('', {
          method: 'data/saveFishingPropertiesRef',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result.records[0])
            notifySuccess(this.$t('success'))
          },
          (error) => {
            console.error(error.message)
            err = true
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
    //console.info("this.cls", this.cls)
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadCls',
        params: ['Typ_FishCatch'],
      })
      .then(
        (response) => {
          this.optCls = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadFishLocationForSelect',
        params: ['Prop_FishLocation'],
      })
      .then(
        (response) => {
          this.optFishLocation = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadFishGearForSelect',
        params: ['Prop_FishGear'],
      })
      .then(
        (response) => {
          this.optFishGear = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadFishManagerForSelect',
        params: ['Prop_FishManager'],
      })
      .then(
        (response) => {
          this.optFishManager = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadFishParticipantsForSelect',
        params: ['Prop_FishParticipants'],
      })
      .then(
        (response) => {
          this.optFishParticipants = response.data.result.records
          //console.info("this.optFishParticipants", this.optFishParticipants)
        })
      .finally(() => {
        this.loading = false
      })
      .then(() => {
        if (this.mode === "upd") {
          this.objFishLocation = this.data.objFishLocation
          this.objFishGear = this.data.objFishGear
          this.objFishManager = this.data.objFishManager

          this.FishParticipants = []
          let lstData = this.data.lstFishParticipants

          lstData.split(",").forEach(id => {
            let arr = id.split("_")
            let key = arr[1] + "_" + arr[2]
            for (let i = 0; i < this.optFishParticipants.length; i++) {
              const it = this.optFishParticipants[i];
              if (key === it.id) {
                this.FishParticipants.push({id: id, name: it.name})
              }
            }
          })
        }
      })
    //


  },
}
</script>
