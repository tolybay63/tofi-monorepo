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
        <!-- LinkToSample -->
        <q-select
          v-model="form.objLinkToSample"
          :model-value="form.objLinkToSample"
          autofocus
          :label="fmReqLabel('sampleNumber')"
          :options="optLinkToSample"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          use-input
          @update:model-value="fnSelectLinkToSample"
          @filter="filterLinkToSample"
        />

        <!-- ResearchNumber -->
        <q-input
          :model-value="form.ResearchNumber"
          v-model="form.ResearchNumber"
          :label="fmReqLabel('ResearchNumber')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- ResearchDate -->
        <q-input
          :model-value="form.ResearchDate"
          v-model="form.ResearchDate"
          type="date"
          :label="fmReqLabel('ResearchDate')"
          :rules="[(val) => !!val || $t('req')]"
        />

        <!-- ResearchExecutor -->
        <q-select
          v-model="form.objResearchExecutor"
          :model-value="form.objResearchExecutor"
          :label="fmReqLabel('executor')"
          :options="optExecutor"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-md"
          use-input
          @update:model-value="fnSelectExecutor"
          @filter="filterExecutor"
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
import { api, baseURL } from 'boot/axios'
import { notifyError, notifySuccess } from 'src/utils/jsutils'

export default {
  props: ['mode', 'data', 'codCls'],

  data() {
    return {
      form: this.data,
      loading: false,
      optLinkToSample: [],
      optLinkToSampleOrg: [],
      optExecutor: [],
      optExecutorOrg: [],
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

    fnSelectLinkToSample(v) {
      //if (v) {
      this.form.objLinkToSample = v.id
      this.form.pvLinkToSample = v["pv"]
      //}
    },

    filterLinkToSample(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optLinkToSample = this.optLinkToSampleOrg
        })
        return
      }
      update(() => {
        if (this.optLinkToSampleOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optLinkToSample = this.optLinkToSampleOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectExecutor(v) {
      this.form.objResearchExecutor = v.id
      this.form.pvResearchExecutor = v["pv"]
    },

    filterExecutor(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optExecutor = this.optExecutorOrg
        })
        return
      }
      update(() => {
        if (this.optExecutorOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optExecutor = this.optExecutorOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    validSave() {
      if (
        !this.form.objLinkToSample ||
        !this.form.ResearchNumber ||
        !this.form.objResearchExecutor ||
        !this.form.ResearchDate
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
      this.$axios
        .post(baseURL, {
          method: 'data/saveResultSamplingProperties',
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

  created() {
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadSampleNumber',
        params: [this.codCls],
      })
      .then(
        (response) => {
          this.optLinkToSample = response.data.result.records
          this.optLinkToSampleOrg = response.data.result.records
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
      .post(baseURL, {
        method: 'data/loadExecutor',
        params: ['Typ_Users', 'Prop_ResearchExecutor'],
      })
      .then(
        (response) => {
          this.optExecutor = response.data.result.records
          this.optExecutorOrg = response.data.result.records
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
  },
}
</script>
