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
      <q-bar class="text-white bg-primary">
        <div>{{ $t('chooseReserVoirs') }}</div>
      </q-bar>

      <q-card-section>

        <!-- Reservoir -->
        <TreeSelect
          v-model="reservoirs"
          :label="fmReqLabel('reservoir')"
          :options="optReservoir"
          class="q-ma-md"
          multiple
          node-key="id"
        />

        <!-- StartDate -->
        <q-input
          v-model="dbeg"
          :label="fmReqLabel('dbeg')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md"
          dense
          type="date"
        />

        <!-- EndDate -->
        <q-input
          v-model="dend"
          :label="fmReqLabel('dend')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md"
          dense
          type="date"
        />

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('select')"
          class="q-mt-xl"
          color="primary"
          icon="save"
          @click="onOKClick"
        />
        <q-btn
          :label="$t('cancel')"
          class="q-mt-xl"
          color="primary" icon="cancel"
          @click="onCancelClick"/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {getCurrentInstance, onMounted, ref} from 'vue'
import {api} from '@/boot/axios.js'
import TreeSelect from "@/components/TreeSelect.vue";
import {useRouter} from "vue-router";
import { date } from 'quasar'

const router = useRouter()

const props = defineProps({
  data: Object,
})

//Получаем первый и последний день текущего месяца в формате 'YYYY-MM-DD'
const currentDate = new Date()

const dbeg = ref(date.formatDate(date.startOfDate(currentDate, 'month'), 'YYYY-MM-DD'));
const dend = ref(date.formatDate(date.endOfDate(currentDate, 'month'), 'YYYY-MM-DD'))

const reservoirs = ref([])
const loading = ref(false)

const optReservoir = ref([])

const emit = defineEmits(['ok', 'hide'])
const {proxy} = getCurrentInstance()

const dialogRef = ref(null)

const validSave = () => {
  return !dbeg.value || !dend.value || reservoirs.value.length === 0;
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

const onCancelClick = () => {
  hide()
  router["push"]('/')
}

const onOKClick = () => {
  hide()
  emit('ok', {reservoirs: reservoirs.value, dbeg: dbeg.value, dend: dend.value})
}

defineExpose({
  show,
  hide,
})

onMounted(() => {

  loading.value = true
  api
    .post('', {method: 'data/loadReservoirAll', params: ['Typ_WaterBodies']})
    .then((res) => {
      optReservoir.value = res.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })

})

</script>
