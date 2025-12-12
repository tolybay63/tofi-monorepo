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
        <!-- Branch -->
        <q-select
          v-model="form.objBranch"
          :model-value="form.objBranch"
          autofocus
          :label="fnReqLabel('branch')"
          :options="optBranch"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          use-input
          :readonly="mode === 'upd'"
          @update:model-value="fnSelectBranch"
          @filter="filterBranch"
        />

        <!-- Reservoir -->
        <q-select
          :readonly="mode === 'upd'"
          v-model="form.objReservoirShore"
          :model-value="form.objReservoirShore"
          :label="fnReqLabel('reservoir')"
          :options="optReservoir"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          use-input
          @update:model-value="fnSelectReservoir"
          @filter="filterReservoir"
        />

        <!--objFishZone-->
        <q-select
          :readonly="mode === 'upd'"
          v-model="form.objFishZone"
          :model-value="form.objFishZone"
          :label="fnReqLabel('fishingArea')"
          :options="optAreaSampling"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          use-input
          @update:model-value="fnSelectAreaSampling"
          @filter="filterAreaSampling"
        />

        <!-- SampleExecutor -->
        <q-select
          v-model="form.objsSampleExecutor"
          :model-value="form.objsSampleExecutor"
          :label="fnLabel('executor')"
          :options="optExecutor"
          dense multiple
          map-options
          emit-value
          @update:model-value="fnSelectSampleExecutor"
        >
          <template v-slot:option="{ itemProps, opt, selected, toggleOption }">
            <q-item v-bind="itemProps">
              <q-item-section>
                <q-item-label v-html="opt.label" />
              </q-item-section>
              <q-item-section side>
                <q-toggle :model-value="selected" @update:model-value="toggleOption(opt)" />
              </q-item-section>
            </q-item>
          </template>

        </q-select>

        <!-- FishGear -->

          <q-select
            v-model="form.objFishGear"
            :model-value="form.objFishGear"
            :label="fnReqLabel('FishGear')"
            :options="optFishGear"
            dense
            map-options
            option-label="name"
            option-value="id"
            class="q-my-md"
            use-input
            @update:model-value="fnSelectFishGear"
            @filter="filterFishGear"
          />


        <div class="row">
        <!-- Dte -->
        <q-input
          :model-value="form.dte"
          v-model="form.dte"
          type="date"
          :label="fnReqLabel('date')"
          :rules="[(val) => !!val || $t('req')]"
          class="col-3"
        />

          <q-space></q-space>
        <!--  PeriodType  -->
        <q-select
          class="q-mt-md"
          v-model="form.periodType"
          :model-value="form.periodType"
          dense
          options-dense
          :options="optPeriod"
          :label="fnReqLabel('periodType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectPeriodType"
          style="width: 300px"
        />

        </div>

        <!-- PropAreaOfTon -->
        <q-input
          :model-value="form['AreaOfTon']"
          v-model="form['AreaOfTon']"
          type="number"
          :label="fnLabel('PropAreaOfTon')"
          class="q-mt-md"
        />

      <!--  cmt  -->
        <q-input
          :model-value="form['cmt']"
          v-model="form['cmt']"
          type="textarea"
          class="q-mb-md"
          dense
          :label="$t('fldCmt')"
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
import {api} from 'boot/axios'
import {notifyError, notifySuccess} from 'src/utils/jsutils'

export default {
  props: ['mode', 'data'],

  data() {
    //console.info("data.optExecutor", this.data.optExecutor)
    return {
      form: this.data,
      loading: false,
      optBranch: [],
      optBranchOrg: [],
      optReservoir: [],
      optReservoirOrg: [],
      optExecutor: [],
      optAreaSampling: [],
      optAreaSamplingOrg: [],
      optFishGear: [],
      optFishGearOrg: [],
      mapMeasure: new Map(),

      optPeriod: [],
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {

    fnSelectSampleExecutor(v) {
      this.form.objsSampleExecutor = v
      //console.log("fnSelectSampleExecutor", this.form.objsSampleExecutor)

    },

    fnSelectPeriodType(v) {
      this.form.periodType = v.id
    },

    fnReqLabel(label) {
      return this.$t(label)+"*"
    },

    fnLabel(label) {
      return this.$t(label)
    },

    fnSelectBranch(v) {
      if (v) {
        this.form.objBranch = v.id
        this.form.pvBranch = v["pv"]

        this.form.objReservoirShore = null
        this.form.pvReservoirShore = null
        this.form.objFishZone = null
        this.form.pvFishZone = null
        this.loadReservoir(v.id)
      }
    },

    filterBranch(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optBranch = this.optBranchOrg
        })
        return
      }
      update(() => {
        if (this.optBranchOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optBranch = this.optBranchOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectReservoir(v) {
      if (v) {
        this.form.objReservoirShore = v.id
        this.form.pvReservoirShore = v["pv"]
        this.loadAreaSampling(v.id)
      }
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

    fnSelectFishGear(v) {
      this.form.objFishGear = v.id
      this.form.pvFishGear = v["pv"]
    },

    filterFishGear(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optFishGear = this.optFishGearOrg
        })
        return
      }
      update(() => {
        if (this.optFishGearOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optFishGear = this.optFishGearOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectAreaSampling(v) {
      this.form.objFishZone = v.id
      this.form.pvFishZone = v["pv"]
      this.form.nameFishZone = v.name
    },

    filterAreaSampling(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optAreaSampling = this.optAreaSamplingOrg
        })
        return
      }
      update(() => {
        if (this.optAreaSamplingOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optAreaSampling = this.optAreaSamplingOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    loadReservoir(branch) {
      this.loading = true
      api
        .post('', {
          method: 'data/loadReservoir',
          params: [branch],
        })
        .then(
          (response) => {
            this.optReservoir = response.data.result.records
            this.optReservoirOrg = response.data.result.records
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

    loadAreaSampling(reservoir) {
      this.loading = true
      api
        .post('', {
          method: 'data/loadAreaSampling',
          params: [reservoir],
        })
        .then(
          (response) => {
            this.optAreaSampling = response.data.result.records
            this.optAreaSamplingOrg = response.data.result.records
            //console.info("optAreaSampling", this.optAreaSampling)
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

    validSave() {
      if (
        !this.form.objBranch ||
        !this.form.objReservoirShore ||
        !this.form.objFishZone || !this.form.objFishGear ||
        !this.form.dte || !this.form.periodType
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
      //console.info("Object", this.form.objsSampleExecutor)

      api
        .post('', {
          method: 'data/saveSamplingProperties',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result.rows.records[0])
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
    this.loading = true
    api
      .post('', {
        method: 'data/loadPeriodType',
        params: [],
      })
      .then(
        (response) => {
          this.optPeriod = response.data.result.records
        })
      .catch(error => {
        notifyError(error.message)
      })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/measureInfo',
        params: [],
      })
      .then((response) => {
        this.mapMeasure = response.data.result
        //console.info("mapMeasure", this.mapMeasure)
      })
      .catch((error) => {
        if (error.response.data.error.message.includes('@')) {
          let msgs = error.response.data.error.message.split('@')
          let m1 = this.$t(`${msgs[0]}`)
          let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
          let msg = m1 + m2
          notifyError(msg)
        } else {
          notifyError(this.$t(error.response.data.error.message))
        }
      })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadBranchName',
        params: ['Cls_Branch'],
      })
      .then((response) => {
        this.optBranch = response.data.result.records
        this.optBranchOrg = response.data.result.records
      })
      .then(() => {
        api
          .post('', {
            method: 'data/loadExecutor',
            params: ['Typ_Users', 'Prop_SampleExecutor'],
          })
          .then((response) => {
            this.optExecutor = response.data.result.records
            //console.info("optExecutor", this.optExecutor)
          })
          .then(()=> {
            if (this.mode==="upd") {
              //console.log("this.form.objsSampleExecutor upd", this.form.objsSampleExecutor)
              if (this.form.objsSampleExecutor) {
                let arr = []
                this.form.objsSampleExecutor.split(",").forEach((item) => {
                  arr.push(parseInt(item, 10))
                })
                this.form.objsSampleExecutor = arr
              }
            }
          })
          .then(() => {
            api
              .post('', {
                method: 'data/loadFishGearName',
                params: ['Cls_FishGearTrade'],
              })
              .then((response) => {
                this.optFishGear = response.data.result.records
                this.optFishGearOrg = response.data.result.records
              })
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
    //
    if (this.mode === 'upd') {
      this.loadReservoir(this.form.objBranch)
      this.loadAreaSampling(this.form.objReservoirShore)
    }
  },
}
</script>
