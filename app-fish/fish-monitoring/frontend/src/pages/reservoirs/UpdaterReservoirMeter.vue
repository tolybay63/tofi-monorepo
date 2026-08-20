<template>
  <q-dialog
    ref="dialogRef"
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
        <q-input
          autofocus
          class="q-my-lg"
          dense
          v-model="form.numberval"
          type="number"
          :label="form.name"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
          class="q-mt-xl"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
          class="q-mt-xl"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from 'vue'
import { notifySuccess } from '@/utils/jsutils'
import { api } from '@/boot/axios'

const props = defineProps({
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)
const form = reactive({ ...props.data })

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
      method: 'data/saveReservoirMeter',
      params: [form],
    })
    .then((response) => {
      err = false
      let index = response.data.result.records.findIndex((rec) => rec.id === form.prop)
      emit('ok', response.data.result.records[index])
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
