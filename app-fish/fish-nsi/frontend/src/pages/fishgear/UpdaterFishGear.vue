<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 800px"
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
          autofocus
          dense
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <q-select
          class="q-mt-md"
          v-model="form.cls"
          dense
          options-dense
          :options="optCls"
          :label="fmReqLabel('FishGearType')"
          option-value="id"
          option-label="name"
          map-options
          :disable="mode === 'upd'"
          @update:model-value="fnSelectCls"
        />
        <q-input v-model="form['Description']" type="textarea" :label="$t('description')" />
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
import { api } from '../../boot/axios'
import { notifySuccess } from '../../utils/jsutils'

const props = defineProps({
  mode: String,
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialog = ref(null)
const loading = ref(false)
const optCls = ref([])
const form = reactive({ ...props.data })

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnSelectCls = (v) => {
  if (v) {
    form.cls = v.id
  }
}

const validSave = () => {
  if (!form.cls || !form.name) return true
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
  let err = false
  form.mode = props.mode
  api
    .post('', {
      method: 'data/saveFishGear',
      params: [form],
    })
    .then((response) => {
      err = false
      emit('ok', response.data.result['records'][0])
      notifySuccess(proxy?.$t('success'))
    })
    .catch(() => {
      err = true
    })
    .finally(() => {
      if (!err) hide()
    })
}

const onCancelClick = () => {
  hide()
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadClsList',
      params: ['Typ_FishGear'],
    })
    .then((response) => {
      optCls.value = response.data.result['records']
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
