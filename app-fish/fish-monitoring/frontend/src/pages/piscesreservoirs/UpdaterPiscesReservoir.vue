<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="height: 400px; width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- Branch -->
        <q-select
          v-model="form.branch"
          :model-value="form.branch"
          :label="fmReqLabel('branch')"
          :options="optBranch"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-lg"
          use-input
          @update:model-value="fnSelectBranch"
          @filter="filterBranch"
        />

        <!-- Reservoir -->
        <q-select
          v-model="form.reservoir"
          :model-value="form.reservoir"
          :label="fmReqLabel('reservoir')"
          :options="optReservoir"
          dense
          map-options
          option-label="name"
          option-value="id"
          class="q-mb-lg"
          use-input
          @update:model-value="fnSelectReservoir"
          @filter="filterReservoir"
        />

        <!-- TypeOfFish -->
        <q-item-label class="text-grey-7" style="font-size: 0.8em">{{fmReqLabel('typeOfFish')}}
        </q-item-label>
        <treeselect
          :options="optTypeOfFish"
          v-model="form.typeoffish"
          maxHeight="800"
          :normalizer="normalizer"
          :placeholder="$t('select')"
          :noChildrenText="$t('noChilds')"
          :noResultsText="$t('noResult')"
          :noOptionsText="$t('noResult')"
          @select="fnSelectTypeOfFish"
          class="q-mb-xl"
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
          class="q-mt-xl"
        />
        <q-btn color="primary" icon="cancel" :label="$t('cancel')" @click="onCancelClick" class="q-mt-xl"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script>

import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import { api, baseURL } from 'boot/axios'
import { notifyError, notifySuccess, pack } from 'src/utils/jsutils'

export default {
  components: {treeselect},
  props: ['mode', 'data'],

  data() {
    return {
      form: this.data,
      loading: false,
      optBranch: [],
      optBranchOrg: [],
      optReservoir: [],
      optReservoirOrg: [],
      optTypeOfFish: [],
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
        this.form.branch = v.id
        this.form.reservoir = null
        this.form.cls1 = null
        this.form.typeoffish = null
        this.form.cls2 = null
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
      this.form.reservoir = v.id
      this.form.cls1 = v.cls
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

    normalizer(node) {
      return {
        id: node.id,
        label: node.name,
      };
    },

    fnSelectTypeOfFish(v) {
      this.form.typeoffish = v.id
      this.form.cls2 = v.cls
    },

    loadReservoir(branch) {
      this.loading = true
      api
        .post(baseURL, {
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
      if (!this.form.branch || !this.form.reservoir || !this.form.typeoffish) return true
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
          method: 'data/savePiscesReservoir',
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
            if (error.response.data.error.message.includes('@')) {
              let msgs = error.response.data.error.message.split('@')
              let m1 = this.$t(`${msgs[0]}`)
              let m2 = msgs.length > 1 ? ': [' + msgs[1] + ']' : ''
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
    },
  },
  created() {
    //
    this.loading = true
    api
      .post(baseURL, {
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
      .post(baseURL, {
        method: 'data/loadTypeOfFishForSelect',
        params: ['Cls_FishTypes'],
      })
      .then(
        (response) => {
          this.optTypeOfFish = pack( response.data.result.records, "id")
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
}
</script>
