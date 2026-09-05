<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    @show="onDialogShow"
    persistent
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
        <q-input
          ref="inputNameRef"
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
import { notifySuccess } from '@/utils/jsutils'
import { api } from '@/boot/axios'

const props = defineProps({
  data: Object,
  mode: String,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)
const inputNameRef = ref(null);
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

const onDialogShow = () => {
  // Принудительно ставим фокус после полного открытия окна и закрытия q-menu
  inputNameRef.value?.focus();
};

const onOKClick = () => {
  let err = false
  api
    .post('', {
      method: 'data/saveFishPage',
      params: [form],
    })
    .then((response) => {
      err = false
      emit('ok', {id: response.data.result, value: form["numberval"]})
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
