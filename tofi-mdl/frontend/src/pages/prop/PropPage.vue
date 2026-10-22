<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="folder" />
              {{ $t('propGr') }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:insgr')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('ins', false)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('create1level') }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:insgr')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm img-vert"
                @click="fnInsGr('ins', true)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('createChild') }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:updgr')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('upd', null)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('editRecord') }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:delgr')"
                dense
                icon="delete"
                color="red"
                class="q-ml-sm"
                @click="fnDelGr(currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('deletingRecord') }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary" />
            </template>
          </q-banner>
          <div style="height: calc(100vh - 250px); width: 100%">
            <QTreeTable
              :cols="cols"
              :rows="rows"
              :icon_leaf="''"
              :FD_PropType="FD_PropType"
              @updateSelect="onUpdateSelect"
              ref="childComp"
              checked_visible="true"
            />
          </div>
        </div>
      </template>

      <template v-slot:after>

        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="credit_score" />
            {{ $t('props') }}
          </div>
          <template v-slot:action>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:prop:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('ins')"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('create1level') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:prop:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('upd')"
              :disable="currentNode2 == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('editRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:prop:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              @click="fnDel(currentNode2)"
              :disable="currentNode2 == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('deletingRecord') }}
              </q-tooltip>
            </q-btn>
            <q-space />

            <q-btn
              v-if="hasTarget('mdl:mn_ds:prop:val')"
              :dense="dense"
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="currentNode2 == null"
              @click="propSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('chooseRecord') }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
          <QTreeTable
            :cols="cols2"
            :rows="rows2"
            :icon_leaf="''"
            @updateSelect="onUpdateSelect2"
            :meterStruct="meterStruct"
            :FD_PropType="FD_PropType"
            ref="childComp2"
            checked_visible="true"
          />
        </div>

      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  pack
} from '@/utils/jsutils'
import QTreeTable from '@/components/QTreeTable.vue'
import UpdateGroup from '@/pages/group/UpdateGroup.vue'
import UpdateProp from '@/pages/prop/UpdateProp.vue'
import allConsts from '@/pages/all-consts'

const router = useRouter()
const route = useRoute()
const $q = useQuasar()
const { proxy } = getCurrentInstance()

const splitterModel = ref(30)
const cols = ref([])
const rows = ref([])
const currentNode = ref(null)
const visible = ref(false)

const dense = ref(true)
const cols2 = ref([])
const rows2 = ref([])
const FD_PropType = reactive(new Map())
const loading2 = ref(false)

const currentNode2 = ref(null)
const meterStruct = ref(0)
const propId = ref(0)
const propGrId = ref(0)
const lang = ref('')

const childComp = ref(null)
const childComp2 = ref(null)

const propSelect = () => {
  router.push({
    name: 'propSelected',
    params: {
      propGr: currentNode.value.id,
      prop: currentNode2.value.id
    }
  })
}

const onUpdateSelect = (item) => {
  propId.value = 0
  if (currentNode2.value)
    childComp2.value?.selectedRow(currentNode2.value)

  currentNode.value = item.selected !== undefined ? item.selected : null
  if (currentNode.value) {
    api
      .post('', {
        method: 'group/loadRec',
        params: [{ id: currentNode.value.id, tableName: 'PropGr' }]
      })
      .then((response) => {
        currentNode.value = response.data.result.records[0]
        fetchData(currentNode.value.id)
      })
  } else {
    currentNode2.value = null
    fetchData(0)
  }
}

const onUpdateSelect2 = (item) => {
  propId.value = 0
  currentNode2.value = item.selected !== undefined ? item.selected : null
  if (currentNode2.value) {
    api
      .post('', {
        method: 'prop/loadRec',
        params: [currentNode2.value.id]
      })
      .then((response) => {
        currentNode2.value = response.data.result.records[0]

        if (currentNode2.value.statusFactor)
          checkStatus(currentNode2.value)

        if (currentNode2.value.providerTyp)
          checkProvider(currentNode2.value)

        let field = ''
        if (currentNode2.value.propType === 1) field = 'factorVal'
        else if (currentNode2.value.propType === 5) field = 'cls'
        else if (currentNode2.value.propType === 6) field = 'relCls'
        else if (currentNode2.value.propType === 7) field = 'measure'
        if (field !== '')
          checkRefValue(currentNode2.value.id, field)
      })
      .finally(() => {
        if (
          currentNode2.value.propType === allConsts.FD_PropType.meter ||
          currentNode2.value.propType === allConsts.FD_PropType.rate
        ) {
          api
            .post('', {
              method: 'meter/loadRec',
              params: [{ id: currentNode2.value.meter }]
            })
            .then((response) => {
              meterStruct.value = response.data.result.records[0].meterStruct
            })
        }
      })
  }
}

const checkRefValue = (prop, field) => {
  api
    .post('', {
      method: 'prop/checkRefValue',
      params: [prop, field]
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t('notPossibleValue') || 'notPossibleValue')
      }
    })
}

const checkStatus = (item) => {
  let prop = item.id
  api
    .post('', {
      method: 'prop/checkStatus',
      params: [prop]
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t('notStatus') || 'notStatus')
      }
    })
}

const checkProvider = (item) => {
  let prop = item.id
  api
    .post('', {
      method: 'prop/checkProvider',
      params: [prop]
    })
    .then((response) => {
      if (!response.data.result) {
        notifyInfo(proxy?.$t('notProvider') || 'notProvider')
      }
    })
}

const fetchDataGr = () => {
  visible.value = true
  currentNode.value = null

  api
    .post('', {
      method: 'group/loadGroup',
      params: [{ tableName: 'PropGr' }]
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result.records, 'id')
        fnExpand()
      },
      (error) => {
        let msg = error.message
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message) || msg
        notifyError(msg)
      }
    )
    .then(() => {
      if (propGrId.value > 0 && childComp.value) {
        let res = []
        findNode(rows.value, 'id', propGrId.value, res)
        currentNode.value = res[0]
        childComp.value?.restoreSelect(currentNode.value)
        fetchData(currentNode.value.id)
      }
    })
    .finally(() => {
      visible.value = false
    })
}

const fnInsGr = (mode, isChild) => {
  let data = {
    id: 0,
    cod: '',
    name: '',
    fullName: '',
    accessLevel: 1,
    cmt: null
  }
  const lg = lang.value
  let parent = null
  let parentName = null
  if (isChild) {
    parent = currentNode.value.id
    parentName = currentNode.value.name
  }
  if (mode === 'ins') {
    data.parent = parent
  } else if (mode === 'upd') {
    data = {
      id: currentNode.value.id,
      cod: currentNode.value.cod,
      name: currentNode.value.name,
      fullName: currentNode.value.fullName,
      accessLevel: currentNode.value.accessLevel,
      parent: currentNode.value.parent,
      cmt: currentNode.value.cmt
    }
    if (currentNode.value.parent > 0) {
      let parentNode = []
      getParentNode(rows.value, currentNode.value.parent, parentNode)
      parentName = parentNode[0].fullName
      isChild = true
    }
  }

  $q.dialog({
    component: UpdateGroup,
    componentProps: {
      data: data,
      mode: mode,
      isChild: isChild,
      tableName: 'PropGr',
      parentName: parentName,
      lg: lg,
      dense: true
    }
  }).onOk((resData) => {
    fetchDataGr()
    currentNode.value = resData
    childComp.value?.restoreSelect(resData)
    onUpdateSelect({ selected: resData })
  })
}

const fnDelGr = (rec) => {
  rec.tableName = 'PropGr'
  $q.dialog({
    title: proxy?.$t('confirmation') || 'confirmation',
    message:
      (proxy?.$t('deleteRecord') || 'deleteRecord') +
      '<div style="color: plum">(' +
      rec.cod +
      ': ' +
      rec.name +
      ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel'
  })
    .onOk(() => {
      api
        .post('', {
          method: 'group/delete',
          params: [rec]
        })
        .then(
          () => {
            fetchDataGr()
          },
          (error) => {
            let msg = error.message
            if (error.response) msg = error.response.data.error.message
            notifyError(msg)
          }
        )
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled') || 'canceled')
    })
}

const edit = (data, mode) => {
  if (
    mode === 'ins' &&
    (data.propType === allConsts.FD_PropType.typ ||
      data.propType === allConsts.FD_PropType.reltyp ||
      data.propType === allConsts.FD_PropType.factor ||
      data.propType === allConsts.FD_PropType.attr ||
      data.propType === allConsts.FD_PropType.complex)
  ) {
    data.isUniq = true
    data.allItem = false
    data.visualFormat = allConsts.FD_VisualFormat.shortt
  }
  const lg = lang.value
  $q.dialog({
    component: UpdateProp,
    componentProps: {
      rec: data,
      mode: mode,
      lg: lg,
      dense: true
    }
  }).onOk((r) => {
    fetchData(currentNode.value.id)
    currentNode2.value = r
    childComp2.value?.restoreSelect(currentNode2.value)
  })
}

const fnIns = (mode) => {
  propId.value = 0

  if (mode === 'ins') {
    currentNode2.value = null
    api
      .post('', {
        method: 'prop/newRec',
        params: [currentNode.value.id]
      })
      .then((response) => {
        edit(response.data.result.records[0], mode)
      })
  } else {
    edit(currentNode2.value, mode)
  }
}

const fnDel = (rec) => {
  propId.value = 0

  $q.dialog({
    title: proxy?.$t('confirmation') || 'confirmation',
    message:
      (proxy?.$t('deleteRecord') || 'deleteRecord') +
      '<div style="color: plum">(' +
      rec.cod +
      ': ' +
      rec.name +
      ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel'
  })
    .onOk(() => {
      api
        .post('', {
          method: 'prop/delete',
          params: [rec]
        })
        .then(
          () => {
            childComp2.value?.clrAny()
            fetchData(currentNode.value.id)
          },
          (error) => {
            let msg = error.message
            if (error.response) msg = error.response.data.error.message
            if (msg === 'NotChangeStructComplexProp')
              msg = proxy?.$t('NotChangeStructComplexProp') || msg
            else if (msg.includes('propperiodtype'))
              msg = 'зависит от Периода'
            else if (msg.includes('propstatus'))
              msg = 'зависит от Статуса'
            else if (msg.includes('proppovider'))
              msg = 'зависит от Поставщика'
            else msg = ':-('
          }
        )
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled') || 'canceled')
    })
}

const fetchData = (propGr) => {
  visible.value = true
  currentNode2.value = null

  api
    .post('', {
      method: 'prop/loadPropTree',
      params: [propGr]
    })
    .then(
      (response) => {
        rows2.value = pack(response.data.result.records, 'ord')
        fnExpand2()
      },
      (error) => {
        let msg = error.message
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message) || msg
        notifyError(msg)
      }
    )
    .then(() => {
      if (propId.value > 0) {
        let res = []
        findNode(rows2.value, 'id', propId.value, res)
        currentNode2.value = res[0]
        childComp2.value?.restoreSelect(currentNode2.value)
      }
    })
    .finally(() => {
      visible.value = false
    })
}

const getColumns = () => [
  {
    name: 'cod',
    label: proxy?.$t('code') || 'code',
    field: 'cod',
    align: 'left',
    headerStyle: 'font-size: 1.2em; width: 30%'
  },
  {
    name: 'name',
    label: proxy?.$t('fldName') || 'fldName',
    field: 'name',
    align: 'left',
    headerStyle: 'font-size: 1.2em; width: 70%'
  }
]

const getColumns2 = () => [
  {
    name: 'cod',
    label: proxy?.$t('code') || 'code',
    field: 'cod',
    align: 'left',
    headerStyle: 'font-size: 1.2em; width: 25%'
  },
  {
    name: 'name',
    label: proxy?.$t('fldName') || 'fldName',
    field: 'name',
    align: 'left',
    headerStyle: 'font-size: 1.2em; width: 20%'
  },
  {
    name: 'fullName',
    label: proxy?.$t('fldFullName') || 'fldFullName',
    field: 'fullName',
    align: 'left',
    headerStyle: 'font-size: 1.2em; width: 25%'
  },
  {
    name: 'propType',
    label: proxy?.$t('propType') || 'propType',
    field: 'propType',
    headerStyle: 'font-size: 1.2em; width: 15%'
  },
  {
    name: 'cmt',
    label: proxy?.$t('fldCmt') || 'fldCmt',
    field: 'cmt',
    align: 'left',
    headerStyle: 'font-size: 1.2em; width: 15%'
  }
]

const fnExpand = () => {
  expandAll(rows.value)
}

const fnCollapse = () => {
  collapsAll(rows.value)
}

const fnExpand2 = () => {
  expandAll(rows2.value)
}

onMounted(() => {
  lang.value = localStorage.getItem('curLang')
  lang.value = lang.value === 'en-US' ? 'en' : lang.value

  api
    .post('', {
      method: 'dict/load',
      params: [{ dict: 'FD_PropType' }]
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_PropType.set(it.id, it.text)
      })
    })

  cols.value = getColumns()
  cols2.value = getColumns2()

  propId.value = parseInt(route.params.prop, 10) || 0
  propGrId.value = parseInt(route.params.propGr, 10) || 0

  fetchDataGr()
})
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
}
</style>
