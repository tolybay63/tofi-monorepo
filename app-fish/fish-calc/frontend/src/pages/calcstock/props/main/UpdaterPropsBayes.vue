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
        <!-- CalcCreatDate -->
        <q-input
          autofocus
          v-model="form['CalcCreatDate']"
          :label="fmReqLabel('CalcCreatDate', true)"
          type="date" :disable="true"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
          class="q-ma-md" dense
        />

        <!-- CalcLastDate -->
        <q-input
          v-model="form['CalcLastDate']"
          :label="fmReqLabel('CalcLastDate', false)"
          type="date" :disable="true"
          :rules="[(val) => checkDate(val) || 'D1 > D2']"
          clearable
          class="q-ma-md" dense
        />
        <!-- CalcStartYear -->
        <q-input
          v-model="form['CalcStartYear']"
          :model-value="form['CalcStartYear']"
          :label="fmReqLabel('CalcStartYear', true)"
          :rules="[(val) => (!!val && val.trim().length===4) || $t('req')]"
          class="q-ma-md" dense mask="####"
        />

        <!-- CalcEndYear -->
        <q-input
          v-model="form['CalcEndYear']"
          :label="fmReqLabel('CalcEndYear', true)"
          :rules="[
            (val) => (!!val && val.trim().length===4) || $t('req'),
            () => checkYear() || 'Y1 > Y2'
            ]"
          class="q-ma-md" dense mask="####"
        />

        <!-- Prop_CalcFishSpec -->
        <q-select
          class="q-ma-md"
          v-model="form['fvCalcFishSpec']"
          dense
          options-dense
          :options="optCalcFishSpec"
          :label="fmReqLabel('CalcFishSpec', true)"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectCalcFishSpec"
        />

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

        <!-- Prop_ReservoirShore -->
        <q-select
          v-model="form.objReservoirShore"
          :label="fmReqLabel('reservoir', true)"
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
          :label="fmReqLabel('CalcUser', false)"
          :options="optCalcUser"
          class="q-ma-md"
          dense map-options
          option-label="name"
          option-value="id"
          clearable
          :disable="true"
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
import {useUserStore} from "@/stores/user-store.js";
import {storeToRefs} from "pinia";
import {date} from "quasar";
import {useI18n} from "vue-i18n";

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
  ...props.data,
  CalcCreatDate: toInputFormat(props.data?.CalcCreatDate),
  CalcLastDate: toInputFormat(props.data?.CalcLastDate),
})

//const form = reactive({ ...props.data })

const optCalcFishSpec = ref([])
const optCalcStatus = ref([])

const optReservoir = ref([])
const optReservoirOrg = ref([])
const optCalcUser = ref([])


const loading = ref(false)

const fmReqLabel = (label, req) => {
  if (req)
    return proxy?.$t(label) + '*'
  else
    return proxy?.$t(label)
}

const checkDate = (val) => {
  if (!val)
    return true
  else
    return form['CalcCreatDate'] <= val
}

const checkYear = () => {
  if (!form['CalcStartYear'] || !form['CalcEndYear']) return true
  return form['CalcStartYear'] <= form['CalcEndYear']
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
  let valid = !form["CalcCreatDate"] || !form["fvCalcFishSpec"] || !form["fvCalcStatus"] || !form["objReservoirShore"];
  if (valid) return true
  if (!form["CalcStartYear"] || (form["CalcStartYear"] && form["CalcStartYear"].length !== 4)) return true;
  return !form["CalcEndYear"] || (form["CalcEndYear"] && form["CalcEndYear"].length !== 4);
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
  const store = useUserStore()
  const { getUserName, getUserId } = storeToRefs(store)

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
    //const resp4 = await api.post('', { method: 'data/loadCalcUser', params: ['Prop_CalcUser'] })
    //optCalcUser.value = resp4.data.result['records']
    let usr = {id: getUserId, name: getUserName, pv: 1102};
    optCalcUser.value.push(usr)
    //
    console.info("optCalcUser", optCalcUser.value)

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
