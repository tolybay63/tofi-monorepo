<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <q-input autofocus class="q-my-lg" dense
                 v-model="form.numberval"
                 :model-value="form.numberval"
                 type="number" :label="form.name" />

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

import {notifySuccess} from 'src/utils/jsutils'
import {api} from "boot/axios.js";

export default {
  props: ['data'],

  data() {
    return {
      form: this.data,
      loading: false,
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {

    validSave() {
      if (!this.form.numberval) return true
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
      api
        .post('', {
          method: 'data/saveFishFecundiry',
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
/*
            if (error.response.data.error.message.includes('@')) {
              let msgs = error.response.data.error.message.split('@')
              let m1 = this.$t(`${msgs[0]}`)
              let m2 = msgs.length > 1 ? ': [' + msgs[1] + ']' : ''
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
  },
}
</script>
