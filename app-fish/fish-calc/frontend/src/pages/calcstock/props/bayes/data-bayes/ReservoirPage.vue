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
                  :disable="!item['id'+col.field.substring(1)]"
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
      </q-markup-table>

    </div>

  </div>
</template>


<script setup>

import {useQuasar} from "quasar";
import {computed, getCurrentInstance, onMounted, ref, watch} from "vue";
import {expandAll, findRowForId, notifyError, notifyInfo, pack} from "@/utils/jsutils.js";
import {api} from "@/boot/axios.js";
import UpdaterReservoirPage from "./UpdaterReservoirPage.vue";

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

const updateRowValue = (item, field, newrec) => {
  let row = findRowForId(rows.value, item.id)
  if (row) {
    row[field] = newrec.value
    let idVal = "id"+field.substring(1)
    row[idVal] = newrec.id
  }
}

const fnEditCell = (item, field) => {
  console.log("item", item)
  console.log("field", field)
  let idVal = "id"+field.substring(1)
  console.log("v", item[idVal])
  //
  const mode = item[idVal] ? "upd" : "ins"
  console.log("mode", mode)
  let rec = {
    obj: props.own,
    prop: item.id,
    name: item.name,
    idval: item[idVal] || 0,
    numberval: item[field] || '',
    year: field.substring(1),
  }

  $q.dialog({
    component: UpdaterReservoirPage,
    componentProps: {
      data: rec,
      mode: mode
    },
  })
    .onOk((r) => {
      updateRowValue(item, field, r)
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const fnDeleteCell = (item, field) => {
  console.log("item", item)
  console.log("field", field)
  console.log("id", item["id"+field.substring(1)])

  let nm = item.name
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '</br>(' + nm + ', за ' + field.substring(1) +'г.)',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deleteReservoirPage',
          params: [item["id"+field.substring(1)]],
        })
        .then(() => {
          let row = findRowForId(rows.value, item.id)
          if (row) {
            row[field] = null
            let idVal = "id"+field.substring(1)
            row[idVal] = null
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

const loadReservoirsMeter = (objId) => {
  if (!objId) return;
  loading.value = true

  api
    .post('', {
      method: 'data/loadReservoirPage',
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
    loadReservoirsMeter(newObj);
  },
  {immediate: true}
)

</script>

<style scoped>

</style>

