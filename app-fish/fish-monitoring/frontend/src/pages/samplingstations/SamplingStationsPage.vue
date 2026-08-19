<template>
  <div class="q-pa-sm">
    <q-table
      style="height: calc(100vh - 140px); width: 100%"
      class="sticky-header-table"
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
          <q-avatar color="black" text-color="white" icon="houseboat"> </q-avatar>
          {{ $t('samplingStations') }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mon:st:ins')"
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
          v-if="hasTarget('mon:st:upd')"
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
          v-if="hasTarget('mon:st:del')"
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

        <q-input dense debounce="300" color="primary" v-model="filter" :label="$t('txt_filter')">
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useQuasar, extend } from 'quasar'
import { api } from 'boot/axios'
import { hasTarget, notifyInfo } from '../../utils/jsutils'
import UpdaterSamplingStation from './UpdaterSamplingStation.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const rows = ref([])
const filter = ref('')
const selected = ref([])
const loading = ref(false)

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName') + '*',
    field: 'name',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 25%',
  },
  {
    name: 'Coordinate',
    label: proxy?.$t('coordinates') + '*',
    field: 'Coordinate',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 20%',
  },
  {
    name: 'nameReservoirShore',
    label: proxy?.$t('reservoir') + '*',
    field: 'nameReservoirShore',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 25%',
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
    name: 'Description',
    label: proxy?.$t('description'),
    field: 'Description',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 25%',
  },
]

const cols = ref(getColumns())

const infoSelected = (row) => {
  return ' ' + row.name
}

const loadSamplingStations = () => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadSamplingStations',
      params: [{ codCls: 'Cls_Station', idObj: 0 }],
    })
    .then((response) => {
      rows.value = response.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })
}

const editRow = (row, mode) => {
  let data = { accessLevel: 1 }
  if (mode === 'upd') {
    data = extend(true, {}, row)
  }

  $q.dialog({
    component: UpdaterSamplingStation,
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
        row[key] = r[key]
      }
    }
  })
}

const removeRow = (row) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '<div style="color: plum">(' + row.name + ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deleteSamplingStation',
          params: [row.obj],
        })
        .then(() => {
          loadSamplingStations()
          selected.value = []
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

onMounted(() => {
  loadSamplingStations()
})
</script>

<style scoped>
.sticky-header-table {
  max-height: 100%;
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
