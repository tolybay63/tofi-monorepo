<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <q-item-section v-if="isChild">
          <div class="row">
            <span class="text-blue q-mt-md-md"> {{ $t('region') }}: </span>
            <span class="q-mb-lg q-ml-md text-bold"> {{ parentName }} </span>
          </div>
        </q-item-section>

        <q-input
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="fmReqLabel('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <q-input
          v-model="form.fullName"
          :label="fmReqLabel('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <q-input v-model="form.cmt" type="textarea" :label="$t('fldCmt')" />
      </q-card-section>

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

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { api } from '@/boot/axios'
import { notifyError } from '../../utils/jsutils'

const props = defineProps({
  mode: String,
  isChild: Boolean,
  parentName: String,
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialog = ref(null)
const loading = ref(false)
const clsRegion = ref(0)
const clsDistrict = ref(0)
const form = reactive({ ...props.data })

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim()
    if (!form.fullName || form.fullName.trim() === '') {
      form.fullName = form.name
    }
  }
}

const validSave = () => {
  return !form.name || !form.fullName
}

const show = () => {
  dialog.value?.show()
}

const hide = () => {
  dialog.value?.hide()
}

const onDialogHide = () => {
  emit('hide')
}

const onOKClick = () => {
  const method = props.mode === 'ins' ? 'insertKATO' : 'updateKATO'
  form.cls = props.isChild ? clsDistrict.value : clsRegion.value

  api
    .post('', {
      method: 'data/' + method,
      params: [form],
    })
    .then((response) => {
      emit('ok', response.data.result['records'][0])
    })
    .catch((error) => {
      let msg = error.response?.data?.error?.message || error.message
      notifyError(msg)
    })
    .finally(() => {
      hide()
    })
}

const onCancelClick = () => {
  hide()
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/getClsIds',
      params: [''],
    })
    .then((response) => {
      clsRegion.value = response.data.result['Cls_Regions']
      clsDistrict.value = response.data.result['Cls_Districts']
    })
    .catch((error) => {
      let msg = error.response?.data?.error?.message || error.message
      notifyError(msg)
    })
    .finally(() => {
      loading.value = false
    })
})

defineExpose({
  show,
  hide,
})
</script>
