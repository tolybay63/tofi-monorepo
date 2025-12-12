<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="min-width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <q-item-label class="text-center q-mt-xl"> Растворенные газы</q-item-label>
        <hr style="margin-top: -2px; margin-bottom: -2px" class="q-mx-sm" />

        <div class="row justify-between">
          <!-- DissGasesCO2 -->
          <div style="width: 200px">
            <q-input
              :model-value="form['DissGasesCO2']"
              v-model="form['DissGasesCO2']"
              stack-label
              autofocus
              type="number"
              class="q-mx-sm"
              label="CO2, мг/дм3"
            />
          </div>

          <!-- DissGasesO2 -->
          <div style="width: 200px">
            <q-input
              :model-value="form['DissGasesO2']"
              v-model="form['DissGasesO2']"
              stack-label
              type="number"
              class="q-mx-sm"
              label="O2, мг/дм3"
            />
          </div>

          <!-- DissGasesCO2Percent -->
          <div style="width: 200px">
            <q-input
              :model-value="form['DissGasesCO2Percent']"
              v-model="form['DissGasesCO2Percent']"
              stack-label
              type="number"
              class="q-mx-sm"
              label="O2, % насыщения"
            />
          </div>
        </div>

        <q-item-label class="text-center q-mt-xl"> Биогенные соединения, мг/дм3</q-item-label>
        <hr style="margin-top: -2px; margin-bottom: -2px" class="q-mx-sm" />

        <div class="row justify-between">
          <!-- BiogenicCompNH4 -->
          <div style="width: 150px">
            <q-input
              :model-value="form['BiogenicCompNH4']"
              v-model="form['BiogenicCompNH4']"
              type="number"
              stack-label
              label="NH4"
              class="q-mx-sm"
            />
          </div>
          <!-- BiogenicCompNO2 -->
          <div style="width: 150px">
            <q-input
              :model-value="form['BiogenicCompNO2']"
              v-model="form['BiogenicCompNO2']"
              type="number"
              stack-label
              label="NO2"
              class="q-mx-sm"
            />
          </div>

          <!-- BiogenicCompNO3 -->
          <div style="width: 150px">
            <q-input
              :model-value="form['BiogenicCompNO3']"
              v-model="form['BiogenicCompNO3']"
              type="number"
              stack-label
              label="NO3"
              class="q-mx-sm"
            />
          </div>

          <!-- BiogenicCompPO4 -->
          <div style="width: 150px">
            <q-input
              :model-value="form['BiogenicCompPO4']"
              v-model="form['BiogenicCompPO4']"
              type="number"
              stack-label
              label="PO4"
              class="q-mx-sm"
            />
          </div>
        </div>

        <div class="row justify-between q-mt-xl">
          <!-- Ph -->
          <div style="width: 160px">
            <q-input
              :model-value="form['Ph']"
              v-model="form['Ph']"
              stack-label
              label="pH показатель"
              type="number"
              class="q-mx-sm"
            />
          </div>

          <!-- OrganicMatter -->
          <div style="width: 240px">
            <q-input
              :model-value="form['OrganicMatter']"
              class="q-mx-sm"
              v-model="form['OrganicMatter']"
              type="number"
              stack-label
              label="Органическое вещество, мгО/дм3"
            />
          </div>

          <!-- Mineralization -->
          <div style="width: 200px">
            <q-input
              :model-value="form['Mineralization']"
              class="q-mx-sm"
              v-model="form['Mineralization']"
              type="number"
              stack-label
              label="Минерализация, мг/дм3"
            />
          </div>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn color="primary" icon="save" :label="$t('save')" @click="onOKClick" />
        <q-btn color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>
import {notifyError, notifySuccess} from 'src/utils/jsutils'

export default {
  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      loading: false,
      mapMeasure: new Map(),
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {

/*
    fmReqLabel(label, prop) {
      if (prop && this.mapMeasure[prop] !== undefined) {
        let o = this.mapMeasure[prop]
        return this.$t(label) + ', ' + o['name'] + '*'
      } else return this.$t(label) + '*'
    },
*/

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
      api
        .post('', {
          method: 'data/saveResultHydrochemistryChild',
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

  created() {},
}
</script>
