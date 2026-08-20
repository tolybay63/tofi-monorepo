<template>
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
    />
  </div>

  <div class="q-table-container q-table--dense wrap bg-orange-1" style="height: 100%">
    <div class="q-pa-sm-sm bg-orange-1 sticky-header-table">
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
          <tr>
            <th style="font-size: 1.2em; width: 50%">
              {{ cols[0]?.label }}
            </th>
            <th style="font-size: 1.2em; width: 14%">
              {{ cols[1]?.label }}
            </th>
            <th style="font-size: 1.2em; width: 14%">
              {{ cols[2]?.label }}
            </th>
            <th style="font-size: 1.2em; width: 12%">
              {{ cols[3]?.label }}
            </th>
            <th></th>
          </tr>
        </thead>

        <tbody style="background: aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="cols[0]?.name" @click="toggle(item)">
              <span class="q-tree-link q-tree-label" :style="setPadding(item)">
                <q-icon :name="iconName(item)" color="secondary" style="cursor: pointer" />
                {{ item.name }}
              </span>
            </td>
            <td :data-th="cols[2]?.name">
              {{ dtFormat(item.dbeg) }}
            </td>
            <td :data-th="cols[3]?.name">
              {{ dtFormat(item.dend) }}
            </td>
            <td :data-th="cols[1]?.name">
              {{ item.numberval }}
            </td>
            <td :data-th="cols[4]?.name">
              <q-btn
                class="no-padding no-margin"
                color="blue"
                dense
                flat
                icon="edit"
                round
                size="sm"
                @click="fnEdit(item)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t('update') }}
                </q-tooltip>
              </q-btn>

              <q-btn
                :disable="!(item.idval > 0)"
                class="no-padding no-margin"
                color="red"
                dense
                flat
                icon="delete"
                round
                size="sm"
                @click="fnDelete(item)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t('deletingRecord') }}
                </q-tooltip>
              </q-btn>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useQuasar, date } from 'quasar'
import { api, tofi_dbeg, tofi_dend } from '@/boot/axios'
import { notifyError, notifyInfo, pack, today } from '@/utils/jsutils'
import UpdaterReservoirMeter from '@/pages/reservoirs/UpdaterReservoirMeter.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const rows = ref([])
const cols = ref([])
const loading = ref(false)
const isExpanded = ref(true)
const itemId = ref(null)
const obj = ref(0)

const dte = ref(today())
const periodType = ref(11)
const optPeriod = ref([])

const dtFormat = (v) => {
  return v <= tofi_dbeg || v >= tofi_dend ? '...' : date.formatDate(v, 'DD.MM.YYYY')
}

const fnSelectPeriodType = (v) => {
  periodType.value = v.id
  loadReservoirsMeter(obj.value)
}

const fnReqLabel = (label) => {
  return proxy?.$t(label) + '*'
}

const fnDt = (val) => {
  if (val && val.length === 10 && date.formatDate(val)) {
    dte.value = val
    loadReservoirsMeter(obj.value)
  }
}

const fnDelete = (row) => {
  let nm = row.name
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
          method: 'data/deleteReservoirsMeter',
          params: [row.idval],
        })
        .then(() => {
          if (row.level === 0) {
            let index = rows.value.findIndex((rec) => rec.id === row.id)
            if (index > -1) {
              rows.value[index].idval = null
              rows.value[index].numberval = null
              rows.value[index].dbeg = null
              rows.value[index].dend = null
            }
          } else {
            let index = rows.value.findIndex((rec) => rec.id === row.parent)
            if (index > -1 && rows.value[index].children) {
              let child = rows.value[index].children
              let index2 = child.findIndex((rec) => rec.id === row.id)
              if (index2 > -1) {
                child[index2].idval = null
                child[index2].numberval = null
                child[index2].dbeg = null
                child[index2].dend = null
              }
            }
          }
        })
        .catch((error) => {
          notifyError(error.message)
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const updateRowValue = (currentRows, targetRec) => {
  for (let row of currentRows) {
    if (row.id === targetRec.id) {
      row.idval = targetRec.idval
      row.numberval = targetRec.numberval
      row.dbeg = targetRec.dbeg
      row.dend = targetRec.dend
      return true
    }
    if (row.children && Array.isArray(row.children) && row.children.length > 0) {
      if (updateRowValue(row.children, targetRec)) {
        return true
      }
    }
  }
  return false
}

const fnEdit = (row) => {
  let rec = {
    obj: obj.value,
    prop: row.id,
    name: row.name,
    idval: row.idval,
    numberval: row.numberval || '',
    dependperiod: row.dependperiod,
    dt: dte.value,
    pt: periodType.value,
    level: row.level,
  }

  $q.dialog({
    component: UpdaterReservoirMeter,
    componentProps: {
      data: rec,
    },
  })
    .onOk((r) => {
      updateRowValue(rows.value, r)
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const recursive = (currentObj, newObj, level, targetItemId, isExpend) => {
  currentObj.forEach(function (o) {
    if (o.children && o.children.length !== 0) {
      o.level = level
      o.leaf = false
      newObj.push(o)
      if (o.id === targetItemId) {
        o.expend = isExpend
      }
      if (o.expend) {
        recursive(o.children, newObj, o.level + 1, targetItemId, isExpend)
      }
    } else {
      o.level = level
      o.leaf = true
      newObj.push(o)
    }
  })
}

const iconName = (item) => {
  if (item.expend) return 'remove_circle_outline'
  if (item.children && item.children.length > 0) return 'control_point'
  return ''
}

const toggle = (item) => {
  itemId.value = item.id
  item.leaf = false
  if (!item.leaf && item.expend === undefined && item.children !== undefined) {
    if (item.children.length !== 0) {
      recursive(item.children, [], item.level + 1, item.id, true)
    }
  }
  if (item.expend && item.children !== undefined) {
    item.children.forEach(function (o) {
      o.expend = undefined
    })
    item.expend = undefined
    item.leaf = false
    itemId.value = null
  }
}

const setPadding = (item) => {
  return `padding-left: ${item.level * 30}px;`
}

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName'),
    field: 'name',
    align: 'left',
    style: 'font-size: 1.2em; width: 54%',
  },
  {
    name: 'dbeg',
    label: proxy?.$t('fldDbegShort'),
    field: 'dbeg',
    align: 'left',
    style: 'font-size: 1.2em; width: 8%',
  },
  {
    name: 'dend',
    label: proxy?.$t('fldDendShort'),
    field: 'dend',
    align: 'left',
    style: 'font-size: 1.2em; width: 8%',
  },
  {
    name: 'numberval',
    label: proxy?.$t('val'),
    field: 'numberval',
    align: 'center',
    style: 'font-size: 1.2em; width: 16%',
  },
  { name: 'cmd', field: 'cmd', align: 'center', style: 'font-size: 1.2em; width: 14%' },
]

const clearData = () => {
  rows.value = []
}

const loadReservoirsMeter = (targetObj) => {
  loading.value = true
  obj.value = targetObj
  api
    .post('', {
      method: 'data/loadReservoirsMeter',
      params: [targetObj, 0, dte.value, periodType.value],
    })
    .then((response) => {
      rows.value = pack(response.data.result['records'], 'id')
    })
    .finally(() => {
      loading.value = false
    })
}

const arrayTreeObj = computed(() => {
  let newObj = []
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value)
  return newObj
})

onMounted(() => {
  cols.value = getColumns()
  loading.value = true
  api
    .post('', { method: 'data/loadPeriodType', params: [] })
    .then((response) => {
      optPeriod.value = response.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })
})

defineExpose({
  clearData,
  loadReservoirsMeter,
})
</script>

<style scoped>
.sticky-header-table {
  max-height: 95%;
  overflow: auto;
}
.sticky-header-table table {
  border-collapse: separate;
  border-spacing: 0;
}
.sticky-header-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background-color: #607d8b;
}
.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>
