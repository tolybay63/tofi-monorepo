<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-down"
    transition-hide="slide-down"
  >
    <q-card class="no-scroll" style="min-width: 80vw; min-height: 80vh; width: 100%; height: 90%">
      <q-bar class="bg-brand-light">
        <q-space></q-space>
        <q-btn @click="hide" color="white" flat icon="close"></q-btn>
      </q-bar>

      <div class="fit">
        <q-pdfviewer type="html5" :src="path" />
      </div>
    </q-card>
  </q-dialog>
</template>
<script>
import { ref } from 'vue'
import { api, baseURL } from 'boot/axios'
import { notifyError } from 'src/utils/jsutils'

export default {
  props: ['id', 'fileName'],

  data() {
    return {
      path: '',
      loading: false,
    }
  },

  emits: ['ok', 'hide'],

  methods: {
    show() {
      this.$refs.dialog.show()
    },

    hide() {
      this.$refs.dialog.hide()
    },

    onDialogHide() {
      this.$emit('hide')
    },

    onOKClick: function () {
      this.hide()
    },
  },
  created() {
    this.loading = ref(true)
    api
      .post(baseURL, {
        method: 'data/getPathFile',
        params: [this.id],
      })
      .then((response) => {
        //this.path = "/pdf/"+this.fileName
        this.path = response.data.result
        console.log('path', this.path)
      })
      .catch((error) => {
        notifyError(error.message)
      })
      .finally(() => {
        this.loading = ref(false)
      })
  },
}
</script>
