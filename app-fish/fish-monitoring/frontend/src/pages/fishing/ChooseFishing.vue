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
        <div>{{ $t('chooseReserVoirs') }}</div>
      </q-bar>

      <q-card-section>

        <!-- Reservoir -->
        <TreeSelect
          v-model="reservoirs"
          :options="optReservoir"
          class="q-ma-md"
          :label="fmReqLabel('reservoir')"
          multiple
          node-key="id"
        />

        <!-- StartDate -->
        <q-input
          v-model="dbeg"
          :label="fmReqLabel('dbeg')"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md"
          dense
        />

        <!-- EndDate -->
        <q-input
          v-model="dend"
          :label="fmReqLabel('dend')"
          type="date"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md"
          dense
        />

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('select')"
          @click="onOKClick"
          :disable="validSave()"
          class="q-mt-xl"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {ref, reactive, getCurrentInstance, onMounted} from 'vue'
import { notifySuccess } from '@/utils/jsutils'
import { api } from '@/boot/axios.js'
import TreeSelect from "@/components/TreeSelect.vue";

const props = defineProps({
  data: Object,
})

const reservoirs = ref([])
const dbeg = ref(null)
const dend = ref(null)
const loading = ref(false)

const optReservoir = ref([])

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)

const validSave = () => {
  if (!dbeg.value || !dend.value || reservoirs.value.length === 0) return true
  else return false
}

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
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
  hide()
  emit('ok', {reservoirs: reservoirs.value, dbeg: dbeg.value, dend: dend.value})
}

const onCancelClick = () => {
  hide()
}

defineExpose({
  show,
  hide,
})

onMounted(()=> {

  loading.value = true
  api
    .post('', { method: 'data/loadReservoirAll', params: ['Typ_WaterBodies'] })
    .then((res) => {
      optReservoir.value = res.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })

})

</script>
