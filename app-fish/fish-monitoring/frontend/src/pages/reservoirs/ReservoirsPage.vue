<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :limits="[40, 100]"
      before-class="overflow-hidden"
      after-class="overflow-hidden q-pl-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 135px); width: 100%"
    >
      <template v-slot:before>
        <q-page class="q-pa-sm" style="height: 100px; width: 100%">
          <q-table
            style="height: 98%; width: 100%"
            class="sticky-header-table"
            dense
            card-class="bg-amber-1 text-brown"
            row-key="obj"
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="horizontal"
            :filter="filter"
            :loading="loading"
            selection="single"
            v-model:selected="selected"
            @update:selected="updateSelected"
            :rows-per-page-options="[25, 0]"
          >
            <template #bottom-row>
              <q-td colspan="100%" v-if="selected.length > 0">
                <span class="text-blue"> {{ $t('selectedRow') }}: </span>
                <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
              </q-td>
              <q-td colspan="100%" v-else-if="rows.length > 0" class="text-bold">
                {{ $t('infoRow') }}
              </q-td>
            </template>

            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                <q-avatar color="black" text-color="white" icon="sailing"></q-avatar>
                {{ $t('reservoirs') }}
              </div>

              <q-space />
              <q-btn
                v-if="hasTarget('mon:vod:ins')"
                icon="post_add"
                dense
                color="secondary"
                :disable="loading"
                @click="editRowRefs(null, 'ins')"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('newRecord') }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="hasTarget('mon:vod:upd')"
                icon="edit"
                dense
                color="secondary"
                class="q-ml-sm"
                :disable="loading || selected.length === 0"
                @click="editRowRefs(selected[0], 'upd')"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('editRecord') }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="hasTarget('mon:vod:del')"
                icon="delete"
                dense
                color="red"
                class="q-ml-lg"
                :disable="loading || selected.length === 0"
                @click="removeRow(selected[0])"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('deletingRecord') }}
                </q-tooltip>
              </q-btn>

              <q-btn
                icon="insert_chart_outlined"
                color="secondary"
                class="q-ml-sm"
                dense
                :disable="loading || selected.length === 0"
                @click="showChart(selected[0])"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('charts') }}
                </q-tooltip>
              </q-btn>

              <q-space />

              <q-input
                dense
                debounce="300"
                color="primary"
                v-model="filter"
                :label="$t('txt_filter')"
              >
                <template v-slot:append>
                  <q-icon name="search" />
                </template>
              </q-input>
            </template>

            <template v-slot:loading>
              <q-inner-loading showing color="secondary" />
            </template>
          </q-table>
        </q-page>
      </template>

      <template v-slot:after>
        <ReservoirsMeter ref="reservoirsMeterRef"></ReservoirsMeter>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useQuasar, extend } from 'quasar'
import { hasTarget, notifyError, notifyInfo } from '@/utils/jsutils'
import { api } from '@/boot/axios'
import UpdaterReservoirRefs from '@/pages/reservoirs/UpdaterReservoirRefs.vue'
import ReservoirsMeter from '@/pages/reservoirs/ReservoirsMeter.vue'
import ChartViewPage from '@/components/ChartViewPage.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const splitterModel = ref(100)
const rows = ref([])
const filter = ref('')
const selected = ref([])
const loading = ref(true)

const optFvReservoirType = ref(new Map())
const optFvReservoirStatus = ref(new Map())
const optFvFishFarmingType = ref(new Map())
const reservoirsMeterRef = ref(null)

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName') + '*',
    field: 'name',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width:20%',
  },
  {
    name: 'nameBranch',
    label: proxy?.$t('filial') + '*',
    field: 'nameBranch',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 25%',
  },
  {
    name: 'nameKATO',
    label: proxy?.$t('kato2') + '*',
    field: 'nameKATO',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 25%',
  },
  {
    name: 'fvReservoirType',
    label: proxy?.$t('ReservoirType') + '*',
    field: 'fvReservoirType',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
    format: (v) => (optFvReservoirType.value ? optFvReservoirType.value[v] : null),
  },
  {
    name: 'fvReservoirStatus',
    label: proxy?.$t('ReservoirStatus') + '*',
    field: 'fvReservoirStatus',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
    format: (v) => (optFvReservoirStatus.value ? optFvReservoirStatus.value[v] : null),
  },
]

const cols = ref(getColumns())

const updateSelected = () => {
  let obj = 0
  if (selected.value.length > 0) {
    splitterModel.value = 60
    obj = selected.value[0].obj
  } else {
    splitterModel.value = 100
    obj = 0
    reservoirsMeterRef.value?.clearData()
  }
  reservoirsMeterRef.value?.loadReservoirsMeter(obj)
}

const showChart = (row) => {
  $q.dialog({
    component: ChartViewPage,
    componentProps: {
      owner: row.obj,
      ownerName: row.name,
      meter: 1007,
    },
  })
}

const editRowRefs = (row, mode) => {
  let data = { accessLevel: 1 }
  if (mode === 'upd') {
    data = extend(true, {}, row)
  }

  $q.dialog({
    component: UpdaterReservoirRefs,
    componentProps: {
      mode: mode,
      data: data,
    },
  }).onOk((r) => {
    if (mode === 'ins') {
      rows.value.push(r)
      selected.value = [r]
    } else {
      Object.keys(row).forEach((key) => {
        row[key] = null
      })
      for (let key in r) {
        row[key] = r[key]
      }
    }
    updateSelected()
  })
}

const removeRow = (row) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message:
      proxy?.$t('deleteRecord') +
      '<div style="color: plum">(' +
      row.name +
      ' - ' +
      row.nameBranch +
      ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deleteOwnerWithProperties',
          params: [row.obj, 1],
        })
        .then(() => {
          loadReservoirs()
          selected.value = []
          updateSelected()
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const infoSelected = (row) => {
  return row ? ' ' + row.name + ' (' + row.nameBranch + ')' : ''
}

const loadReservoirs = () => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadReservoirs',
      params: [{ codTyp: 'Typ_WaterBodies', idObj: 0 }],
    })
    .then((response) => {
      let obj = 0
      if (selected.value.length > 0) {
        obj = selected.value[0].obj
      }
      rows.value = response.data.result['records']
      if (obj > 0) {
        selected.value = []
        let sel = rows.value.filter((item) => item['obj'] === obj)
        if (sel.length > 0) {
          selected.value.push(sel[0])
          updateSelected()
        }
      }
    })
    .catch((error) => {
      const errMsg = error.response?.data?.error?.message || ''
      if (errMsg.includes('@')) {
        let msgs = errMsg.split('@')
        let m1 = proxy?.$t(`${msgs[0]}`) || msgs[0]
        let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
        notifyError(m1 + m2)
      } else if (errMsg) {
        notifyError(proxy?.$t(errMsg) || errMsg)
      }
    })
    .finally(() => {
      loading.value = false
    })
}

onMounted(() => {
  api.post('', { method: 'data/loadPeriodType', params: [] }).catch(() => {})

  api
    .post('', { method: 'data/loadFvReservoirTypeAsMap', params: ['Prop_ReservoirType'] })
    .then((res) => {
      optFvReservoirType.value = res.data.result
    })

  api
    .post('', { method: 'data/loadFvReservoirStatusAsMap', params: ['Prop_ReservoirStatus'] })
    .then((res) => {
      optFvReservoirStatus.value = res.data.result
    })

  api
    .post('', { method: 'data/loadFvFishFarmingTypeAsMap', params: ['Prop_FishFarmingType'] })
    .then((res) => {
      optFvFishFarmingType.value = res.data.result
      loadReservoirs()
    })
})
</script>

<style lang="sass">
.sticky-header-table
  height: calc(100vh - 140px)
  background-color: #607d8b

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    top: 48px

  tbody
    scroll-margin-top: 48px
</style>
