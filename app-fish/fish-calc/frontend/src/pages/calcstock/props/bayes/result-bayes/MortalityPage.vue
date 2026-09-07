<template>

  <div class="column no-wrap fit">

    <div style="height: 64px">

      <q-table
        :columns="cols2"
        :loading="loading2"
        :rows="rows2"
        :rows-per-page-options="[0]" :wrap-cells="true"
        card-class="bg-blue-1 text-brown"
        color="primary"
        dense
        row-key="cod"
        separator="cell"
        table-header-class="text-bold text-white bg-blue-grey-13"
      >
      </q-table>

    </div>

    <div class="column no-wrap fit bg-orange-1 sticky-header-table">
      <q-markup-table bordered separator="cell" wrap-cells>
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr>
          <th v-for="col in cols" :style="col.style">
            {{ col?.label }}
          </th>
        </tr>
        </thead>

        <tbody style="background: aliceblue">
        <tr v-for="(item, index) in arrayTreeObj" :key="index">
          <td :data-th="cols[0]?.name" @click="toggle(item)">
              <span :style="setPadding(item)" class="q-tree-link q-tree-label">
                <q-icon :name="iconName(item)" color="secondary" style="cursor: pointer"/>
                {{ item.name }}
              </span>
          </td>
          <td v-for="(col, i) in cols_" :key="i" :data-th="col.name">
            {{ item[col.field] }}
          </td>

        </tr>
        </tbody>
      </q-markup-table>

    </div>
  </div>
</template>


<script setup>

import {computed, getCurrentInstance, onMounted, ref, watch} from "vue";
import {expandAll, pack} from "@/utils/jsutils.js";
import {api} from "@/boot/axios.js";

const {proxy} = getCurrentInstance()

const props = defineProps({
  own: Number,
  name: String
})

const rows = ref([])
const cols = ref([])
const loading = ref(false)
const isExpanded = ref(true)
const itemId = ref(null)
//
const loading2 = ref(false)
const cols2 = ref([])
const rows2 = ref([])


const loadResultMortalityNotPeriod = (objId) => {
  if (!objId) return;
  loading2.value = true
  api
    .post('', {
      method: 'data/loadResultMortalityNotPeriod',
      params: [objId],
    })
    .then((response) => {
      rows2.value = response.data.result['records']
      console.info("rows2", rows2.value)
    })
    .finally(() => {
      loading2.value = false
    })
}

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName'),
    field: 'name',
    align: 'left',
    headerStyle: "font-size: 1.2em; width: 70%",

  },
  {
    name: 'numberval',
    label: proxy?.$t('val'),
    field: 'numberval',
    align: 'center',
    headerStyle: "font-size: 1.2em; width: 30%",

  }
]

const loadResultMortality = (objId) => {
  if (!objId) return;
  loading.value = true
  api
    .post('', {
      method: 'data/loadResultMortality',
      params: [objId],
    })
    .then((response) => {
      //console.info("rows", response.data.result['records'])
      rows.value = pack(response.data.result['records'], 'id')
    })
    .finally(() => {
      loading.value = false
      expandAll(rows.value)
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

const cols_ = computed(() => {
  return cols.value.slice(1);
})

const arrayTreeObj = computed(() => {
  let newObj = []
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value)
  return newObj
})

onMounted(() => {
  if (!props.own) return
  loading.value = true
  api
    .post('', {
      method: 'data/getCols',
      params: [props.own],
    })
    .then((response) => {
      cols.value = response.data.result
    })
    .finally(() => {
      loading.value = false
    })

  cols2.value = getColumns()
})

watch(
  () => props.own,
  (newObj) => {
    loadResultMortality(newObj)
    loadResultMortalityNotPeriod(newObj)
  },
  {immediate: true}
)

</script>

<style scoped>
.sticky-header-table {
  /* Ограничиваем высоту контейнера, чтобы появилась прокрутка */
  max-height: 100%;
  overflow: auto;
}

.sticky-header-table table {
  /* Убираем схлопывание границ, чтобы sticky работал корректно в некоторых браузерах */
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  /* Делаем заголовок липким */
  position: sticky;
  top: 0;
  /* Z-index нужен, чтобы содержимое body не перекрывало заголовок */
  z-index: 1;
  /* Фон обязателен, иначе заголовок будет прозрачным */
  background-color: #607d8b; /* Аналог bg-blue-grey-13 */
}

/* Опционально: если у таблицы есть границы, фиксируем их отображение */
.sticky-header-table .q-table--bordered {
  border-top: none;
}

</style>

