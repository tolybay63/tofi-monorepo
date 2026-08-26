<template>
  <div class="q-pa-md bg-amber-1 no-scroll">
    <q-banner dense inline-actions class="bg-orange-1" style="margin-bottom: 5px">
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" text-color="white" icon="home_work"></q-avatar>
        {{ $t('kato') }}
      </div>
      <template v-slot:action>
        <q-btn
          v-if="hasTarget('nsi:kato:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('addRegion') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('nsi:kato:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins', true)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('addDistrict') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('nsi:kato:upd')"
          dense
          icon="edit_note"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('upd', false)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('editRecord') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('nsi:kato:del')"
          dense
          icon="delete"
          color="red"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('deletingRecord') }}
          </q-tooltip>
        </q-btn>
        <q-inner-loading :showing="visible" color="secondary" />
      </template>
    </q-banner>

    <div style="height: calc(100vh - 250px); width: 100%" class="scroll">
      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { collapsAll, getParentNode, hasTarget, notifyError, pack } from '@/utils/jsutils'
import QTreeTable from '@/components/QTreeTable.vue'
import UpdaterKATO from './UpdaterKATO.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const selected = ref([])
const rows = ref([])
const currentNode = ref(null)
const visible = ref(false)
const childComp = ref(null)

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName'),
    field: 'name',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 30%',
  },
  {
    name: 'fullName',
    label: proxy?.$t('fldFullName'),
    field: 'fullName',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 40%',
  },
  {
    name: 'cmt',
    label: proxy?.$t('fldCmt'),
    field: 'cmt',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 30%',
  },
]

const cols = ref(getColumns())

const clearAny = () => {
  childComp.value?.clrAny()
}

const onUpdateSelect = (data) => {
  currentNode.value = data.selected !== undefined ? data.selected : null
}

const fetchData = () => {
  visible.value = true
  api
    .post('', {
      method: 'data/loadKATO',
      params: [],
    })
    .then((response) => {
      rows.value = pack(response.data.result['records'], 'ord')
      collapsAll(rows.value)
    })
    .catch((error) => {
      let msg = error.message
      if (error.response) msg = proxy?.$t(error.response.data.error.message)
      notifyError(msg)
    })
    .finally(() => {
      visible.value = false
    })
}

const fnIns = (mode, isChild) => {
  let data = {
    accessLevel: 1,
  }

  let parent = null
  let parentName = null

  if (isChild) {
    if (currentNode.value.parent) {
      parent = currentNode.value.parent
      let parentNode = []
      getParentNode(rows.value, currentNode.value.parent, parentNode)
      parentName = parentNode[0].fullName
    } else {
      parent = currentNode.value.id
      parentName = currentNode.value.fullName
    }
  }

  if (mode === 'ins') {
    data.parent = parent
  } else if (mode === 'upd') {
    data = {
      id: currentNode.value.id,
      cls: currentNode.value.cls,
      parent: currentNode.value.parent,
      cod: currentNode.value.cod,
      accessLevel: currentNode.value.accessLevel,
      name: currentNode.value.name,
      fullName: currentNode.value.fullName,
      cmt: currentNode.value.cmt,
    }
    if (currentNode.value.parent > 0) {
      let parentNode = []
      getParentNode(rows.value, currentNode.value.parent, parentNode)
      parentName = parentNode[0].fullName
      isChild = true
    }
  }

  $q.dialog({
    component: UpdaterKATO,
    componentProps: {
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      data: data,
    },
  }).onOk((updatedData) => {
    fetchData()
    currentNode.value = updatedData
    childComp.value?.restoreSelect(updatedData)
  })
}

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '<div style="color: plum">(' + rec.fullName + ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  }).onOk(() => {
    api
      .post('', {
        method: 'data/deleteKATO',
        params: [rec.id],
      })
      .then(() => {
        fetchData()
        clearAny()
        selected.value = []
        currentNode.value = null
      })
      .catch((error) => {
        let msg = error.response?.data?.error?.message || error.message
        notifyError(msg)
      })
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.img-vert {
  transform: scaleY(-1);
  -ms-filter: 'FlipV';
}
</style>
