<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="min-width: 60%">
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
          :label="fmReqLabel('StartDate')"
          :model-value="form.StartDate"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md"
          dense
        />


        <q-select
          v-model="form['objFishLocation']"
          :model-value="form['objFishLocation']"
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
        />

        <!-- AreaOfTon -->
        <q-input
          v-model="form['AreaOfTon']"
          :label="fmReqLabel('AreaOfTon')"
          type="number"
          class="q-ma-md"
          dense
        />

        <q-select
          v-model="form['objFishGear']"
          :model-value="form['objFishGear']"
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
        />

        <q-select
          v-model="form['objFishManager']"
          :model-value="form['objFishManager']"
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
        />

        <q-select
          v-model="form['objFishParticipants']"
          :model-value="form['objFishParticipants']"
          :label="fmReqLabel('FishParticipants')"
          :options="optFishParticipants"
          class="q-ma-md"
          color="blue"
          dense multiple
          map-options
          option-label="name"
          option-value="id"
          options-dense
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

import "vue3-treeselect/dist/vue3-treeselect.css";
import {api} from 'boot/axios'
import {notifySuccess, pack, today} from 'src/utils/jsutils'

export default {
  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      optCls: [],

      optFishLocation: [],
      optFishGear: [],
      optFishManager: [],
      optFishParticipants: [],

      loading: false,
      today: today(),
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {
    normalizer(node) {
      return {
        id: node.key,
        label: node.name,
      };
    },

    fmReqLabel(label) {
      return this.$t(label) + '*'
    },

    fnSelectCls(val) {
      this.form.cls = val.id
    },

    fnSelectFvReservoirType(v) {
      if (v) {
        this.form.fvReservoirType = v.id
        this.form.pvReservoirType = v["pv"]
      }
    },


    fnSelectFvFishFarmingType(v) {
      if (v) {
        this.form.fvFishFarmingType = v.id
        this.form.pvFishFarmingType = v["pv"]
      }
    },

    fnClearFvFishFarmingType() {
      this.form.fvFishFarmingType = null
      this.form.pvFishFarmingType = null
    },

    fnSelectFvReservoirStatus(v) {
      if (v) {
        this.form.fvReservoirStatus = v.id
        this.form.pvReservoirStatus = v["pv"]
      }
    },

    validSave() {
      let nm = this.form.name
      nm = nm ? nm.trim() : null
      if (!nm || !this.form.cls || !this.objKATO || !this.objBranch ||
        !this.form.fvReservoirType || !this.form.fvReservoirStatus) return true
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
      let nm = this.form.name
      this.form.name = nm.trim()
      this.form.objBranch = this.objBranch
      this.form.objKATO = this.objKATO

      api
        .post('', {
          method: 'data/saveReservoirPropertiesRef',
          params: [this.form],
        })
        .then(
          (response) => {
            //console.log("recResoirvor", response.data.result.records[0]);
            err = false
            this.$emit('ok', response.data.result.records[0])
            notifySuccess(this.$t('success'))
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
            err = true
            /*
                        if (error.response.data.error.message.includes('@')) {
                          let msgs = error.response.data.error.message.split('@')
                          let m1 = this.$t(`${msgs[0]}`)
                          let m2 = msgs.length > 1 ? ' [' + this.$t(msgs[1]) + ']' : ''
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
          //console.info("optFishLocation", this.optFishLocation)
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
        })
      .finally(() => {
        this.loading = false
      })
    //

    if (this.mode === "upd") {

    }
  },
}
</script>
