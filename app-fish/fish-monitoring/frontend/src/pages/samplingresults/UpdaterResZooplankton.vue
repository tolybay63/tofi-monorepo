<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t('update') }}</div>
      </q-bar>

      <q-card-section>
        <div class="text-h6">
          <div>Измерение 1: {{ dim1 }}</div>
          <div>Измерение 2: {{ dim2 }}</div>
        </div>

        <!-- Value -->
        <q-input :model-value="form.val" v-model="form.val" :label="$t('val')" autofocus />
      </q-card-section>
      <!---->

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
  props: ['data', 'dim1', 'dim2'],

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

      api
        .post('', {
          method: 'datamulti/saveResultZooplankton',
          params: [this.form],
        })
        .then(
          () => {
            err = false
            this.$emit('ok', { res: true })
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
