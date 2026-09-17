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
            :disable="!form['dependperiod']"
            class="q-pl-xl no-padding no-margin"
            color="blue" dense flat icon="refresh"
            round @click="loadAlgo"
          >
            <q-tooltip>
              {{ $t('refresh') }}
            </q-tooltip>
          </q-btn>

          <q-space/>

          <q-btn
            :label="$t('save')" color="primary" dense
            icon="save" @click="fnSave"
            :disable="!bSave"
            :class="{ 'btn-blink': bSave }"
          />

          <q-btn
            :disable="bSave"
            :label="$t('goAlgo')" color="primary" dense
            icon="settings" @click="fnCalc"
            class="q-ml-lg"
          />

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
                      v-if="props.row['p'+props.col.field.substring(2)]!==0"
                    >
                      <q-menu auto-close>
                        <q-btn
                          class="no-padding no-margin" color="blue" dense flat icon="edit" round
                          size="sm" @click="fnEditCell(props.row, props.col)"
                        >
                          <q-tooltip>
                            {{ $t("update") }}
                          </q-tooltip>
                        </q-btn>

                        <q-btn
                          :disable="!props.row['v'+props.col.field.substring(2)]" class="no-padding no-margin" color="red" dense
                          flat icon="delete" round
                          size="sm"
                          @click="fnDeleteCell(props.row, props.col)"
                        >
                          <q-tooltip>
                            {{ $t("deletingRecord") }}
                          </q-tooltip>
                        </q-btn>
                      </q-menu>
                    </q-btn>
                  </div>
                  <div v-else>
                    {{ summ(props.col) }}
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
      <q-card-actions align="right" class="q-pr-lg">
        <q-btn

          :label="$t('close')"
          color="primary"
          icon="close"
          @click="onOKClick"
        />

<!--        <q-btn
          :label="$t('cancel')"
          color="primary"
          icon="cancel"
          @click="onCancelClick"
        />-->

      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {getCurrentInstance, onMounted, reactive, ref} from 'vue'
import {api} from '@/boot/axios'
import {date, useQuasar} from "quasar";
import {notifyError, notifyInfo, notifySuccess} from "@/utils/jsutils.js";
import UpdaterFormAlgo from "@/pages/reservoirs/UpdaterFormAlgo.vue";

const props = defineProps({
  data: Object,
})
const $q = useQuasar()

const loading = ref(false)
const cols = ref([])
const rows = ref([])
const bSave = ref(true)

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

const fnEditCell = (row, col)=> {
  console.info(row, col)

  const data = {
    numberval: row[col.field],
    obj: form["own"],
    prop: row["p"+col.field.substring(2)],
    dependperiod: form["dependperiod"],
    dte: form["dte"],
    periodType: form["periodType"],
    title: col.label + " ("+row["name"]+")",
  }

  $q.dialog({
    component: UpdaterFormAlgo,
    componentProps: {
      data: data,
    },
  })
    .onOk((r) => {
      console.info("onOk", r)
      row[col.field] = r.numberval
      row["v"+col.field.substring(2)] = r.idval
      bSave.value=true
    })

}

const fnDeleteCell = (row, col)=> {
  console.info(row.name, col.label, row["v"+col.field.substring(2)])

  let nm = col.label + " ("+row.name+")"
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '</br>(' + nm + ')',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deleteAlgo',
          params: [row["v"+col.field.substring(2)]],
        })
        .then(() => {
          row["v"+col.field.substring(2)] = null
          row[col.field] = null
          bSave.value=true
        })
        .catch((error) => {
          notifyError(error.message)
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const fnSave = async () => {
  console.info("fnSave", rows.value[0])
  let params = []
  for (let key in rows.value[0]) {
    if (key.includes("fv")) {
      if (rows.value[0][key]) {
        let data = {
          obj: form["own"],
          dependperiod: form["dependperiod"],
          dte: form["dte"],
          periodType: form["periodType"],
          numberval: rows.value[0][key],
          prop: rows.value[0]["p"+key.substring(2)]
        }
        //
        params.push(data)
      }
    }
  }
  if (params.length>0) {
    console.info("params", params)

    const resp = await api
      .post('', {
        method: 'data/saveAlgo1Lev',
        params: [params],
      })
      .then(() => {
        bSave.value = !bSave.value
        notifySuccess("Saved!")
      })
      .catch((error) => {
        console.error(error.message)
      })
      .finally(() => {
      })
  }

}

const fnCalc = () => {
  setTimeout(()=> {
    bSave.value = !bSave.value
    notifyInfo("Calculation...")
  }, 2000)

}

const reqSave = (c) => {

}

const summ = (c) => {
  //
  let s = 0
  for (let key in rows.value) {
    if (rows.value[key]["id"] !== 0) {
      let x = !rows.value[key][c.field] || rows.value[key][c.field] === undefined
        ? 0 : rows.value[key][c.field]
      s = s + parseInt(x, 10)
    }
  }
  //
  rows.value[0][c.field] = s===0? null : s
  return rows.value[0][c.field];
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
  emit('ok', {res: true})
  hide()
}

/*const onCancelClick = () => {
  hide()
}*/

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


<style scoped>
@keyframes blink {
  0% { opacity: 1; }
  50% { opacity: 0.3; }
  100% { opacity: 1; }
}

.btn-blink {
  animation: blink 1.2s infinite ease-in-out;
}
</style>
