<template>
  <q-page class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :limits="[80, 100]"
      after-class="overflow-hidden q-pl-sm"
      before-class="overflow-hidden q-pr-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <q-table
          v-model:pagination="pagination"
          v-model:selected="selected"
          :columns="cols"
          :filter="filter"
          :loading="loading"
          :rows="rows"
          :rows-per-page-options="[25, 0]"
          :table-colspan="4"
          :wrap-cells="true"
          card-class="bg-amber-1"
          class="sticky-header-table"
          color="primary"
          dense
          row-key="own"
          selection="single"
          separator="cell"
          table-header-class="text-bold text-white bg-blue-grey-13"
          @request="requestData"
          @update:selected="updateSelected"
        >
          <template #bottom-row>
            <q-td v-if="selected.length > 0" colspan="100%">
              <span class="text-blue"> {{ $t('selectedRow') }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td v-else-if="rows.length > 0" class="text-bold" colspan="100%">
              {{ $t('infoRow') }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold">
              <q-avatar color="black" icon="groups" text-color="white" />
              {{ $t('personnel') }}
            </div>

            <q-space />
            <q-btn
              v-if="hasTarget('st:per:ins')"
              dense
              :disable="loading"
              color="secondary"
              icon="post_add"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t('newRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('st:per:upd')"
              :disable="loading || selected.length === 0"
              class="q-ml-sm"
              color="secondary"
              dense
              icon="edit"
              @click="editRow(selected[0], 'upd')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t('editRecord') }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('st:per:del')"
              :disable="loading || selected.length === 0"
              class="q-ml-sm"
              color="red"
              dense
              icon="delete"
              @click="removeRow(selected[0])"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ $t('deletingRecord') }}
              </q-tooltip>
            </q-btn>

            <q-space />
            <q-input
              v-model="filter"
              :label="$t('txt_filter')"
              color="primary"
              debounce="300"
              dense
            >
              <template v-slot:append>
                <q-icon name="search" />
              </template>
            </q-input>
          </template>

          <template #loading>
            <q-inner-loading color="secondary" showing></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <q-card class="bg-amber-1 full-height">
          <q-card-section>
            <q-input
              v-model="form.UserSecondName"
              :label="fnLabel('UserSecondName', true)"
              readonly
            />
            <q-input
              v-model="form.UserFirstName"
              :label="fnLabel('UserFirstName', true)"
              readonly
            />
            <q-input
              v-model="form.UserMiddleName"
              :label="fnLabel('UserMiddleName', false)"
              readonly
            />
            <q-select
              v-model="form.fvUserSex"
              :label="fnLabel('UserSex', true)"
              :options="optUserSex"
              map-options
              option-label="name"
              option-value="id"
              readonly
            />
            <q-input
              v-model="form.UserDateBirth"
              :label="fnLabel('UserDateBirth', false)"
              readonly
            />
            <q-input v-model="form.UserEmail" :label="fnLabel('UserEmail', false)" readonly />
            <q-input v-model="form.UserPhone" :label="fnLabel('UserPhone', false)" readonly />
            <q-select
              v-model="form.UserId"
              :label="fnLabel('UserId', false)"
              :options="optUserId"
              map-options
              option-label="name"
              option-value="id"
              readonly
            />
          </q-card-section>
        </q-card>
      </template>
    </q-splitter>
  </q-page>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useQuasar, extend } from 'quasar'
import { api } from '@/boot/axios'
import { hasTarget, notifyInfo, notifySuccess } from '@/utils/jsutils.js'
import UpdaterPersonnel from './UpdaterPersonnel.vue'

const $q = useQuasar()
const { proxy } = getCurrentInstance()

const splitterModel = ref(100)
const rows = ref([])
const filter = ref('')
const loading = ref(false)
const selected = ref([])
const optUserId = ref([])
const optUserSex = ref([])

const form = reactive({
  UserSecondName: null,
  UserFirstName: null,
  UserMiddleName: null,
  UserDateBirth: null,
  UserEmail: null,
  UserPhone: null,
  UserId: null,
})

const pagination = ref({
  sortBy: null,
  descending: false,
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
})

const requestParam = {
  page: 1,
  rowsPerPage: 25,
  rowsNumber: 0,
  filter: '',
  descending: false,
  sortBy: null,
}

const getColumns = () => [
  {
    name: 'fio',
    label: proxy?.$t('fio') + '*',
    field: 'fio',
    align: 'left',
    sortable: true,
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width: 40%',
  },
  {
    name: 'nameUserPosition',
    label: proxy?.$t('UserPosition') + '*',
    field: 'nameUserPosition',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em',
    style: 'width: 25%',
  },
  {
    name: 'nameUserOrg',
    label: proxy?.$t('UserOrg') + '*',
    field: 'nameUserOrg',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em',
    style: 'width: 35%',
  },
]

const cols = ref(getColumns())

const fnLabel = (txt, req) => {
  return req ? proxy?.$t(txt) + '*' : proxy?.$t(txt)
}

const infoSelected = (row) => {
  return ' ' + row.fio
}

const updateSelected = () => {
  Object.keys(form).forEach((k) => (form[k] = null))
  if (selected.value.length > 0) {
    splitterModel.value = 80
    extend(true, form, selected.value[0])
    if (form.UserPhone !== null && String.isEmpty(form.UserPhone) &&
        form.UserPhone !== undefined && form.UserPhone.length === 10) {
      form.UserPhone =
        '+7 ' +
        form.UserPhone.substring(0, 3) +
        ' ' +
        form.UserPhone.substring(3, 6) +
        ' ' +
        form.UserPhone.substring(6, 10)
    }
    if (form.UserId) {
      form.UserId = parseInt(form.UserId, 10)
    }
  } else {
    splitterModel.value = 100
  }
}

const fetchData = (requestProps) => {
  loading.value = true
  api
    .post('', {
      method: 'data/loadPersonnel',
      params: [
        {
          page: requestProps.page,
          limit: requestProps.rowsPerPage,
          orderBy: requestProps.sortBy,
          filter: requestProps.filter,
        },
      ],
    })
    .then((response) => {
      rows.value = response.data.result.store.records
      const meta = response.data.result.meta
      pagination.value.page = meta.page
      pagination.value.rowsPerPage = meta.limit === meta.total ? 0 : meta.limit
      pagination.value.rowsNumber = meta.total
      selected.value = []
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

const requestData = (requestProps) => {
  const sb = requestProps.pagination.sortBy
  const des = requestProps.pagination.descending
  if (sb === null) {
    requestParam.sortBy = null
  } else {
    requestParam.sortBy = des ? sb + ' desc' : sb
  }
  requestParam.descending = requestProps.pagination.descending
  requestParam.filter = requestProps.filter
  requestParam.page = requestProps.pagination.page
  requestParam.rowsPerPage = requestProps.pagination.rowsPerPage
  requestParam.rowsNumber = requestProps.pagination.rowsNumber

  pagination.value.sortBy = requestProps.pagination.sortBy
  pagination.value.descending = requestProps.pagination.descending

  fetchData(requestParam)
}

const editRow = (rec, mode) => {
  let data = {}
  if (mode === 'ins') {
    loading.value = true
    api
      .post('', {
        method: 'data/newRec',
        params: [],
      })
      .then((response) => {
        data = response.data.result.records[0]
      })
      .finally(() => {
        loading.value = false
      })
  } else {
    extend(true, data, rec)
    if (data.UserId) data.UserId = parseInt(data.UserId, 10)
  }

  $q.dialog({
    component: UpdaterPersonnel,
    componentProps: {
      data: data,
      mode: mode,
    },
  }).onOk((r) => {
    selected.value = []
    if (mode === 'ins') {
      rows.value.push(r)
    } else {
      for (let key in r) {
        if (Object.prototype.hasOwnProperty.call(r, key)) {
          rec[key] = r[key]
        }
      }
    }
    selected.value.push(r)
    updateSelected()
  })
}

const removeRow = (rec) => {
  $q.dialog({
    title: proxy?.$t('confirmation'),
    message: proxy?.$t('deleteRecord') + '<div style="color: plum">(' + rec.fio + ')</div>',
    html: true,
    cancel: true,
    persistent: true,
    focus: 'cancel',
  })
    .onOk(() => {
      let index = rows.value.findIndex((row) => row.id === rec.id)
      api
        .post('', {
          method: 'data/deletePersonnel',
          params: [rec.own],
        })
        .then(() => {
          rows.value.splice(index, 1)
          selected.value = []
          notifySuccess(proxy?.$t('success'))
        })
        .catch((error) => {
          let msg = error.message
          if (error.response) msg = error.response.data.error.message
          console.error(msg)
        })
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t('canceled'))
    })
}

onMounted(() => {
  loading.value = true
  api
    .post('', {
      method: 'data/selectFV',
      params: ['Prop_UserSex'],
    })
    .then((response) => {
      optUserSex.value = response.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  loading.value = true
  api
    .post('', {
      method: 'data/selectUser',
      params: [],
    })
    .then((response) => {
      optUserId.value = response.data.result.records
    })
    .finally(() => {
      loading.value = false
    })

  fetchData(requestParam)
})
</script>

<style lang="sass">
.sticky-header-table
  height: calc(100vh - 150px)

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    top: 48px

  tbody
    scroll-margin-top: 48px
</style>
