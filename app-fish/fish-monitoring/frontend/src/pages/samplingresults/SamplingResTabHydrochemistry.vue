<template>
  <div class="no-padding no-margin bg-amber-1" style="height: calc(100vh - 250px)">
    <div class="row">
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" text-color="white" icon="free_cancellation"> </q-avatar>
        {{ $t('researchResults') }}
      </div>

      <q-space />
      <q-btn
        v-if="hasTarget('mon:rzp:him:res:upd')"
        icon="edit"
        dense
        color="secondary"
        class="q-ma-sm"
        :disable="loading"
        @click="editObjPropsChild(form, 'upd')"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t('editRecord') }}
        </q-tooltip>
      </q-btn>
      <q-btn
        v-if="hasTarget('mon:rzp:him:res:del')"
        icon="delete"
        dense
        color="secondary"
        class="q-ma-sm"
        :disable="loading || Object.keys(form).length === 3"
        @click="removeObjPropsChild(form)"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t('deletingRecord') }}
        </q-tooltip>
      </q-btn>
      <q-space />
    </div>

    <q-item-label class="text-center q-mt-xl"> Растворенные газы</q-item-label>
    <hr style="margin-top: -2px; margin-bottom: -2px" class="q-mx-sm" />

    <div class="row justify-between">
      <!-- DissGasesCO2 -->
      <div class="col">
        <q-input
          :model-value="form['DissGasesCO2']"
          stack-label
          v-model="form['DissGasesCO2']"
          type="number"
          class="q-mx-sm"
          label="CO2, мг/дм3"
          readonly
        />
      </div>
      <!-- DissGasesO2 -->
      <div class="col">
        <q-input
          :model-value="form['DissGasesO2']"
          stack-label
          v-model="form['DissGasesO2']"
          type="number"
          class="q-mx-sm"
          label="O2, мг/дм3"
          readonly
        />
      </div>
      <!-- DissGasesCO2Percent -->
      <div class="col">
        <q-input
          :model-value="form['DissGasesCO2Percent']"
          stack-label
          v-model="form['DissGasesCO2Percent']"
          type="number"
          class="q-mx-sm"
          label="O2, % насыщения"
          readonly
        />
      </div>
    </div>

    <q-item-label class="text-center q-mt-xl"> Биогенные соединения, мг/дм3</q-item-label>
    <hr style="margin-top: -2px; margin-bottom: -2px" class="q-mx-sm" />

    <div class="row justify-between">
      <!-- BiogenicCompNH4 -->
      <div class="col">
        <q-input
          :model-value="form['BiogenicCompNH4']"
          v-model="form['BiogenicCompNH4']"
          type="number"
          stack-label
          label="NH4"
          class="q-mx-sm"
          readonly
        />
      </div>
      <div class="col">
        <!-- BiogenicCompNO2 -->
        <q-input
          :model-value="form['BiogenicCompNO2']"
          v-model="form['BiogenicCompNO2']"
          type="number"
          stack-label
          label="NO2"
          class="q-mx-sm"
          readonly
        />
      </div>
      <div class="col">
        <!-- BiogenicCompNO3 -->
        <q-input
          :model-value="form['BiogenicCompNO3']"
          v-model="form['BiogenicCompNO3']"
          type="number"
          stack-label
          label="NO3"
          class="q-mx-sm"
          readonly
        />
      </div>
      <div class="col">
        <!-- BiogenicCompPO4 -->
        <q-input
          :model-value="form['BiogenicCompPO4']"
          v-model="form['BiogenicCompPO4']"
          type="number"
          stack-label
          label="PO4"
          class="q-mx-sm"
          readonly
        />
      </div>
    </div>

    <div class="row justify-between q-mt-xl">
      <!-- Ph -->
      <div class="col-3">
        <q-input
          :model-value="form['Ph']"
          v-model="form['Ph']"
          stack-label
          label="pH показатель"
          type="number"
          class="q-mx-sm"
          readonly
        />
      </div>
      <!-- OrganicMatter -->
      <div class="col-5">
        <q-input
          :model-value="form['OrganicMatter']"
          class="q-mx-sm"
          v-model="form['OrganicMatter']"
          type="number"
          stack-label
          label="Органическое вещество, мгО/дм3"
          readonly
        />
      </div>
      <!-- Mineralization -->
      <div class="col-4">
        <q-input
          :model-value="form['Mineralization']"
          class="q-mx-sm"
          v-model="form['Mineralization']"
          type="number"
          stack-label
          label="Минирализация, мг/дм3"
          readonly
        />
      </div>
    </div>
  </div>
</template>

<script>
import {api} from 'boot/axios'
import {hasTarget, notifyError} from 'src/utils/jsutils'
import {extend} from 'quasar'
import UpdaterResHydrochemistryChild from 'pages/samplingresults/UpdaterResHydrochemistryChild.vue'

export default {
  name: 'SamplingResTabHydrochemistry',

  props: [
    'codCls', //cod класса забора
    'cls', //id класса исследования забора
    'obj',
  ],

  data() {
    return {
      form: { obj: this.obj, cls: this.cls },
      loading: false,
      mapMeasure: new Map(),
    }
  },

  methods: {
    hasTarget,

    editObjPropsChild(row, mode) {
      let data = { parent: this.obj, cls: this.cls }
      if (mode === 'upd') {
        extend(true, data, row)
      }

      this.$q
        .dialog({
          component: UpdaterResHydrochemistryChild,
          componentProps: {
            data: data,
            mode: mode,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          Object.keys(row).forEach((key) => {
            row[key] = null
          })
          for (let key in r) {
            row[key] = r[key]
            /*
            if (r.hasOwnProperty(key)) {
              row[key] = r[key]
            }
*/
          }
        })
    },

    removeObjPropsChild(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord'),
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          api
            .post('', {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1],
            })
            .then(
              () => {
                this.form = {}
              },
              (error) => {
                let msg = error.message
                if (error.response) msg = error.response.data.error.message
                notifyError(msg)
              }
            )
        })
    },

    loadHydrochemistry(obj) {
      this.loading = true
      api
        .post('', {
          method: 'data/loadResultHydrochemistry',
          params: [obj, false, 0],
        })
        .then((response) => {
          this.form = response.data.result.records[0]
          //console.info("rows child", this.form)
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
    },
  },

  created() {
    this.loading = true
    api
      .post('', {
        method: 'data/measureInfo',
        params: [],
      })
      .then((response) => {
        this.mapMeasure = response.data.result
      })
  },
}
</script>

<style></style>
