<template>
  <div class="q-pa-sm">

    <q-splitter
      v-model="splitterModel"
      :limits="[40, 100]"
      after-class="overflow-hidden q-ml-sm"
      before-class="overflow-hidden q-mr-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 135px); width: 100%"
    >
      <template v-slot:before>
        <q-table
          style="height: calc(100vh - 140px); width: 100%"
          v-model:selected="selected"
          :columns="cols"
          :filter="filter"
          :loading="loading"
          :rows="rows"
          :table-colspan="4"
          :wrap-cells="true"
          card-class="bg-amber-1 text-brown"
          class="sticky-header-table"
          table-header-class="text-bold text-white bg-blue-grey-13"
          color="primary"
          dense
          row-key="obj"
          selection="single"
          separator="horizontal"
          @update:selected="updateSelected"
          :rows-per-page-options="[25, 0]"
        >
          <template #bottom-row>
            <q-td v-if="selected.length > 0" colspan="100%">
              <span class="text-blue"> {{ $t('selectedRow') }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td v-else-if="rows.length > 0" class="text-bold" colspan="100%">
              {{ $t('infoRow') }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold">
              <q-avatar color="black" icon="set_meal" text-color="white"></q-avatar>
              {{ $t('typesOfFish') }}
            </div>

            <q-space/>
            <q-btn
              v-if="hasTarget('mon:vr:ins')"
              :disable="loading"
              color="secondary"
              dense
              icon="post_add"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t('newRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mon:vr:upd')"
              :disable="loading || selected.length === 0"
              class="q-ml-sm"
              color="secondary"
              dense
              icon="edit"
              @click="editRow(selected[0], 'upd')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t('editRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mon:vr:del')"
              :disable="loading || selected.length === 0"
              class="q-ml-lg"
              color="red"
              dense
              icon="delete"
              @click="removeRow(selected[0])"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t('deletingRecord') }}
              </q-tooltip>
            </q-btn>

            <q-space/>

            <q-input v-model="filter" :label="$t('txt_filter')" color="primary" debounce="300" dense>
              <template v-slot:append>
                <q-icon name="search"/>
              </template>
            </q-input>
          </template>

          <template #loading>
            <q-inner-loading color="secondary" showing></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <TypesFishMeters ref="typesFishMetersRef" :name="name"/>
      </template>

    </q-splitter>
  </div>
</template>

<script setup>
import {getCurrentInstance, onMounted, ref} from 'vue'
import {date, extend, useQuasar} from 'quasar'
import {api} from '@/boot/axios'
import {hasTarget, notifyInfo} from '@/utils/jsutils'
import UpdaterTypesFish from '@/pages/typesfish/UpdaterTypesFish.vue'
import TypesFishMeters from "@/pages/typesfish/TypesFishMeters.vue";

const $q = useQuasar()
const {proxy} = getCurrentInstance()

const splitterModel = ref(100)
const rows = ref([])
const filter = ref('')
const selected = ref([])
const loading = ref(false)
const name = ref('')
const typesFishMetersRef = ref(null)

const FishFamily = ref({})
const FishTyp = ref({})

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName') + '*',
    field: 'name',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 30%',
  },
  {
    name: 'fvFishFamily',
    label: proxy?.$t('FishFamily') + '*',
    field: 'fvFishFamily',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width:20%',
    format: (v) => (FishFamily.value ? FishFamily.value[v] : null),
  },
  {
    name: 'fvFishTyp',
    label: proxy?.$t('FishType') + '*',
    field: 'fvFishTyp',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 20%',
    format: (v) => (FishTyp.value ? FishTyp.value[v] : null),
  },
  {
    name: 'Description',
    label: proxy?.$t('description'),
    field: 'Description',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 30%',
  },
]

const cols = ref(getColumns())

const infoSelected = (row) => {
  return ' ' + row.name
}

const updateSelected = () => {
  let obj = 0
  if (selected.value.length > 0) {
    splitterModel.value = 50
    obj = selected.value[0].obj
    name.value =
      selected.value[0].name +
      ' (' +
      FishFamily.value[selected.value[0].fvFishFamily] + ", "+
      FishTyp.value[selected.value[0].fvFishTyp] +
      ')'
  } else {
    splitterModel.value = 100
    obj = 0
    name.value = ''
    typesFishMetersRef.value?.clearTypesFishData()
  }
  typesFishMetersRef.value?.loadTypesFishMeters(obj)
}

const loadTypesFish = () => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadTypesFish',
      params: [{codTyp: 'Typ_Fish', idObj: 0}],
    })
    .then((response) => {
      rows.value = response.data.result['records']
    })
    .finally(() => {
      loading.value = false
    })
}

const editRow = (row, mode) => {
  let data = {accessLevel: 1}
  if (mode === 'upd') {
    data = extend(true, {}, row)
  }

  $q.dialog({
    component: UpdaterTypesFish,
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
          method: 'data/deleteTypesFish',
          params: [row.obj],
        })
        .then(() => {
          loadTypesFish()
          selected.value = []
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadFVasMap',
      params: ['Prop_FishFamily'],
    })
    .then((response) => {
      FishFamily.value = response.data.result
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/loadFVasMap',
      params: ['Prop_FishTyp'],
    })
    .then((response) => {
      FishTyp.value = response.data.result
    })
    .finally(() => {
      loading.value = false
    })

  loadTypesFish()
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
