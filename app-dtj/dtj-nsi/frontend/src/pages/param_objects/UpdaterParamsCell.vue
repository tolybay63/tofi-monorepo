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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <h6 class="q-mt-none text-center"> {{title}} </h6>

        <q-input autofocus class="q-my-lg" dense v-model="form.val" :model-value="form.val" type="number" :label="fnLabel()" />

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

import { api, baseURL } from 'boot/axios'
import { notifyError, notifySuccess } from 'src/utils/jsutils'

export default {
  props: ['mode', 'data'],

  data() {
    //console.info("data", this.data)
    let tit = this.data.name + " (" + this.$t(this.data["codProp"]) + ")"

    return {
      form: this.data,
      title: tit,
      loading: false,
    }
  },

  emits: [
    // REQUIRED
    'ok',
    'hide',
  ],

  methods: {
    fnLabel() {
      return this.$t(this.data["codProp"])
    },

    validSave() {
      if (!this.form.val) return true
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
        .post(baseURL, {
          method: 'data/saveParamComponentValue',
          params: [this.form],
        })
        .then(
          (response) => {
            err = false
            this.$emit('ok', response.data.result)
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
  },
}
</script>
