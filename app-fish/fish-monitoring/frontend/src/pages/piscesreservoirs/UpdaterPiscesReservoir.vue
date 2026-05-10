<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>

        <!-- Reservoir -->
        <q-select
          v-model="form.reservoir"
          :label="fmReqLabel('reservoir')"
          :model-value="form.reservoir"
          :options="optReservoir"
          class="q-mb-lg"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterReservoir"
          @update:model-value="fnSelectReservoir"
        />

        <!-- TypeOfFish -->
        <q-select
          v-model="form.typeOfFish"
          :label="fmReqLabel('typeOfFish')"
          :options="optTypeOfFish"
          class="q-mb-lg"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterTypeOfFish"
          @update:model-value="fnSelectTypeOfFish"
        />
        <!-- FishSpawPeriod -->
        <q-input v-model="form['FishSpawPeriod']" :label="$t('FishSpawPeriod')"
                 class="q-mb-lg"
                 dense/>

        <!-- FishStartPuberty -->
        <q-input v-model="form['FishStartPuberty']" :label="$t('FishStartPuberty')"
                 class="q-mb-lg"
                 dense type="number"/>

        <!-- FishEndPuberty -->
        <q-input v-model="form['FishEndPuberty']" :label="$t('FishEndPuberty')" class="q-mb-lg"
                 dense type="number"/>

        <!-- FishSpawFrequency -->
        <q-input v-model="form['FishSpawFrequency']" :label="$t('FishSpawFrequency')"
                 class="q-mb-lg"
                 dense/>

      </q-card-section>
      <!---->
      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('save')"
          class="q-mt-xl"
          color="primary"
          icon="save"
          @click="onOKClick"
        />
        <q-btn :label="$t('cancel')" class="q-mt-xl" color="primary" icon="cancel" @click="onCancelClick"/>
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
      loading: false,
      optReservoir: [],
      optReservoirOrg: [],
      optTypeOfFish: [],
      optTypeOfFishOrg: [],
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

    fnSelectReservoir(v) {
      this.form.reservoir = v.id
      this.form.cls1 = v.cls
    },

    filterReservoir(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optReservoir = this.optReservoirOrg
        })
        return
      }
      update(() => {
        if (this.optReservoirOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optReservoir = this.optReservoirOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectTypeOfFish(v) {
      this.form.typeOfFish = v.id
      this.form.cls2 = v.cls
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
      if (!this.form.reservoir || !this.form.typeOfFish) return true
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
      api
        .post('', {
          method: 'data/savePiscesReservoir',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result.records[0])
            notifySuccess(this.$t('success'))
          },
          () => {
            err = true
            /*
                        if (error.response.data.error.message.includes('@')) {
                          let msgs = error.response.data.error.message.split('@')
                          let m1 = this.$t(`${msgs[0]}`)
                          let m2 = msgs.length > 1 ? ': [' + msgs[1] + ']' : ''
                          let msg = m1 + m2
                          notifyError(msg)
                        } else {
                          notifyError(this.$t(error.response.data.error.message))
                        }
            */
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
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadReservoir',
        params: ['Typ_WaterBodies'],
      })
      .then(
        (response) => {
          this.optReservoir = response.data.result["records"]
          this.optReservoirOrg = response.data.result["records"]
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadTypeOfFish',
        params: ['Typ_Fish'],
      })
      .then(
        (response) => {
          this.optTypeOfFish = response.data.result["records"]
          this.optTypeOfFishOrg = response.data.result["records"]
        })
      .finally(() => {
        this.loading = false
      })
  },
}
</script>
