<template>
  <q-dialog
    ref="dialog"
    autofocus
    persistent
    style="width: 600px"
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <q-item-section v-if="isChild"> {{ $t('parentObj') }}: {{ parentName }} </q-item-section>

        <q-input
          v-model="form.name"
          :label="fnLabel('fldName', true)"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          autofocus
          @blur="onBlurName"
        />
        <q-input
          v-model="form.fullName"
          :label="fnLabel('fldFullName', true)"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <q-select
          v-model="form.cls"
          :label="fnLabel('cls', true)"
          :options="optCls"
          map-options
          option-label="name"
          option-value="id"
          @update:model-value="fnSelect"
        />

        <q-input v-model="form.cmt" :label="fnLabel('fldCmt', false)" type="textarea" />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validName()"
          :label="$t('save')"
          color="primary"
          icon="save"
          @click="onOKClick"
        />
        <q-btn :label="$t('cancel')" color="primary" icon="cancel" @click="onCancelClick" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { api } from '../../boot/axios'
import { notifyError, notifySuccess } from '../../utils/jsutils'

const props = defineProps({
  mode: String,
  isChild: Boolean,
  parentName: String,
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialog = ref(null)
const form = reactive({ ...props.data })
const optCls = ref([])

const fnLabel = (txt, req) => {
  return req ? proxy?.$t(txt) + '*' : proxy?.$t(txt)
}

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim()
    if (!form.fullName || form.fullName.trim() === '') {
      form.fullName = form.name
    }
  }
}

const fnSelect = () => {
  // placeholder for select event handler if needed
}

const validName = () => {
  return !form.name || !form.fullName || !form.cls
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
  form.cls = typeof form.cls === 'object' ? form.cls.id : form.cls

  const method = props.mode === 'ins' ? 'insertEnterprise' : 'updateEnterprise'
  api
    .post('', {
      method: 'data/' + method,
      params: [form],
    })
    .then(() => {
      emit('ok', { res: true })
      notifySuccess(proxy?.$t('success'))
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
  let flag = props.isChild ? 'childs' : 'parents'
  api
    .post('', {
      method: 'data/loadCls',
      params: ['Typ_Enterprise', flag],
    })
    .then((response) => {
      optCls.value = response.data.result.records
    })
    .then(() => {
      if (props.mode === 'ins' && flag === 'parents' && optCls.value.length > 0) {
        form.cls = optCls.value[0].id
      }
    })
})

defineExpose({
  show,
  hide,
})
</script>
