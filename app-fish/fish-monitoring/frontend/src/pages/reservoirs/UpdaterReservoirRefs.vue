<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="min-width: 40%">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }} на {{ labDte() }}</div>
        <q-space />
        <div>Текущая дата: {{ labToday() }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }} на {{ labDte() }}</div>
        <q-space />
        <div>Текущая дата: {{ labToday() }}</div>
      </q-bar>

      <q-card-section>
        <!-- class -->
        <div class="row">
          <div class="col">
            <q-select
              v-model="form.cls"
              :model-value="form.cls"
              autofocus
              :label="fmReqLabel('vidReservoir')"
              :options="optCls"
              dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              :disable="mode==='upd'"
              @update:model-value="fnSelectCls"
            />
          </div>
          <div class="col">
            <!-- name -->
            <q-input
              :model-value="form.name"
              v-model="form.name"
              :label="fmReqLabel('fldName')"
              class="q-ma-md"
              dense
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />
          </div>
        </div>
        <div class="row">
          <div class="col">
            <!-- Region -->
            <q-select
              v-model="form.objRegion"
              :model-value="form.objRegion"
              options-dense
              :label="fmReqLabel('region')"
              :options="optRegion"
              dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              use-input
              @update:model-value="fnSelectRegion"
              @filter="filterRegion"
            />
          </div>
          <!-- District -->
          <div class="col">
            <q-select
              v-model="form.objDistrict"
              :model-value="form.objDistrict"
              :label="$t('district')"
              :options="optDistrict"
              dense
              options-dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              use-input
              clearable
              @update:model-value="fnSelectDistrict"
              @filter="filterDistrict"
              @clear="fnClearDistrict"
            />
          </div>
        </div>

        <div class="row">
          <!-- Branch -->
          <div class="col">
            <q-select
              v-model="form.objBranch"
              :model-value="form.objBranch"
              :label="fmReqLabel('branch')"
              :options="optBranch"
              dense
              options-dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              use-input
              @update:model-value="fnSelectBranch"
              @filter="filterBranch"
            />
          </div>
          <div class="col">
            <!-- F_ReservoirType -->
            <q-select
              v-model="form.fvReservoirType"
              :model-value="form.fvReservoirType"
              :label="$t('typeReservoir')"
              :options="optFvReservoirType"
              dense
              options-dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              clearable
              @update:model-value="fnSelectFvReservoirType"
              @clear="fnClearFvReservoirType"
            />
          </div>
        </div>

        <div class="row">
          <div class="col">
            <!-- F_ReservoirStatus -->
            <q-select
              v-model="form.fvReservoirStatus"
              :model-value="form.fvReservoirStatus"
              :label="$t('statusReservoir')"
              :options="optFvReservoirStatus"
              dense
              options-dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              clearable
              @update:model-value="fnSelectFvReservoirStatus"
              @clear="fnClearFvReservoirStatus"
            />
          </div>

          <div class="col">
            <!--  F_FishFarmingType   -->
            <q-select
              v-model="form.fvFishFarmingType"
              :model-value="form.fvFishFarmingType"
              :label="$t('fishFarmingType')"
              :options="optFvFishFarmingType"
              dense
              options-dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-md"
              clearable
              @update:model-value="fnSelectFvFishFarmingType"
              @clear="fnClearFvFishFarmingType"
            />
          </div>
        </div>

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn color="primary" icon="cancel" dense :label="$t('cancel')" @click="onCancelClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {api} from 'boot/axios'
import {notifyError, notifySuccess, today} from 'src/utils/jsutils'
import {date} from 'quasar'
import LifiInfo from 'pages/reservoirs/LifiInfo.vue'

export default {
  components: { LifiInfo },
  props: ['mode', 'data', 'dte', 'periodType'],

  data() {
    return {
      form: this.data,
      optCls: [],
      optRegion: [],
      optRegionOrg: [],
      optDistrict: [],
      optDistrictOrg: [],
      optBranch: [],
      optBranchOrg: [],
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

    fnSelectRegion(v) {
      if (v) {
        this.form.objRegion = v.id
        this.form.pvRegion = v["pv"]
        this.form.objDistrict = null
        this.form.pvDistrict = null
        this.loadDistrict(v.id)
      }
    },

    loadDistrict(region) {
      this.loading = true
      api
        .post('', {
          method: 'data/loadDistrict',
          params: [region],
        })
        .then(
          (response) => {
            this.optDistrict = response.data.result.records
            this.optDistrictOrg = response.data.result.records
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

    fnSelectDistrict(v) {
      this.form.objDistrict = v.id
      this.form.pvDistrict = v["pv"]
    },

    fnClearDistrict() {
      this.form.objDistrict = null
      this.form.pvDistrict = null
    },

    fnSelectBranch(v) {
      this.form.objBranch = v.id
      this.form.pvBranch = v["pv"]
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

    filterRegion(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optRegion = this.optRegionOrg
        })
        return
      }
      update(() => {
        if (this.optRegionOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optRegion = this.optRegionOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    filterDistrict(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optDistrict = this.optDistrictOrg
        })
        return
      }
      update(() => {
        if (this.optDistrictOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optDistrict = this.optDistrictOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
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

    validSave() {
      let nm = this.form.name
      nm = nm ? nm.trim() : null
      if (!nm || !this.form.cls || !this.form.objRegion || !this.form.objBranch) return true
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
      let nm = this.form.name
      this.form.name = nm.trim()
      this.form.dte = this.dte
      this.form.periodType = this.periodType

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
            if (error.response.data.error.message.includes('@')) {
              let msgs = error.response.data.error.message.split('@')
              let m1 = this.$t(`${msgs[0]}`)
              let m2 = msgs.length > 1 ? ' [' + this.$t(msgs[1]) + ']' : ''
              let msg = m1 + m2
              notifyError(msg)
            } else {
              notifyError(this.$t(error.response.data.error.message))
            }

            /*
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            notifyError(msg);
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
        method: 'data/loadChildClsForSelect',
        params: ['Typ_WaterBodies'],
      })
      .then(
        (response) => {
          this.optCls = response.data.result.records
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
      .post('', {
        method: 'data/loadRegion',
        params: ['Cls_Regions'],
      })
      .then(
        (response) => {
          this.optRegion = response.data.result.records
          this.optRegionOrg = response.data.result.records
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
      .post('', {
        method: 'data/loadBranchName',
        params: ['Cls_Branch'],
      })
      .then(
        (response) => {
          this.optBranch = response.data.result.records
          this.optBranchOrg = response.data.result.records
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
      .post('', {
        method: 'data/loadFvReservoirType',
        params: ['Factor_ReservoirType'],
      })
      .then(
        (response) => {
          this.optFvReservoirType = response.data.result.records
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
      .post('', {
        method: 'data/loadFvReservoirStatus',
        params: ['Factor_ReservoirStatus'],
      })
      .then(
        (response) => {
          this.optFvReservoirStatus = response.data.result.records
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
      .post('', {
        method: 'data/loadFvFishFarmingType',
        params: ['Factor_FishFarmingType'],
      })
      .then(
        (response) => {
          this.optFvFishFarmingType = response.data.result.records
          //console.info("this.optFvFishFarmingType", this.optFvFishFarmingType)
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
    if (this.mode === 'upd') {
      this.loadDistrict(this.form.objRegion)
    }
  },
}
</script>
