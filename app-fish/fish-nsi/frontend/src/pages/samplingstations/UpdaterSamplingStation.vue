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

        <!-- name -->
        <q-input
          autofocus dense
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- Coordinate -->
        <q-input
          v-model="form['Coordinate']"
          :label="fmReqLabel('coordinates')"
          dense
          class="q-mb-md"
        />

        <!-- Reservoir -->
        <q-select
          v-model="form.objReservoirShore"
          :label="fmReqLabel('reservoir')"
          :model-value="form.objReservoirShore"
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
<!--:disable="mode === 'upd'"-->

        <!-- AreaOfTon -->
        <q-input
          v-model="form.AreaOfTon"
          :label="fmReqLabel('AreaOfTon')"
          type="number" dense
          class="q-mb-md"
        />

        <!-- Description -->
        <q-input v-model="form['Description']" type="textarea" :label="$t('description')" />
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
import {api} from 'boot/axios'
import {notifySuccess} from 'src/utils/jsutils'

export default {
  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      optReservoir: [],
      optReservoirOrg: [],
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

    validSave() {
      if (!this.form.AreaOfTon || !this.form["Coordinate"] ||
        !this.form.name || !this.form.objReservoirShore) return true
    },

    fnSelectReservoir(v) {
      this.form.objReservoirShore = v.id
      this.form.pvReservoirShore = v.pv
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
          method: 'data/saveSamplingStation',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result["records"][0])
            notifySuccess(this.$t('success'))
          })
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
    this.loading = true
    api
      .post('', {
        method: 'data/loadReservoir',
        params: ['Prop_ReservoirShore'],
      })
      .then(
        (response) => {
          this.optReservoir = response.data.result["records"]
          this.optReservoirOrg = response.data.result["records"]
        })
      .finally(() => {
        this.loading = false
      })

  },
}
</script>
