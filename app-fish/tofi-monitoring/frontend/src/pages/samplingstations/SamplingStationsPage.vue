<template>
  <div class="q-pa-none">
    <q-table
      style="height: calc(100vh - 200px); width: 100%"
      class="my-sticky-header-table"
      color="primary"
      dense
      card-class="bg-amber-1 text-brown"
      row-key="obj"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="horizontal"
      :filter="filter"
      :loading="loading"
      selection="single"
      v-model:selected="selected"
      @update:selected="updateSelected"
      :rows-per-page-options="[25, 0]"
    >
      <template #bottom-row>
        <q-td colspan="100%" v-if="selected.length > 0">
          <span class="text-blue"> {{ $t('selectedRow') }}: </span>
          <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
        </q-td>
        <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
          {{ $t('infoRow') }}
        </q-td>
      </template>

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="houseboat"> </q-avatar>
          {{ $t('samplingStations') }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mon:st:ins')"
          icon="post_add"
          dense
          color="secondary"
          :disable="loading"
          @click="editRow(null, 'ins')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('newRecord') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mon:st:upd')"
          icon="edit"
          dense
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editRow(selected[0], 'upd')"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('editRecord') }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mon:st:del')"
          icon="delete"
          dense
          color="secondary"
          class="q-ml-lg"
          :disable="loading || selected.length === 0"
          @click="removeRow(selected[0])"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t('deletingRecord') }}
          </q-tooltip>
        </q-btn>

        <q-space />

        <q-input
          dense
          debounce="300"
          color="primary"
          v-model="filter"
          :label="$t('txt_filter')"
        >
          <template v-slot:append>
            <q-icon name="search" />
          </template>
        </q-input>
      </template>

      <template #loading>
        <q-inner-loading showing color="secondary"></q-inner-loading>
      </template>
    </q-table>
  </div>
</template>

<script>
import {extend} from 'quasar'
import { api, baseURL } from 'boot/axios'
import { hasTarget, notifyError, notifyInfo } from 'src/utils/jsutils'
import UpdaterSamplingStation from 'pages/samplingstations/UpdaterSamplingStation.vue'

export default {
  name: 'SamplingStationsPage',
  props: [],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: '',
      selected: [],
      loading: false,
      mapBranch: new Map(),
      mapReservoir: new Map(),
    }
  },

  methods: {
    hasTarget,
    updateSelected() {},

    editRow(row, mode) {
      let data = { accessLevel: 1 }
      if (mode === 'upd') {
        data = extend(true, {}, row)
      }

      this.$q
        .dialog({
          component: UpdaterSamplingStation,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === 'ins') {
            this.rows.push(r)
            this.selected = []
            this.selected.push(r)
          } else {
            for (let key in r) {
              row[key] = r[key]
              /*
              if (r.hasOwnProperty(key)) {
                row[key] = r[key]
              }
*/
            }
          }
        })
    },

    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '<div style="color: plum">(' + row.name + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          this.$axios
            .post(baseURL, {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1],
            })
            .then(() => {
              this.loadSamplingStations()
              this.selected = []
            })
            .catch((error) => {
              //console.log(error.message)
              if (error.response.data.error.message.includes('@')) {
                let msgs = error.response.data.error.message.split('@')
                let m1 = msgs[0]
                let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
                let msg = ''
                if (m1 === 'existsSampling') {
                  msg = `
                  Заборы проб:
                  Существует - ${m2}
                  `
                }
                notifyError(msg)
              } else {
                notifyError(this.$t(error.response.data.error.message))
              }
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

    loadSamplingStations() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadSamplingStations',
          params: [{ codCls: 'Cls_StationSampling', isRec: false, idObj: 0 }],
        })
        .then(
          (response) => {
            this.rows = response.data.result.records
          },
          (error) => {
            let msg = error.message
            if (error.response) msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .finally(() => {
          this.loading = false
        })
    },

    getColumns() {
      return [
        {
          name: 'objBranch',
          label: this.$t('branch')+"*",
          field: 'objBranch',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:20%',
          format: (v) => (this.mapBranch ? this.mapBranch.get(v) : null),
        },
        {
          name: 'objReservoirShore',
          label: this.$t('reservoir')+"*",
          field: 'objReservoirShore',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 25%',
          format: (v) => (this.mapReservoir ? this.mapReservoir.get(v) : null),
        },
        {
          name: 'name',
          label: this.$t('fldName')+"*",
          field: 'name',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 25%',
        },
        {
          name: 'cmt',
          label: this.$t('fldCmt'),
          field: 'cmt',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 30%',
        },
      ]
    },

    infoSelected(row) {
      return ' ' + row.name
    },
  },

  created() {
    this.cols = this.getColumns()
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadBranchName',
        params: ['Cls_Branch'],
      })
      .then(
        (response) => {
          response.data.result.records.forEach((it) => {
            this.mapBranch.set(it["id"], it["name"])
          })
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post(baseURL, {
        method: 'data/loadReservoirName',
        params: [],
      })
      .then(
        (response) => {
          response.data.result.records.forEach((it) => {
            this.mapReservoir.set(it["id"], it["name"])
          })
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })
    //
    this.loadSamplingStations()
  },
}
</script>

<style lang="sass">
.my-sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 190px)

  .q-table__top,
  .q-table__bottom,
  thead tr:first-child th
    /* bg color is important for th; just specify one */
    background-color: #bdbdbd

  thead tr th
    position: sticky
    z-index: 1
  thead tr:first-child th
    top: 0

  /* this is when the loading indicator appears */
  &.q-table--loading thead tr:last-child th
    /* height of all previous header rows */
    top: 48px

  /* prevent scrolling behind sticky top row on focus */
  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px
</style>

