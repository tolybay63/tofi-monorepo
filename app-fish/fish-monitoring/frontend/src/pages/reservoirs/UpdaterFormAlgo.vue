<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <div> {{form['title']}}: </div>
        <q-input
          autofocus
          class="q-my-md"
          dense
          v-model="form['numberval']"
          type="number"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import {notifySuccess} from "@/utils/jsutils.js";
import {api} from "@/boot/axios.js";


const props = defineProps({
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)
const form = reactive({ ...props.data })

console.info("UPDATER", form)
const validSave = () => {
  if (!form.numberval) return true
}

const show = () => {
  dialogRef.value?.show()
}

const hide = () => {
  dialogRef.value?.hide()
}

const onDialogHide = () => {
  emit('hide')
}

const onOKClick = () => {
  let err = false
  api
    .post('', {
      method: 'data/saveAlgo',
      params: [form],
    })
    .then((response) => {
      err = false
      emit('ok', {idval: response.data.result, numberval: form["numberval"]})
      notifySuccess(proxy?.$t('success'))
    })
    .catch((error) => {
      err = true
      console.error(error.message)
    })
    .finally(() => {
      if (!err) hide()
    })

}

const onCancelClick = () => {
  hide()
}

defineExpose({
  show,
  hide,
})
</script>
