<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    transition-show="slide-up"
    transition-hide="slide-down"
    class="full-height full-width"
  >
    <q-card class="q-dialog-plugin full-height full-width" style="min-width: 100%">
      <q-bar class="text-white bg-primary">
        <div>{{ form.name }}</div>
      </q-bar>

      <q-card-section>
        <div>
          {{form}}
        </div>

        <div class="q-pa-sm">
          <q-table
            color="primary"
            dense
            card-class="bg-amber-1 text-brown"
            row-key="obj"
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading"
            :rows-per-page-options="[0]"
          >
            <template #bottom-row>
            </template>

            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                <q-avatar color="black" text-color="white" icon="houseboat"> </q-avatar>
                AAAAAA
              </div>

            </template>

          </q-table>
        </div>




      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="close"
          :label="$t('close')"
          @click="onOKClick"
          class="q-mt-xl absolute-bottom-right"
        />

      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {ref, reactive, getCurrentInstance, computed, onMounted} from 'vue'
import {hasTarget, notifySuccess} from '@/utils/jsutils'
import { api } from '@/boot/axios'

const props = defineProps({
  data: Object,
})

const loading = ref(false)
const cols = ref([])
const rows = ref([])


const emit = defineEmits(['ok', 'hide'])
const { proxy } = getCurrentInstance()

const dialogRef = ref(null)
const form = reactive({ ...props.data })

const loadAlgo = ()=> {
  loading.value = true
  api
    .post('', {
      method: 'data/loadAlgo',
      params: [form],
    })
    .then((response) => {
      cols.value = response.data.result.cols
      rows.value = response.data.result["store"]["records"]
    })
    .finally(() => {
      loading.value = false
    })
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
}

onMounted(() => {
  loadAlgo()
})

defineExpose({
  show,
  hide,
})
</script>
