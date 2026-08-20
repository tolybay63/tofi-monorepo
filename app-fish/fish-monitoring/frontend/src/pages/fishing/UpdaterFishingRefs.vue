<template>
  <q-dialog
    ref="dialogRef"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="min-width: 40%">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- class -->
        <q-select
          v-model="form.cls"
          :disable="mode === 'upd'"
          :label="fmReqLabel('fishingType')"
          :options="optCls"
          autofocus
          class="q-ma-md"
          dense
          map-options
          option-label="name"
          option-value="id"
          @update:model-value="fnSelectCls"
        />
        <!-- StartDate -->
        <q-input
          v-model="form.StartDate"
          :label="fmReqLabel('StartDate')"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md"
          dense
        />

        <!-- Reservoir -->
        <q-select
          :disable="mode === 'upd'"
          v-model="form.objReservoirShore"
          :label="fmReqLabel('reservoir')"
          :options="optReservoir"
          class="q-ma-md"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterReservoir"
          @update:model-value="fnSelectReservoir"
        />

        <!-- objFishLocation -->
        <q-select
          :disable="mode === 'upd'"
          v-model="objFishLocation"
          :label="fmReqLabel('FishLocation')"
          :options="optFishLocation"
          class="q-ma-md"
          color="blue"
          dense
          map-options
          option-label="name"
          option-value="id"
          options-dense
          options-selected-class="text-blue"
          @update:model-value="fnSelectFishLocation"
        />
        <!-- AreaOfTon -->
        <q-input
          v-model="form['AreaOfTon']"
          :label="fmReqLabel('AreaOfTon')"
          type="number"
          class="q-ma-md"
          dense
        />
        <!-- objFishGear -->
        <q-select
          :disable="mode === 'upd'"
          v-model="objFishGear"
          :label="fmReqLabel('FishGear')"
          :options="optFishGear"
          class="q-ma-md"
          color="blue"
          dense
          map-options
          option-label="name"
          option-value="id"
          options-dense
          options-selected-class="text-blue"
          @update:model-value="fnSelectFishGear"
        />
        <!-- objFishManager -->
        <q-select
          v-model="objFishManager"
          :label="fmReqLabel('FishManager')"
          :options="optFishManager"
          class="q-ma-md"
          color="blue"
          dense
          map-options
          option-label="name"
          option-value="id"
          options-dense
          options-selected-class="text-blue"
          @update:model-value="fnSelectFishManager"
        />

        <!-- FishParticipants -->
        <q-select
          v-model="FishParticipants"
          :label="fmReqLabel('FishParticipants')"
          :options="optFishParticipants"
          class="q-ma-md"
          options-dense
          dense
          map-options
          multiple
          use-chips
          option-label="name"
          option-value="id"
          options-selected-class="text-blue"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('save')"
          color="primary"
          dense
          icon="save"
          @click="onOKClick"
        />
        <q-btn :label="$t('cancel')" color="primary" dense icon="cancel" @click="onCancelClick" />
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

const dialogRef = ref(null)
const form = reactive({ ...props.data })

const optCls = ref([])
const objFishLocation = ref(null)
const optFishLocation = ref([])
const objFishGear = ref(null)
const optFishGear = ref([])
const objFishManager = ref(null)
const optFishManager = ref([])
const FishParticipants = ref([])
const optFishParticipants = ref([])
const optReservoir = ref([])
const optReservoirOrg = ref([])
const loading = ref(false)

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnSelectCls = (val) => {
  if (val) {
    form.cls = val.id
  }
}

const fnSelectFishLocation = (v) => {
  if (v) {
    form.objFishLocation = v.id
    form.pvFishLocation = v.pv
  }
}

const fnSelectReservoir = (v) => {
  if (v) {
    objFishLocation.value = null
    form.objFishLocation = null
    form.pvFishLocation = null

    form.objReservoirShore = v.id
    form.pvReservoirShore = v.pv
    loadFishLocationForSelect(v.id)
  } else {
    form.objReservoirShore = null
    form.pvReservoirShore = null
    loadFishLocationForSelect(0)
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

const fnSelectFishGear = (v) => {
  if (v) {
    form.objFishGear = v.id
    form.pvFishGear = v.pv
  }
}

const fnSelectFishManager = (v) => {
  if (v) {
    form.objFishManager = v.id
    form.pvFishManager = v.pv
  }
}

const loadFishLocationForSelect = (reservoir) => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadFishLocationForSelect',
      params: [reservoir],
    })
    .then((response) => {
      optFishLocation.value = response.data.result.records
    })
    .finally(() => {
      loading.value = false
    })
}

const validSave = () => {
  if (
    !form.cls ||
    !form.AreaOfTon ||
    !objFishLocation.value ||
    !objFishGear.value ||
    !objFishManager.value ||
    FishParticipants.value.length === 0
  )
    return true
  else return false
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
  form.mode = props.mode

  form.FishParticipants = []
  FishParticipants.value.forEach((it) => {
    form.FishParticipants.push(it.id)
  })

  api
    .post('', {
      method: 'data/saveFishingPropertiesRef',
      params: [form],
    })
    .then((response) => {
      err = false
      emit('ok', response.data.result.records[0])
      notifySuccess(proxy?.$t('success'))
    })
    .catch((error) => {
      console.error(error.message)
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
    .post('', { method: 'data/loadCls', params: ['Typ_FishCatch'] })
    .then((res) => {
      optCls.value = res.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', { method: 'data/loadReservoir', params: ['Prop_ReservoirShore'] })
    .then((res) => {
      optReservoir.value = res.data.result['records']
      optReservoirOrg.value = res.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', { method: 'data/loadFishGearForSelect', params: ['Prop_FishGear'] })
    .then((res) => {
      optFishGear.value = res.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', { method: 'data/loadFishManagerForSelect', params: ['Prop_FishManager'] })
    .then((res) => {
      optFishManager.value = res.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', { method: 'data/loadFishParticipantsForSelect', params: ['Prop_FishParticipants'] })
    .then((res) => {
      optFishParticipants.value = res.data.result.records
    })
    .finally(() => {
      loading.value = false
      if (props.mode === 'upd') {
        loadFishLocationForSelect(props.data.objReservoirShore)
        objFishLocation.value = props.data.objFishLocation
        objFishGear.value = props.data.objFishGear
        objFishManager.value = props.data.objFishManager

        FishParticipants.value = []
        let lstData = props.data.lstFishParticipants

        if (lstData) {
          lstData.split(',').forEach((id) => {
            let arr = id.split('_')
            let key = arr[1] + '_' + arr[2]
            for (let i = 0; i < optFishParticipants.value.length; i++) {
              const it = optFishParticipants.value[i]
              if (key === it.id) {
                FishParticipants.value.push({ id: id, name: it.name })
              }
            }
          })
        }
      }
    })
})

defineExpose({
  show,
  hide,
})
</script>
