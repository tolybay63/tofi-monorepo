<template>
  <q-page class="q-pa-md bg-amber-1">
    <q-banner dense inline-actions class="bg-orange-1" style="margin-bottom: 5px">
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" text-color="white" icon="apartment"></q-avatar>
        {{ $t('struct_enterprise') }}
      </div>
      <template v-slot:action>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:mea:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('addEnt') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:mea:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          @click="fnIns('ins', true)"
          :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('addChild') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:mea:upd')"
          dense
          icon="edit"
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
          v-if="hasTarget('mdl:mn_ds:mea:del')"
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

        <q-inner-loading :showing="loading" color="secondary" />
      </template>
    </q-banner>

    <div style="height: calc(100vh - 250px); width: 100%">
      <QTreeTable
        :cols="cols"
        :rows="rows"
        :icon_leaf="''"
        @updateSelect="onUpdateSelect"
        checked_visible="true"
        ref="childComp"
      />
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '../../boot/axios'
import { expandAll, getParentNode, hasTarget, notifyInfo, pack } from '../../utils/jsutils'
import QTreeTable from '../../components/QTreeTable.vue'
import UpdaterStructEnterprise from './UpdaterStructEnterprise.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

//const selected = ref([])
const rows = ref([])
const currentNode = ref(null)
const loading = ref(false)
const childComp = ref(null)

const getColumns = () => [
  {
    name: 'name',
    label: proxy?.$t('fldName') + '*',
    field: 'name',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.3em;',
    headerClass: 'text-bold text-white bg-blue-grey-13 ',
    style: 'text-align: left; width:20%',
  },
  {
    name: 'fullname',
    label: proxy?.$t('fldFullName') + '*',
    field: 'fullname',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.3em;',
    headerClass: 'text-bold text-white bg-blue-grey-13',
    style: 'text-align: left; width:30%',
  },
  {
    name: 'namecls',
    label: proxy?.$t('cls') + '*',
    field: 'namecls',
    headerStyle: 'font-size: 1.3em;',
    headerClass: 'text-bold text-white bg-blue-grey-13',
    style: 'text-align: right; width:20%;',
  },
  {
    name: 'cmt',
    label: proxy?.$t('fldCmt'),
    field: 'cmt',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.3em;',
    headerClass: 'text-bold text-white bg-blue-grey-13',
    style: 'text-align: left; width:30%',
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
  loading.value = true
  api
    .post('', {
      method: 'data/loadEnterprise',
      params: ['Typ_Enterprise'],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, 'ord')
      expandAll(rows.value)
    })
    .catch((error) => {
      let msg = error.message
      if (error.response) msg = proxy?.$t(error.response.data.error.message)

      console.error(msg)
    })
    .finally(() => {
      loading.value = false
    })
}

const fnIns = (mode, isChild) => {
  let data = {}
  let parent = null
  let parentName = null

  if (isChild) {
    parent = currentNode.value.id
    parentName = currentNode.value.fullname
  }

  if (mode === 'ins') {
    data.parent = parent
  } else if (mode === 'upd') {
    data = {
      id: currentNode.value.id,
      parent: currentNode.value.parent,
      name: currentNode.value.name,
      cls: currentNode.value.cls,
      nameCls: currentNode.value.namecls,
      fullName: currentNode.value.fullname,
      cmt: currentNode.value.cmt,
    }
    if (currentNode.value.parent > 0) {
      let parentNode = []
      getParentNode(rows.value, currentNode.value.parent, parentNode)
      parentName = parentNode[0].fullname
      isChild = true
    }
  }

  $q.dialog({
    component: UpdaterStructEnterprise,
    componentProps: {
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      data: data,
    },
  }).onOk((resData) => {
    fetchData()
    currentNode.value = resData
    childComp.value?.restoreSelect(resData)
  })
}

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '<div style="color: plum">(' + rec.name + ')</div>',
    html: true,
    cancel: true,
    persistent: true,
  })
    .onOk(() => {
      loading.value = true
      api
        .post('', {
          method: 'data/deleteEnterprise',
          params: [rec.id],
        })
        .then(() => {
          fetchData()
          clearAny()
        })
        .finally(() => {
          loading.value = false
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: 'FlipV';
}
</style>
