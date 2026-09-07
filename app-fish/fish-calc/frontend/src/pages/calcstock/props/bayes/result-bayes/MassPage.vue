<template>
  <div class="column no-wrap fit">

    <div class="bg-orange-1" style="height: 100%">

      <q-markup-table separator="cell" bordered wrap-cells class="fit">
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

import {useQuasar} from "quasar";
import {computed, getCurrentInstance, onMounted, ref, watch} from "vue";
import {expandAll, findRowForId, notifyError, notifyInfo, pack} from "@/utils/jsutils.js";
import {api} from "@/boot/axios.js";

const $q = useQuasar()
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

const loadResultMass = (objId) => {
  if (!objId) return;
  loading.value = true

  api
    .post('', {
      method: 'data/loadResultMass',
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
})

watch(
  () => props.own,
  (newObj) => {
    loadResultMass(newObj);
  },
  {immediate: true}
)

</script>

<style scoped>

</style>

