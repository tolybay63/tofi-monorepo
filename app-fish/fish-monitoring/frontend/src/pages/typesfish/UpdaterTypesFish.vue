<template>
  <q-dialog
    ref="dialogRef"
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
          class="q-mt-md"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- Class -->
        <q-select
          class="q-mt-md"
          v-model="form.cls"
          dense
          options-dense
          :options="optCls"
          :label="fmReqLabel('typeOfFish')"
          option-value="id"
          option-label="name"
          map-options
          :disable="mode === 'upd'"
          @update:model-value="fnSelectCls"
        />

        <!-- FishFamily -->
        <q-select
          class="q-mt-md"
          v-model="form.fvFishFamily"
          dense
          options-dense
          :options="optFishFamily"
          :label="fmReqLabel('FishFamily')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectFishFamily"
        />

        <!-- FishTyp -->
        <q-select
          class="q-mt-md"
          v-model="form.fvFishTyp"
          dense
          options-dense
          :options="optFishTyp"
          :label="fmReqLabel('FishType')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectFishTyp"
        />

        <!-- Description -->
        <q-input
          v-model="form['Description']"
          type="textarea"
          :label="$t('description')"
          class="q-mt-md"
        />
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

const dialogRef = ref(null)
const loading = ref(false)
const form = reactive({ ...props.data })

const optCls = ref([])
const optFishFamily = ref([])
const optFishTyp = ref([])

const fmReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnSelectCls = (v) => {
  if (v) {
    form.cls = v.id
  }
}

const fnSelectFishFamily = (v) => {
  if (v) {
    form.fvFishFamily = v.id
    form.pvFishFamily = v.pv
  }
}

const fnSelectFishTyp = (v) => {
  if (v) {
    form.fvFishTyp = v.id
    form.pvFishTyp = v.pv
  }
}

const validSave = () => {
  if (!form.cls || !form.fvFishFamily || !form.fvFishTyp || !form.name) return true
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
      method: 'data/saveTypesFish',
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

onMounted(async () => {
  loading.value = true
  try {
    const clsRes = await api.post('', { method: 'data/loadCls', params: ['Typ_Fish'] })
    optCls.value = clsRes.data.result['records']

    const familyRes = await api.post('', {
      method: 'data/loadFVasStore',
      params: ['Prop_FishFamily'],
    })
    optFishFamily.value = familyRes.data.result['records']

    const typRes = await api.post('', { method: 'data/loadFVasStore', params: ['Prop_FishTyp'] })
    optFishTyp.value = typRes.data.result['records']
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
