<template>
  <div class="column no-wrap fit">

    <div class="bg-orange-1" style="height: 100%">

      <q-markup-table bordered class="fit" separator="cell" wrap-cells>
        <thead class="text-bold text-white bg-blue-grey-13">
          <tr>
            <th :style="cols[0]?.style">
              {{ cols[0]?.label }}
            </th>
            <th :style="cols[1]?.style">
              {{ cols[1]?.label }}
            </th>
            <th></th>
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

            <td :data-th="cols[1]?.name">
              {{ item.numberval }}
            </td>

            <td :data-th="cols[2]?.name">
              <q-btn
                class="no-padding no-margin" color="blue"
                dense flat icon="edit" round size="sm"
                @click="fnEditCell(item)"
              >
                <q-tooltip>
                  {{ $t('update') }}
                </q-tooltip>
              </q-btn>

              <q-btn
                :disable="!(item.idval > 0)"
                class="no-padding no-margin" color="red"
                dense flat icon="delete" round size="sm" @click="fnDeleteCell(item)"
              >
                <q-tooltip>
                  {{ $t('deletingRecord') }}
                </q-tooltip>
              </q-btn>
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
import UpdaterFishPage from "@/pages/calcstock/props/bayes/data-bayes/UpdaterFishPage.vue";

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

const updateRowValue = (item, newrec) => {
  let row = findRowForId(rows.value, item.id)
  if (row) {
    row["numberval"] = newrec.value
    row["idval"] = newrec.id
  }
}

const fnEditCell = (item) => {
  console.log("item", item)
  //
  const mode = item["idval"] ? "upd" : "ins"
  console.log("mode", mode)
  let rec = {
    obj: props.own,
    prop: item.id,
    name: item.name,
    idval: item["idval"] || 0,
    numberval: item["numberval"] || '',
  }

  $q.dialog({
    component: UpdaterFishPage,
    componentProps: {
      data: rec,
      mode: mode
    },
  })
    .onOk((r) => {
      updateRowValue(item, r)
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

const fnDeleteCell = (item) => {
  console.log("item", item)

  let nm = item.name
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '</br>(' + nm + ')',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      api
        .post('', {
          method: 'data/deleteFishPage',
          params: [item["idval"]],
        })
        .then(() => {
          let row = findRowForId(rows.value, item.id)
          if (row) {
            row["numberval"] = null
            row["idval"] = null
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

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName'),
    field: 'name',
    align: 'left',
    style: 'font-size: 1.2em; width: 70%',
  },
  {
    name: 'numberval',
    label: proxy?.$t('val'),
    field: 'numberval',
    align: 'center',
    style: 'font-size: 1.2em; width: 20%',
  },
  {name: 'cmd', field: 'cmd', align: 'center', style: 'font-size: 1.2em; width: 10%'},
]

const loadFishPage = (objId) => {
  if (!objId) return;
  loading.value = true

  api
    .post('', {
      method: 'data/loadFishPage',
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
    loadFishPage(newObj);
  },
  {immediate: true}
)

</script>

<style scoped>

</style>

