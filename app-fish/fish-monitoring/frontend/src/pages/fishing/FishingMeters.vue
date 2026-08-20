<template>
  <div class="q-pa-md bg-amber-1" style="font-size: 18px"> {{ name }}</div>

  <div class="q-pa-sm-sm bg-orange-1 sticky-header-table">
    <table class="q-table q-table--cell-separator q-table--bordered wrap">
      <thead class="text-bold text-white bg-blue-grey-13">
      <tr>
        <th style="font-size: 1.2em; width: 60%">
          {{ cols[0]?.label }}
        </th>
        <th style="font-size: 1.2em; width: 25%">
          {{ cols[1]?.label }}
        </th>
        <th></th>
      </tr>
      </thead>

      <tbody style="background: aliceblue">
      <tr v-for="(item, index) in arrayTreeObj" :key="index">
        <td :data-th="cols[0]?.name" @click="toggle(item)">
          <span class="q-tree-link q-tree-label" :style="setPadding(item)">
            <q-icon :name="iconName(item)" color="secondary" style="cursor: pointer"/>
            {{ item.name }}
          </span>
        </td>
        <td :data-th="cols[1]?.name">
          {{ item.numberval }}
        </td>
        <td :data-th="cols[2]?.name">
          <q-btn
            class="no-padding no-margin" color="blue" dense flat icon="edit" round
            size="sm" @click="fnEdit(item)"
          >
            <q-tooltip transition-hide="rotate" transition-show="rotate">
              {{ $t("update") }}
            </q-tooltip>
          </q-btn>

          <q-btn
            class="no-padding no-margin" color="red" dense flat icon="delete" round
            size="sm" @click="fnDelete(item)" :disable="!(item.idval > 0)"
          >
            <q-tooltip transition-hide="rotate" transition-show="rotate">
              {{ $t("deletingRecord") }}
            </q-tooltip>
          </q-btn>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { notifyError, notifyInfo, pack } from '@/utils/jsutils'
import UpdaterFishingMeters from "@/pages/fishing/UpdaterFishingMeters.vue"

const props = defineProps({
  name: String
})

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const rows = ref([])
const cols = ref([])
const loading = ref(false)
const isExpanded = ref(true)
const itemId = ref(null)
const obj = ref(0)

const fnDelete = (row) => {
  let nm = row.name
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message: proxy?.$t("deleteRecord") + "</br>(" + nm + ")",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  })
    .onOk(() => {
      api.post('', {
        method: "data/deleteFishingMeters",
        params: [row.idval],
      })
        .then(() => {
          if (row.level === 0) {
            rows.value[0].idval = null
            rows.value[0].numberval = null
          } else {
            let childs = rows.value[0].children
            let index = childs.findIndex((rec) => rec.id === row.id)
            childs[index].idval = null
            childs[index].numberval = null
          }
        })
        .catch((error) => {
          notifyError(error.message)
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"))
    })
}

const updateRowValue = (currentRows, targetRec) => {
  for (let row of currentRows) {
    if (row.id === targetRec.id) {
      row.idval = targetRec.idval
      row.numberval = targetRec.numberval
      row.dbeg = targetRec.dbeg
      row.dend = targetRec.dend
      return true
    }
    if (row.children && Array.isArray(row.children) && row.children.length > 0) {
      if (updateRowValue(row.children, targetRec)) {
        return true
      }
    }
  }
  return false
}

const fnEdit = (row) => {
  let rec = {
    obj: obj.value,
    prop: row.id,
    numberval: row.numberval || "",
    name: row.name,
    idval: row.idval
  }
  $q.dialog({
    component: UpdaterFishingMeters,
    componentProps: {
      data: rec,
    },
  })
    .onOk((r) => {
      updateRowValue(rows.value, r)
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"))
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
  if (item.expend) return "remove_circle_outline"
  if (item.children && item.children.length > 0) return "control_point"
  return ""
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

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    style: "font-size: 1.2em; width: 60%",
  },
  {
    name: "numberval",
    label: proxy?.$t("val"),
    field: "numberval",
    align: "center",
    style: "font-size: 1.2em; width: 15%",
  },
  {
    name: "cmd",
    field: "cmd",
    align: "center",
    style: "font-size: 1.2em; width: 10%",
  }
]

const clearFishingData = () => {
  rows.value = []
}

const loadFishingMeters = (targetObj) => {
  loading.value = true
  obj.value = targetObj
  api.post('', {
    method: 'data/loadFishingMeters',
    params: [targetObj],
  })
    .then((response) => {
      rows.value = pack(response.data.result["records"], "id")
    })
    .finally(() => {
      loading.value = false
    })
}

const arrayTreeObj = computed(() => {
  let newObj = []
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value)
  return newObj
})

onMounted(() => {
  cols.value = getColumns()
})

defineExpose({
  clearFishingData,
  loadFishingMeters
})
</script>

<style scoped>
.sticky-header-table {
  max-height: 95%;
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
