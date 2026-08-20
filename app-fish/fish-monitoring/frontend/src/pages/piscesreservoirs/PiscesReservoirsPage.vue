<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :limits="[70, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 135px); width: 100%"
    >
      <template v-slot:before>
        <q-table
          style="height: calc(100vh - 140px); width: 100%"
          class="my-sticky-header-table"
          color="primary"
          dense
          card-class="bg-amber-1 text-brown"
          row-key="relobj"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          :table-colspan="4"
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
              <q-avatar color="black" text-color="white" icon="tsunami"></q-avatar>
              {{ $t('piscesInReservoirs') }}
            </div>

            <q-space />
            <q-btn
              v-if="hasTarget('mon:rpv:ins')"
              icon="post_add"
              dense
              color="secondary"
              :disable="loading"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('newRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mon:rpv:upd')"
              icon="edit"
              dense
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="editRow(selected[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('editRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mon:rpv:del')"
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

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <FishFecundityPage ref="fishFecundityRef" :name="name"></FishFecundityPage>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useQuasar, extend } from 'quasar'
import { api } from '@/boot/axios'
import { hasTarget, notifyInfo } from '@/utils/jsutils'
import UpdaterPiscesReservoir from '@/pages/piscesreservoirs/UpdaterPiscesReservoir.vue'
import FishFecundityPage from '@/pages/piscesreservoirs/FishFecundityPage.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const splitterModel = ref(100)
const rows = ref([])
const filter = ref('')
const selected = ref([])
const loading = ref(false)
const name = ref('')
const fishFecundityRef = ref(null)

const mapReservoir = ref(new Map())
const mapTypeOfFish = ref(new Map())

const getColumns = () => [
  {
    name: 'reservoir',
    label: proxy?.$t('reservoir') + '*',
    field: 'reservoir',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 20%',
    format: (v) => (mapReservoir.value ? mapReservoir.value.get(v) : null),
  },
  {
    name: 'typeOfFish',
    label: proxy?.$t('typeOfFish') + '*',
    field: 'typeOfFish',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 20%',
    format: (v) => (mapTypeOfFish.value ? mapTypeOfFish.value.get(v) : null),
  },
  {
    name: 'FishSpawPeriod',
    label: proxy?.$t('FishSpawPeriod'),
    field: 'FishSpawPeriod',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
  },
  {
    name: 'FishStartPuberty',
    label: proxy?.$t('FishStartPuberty'),
    field: 'FishStartPuberty',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
  },
  {
    name: 'FishEndPuberty',
    label: proxy?.$t('FishEndPuberty'),
    field: 'FishEndPuberty',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
  },
  {
    name: 'FishSpawFrequency',
    label: proxy?.$t('FishSpawFrequency'),
    field: 'FishSpawFrequency',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
  },
]

const cols = ref(getColumns())

const updateSelected = () => {
  let relobj = 0
  if (selected.value.length > 0) {
    splitterModel.value = 70
    relobj = selected.value[0].relobj
    name.value =
      mapReservoir.value.get(selected.value[0].reservoir) +
      ' - ' +
      mapTypeOfFish.value.get(selected.value[0].typeOfFish)
  } else {
    splitterModel.value = 100
    relobj = 0
    name.value = ''
    fishFecundityRef.value?.clearData()
  }
  fishFecundityRef.value?.loadFishFecundity(relobj)
}

const editRow = (row, mode) => {
  let data = { accessLevel: 1 }
  if (mode === 'upd') {
    data = extend(true, {}, row)
  }

  $q.dialog({
    component: UpdaterPiscesReservoir,
    componentProps: {
      mode: mode,
      data: data,
    },
  }).onOk((r) => {
    if (mode === 'ins') {
      rows.value.push(r)
      selected.value = [r]
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          row[key] = r[key]
        }
      }
      if (r['FishSpawPeriod'] !== null) row['FishSpawPeriod'] = r['FishSpawPeriod']
      if (r['FishStartPuberty'] !== null) row['FishStartPuberty'] = r['FishStartPuberty']
      if (r['FishEndPuberty'] !== null) row['FishEndPuberty'] = r['FishEndPuberty']
      if (r['FishSpawFrequency'] !== null) row['FishSpawFrequency'] = r['FishSpawFrequency']
    }
  })
}

const removeRow = (row) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message:
      proxy?.$t('deleteRecord') +
      '<div style="color: plum">(' +
      mapReservoir.value.get(row.reservoir) +
      ' - ' +
      mapTypeOfFish.value.get(row.typeoffish) +
      ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deletePiscesReservoir',
          params: [row.relobj],
        })
        .then(() => {
          loadData()
          selected.value = []
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const loadData = () => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadPiscesReservoir',
      params: [{ codRelTyp: 'RelTyp_FishReservoir' }],
    })
    .then((response) => {
      rows.value = response.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })
}

const infoSelected = (row) => {
  return (
    ' ' +
    mapReservoir.value.get(row.reservoir) +
    ' (' +
    mapTypeOfFish.value.get(row.typeoffish) +
    ')'
  )
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadReservoir',
      params: ['Typ_WaterBodies'],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        mapReservoir.value.set(it['id'], it['name'])
      })
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/loadTypeOfFish',
      params: ['Typ_Fish'],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        mapTypeOfFish.value.set(it['id'], it['name'])
      })
    })
    .finally(() => {
      loading.value = false
    })

  setTimeout(() => {
    loadData()
  }, 200)
})
</script>

<style lang="sass">
.my-sticky-header-table
  height: calc(100vh - 190px)

  thead tr:first-child th
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
