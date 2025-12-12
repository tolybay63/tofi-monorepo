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

      <q-bar class="text-white bg-primary">
        <div>Текущая дата: {{ labToday() }}</div>
      </q-bar>

      <q-card-section>

        <div class="row">
          <div v-if="mode==='upd'"> {{ $t('editRecord') }} на </div>
          <div v-else> {{ $t('newRecord') }} на </div>

          <q-input
            v-model="dt"
            :label="$t('date')"
            :model-value="dt"
            class="q-ml-lg"
            dense
            stack-label
            style="width: 100px"
            type="date"
            :disable="mode==='upd'"
          />

          <!--  PeriodType  -->
          <q-select
            class="q-ml-lg"
            v-model="pt"
            :model-value="pt"
            dense
            options-dense
            :options="optPeriodType"
            :label="fnReqLabel('periodType')"
            option-value="id"
            option-label="text"
            map-options
            style="width: 100px"
            :disable="mode==='upd'"
          />


        </div>


        <!--Prop_WaterArea-->
        <q-input
          :model-value="form.WaterArea"
          v-model="form.WaterArea"
          :label="$t('WaterArea')"
          type="number"
          class="q-ma-md"
          dense
        >
          <template v-slot:after>
            <q-btn round dense flat size="sm" color="purple" icon="help_outline">
              <q-menu auto-close>
                <div>
                  <lifi-info :field="'WaterArea'" :rec="form"></lifi-info>
                </div>
              </q-menu>
            </q-btn>
          </template>
        </q-input>


        <!--Prop_WaterAreaFishing-->
        <q-input
          :model-value="form.WaterAreaFishing"
          v-model="form.WaterAreaFishing"
          class="q-ma-md"
          :label="$t('WaterAreaFishing')"
          type="number"
          dense
        >
          <template v-slot:after>
            <q-btn round dense flat size="sm" color="purple" icon="help_outline">
              <q-menu auto-close>
                <div>
                  <lifi-info :field="'WaterAreaFishing'" :rec="form"></lifi-info>
                </div>
              </q-menu>
            </q-btn>
          </template>
        </q-input>


        <!-- Prop_WaterAreaLittoral-->
        <q-input
          :model-value="form.WaterAreaLittoral"
          v-model="form.WaterAreaLittoral"
          class="q-ma-md"
          :label="$t('WaterAreaLittoral')"
          type="number"
          dense
        >
          <template v-slot:after>
            <q-btn round dense flat size="sm" color="purple" icon="help_outline">
              <q-menu auto-close>
                <div>
                  <lifi-info :field="'WaterAreaLittoral'" :rec="form"></lifi-info>
                </div>
              </q-menu>
            </q-btn>
          </template>
        </q-input>

        <!--Prop_ReservoirHydroLevel-->
        <q-input
          :model-value="form.ReservoirHydroLevel"
          v-model="form.ReservoirHydroLevel"
          :label="$t('ReservoirHydroLevel')"
          type="number"
          class="q-ma-md"
          dense
        >
          <template v-slot:after>
            <q-btn round dense flat size="sm" color="purple" icon="help_outline">
              <q-menu auto-close>
                <div>
                  <lifi-info :field="'ReservoirHydroLevel'" :rec="form"></lifi-info>
                </div>
              </q-menu>
            </q-btn>
          </template>
        </q-input>


      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          dense
          :label="$t('save')"
          @click="onOKClick"
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
      loading: false,
      today: today(),
      optPeriodType: [],
      dt: this.dte,
      pt: this.periodType
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide'
  ],

  methods: {

    labToday() {
      return date.formatDate(this.today, 'DD.MM.YYYY')
    },

    fnReqLabel(label) {
      return this.$t(label) + '*'
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

      this.form.dte = this.dt
      this.form.periodType = this.pt
      this.form.mode = this.mode
      if (typeof this.form.periodType === "object")
        this.form.periodType = parseInt(this.form["periodType"]["id"], 10)
console.log("this.form", this.form)
      api
        .post('', {
          method: 'data/saveReservoirPropertiesMeter',
          params: [this.form]
        })
        .then(
          () => {
            err = false
            this.$emit('ok', this.form)
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
          }
        )
        .finally(() => {
          if (!err) this.hide()
        })
    },

    onCancelClick() {
      // we just need to hide the dialog
      this.hide()
    }
  },

  created() {
    //console.info("data", this.data)
    this.loading = true
    api
      .post('', {
        method: 'data/loadPeriodType',
        params: [],
      })
      .then(
        (response) => {
          this.optPeriodType = response.data.result.records
        })
      .catch(error => {
        notifyError(error.message)
      })
      .finally(() => {
        this.loading = false
      })


  }
}
</script>
