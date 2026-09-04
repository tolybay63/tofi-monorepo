<template>
  <div class="column no-wrap fit">

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
            <td :data-th="cols[1]?.name">
              {{ item.v_2020 }}
            </td>
            <td :data-th="cols[2]?.name">
              {{ item.v_2021 }}
            </td>

          </tr>
          </tbody>
        </table>
      </div>
    </div>

  </div>
</template>


<script setup>

import {useQuasar} from "quasar";
import {computed, getCurrentInstance, onMounted, ref, watch} from "vue";
import {pack} from "@/utils/jsutils.js";
import {api} from "@/boot/axios.js";
//import {__esModule as PropsPageBayesRef} from "vue-router/unplugin/vite.cjs";

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const rows = ref([])
const cols = ref([])
const loading = ref(false)
const isExpanded = ref(true)
const itemId = ref(null)


const props = defineProps({
  own: Number,
  name: String
})

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName'),
    field: 'name',
    align: 'left',
    style: 'font-size: 1.2em; width: 54%',
  },
  {
    name: 'v_2020',
    label: 'v_2020',
    field: 'v_2020',
    align: 'left',
    style: 'font-size: 1.2em; width: 8%',
  },
  {
    name: 'v_2021',
    label: 'v_2021',
    field: 'v_2021',
    align: 'left',
    style: 'font-size: 1.2em; width: 8%',
  },
]

//const year1 = () => PropsPageBayesRef?.value.getYear1()
//const year2 = () => PropsPageBayesRef?.value.getYear2()

const loadReservoirsMeter = (objId) => {
  if (!objId) return;
  loading.value = true

  api
    .post('', {
      method: 'data/loadReservoirsMeter',
      params: [objId/*, year1, year2*/],
    })
    .then((response) => {
      rows.value = pack(response.data.result['records'], 'id')
    })
    .finally(() => {
      loading.value = false
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

const arrayTreeObj = computed(() => {
  let newObj = []
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value)
  return newObj
})

onMounted(() => {
  cols.value = getColumns()
})

watch(
  () => props.own,
  (newObj) => {
    loadReservoirsMeter(newObj);
  },
  { immediate: true }
)

</script>

<style scoped>
.custom-table {
  display: flex;
  flex-direction: column;
  height: 100% !important;
}

/* Шапка таблицы фиксируется */
:deep(.q-table__top) {
  padding-left: 0;
  padding-right: 0;
  flex-shrink: 0;
}

/* Контейнер сетки растягивается на всю оставшуюся высоту и получает скролл */
:deep(.q-table__grid-content) {
  padding: 0;
  flex: 1 1 auto;
  overflow-y: auto !important;
}
</style>

