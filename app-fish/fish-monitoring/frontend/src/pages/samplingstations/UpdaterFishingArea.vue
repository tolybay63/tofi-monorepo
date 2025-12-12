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
          :label="fmReqLabel('branch')"
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
          v-model="form.objReservoirShore"
          :model-value="form.objReservoirShore"
          :label="fmReqLabel('reservoir')"
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

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- Description -->
        <q-input :model-value="form['cmt']" v-model="form['cmt']" type="textarea" :label="$t('fldCmt')" />
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
    return {
      form: this.data,
      loading: false,
      optBranch: [],
      optBranchOrg: [],
      optReservoir: [],
      optReservoirOrg: [],
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

    fnSelectBranch(v) {
      if (v) {
        this.form.objBranch = v.id
        this.form.pvBranch = v["pv"]

        this.form.objReservoirShore = null
        this.form.pvReservoirShore = null
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
      this.form.objReservoirShore = v.id
      this.form.pvReservoirShore = v["pv"]
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

    validSave() {
      if (!this.form.objBranch || !this.form.objReservoirShore || !this.form.name) return true
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
      api
        .post('', {
          method: 'data/saveFishingArea',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result.records[0])
            notifySuccess(this.$t('success'))
          },
          (error) => {
            //console.log("error.response.data=>>>", error.response.data.error.message)
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
    if (this.mode === 'upd') {
      this.loadReservoir(this.form.objBranch)
    }
  },
}
</script>
