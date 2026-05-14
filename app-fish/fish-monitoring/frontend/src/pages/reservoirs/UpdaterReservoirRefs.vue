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
        <div class="row">
          <div class="col">
            <q-select
              v-model="form.cls"
              :disable="mode==='upd'"
              :label="fmReqLabel('vidReservoir')"
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
          </div>
          <div class="col">
            <!-- name -->
            <q-input
              v-model="form.name"
              :label="fmReqLabel('fldName')"
              :model-value="form.name"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
              class="q-ma-md"
              dense
            />
          </div>
        </div>
        <div class="row">
          <div class="col">
            <q-select
              v-model="objBranch"
              :model-value="objBranch"
              :label="fmReqLabel('Branch')"
              :options="optBranch"
              class="q-ma-md"
              dense emit-value
              map-options
              multiple
              option-label="name"
              option-value="id"
              options-dense
            />
          </div>

          <div class="col">
            <!-- KATO -->
            <q-select
              v-model="objKATO"
              :label="fmReqLabel('KATO')"
              :options="optKATO"
              class="q-ma-md"
              dense emit-value
              map-options
              multiple
              option-label="name"
              option-value="id"
              options-dense
            />
          </div>

        </div>

        <div class="row">
          <div class="col">
            <!-- F_ReservoirType -->
            <q-select
              v-model="form.fvReservoirType"
              :label="fmReqLabel('ReservoirType')"
              :model-value="form.fvReservoirType"
              :options="optFvReservoirType"
              class="q-ma-md"
              clearable
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @clear="fnClearFvReservoirType"
              @update:model-value="fnSelectFvReservoirType"
            />
          </div>
          <div class="col">
            <!-- F_ReservoirStatus -->
            <q-select
              v-model="form.fvReservoirStatus"
              :label="fmReqLabel('ReservoirStatus')"
              :model-value="form.fvReservoirStatus"
              :options="optFvReservoirStatus"
              class="q-ma-md"
              clearable
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @clear="fnClearFvReservoirStatus"
              @update:model-value="fnSelectFvReservoirStatus"
            />
          </div>
        </div>

        <div class="row">
          <div class="col">
            <!--  F_FishFarmingType   -->
            <q-select
              v-model="form.fvFishFarmingType"
              :label="$t('FishFarmingType')"
              :model-value="form.fvFishFarmingType"
              :options="optFvFishFarmingType"
              class="q-ma-md"
              clearable
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @clear="fnClearFvFishFarmingType"
              @update:model-value="fnSelectFvFishFarmingType"
            />
          </div>
          <div class="col">
            <!-- Coordinate -->
            <q-input
              v-model="form['Coordinate']"
              :label="$t('coordinates')"
              class="q-ma-md"
              dense
            />
          </div>
        </div>
        <div class="row">
          <div class="col">
            <!-- Description -->
            <q-input v-model="form['Description']" :label="$t('description')" class="q-ma-md" type="textarea"/>
          </div>
        </div>
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
import {notifySuccess, today} from 'src/utils/jsutils'
import {date} from 'quasar'

export default {
  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      optCls: [],

      objBranch: [],
      optBranch: [],

      objKATO: [],
      optKATO: [],

      optFvReservoirType: [],
      optFvReservoirStatus: [],
      optFvFishFarmingType: [],
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
    labDte() {
      return date.formatDate(this.dte, 'DD.MM.YYYY')
    },

    labToday() {
      return date.formatDate(this.today, 'DD.MM.YYYY')
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

    fnClearFvReservoirType() {
      this.form.fvReservoirType = null
      this.form.pvReservoirType = null
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

    fnClearFvReservoirStatus() {
      this.form.fvReservoirStatus = null
      this.form.pvReservoirStatus = null
    },

    validSave() {
      let nm = this.form.name
      nm = nm ? nm.trim() : null
      if (!nm || !this.form.cls || !this.form.objKATO || !this.objBranch ||
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
      //this.form.periodType = this.periodType

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
        params: ['Typ_WaterBodies'],
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
        method: 'data/loadBranchForSelect',
        params: ['Prop_Branch'],
      })
      .then(
        (response) => {
          this.optBranch = response.data.result.records
          //console.info("optBranch", this.optBranch)
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadKatoForSelect',
        params: ['Prop_KATO'],
      })
      .then(
        (response) => {
          this.optKATO = response.data.result.records
          //console.info("optKATO", this.optKATO)
        })
      .finally(() => {
        this.loading = false
      })
    //

    this.loading = true
    api
      .post('', {
        method: 'data/loadFvReservoirTypeAsStore',
        params: ['Prop_ReservoirType'],
      })
      .then(
        (response) => {
          this.optFvReservoirType = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadFvReservoirStatusAsStore',
        params: ['Prop_ReservoirStatus'],
      })
      .then(
        (response) => {
          this.optFvReservoirStatus = response.data.result.records
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadFvFishFarmingTypeAsStore',
        params: ['Prop_FishFarmingType'],
      })
      .then(
        (response) => {
          this.optFvFishFarmingType = response.data.result.records
          //console.info("this.optFvFishFarmingType", this.optFvFishFarmingType)
        })
      .finally(() => {
        this.loading = false
      })
    //
    if (this.mode==="upd") {
      let arr = this.data.objBranch.split(',') || []
      arr.forEach(item => {
        this.objBranch.push(item)
      })
      //
      arr = this.data.objKATO.split(',') || []
      arr.forEach(item => {
        this.objKATO.push(item)
      })
    }
  },
}
</script>
