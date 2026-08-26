<template>
  <q-dialog
    ref="dialog"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t('newRecord') }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t('editRecord') }}</div>
      </q-bar>

      <q-card-section>
        <!-- UserSecondName -->
        <q-input
          v-model="form.UserSecondName"
          autofocus
          dense
          :label="fnLabel('UserSecondName', true)"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- UserFirstName-->
        <q-input
          v-model="form.UserFirstName"
          :label="fnLabel('UserFirstName', true)"
          dense
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- UserMiddleName-->
        <q-input v-model="form.UserMiddleName" :label="fnLabel('UserMiddleName', false)" dense />
        <!-- UserSex -->
        <q-select
          v-model="form.fvUserSex"
          :label="fnLabel('UserSex', true)"
          :options="optUserSex"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterUserSex"
          @update:model-value="fnSelectUserSex"
        />
        <!-- UserPosition -->
        <q-select
          v-model="form.fvUserPosition"
          :label="fnLabel('UserPosition', true)"
          :options="optUserPosition"
          map-options
          option-label="name"
          option-value="id"
          use-input
          @filter="filterUserPosition"
          @update:model-value="fnSelectUserPosition"
        />

        <!-- UserOrg (интегрирован наш універсальный TreeSelect) -->
        <TreeSelect
          v-model="form.objUserOrg"
          :options="optUserOrg"
          :label="fnLabel('UserOrg', true)"
          @select="fnSelectUserOrg"
        />

        <!-- UserDateBirth -->
        <q-input
          v-model="form.UserDateBirth"
          :label="fnLabel('UserDateBirth', false)"
          stack-label
          type="date"
          @update:model-value="fnSelectUserDateBirth"
        />
        <!-- UserEmail -->
        <q-input
          v-model="form.UserEmail"
          type="email"
          :label="fnLabel('UserEmail', false)"
          :rules="[(val) => emailTest(val) || 'Ошибка формата']"
        />

        <!-- UserPhone -->
        <q-input
          v-model="form.UserPhone"
          unmasked-value
          :label="fnLabel('UserPhone', false)"
          prefix="+7"
          mask="### ### ####"
          fill-mask="_"
          bottom-slots
          @update:model-value="isValidPhone"
        >
          <template v-slot:error> Please use 10 characters. </template>
        </q-input>
        <!-- UserId -->
        <q-select
          v-model="form.UserId"
          :label="fnLabel('UserId', false)"
          :options="optUserId"
          map-options
          option-label="name"
          option-value="id"
          use-input
          clearable
          @filter="filterUserId"
          @update:model-value="fnSelectUserId"
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
import { date } from 'quasar'
import TreeSelect from '@/components/TreeSelect.vue'
import { api } from '@/boot/axios'
import { notifySuccess, pack } from '@/utils/jsutils'

const props = defineProps({
  data: Object,
  mode: String,
})

const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialog = ref(null)
const loading = ref(false)
const form = reactive({ ...props.data })

const optUserSex = ref([])
const optUserSexOrg = ref([])
const optUserPosition = ref([])
const optUserPositionOrg = ref([])
const optUserId = ref([])
const optUserIdOrg = ref([])
const optUserOrg = ref([])

const fnLabel = (txt, req) => {
  return req ? proxy?.$t(txt) + '*' : proxy?.$t(txt)
}

const fnSelectUserDateBirth = (v) => {
  if (v && v.length === 10 && date.formatDate(v)) {
    form.UserDateBirth = v
  }
}

const fnSelectUserSex = (v) => {
  if (v) {
    form.fvUserSex = v.id
    form.pvUserSex = v.pv
  }
}

const filterUserSex = (val, update) => {
  if (val === null || val === '') {
    update(() => {
      optUserSex.value = optUserSexOrg.value
    })
    return
  }
  update(() => {
    if (optUserSexOrg.value.length < 2) return
    const needle = val.toLowerCase()
    optUserSex.value = optUserSexOrg.value.filter((v) => {
      return v.name?.toLowerCase().indexOf(needle) > -1
    })
  })
}

const fnSelectUserPosition = (v) => {
  if (v) {
    form.fvUserPosition = v.id
    form.pvUserPosition = v.pv
  }
}

const filterUserPosition = (val, update) => {
  if (val === null || val === '') {
    update(() => {
      optUserPosition.value = optUserPositionOrg.value
    })
    return
  }
  update(() => {
    if (optUserPositionOrg.value.length < 2) return
    const needle = val.toLowerCase()
    optUserPosition.value = optUserPositionOrg.value.filter((v) => {
      return v.name?.toLowerCase().indexOf(needle) > -1
    })
  })
}

const fnSelectUserOrg = (v) => {
  if (v) {
    form.objUserOrg = v.id
    form.pvUserOrg = v.pv
  } else {
    form.objUserOrg = null
    form.pvUserOrg = null
  }
}

const fnSelectUserId = (v) => {
  form.UserId = v ? v.id : null
}

const filterUserId = (val, update) => {
  if (val === null || val === '') {
    update(() => {
      optUserId.value = optUserIdOrg.value
    })
    return
  }
  update(() => {
    if (optUserIdOrg.value.length < 2) return
    const needle = val.toLowerCase()
    optUserId.value = optUserIdOrg.value.filter((v) => {
      return v.name?.toLowerCase().indexOf(needle) > -1
    })
  })
}

const emailTest = (v) => {
  if (!v) return true
  return /^(?=[a-zA-Z0-9@._%+-]{6,254}$)[a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\.){1,8}[a-zA-Z]{2,63}$/.test(
    v,
  )
}

const isValidPhone = () => {
  return form.UserPhone?.length === 10
}

const validSave = () => {
  return (
    !form.UserSecondName ||
    !form.UserFirstName ||
    !form.fvUserSex ||
    !form.fvUserPosition ||
    !form.objUserOrg
  )
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
  loading.value = true
  let err = false
  api
    .post('', {
      method: 'data/savePersonnel',
      params: [props.mode, form],
    })
    .then((response) => {
      emit('ok', response.data.result.records[0])
      notifySuccess(proxy?.$t('success'))
    })
    .catch((error) => {
      err = true
      console.error(error.response?.data?.error?.message || error.message)
    })
    .finally(() => {
      loading.value = false
      if (!err) hide()
    })
}

const onCancelClick = () => {
  loading.value = false
  hide()
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/selectFV',
      params: ['Prop_UserSex'],
    })
    .then((response) => {
      optUserSex.value = response.data.result.records
      optUserSexOrg.value = response.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/selectFV',
      params: ['Prop_UserPosition'],
    })
    .then((response) => {
      optUserPosition.value = response.data.result.records
      optUserPositionOrg.value = response.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/selectObj',
      params: ['Prop_UserOrg'],
    })
    .then((response) => {
      const rawRecords = response.data?.result?.records || []
      optUserOrg.value = pack(rawRecords, 'id')
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/selectUser',
      params: [],
    })
    .then((response) => {
      optUserId.value = response.data.result.records
      optUserIdOrg.value = response.data.result.records
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
