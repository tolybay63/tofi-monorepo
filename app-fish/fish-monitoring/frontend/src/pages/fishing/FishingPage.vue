<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :limits="[60, 100]"
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
          row-key="obj"
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
              <q-avatar color="black" text-color="white" icon="location_on"></q-avatar>
              {{ $t('fishing') }}
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
        <FishingMeters ref="fishingMetersRef" :name="name"></FishingMeters>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useQuasar, date, extend } from 'quasar'
import { api, tofi_dbeg, tofi_dend } from '@/boot/axios'
import { hasTarget, notifyInfo, today } from '@/utils/jsutils'
import UpdaterFishingRefs from '@/pages/fishing/UpdaterFishingRefs.vue'
import FishingMeters from '@/pages/fishing/FishingMeters.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const splitterModel = ref(100)
const rows = ref([])
const filter = ref('')
const selected = ref([])
const loading = ref(false)
const name = ref('')
const fishingMetersRef = ref(null)

const mapFishGear = ref(new Map())
const mapFishManager = ref(new Map())

const getColumns = () => [
  {
    name: 'nameCls',
    label: proxy?.$t('fishingType') + '*',
    field: 'nameCls',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 10%',
  },
  {
    name: 'StartDate',
    label: proxy?.$t('StartDate') + '*',
    field: 'StartDate',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 10%',
    format: (val) =>
      val <= tofi_dbeg || val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY'),
  },
  {
    name: 'nameFishLocation',
    label: proxy?.$t('FishLocation') + '*',
    field: 'nameFishLocation',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 10%',
  },
  {
    name: 'nameReservoirShore',
    label: proxy?.$t('reservoir') + '*',
    field: 'nameReservoirShore',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 10%',
  },
  {
    name: 'AreaOfTon',
    label: proxy?.$t('AreaOfTon') + '*',
    field: 'AreaOfTon',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 10%',
  },
  {
    name: 'objFishGear',
    label: proxy?.$t('FishGear') + '*',
    field: 'objFishGear',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 10%',
    format: (v) => (mapFishGear.value ? mapFishGear.value.get(v) : null),
  },
  {
    name: 'objFishManager',
    label: proxy?.$t('FishManager') + '*',
    field: 'objFishManager',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 15%',
    format: (v) => (mapFishManager.value ? mapFishManager.value.get(v) : null),
  },
  {
    name: 'nameFishParticipants',
    label: proxy?.$t('FishParticipants') + '*',
    field: 'nameFishParticipants',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 25%',
  },
]

const cols = ref(getColumns())

const updateSelected = () => {
  let obj = 0
  if (selected.value.length > 0) {
    splitterModel.value = 70
    obj = selected.value[0].obj
    name.value =
      selected.value[0].nameFishLocation +
      ' (' +
      selected.value[0].nameCls +
      ' - ' +
      date.formatDate(selected.value[0].StartDate, 'DD.MM.YYYY') +
      ')'
  } else {
    splitterModel.value = 100
    obj = 0
    name.value = ''
    fishingMetersRef.value?.clearFishingData()
  }
  fishingMetersRef.value?.loadFishingMeters(obj)
}

const editRow = (row, mode) => {
  let data = { accessLevel: 1, StartDate: today() }
  if (mode === 'upd') {
    extend(true, data, row)
  }

  $q.dialog({
    component: UpdaterFishingRefs,
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
    }
  })
}

const removeRow = (row) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message:
      proxy?.$t('deleteRecord') +
      '<div style="color: plum">(' +
      row.nameFishLocation +
      ' (' +
      row.nameCls +
      ' - ' +
      date.formatDate(row.StartDate, 'DD.MM.YYYY') +
      ')' +
      ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deleteFishing',
          params: [row.obj],
        })
        .then(() => {
          loadData()
          selected.value = []
          updateSelected()
        })
        .catch((error) => {
          console.log(error.message)
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
      method: 'data/loadFishing',
      params: [0],
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
    row.nameFishLocation +
    ' (' +
    row.nameCls +
    ' - ' +
    date.formatDate(row.StartDate, 'DD.MM.YYYY') +
    ')'
  )
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadFishGearForSelect',
      params: ['Prop_FishGear'],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        mapFishGear.value.set(it['id'], it['name'])
      })
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/loadFishManagerForSelect',
      params: ['Prop_FishManager'],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        mapFishManager.value.set(it['id'], it['name'])
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
