<template>
  <q-dialog
    ref="dialogRef"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="min-width: 60%">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- class -->
        <div class="row">
          <div class="col">
            <q-select
              v-model="form.cls"
              :disable="mode === 'upd'"
              :label="fmReqLabel('vidReservoir')"
              :options="optCls"
              autofocus
              class="q-ma-md"
              dense
              map-options
              option-label="name"
              option-value="id"
              @update:model-value="fnSelectCls"
            />
          </div>
          <!-- objKATO -->
          <div class="col">
            <TreeSelect
              v-model="objKATO"
              :label="fmReqLabel('kato2')"
              :options="optKATO"
              class="q-ma-md"
              multiple
              node-key="key"
            />
          </div>
        </div>
        <div class="row">
          <!-- name -->
          <div class="col">
            <q-input
              v-model="form.name"
              :label="fmReqLabel('fldName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
              class="q-ma-md"
              dense
            />
          </div>
        </div>

        <div class="row">
          <div class="col">
            <!-- F_ReservoirType -->
            <q-select
              v-model="form.fvReservoirType"
              :label="fmReqLabel('ReservoirType')"
              :options="optFvReservoirType"
              class="q-ma-md"
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @update:model-value="fnSelectFvReservoirType"
            />
          </div>
          <div class="col">
            <!-- F_ReservoirStatus -->
            <q-select
              v-model="form.fvReservoirStatus"
              :label="fmReqLabel('ReservoirStatus')"
              :options="optFvReservoirStatus"
              class="q-ma-md"
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @update:model-value="fnSelectFvReservoirStatus"
            />
          </div>
        </div>

        <div class="row">
          <div class="col">
            <!-- F_FishFarmingType -->
            <q-select
              v-model="form.fvFishFarmingType"
              :label="$t('FishFarmingType')"
              :options="optFvFishFarmingType"
              class="q-ma-md"
              clearable
              dense
              map-options
              option-label="name"
              option-value="id"
              options-dense
              @clear="fnClearFvFishFarmingType"
              @update:model-value="fnSelectFvFishFarmingType"
            />
          </div>
          <div class="col">
            <!-- Coordinate -->
            <q-input
              v-model="form['Coordinate']"
              :label="$t('coordinates')"
              class="q-ma-md"
              dense
            />
          </div>
        </div>

        <div class="row">
          <div class="col">
            <!-- Description -->
            <q-input
              v-model="form['Description']"
              :label="$t('description')"
              class="q-ma-md"
              type="textarea"
            />
          </div>
        </div>
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
import { notifyError, notifySuccess, pack  } from '@/utils/jsutils'
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  mode: String,
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)
const form = reactive({ ...props.data })

const optCls = ref([])
const objKATO = ref([])
const optKATO = ref([])
const optFvReservoirType = ref([])
const optFvReservoirStatus = ref([])
const optFvFishFarmingType = ref([])

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnSelectCls = (val) => {
  form.cls = val.id
}

const fnSelectFvReservoirType = (v) => {
  if (v) {
    form.fvReservoirType = v.id
    form.pvReservoirType = v['pv']
  }
}

const fnSelectFvFishFarmingType = (v) => {
  if (v) {
    form.fvFishFarmingType = v.id
    form.pvFishFarmingType = v['pv']
  }
}

const fnClearFvFishFarmingType = () => {
  form.fvFishFarmingType = null
  form.pvFishFarmingType = null
}

const fnSelectFvReservoirStatus = (v) => {
  if (v) {
    form.fvReservoirStatus = v.id
    form.pvReservoirStatus = v['pv']
  }
}

const validSave = () => {
  let nm = form.name
  nm = nm ? nm.trim() : null
  if (
    !nm ||
    !form.cls ||
    objKATO.value.length === 0 ||
    !form.fvReservoirType ||
    !form.fvReservoirStatus
  )
    return true
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
  if (form.name) {
    form.name = form.name.trim()
  }
  form.objKATO = objKATO.value

  api
    .post('', {
      method: 'data/saveReservoirPropertiesRef',
      params: [form],
    })
    .then((response) => {
      err = false
      emit('ok', response.data.result.records[0])
      notifySuccess(proxy?.$t('success'))
    })
    .catch((error) => {
      err = true
      notifyError(error?.response?.data?.error?.message)
    })
    .finally(() => {
      if (!err) hide()
    })
}

const onCancelClick = () => {
  hide()
}

onMounted(() => {
  api.post('', { method: 'data/loadCls', params: ['Typ_WaterBodies'] }).then((res) => {
    optCls.value = res.data.result.records
  })

  api.post('', { method: 'data/loadKatoForSelect', params: ['Prop_KATO'] }).then((res) => {
    optKATO.value = pack(res.data.result.records, 'id')
  })

  api
    .post('', { method: 'data/loadFvReservoirTypeAsStore', params: ['Prop_ReservoirType'] })
    .then((res) => {
      optFvReservoirType.value = res.data.result.records
    })

  api
    .post('', { method: 'data/loadFvReservoirStatusAsStore', params: ['Prop_ReservoirStatus'] })
    .then((res) => {
      optFvReservoirStatus.value = res.data.result.records
    })

  api
    .post('', { method: 'data/loadFvFishFarmingTypeAsStore', params: ['Prop_FishFarmingType'] })
    .then((res) => {
      optFvFishFarmingType.value = res.data.result.records
      if (props.mode === 'upd' && props.data.objKATO) {
        let arr = props.data.objKATO.split(',') || []
        arr.forEach((item) => objKATO.value.push(item))
      }
    })
})

defineExpose({
  show,
  hide,
})
</script>
