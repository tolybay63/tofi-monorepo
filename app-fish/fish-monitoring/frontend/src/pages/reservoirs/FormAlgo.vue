<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    transition-show="slide-up"
    transition-hide="slide-down"
    class="full-height full-width"
  >
    <q-card class="q-dialog-plugin full-width" style="min-width: 100%">
      <q-bar class="text-white bg-primary">
        <div>{{ form.name }}</div>
      </q-bar>

      <q-card-section>
        <div class="q-pa-sm row bg-amber-1">
          <!-- Date -->
          <q-input
            v-model="dte"
            :label="$t('date')"
            class="q-mr-lg"
            dense
            stack-label
            style="width: 100px"
            type="date"
            @update:model-value="fnDt"
            :disable="!form['dependperiod']"
          />
          <!-- PeriodType -->
          <q-select
            v-model="periodType"
            :label="fnReqLabel('periodType')"
            :options="optPeriod"
            class="q-ml-lg"
            dense
            map-options
            option-label="text"
            option-value="id"
            options-dense
            style="width: 100px"
            @update:model-value="fnSelectPeriodType"
            :disable="true"
          />
          <q-btn
            class="q-pl-xl no-padding no-margin"
            color="blue" dense flat icon="refresh"
            round @click="loadAlgo"
          >
            <q-tooltip>
              {{ $t('refresh') }}
            </q-tooltip>
          </q-btn>
          <q-space/>
          <div class="q-pt-md" v-if="form['dependperiod']">
            Зависит от периода
          </div>
          <div class="q-pt-md" v-else>
            Не зависит от периода
          </div>


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

            <template #body-cell="props">
              <q-td :props="props">
                <div v-if="props.col.field.includes('fv')">
                  <q-btn
                    color="blue" round size="sm" flat dense icon="more_vert" class="absolute-right"
                  >
                    <q-menu auto-close>
                      <q-btn
                        round size="sm" icon="edit" color="blue" flat dense
                        @click="fnEditCell(item, col.field)" class="no-padding no-margin"
                      >
                        <q-tooltip>
                          {{ $t("update") }}
                        </q-tooltip>
                      </q-btn>

                      <q-btn
                        round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                        @click="fnDeleteCell(item, col.field)"
                        :disable="!item['id'+col.field.substring(1)]"
                      >
                        <q-tooltip>
                          {{ $t("deletingRecord") }}
                        </q-tooltip>
                      </q-btn>
                    </q-menu>
                  </q-btn>
                  {{props.value}}
                </div>
                <div v-else>
                  {{props.value}}
                </div>
              </q-td>
            </template>




            <template #bottom-row>
            </template>

            <template v-slot:top>
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
import { api } from '@/boot/axios'
import {date} from "quasar";

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

let dte = form["dte"]
const periodType = form["periodType"]
const optPeriod = ref([])

const fnSelectPeriodType = (v) => {
  periodType.value = v.id
  //loadAlgo()
}

const fnReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnDt = (val) => {
  if (val && val.length === 10 && date.formatDate(val)) {
    dte = val
    form.dte = val
  }
}



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
  loading.value = true
  api
    .post('', { method: 'data/loadPeriodType', params: [] })
    .then((response) => {
      optPeriod.value = response.data.result['records']
    })
    .finally(() => {
      loadAlgo()
      loading.value = false
    })


})

defineExpose({
  show,
  hide,
})
</script>
