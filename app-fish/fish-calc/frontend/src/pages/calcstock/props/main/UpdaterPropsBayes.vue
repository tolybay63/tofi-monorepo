<template>
  <q-dialog
    ref="dialogRef"
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

        <!-- Prop_CalcStatus -->
        <q-select
          class="q-ma-md"
          v-model="form['fvCalcStatus']"
          dense
          options-dense
          :options="optCalcStatus"
          :label="fmReqLabel('CalcStatus', true)"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectCalcStatus"
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
import {getCurrentInstance, onMounted, reactive, ref} from 'vue'
import {api} from '@/boot/axios'
import {notifySuccess} from '@/utils/jsutils'
import {date} from "quasar";

const props = defineProps({
  data: Object,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)

// Функция конвертации из dd.MM.yyyy (или любого другого) в yyyy-MM-dd для q-input type="date"
const toInputFormat = (val) => {
  if (!val) return ''
  // Пробуем распарсить как dd.MM.yyyy
  let parsed = date.extractDate(val, 'DD.MM.YYYY')
  if (!isNaN(parsed) && val.includes('.')) {
    return date.formatDate(parsed, 'YYYY-MM-DD')
  }
  return val // если уже yyyy-MM-dd
}

// Конвертируем даты при инициализации формы
const form = reactive({
  ...props.data
})

const optCalcStatus = ref([])

const loading = ref(false)

const fmReqLabel = (label, req) => {
  if (req)
    return proxy?.$t(label) + '*'
  else
    return proxy?.$t(label)
}

const fnSelectCalcStatus = (v) => {
  if (v) {
    form.fvCalcStatus = v.id
    form.pvCalcStatus = v["pv"]
  }
}


const validSave = () => {
  return !form["fvCalcStatus"];
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
    const resp = await api.post('', { method: 'data/loadFVasStore', params: ['Prop_CalcStatus'] })
    optCalcStatus.value = resp.data.result['records']
    //
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
})

defineExpose({
  show,
  hide,
})
</script>
