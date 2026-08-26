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
        <!-- name -->
        <q-input
          autofocus
          dense
          v-model="form.name"
          :label="fmReqLabel('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- Coordinate -->
        <q-input
          v-model="form['Coordinate']"
          :label="fmReqLabel('coordinates')"
          dense
          class="q-mb-md"
        />

        <!-- Reservoir -->
        <q-select
          v-model="form.objReservoirShore"
          :label="fmReqLabel('reservoir')"
          :model-value="form.objReservoirShore"
          :options="optReservoir"
          class="q-mb-lg"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterReservoir"
          @update:model-value="fnSelectReservoir"
        />

        <!-- AreaOfTon -->
        <q-input
          v-model="form.AreaOfTon"
          :label="fmReqLabel('AreaOfTon')"
          type="number"
          dense
          class="q-mb-md"
        />

        <!-- Description -->
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
import { api } from '@/boot/axios'
import { notifySuccess } from '@/utils/jsutils'

const props = defineProps({
  mode: String,
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialog = ref(null)
const optReservoir = ref([])
const optReservoirOrg = ref([])
const loading = ref(false)
const form = reactive({ ...props.data })

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const validSave = () => {
  if (!form.AreaOfTon || !form['Coordinate'] || !form.name || !form.objReservoirShore) {
    return true
  }
}

const fnSelectReservoir = (v) => {
  if (v) {
    form.objReservoirShore = v.id
    form.pvReservoirShore = v.pv
  } else {
    form.objReservoirShore = null
    form.pvReservoirShore = null
  }
}

const filterReservoir = (val, update) => {
  if (val === null || val === '') {
    update(() => {
      optReservoir.value = optReservoirOrg.value
    })
    return
  }
  update(() => {
    if (optReservoirOrg.value.length < 2) return
    const needle = val.toLowerCase()
    optReservoir.value = optReservoirOrg.value.filter((v) => {
      return v.name?.toLowerCase().indexOf(needle) > -1
    })
  })
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
      method: 'data/saveSamplingStation',
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
      method: 'data/loadReservoir',
      params: ['Prop_ReservoirShore'],
    })
    .then((response) => {
      optReservoir.value = response.data.result['records']
      optReservoirOrg.value = response.data.result['records']
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
