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
      <q-bar class="text-white bg-primary">
        <div>{{ $t('updRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- CalcCreatDate -->
        <q-input
          v-model="form['CalcCreatDate']"
          :label="fmReqLabel('CalcCreatDate')"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md" dense
        />

        <!-- CalcLastDate -->
        <q-input
          v-model="form['CalcLastDate']"
          :label="fmReqLabel('CalcLastDate')"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md" dense
        />
        <!-- CalcStartYear -->
        <q-input
          v-model="form['CalcStartYear']"
          :label="fmReqLabel('CalcStartYear')"
          :rules="[(val) => (!!val && val.trim().length===4) || $t('req')]"
          class="q-ma-md" dense mask="####"
        />

        <!-- CalcEndYear -->
        <q-input
          v-model="form['CalcEndYear']"
          :label="fmReqLabel('CalcEndYear')"
          :rules="[(val) => (!!val && val.trim().length===4) || $t('req')]"
          class="q-ma-md" dense mask="####"
        />

        <!-- Prop_CalcFishSpec -->
        <q-select
          class="q-mt-md"
          v-model="form['fvCalcFishSpec']"
          dense
          options-dense
          :options="optCalcFishSpec"
          :label="fmReqLabel('CalcFishSpec')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectCalcFishSpec"
        />

        <!-- Prop_CalcStatus -->
        <q-select
          class="q-mt-md"
          v-model="form['fvCalcStatus']"
          dense
          options-dense
          :options="optCalcStatus"
          :label="fmReqLabel('CalcStatus')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectCalcStatus()"
        />

        <!-- Prop_ReservoirShore -->
        <q-select
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

        <!-- Prop_CalcUser -->
        <q-select
          v-model="form['objCalcUser']"
          :label="fmReqLabel('CalcUser')"
          :options="optCalcUser"
          class="q-ma-md"
          dense
          map-options
          option-label="name"
          option-value="id"
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

const optCalcFishSpec = ref([])
const optCalcStatus = ref([])

const optReservoir = ref([])
const optReservoirOrg = ref([])
const optCalcUser = ref([])


const loading = ref(false)

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnSelectCalcFishSpec = (v) => {
  if (v) {
    form.fvCalcFishSpec = v.id
    form.pvCalcFishSpec = v["pv"]
  }
}

const fnSelectCalcStatus = (v) => {
  if (v) {
    form.fvCalcStatus = v.id
    form.pvCalcStatus = v["pv"]
  }
}

const fnSelectReservoir = (v) => {
  if (v) {
    form.objReservoirShore = v.id
    form.pvReservoirShore = v["pv"]
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

const validSave = () => {
  return !form["CalcCreatDate"] ||
    !form["CalcLastDate"] ||
    !form["CalcStartYear"] ||
    !form["CalcEndYear"] ||
    !form["fvCalcFishSpec"] ||
    !form["fvCalcStatus"] ||
    !form["objCalcUser"] ||
    !form["objReservoirShore"];
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
      method: 'data/saveMainProps',
      params: [form],
    })
    .then(() => {
      err = false
      emit('ok', {ok: true})
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

onMounted(async () => {
  loading.value = true

  try {
    const resp1 = await api.post('', { method: 'data/loadFVasStore', params: ['Prop_CalcFishSpec'] })
    optCalcFishSpec.value = resp1.data.result['records']
    //
    const resp2 = await api.post('', { method: 'data/loadFVasStore', params: ['Prop_CalcStatus'] })
    optCalcStatus.value = resp2.data.result['records']
    //
    const resp3 = await api.post('', { method: 'data/loadReservoirs', params: ['Prop_ReservoirShore'] })
    optReservoir.value = resp3.data.result['records']
    optReservoirOrg.value = resp3.data.result['records']
    //
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false

    console.info("data", props.data)
    console.info("form", form)
  }
})

defineExpose({
  show,
  hide,
})
</script>
