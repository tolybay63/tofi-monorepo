<template>
  <div class="column no-wrap fit">

    <div class="bg-orange-1" style="height: 100%">

      <table class="q-table q-table--cell-separator q-table--bordered wrap sticky-header-table">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr>
          <th v-for="col in cols" :style="col.style">
            {{ col?.label }}
          </th>
        </tr>
        </thead>

        <tbody style="background: aliceblue" >
        <tr v-for="(item, index) in arrayTreeObj" :key="index">
          <td :data-th="cols[0]?.name" @click="toggle(item)">
              <span :style="setPadding(item)" class="q-tree-link q-tree-label">
                <q-icon :name="iconName(item)" color="secondary" style="cursor: pointer"/>
                {{ item.name }}
              </span>
          </td>
          <td v-for="(col, i) in cols_" :key="i" :data-th="col.name" class="q-table--bordered">
            <q-btn
              color="primary" round size="sm" flat dense icon="more_vert" class="absolute-right"
            >
              <q-menu auto-close>
                <q-btn
                  round size="sm" icon="edit" color="blue" flat dense
                  @click="fnEditCell(item, col.field)" class="no-padding no-margin"
                >
                  <q-tooltip>
                    {{ $t("update") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                  @click="fnDeleteCell(item, col.field)"

                >
                  <q-tooltip>
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>
              </q-menu>
            </q-btn>

            {{ item[col.field] }}
          </td>

        </tr>
        </tbody>
      </table>

    </div>

  </div>
</template>


<script setup>

import {useQuasar} from "quasar";
import {computed, getCurrentInstance, onMounted, ref, watch} from "vue";
import {pack} from "@/utils/jsutils.js";
import {api} from "@/boot/axios.js";

const $q = useQuasar()
const {proxy} = getCurrentInstance()

const rows = ref([])
const cols = ref([])
const loading = ref(false)
const isExpanded = ref(true)
const itemId = ref(null)

const fnEditCell = (item, field) => {
  console.log("item", item)
  console.log("field", field)
  let id = "id"+field.substring(1)
  console.log("v", item[id])


}

const fnDeleteCell = (item, field) => {
  console.log("item", item)
  console.log("field", field)
}

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
    style: 'font-size: 1.2em; width: 74%',
  },
  {
    name: 'v_2020',
    label: 'v_2020',
    field: 'v_2020',
    align: 'left',
    style: 'font-size: 1.2em; width: 10.3%',
  },
  {
    name: 'v_2021',
    label: 'v_2021',
    field: 'v_2021',
    align: 'left',
    style: 'font-size: 1.2em; width: 15.7%',
  },
]


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
})

watch(
  () => props.own,
  (newObj) => {
    loadReservoirsMeter(newObj);
  },
  {immediate: true}
)

</script>

<style scoped>
.sticky-header-table {
  /* Ограничиваем высоту контейнера, чтобы появилась прокрутка */
  max-height: 95%;
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
  border-top: 3px;
}
</style>

