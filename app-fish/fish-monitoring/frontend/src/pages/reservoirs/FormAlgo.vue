<template>
  <q-dialog
    ref="dialogRef"
    class="full-height full-width"
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
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
            :disable="!form['dependperiod']"
            :label="$t('date')"
            class="q-mr-lg"
            dense
            stack-label
            style="width: 100px"
            type="date"
            @update:model-value="fnDt"
          />
          <!-- PeriodType -->
          <q-select
            v-model="periodType"
            :disable="true"
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
          <div v-if="form['dependperiod']" class="q-pt-md">
            Зависит от периода
          </div>
          <div v-else class="q-pt-md">
            Не зависит от периода
          </div>


        </div>
        <div class="q-pa-sm">
          <q-table
            :columns="cols"
            :loading="loading"
            :rows="rows"
            :rows-per-page-options="[0]"
            :wrap-cells="true"
            card-class="bg-amber-1 text-brown"
            color="primary"
            dense
            row-key="obj"
            separator="cell"
            table-header-class="text-bold text-white bg-blue-grey-13"
          >

            <template #body-cell="props">
              <q-td :props="props">
                <div v-if="props.col.field.includes('fv')">
                  <div v-if="props.row['id']!==0">
                    {{ props.value }}
                    <q-btn
                      class="absolute-right" color="blue" dense flat icon="more_vert" round size="sm"
                    >
                      <q-menu auto-close>
                        <q-btn
                          class="no-padding no-margin" color="blue" dense flat icon="edit" round
                          size="sm" @click="fnEditCell(item, col.field)"
                        >
                          <q-tooltip>
                            {{ $t("update") }}
                          </q-tooltip>
                        </q-btn>

                        <q-btn
                          :disable="!item['id'+col.field.substring(1)]" class="no-padding no-margin" color="red" dense flat icon="delete" round
                          size="sm"
                          @click="fnDeleteCell(item, col.field)"
                        >
                          <q-tooltip>
                            {{ $t("deletingRecord") }}
                          </q-tooltip>
                        </q-btn>
                      </q-menu>
                    </q-btn>
                  </div>
                  <div v-else>
                    {{ summ(props.row, props.col) }}
                  </div>
                </div>
                <div v-else>
                  {{ props.value }}
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
          :label="$t('save')"
          color="primary"
          icon="save"
          @click="onOKClick"
        />

        <q-btn
          :label="$t('cancel')"
          color="primary"
          icon="cancel"
          @click="onCancelClick"
        />

      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {getCurrentInstance, onMounted, reactive, ref} from 'vue'
import {api} from '@/boot/axios'
import {date} from "quasar";

const props = defineProps({
  data: Object,
})


const loading = ref(false)
const cols = ref([])
const rows = ref([])


const emit = defineEmits(['ok', 'hide'])
const {proxy} = getCurrentInstance()

const dialogRef = ref(null)
const form = reactive({...props.data})

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

const summ = (r, c) => {
  let s = 0
  for (let key in rows.value) {
    if (rows.value[key]["id"] !== 0) {
      let x = rows.value[key][c.field]===undefined ? 0 : rows.value[key][c.field]
      s = s + parseInt(x, 10)
    }
  }
  return s
}

const loadAlgo = () => {
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

const onCancelClick = () => {
  hide()
}

onMounted(() => {
  loading.value = true
  api
    .post('', {method: 'data/loadPeriodType', params: []})
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
