<template>
  <q-dialog
    ref="dialogRef"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- Reservoir -->
        <q-select
          v-model="form.reservoir"
          :label="fmReqLabel('reservoir')"
          :options="optReservoir"
          class="q-mb-lg"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterReservoir"
          @update:model-value="fnSelectReservoir"
          :disable="mode === 'upd'"
        />

        <!-- TypeOfFish -->
        <q-select
          v-model="form.typeOfFish"
          :label="fmReqLabel('typeOfFish')"
          :options="optTypeOfFish"
          class="q-mb-lg"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterTypeOfFish"
          @update:model-value="fnSelectTypeOfFish"
          :disable="mode === 'upd'"
        />
        <!-- FishSpawPeriod -->
        <q-input
          v-model="form['FishSpawPeriod']"
          :label="$t('FishSpawPeriod')"
          class="q-mb-lg"
          dense
        />

        <!-- FishStartPuberty -->
        <q-input
          v-model="form['FishStartPuberty']"
          :label="$t('FishStartPuberty')"
          class="q-mb-lg"
          dense
          type="number"
        />

        <!-- FishEndPuberty -->
        <q-input
          v-model="form['FishEndPuberty']"
          :label="$t('FishEndPuberty')"
          class="q-mb-lg"
          dense
          type="number"
        />

        <!-- FishSpawFrequency -->
        <q-input
          v-model="form['FishSpawFrequency']"
          :label="$t('FishSpawFrequency')"
          class="q-mb-lg"
          dense
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('save')"
          class="q-mt-xl"
          color="primary"
          icon="save"
          @click="onOKClick"
        />
        <q-btn
          :label="$t('cancel')"
          class="q-mt-xl"
          color="primary"
          icon="cancel"
          @click="onCancelClick"
        />
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

const optReservoir = ref([])
const optReservoirOrg = ref([])
const optTypeOfFish = ref([])
const optTypeOfFishOrg = ref([])
const loading = ref(false)

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnSelectReservoir = (v) => {
  if (v) {
    form.reservoir = v.id
    form.cls1 = v.cls
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

const fnSelectTypeOfFish = (v) => {
  if (v) {
    form.typeOfFish = v.id
    form.cls2 = v.cls
  }
}

const filterTypeOfFish = (val, update) => {
  if (val === null || val === '') {
    update(() => {
      optTypeOfFish.value = optTypeOfFishOrg.value
    })
    return
  }
  update(() => {
    if (optTypeOfFishOrg.value.length < 2) return
    const needle = val.toLowerCase()
    optTypeOfFish.value = optTypeOfFishOrg.value.filter((v) => {
      return v.name?.toLowerCase().indexOf(needle) > -1
    })
  })
}

const validSave = () => {
  if (!form.reservoir || !form.typeOfFish) return true
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
  api
    .post('', {
      method: 'data/savePiscesReservoir',
      params: [form],
    })
    .then((response) => {
      err = false
      emit('ok', response.data.result.records[0])
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
      params: ['Typ_WaterBodies'],
    })
    .then((res) => {
      optReservoir.value = res.data.result['records']
      optReservoirOrg.value = res.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/loadTypeOfFish',
      params: ['Typ_Fish'],
    })
    .then((res) => {
      optTypeOfFish.value = res.data.result['records']
      optTypeOfFishOrg.value = res.data.result['records']
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
