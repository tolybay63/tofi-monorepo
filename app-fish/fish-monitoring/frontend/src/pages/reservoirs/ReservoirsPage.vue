<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[60, 100]"
      before-class="overflow-hidden"
      after-class="overflow-hidden q-pl-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 135px); width: 100%"
    >

      <template v-slot:before>
        <q-page class="q-pa-sm" style="height: 100px; width: 100% ">
          <q-table
            style="height: 98%; width: 100%"
            class="sticky-header-table"
            dense
            card-class="bg-amber-1 text-brown"
            row-key="obj"
            :columns="cols"
            :rows="rows"
            :wrap-cells="true"
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
                <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
              </q-td>
              <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
                {{ $t('infoRow') }}
              </q-td>
            </template>

            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                <q-avatar color="black" text-color="white" icon="sailing"></q-avatar>
                {{ $t('reservoirs') }}
              </div>

              <q-space/>
              <q-btn
                v-if="hasTarget('mon:vod:ins')"
                icon="post_add"
                dense
                color="secondary"
                :disable="loading"
                @click="editRowRefs(null, 'ins')"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('newRecord') }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="hasTarget('mon:vod:upd')"
                icon="edit"
                dense
                color="secondary"
                class="q-ml-sm"
                :disable="loading || selected.length === 0"
                @click="editRowRefs(selected[0], 'upd')"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('editRecord') }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="hasTarget('mon:vod:del')"
                icon="delete"
                dense
                color="red"
                class="q-ml-lg"
                :disable="loading || selected.length === 0"
                @click="removeRow(selected[0])"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('deletingRecord') }}
                </q-tooltip>
              </q-btn>

              <q-space/>

              <q-input
                dense
                debounce="300"
                color="primary"
                v-model="filter"
                :label="$t('txt_filter')"
              >
                <template v-slot:append>
                  <q-icon name="search"/>
                </template>
              </q-input>
            </template>

            <template v-slot:loading>
              <q-inner-loading showing color="secondary"/>
            </template>
          </q-table>
        </q-page>
      </template>

      <template v-slot:after>

        <ReservoirsMeter ref="ReservoirsMeter"></ReservoirsMeter>

      </template>
    </q-splitter>
  </div>
</template>

<script>
import {hasTarget, notifyError, notifyInfo, today} from 'src/utils/jsutils'
import {api} from 'boot/axios'
import {date, extend} from 'quasar'
import {ref} from 'vue'
import LifiInfo from 'pages/reservoirs/LifiInfo.vue'
import UpdaterReservoirRefs from 'pages/reservoirs/UpdaterReservoirRefs.vue'
import UpdaterReservoirMeterOld from 'pages/reservoirs/UpdaterReservoirMeter_old.vue'
import ReservoirsMeter from "pages/reservoirs/ReservoirsMeter.vue";

export default {
  name: 'ReservoirsPage',
  components: {ReservoirsMeter, LifiInfo},
  props: [],

  data: function () {
    return {
      splitterModel: 100,
      cols: [],
      rows: [],
      filter: '',
      selected: [],
      recUpd: {},
      loading: true,
      optFvReservoirType: new Map(),
      optFvReservoirStatus: new Map(),
      optFvFishFarmingType: new Map(),
      pagination: ref({
        page: 1,
        rowsPerPage: 25,
        rowsNumber: 0,
        descending: false,
        sortBy: 'name'
      }),



    }
  },

  methods: {
    hasTarget,

    editRowMeters(mode) {
      let data

      if (mode === "upd") {
        data = {
          didReservoirHydroLevel: this.recUpd.didReservoirHydroLevel,
          idReservoirHydroLevel: this.recUpd.idReservoirHydroLevel,
          ReservoirHydroLevel: this.recUpd.ReservoirHydroLevel,
          didWaterArea: this.recUpd.didWaterArea,
          idWaterArea: this.recUpd.idWaterArea,
          WaterArea: this.recUpd.WaterArea,
          didWaterAreaFishing: this.recUpd.didWaterAreaFishing,
          idWaterAreaFishing: this.recUpd.idWaterAreaFishing,
          WaterAreaFishing: this.recUpd.WaterAreaFishing,
          didWaterAreaLittoral: this.recUpd.didWaterAreaLittoral,
          idWaterAreaLittoral: this.recUpd.idWaterAreaLittoral,
          WaterAreaLittoral: this.recUpd.WaterAreaLittoral,
          obj: this.recUpd.obj
        }
      } else {
        data = {
          didReservoirHydroLevel: this.recUpd.didReservoirHydroLevel,
          didWaterArea: this.recUpd.didWaterArea,
          didWaterAreaFishing: this.recUpd.didWaterAreaFishing,
          didWaterAreaLittoral: this.recUpd.didWaterAreaLittoral,
          obj: this.recUpd.obj
        }
      }
      this.$q
        .dialog({
          component: UpdaterReservoirMeterOld,
          componentProps: {
            mode: mode,
            data: data,
            dte: this.dte,
            periodType: this.periodType
            // ...
          }
        })
        .onOk(() => {
          this.loadReservors()
        })
    },

    updateSelected() {
      let obj = 0

      if (this.selected.length > 0) {
        this.splitterModel = 60
        obj = this.selected[0].obj
      } else {
        this.splitterModel = 100
        obj = 0
        this.$refs.ReservoirsMeter.clearData()
      }
      this.$refs.ReservoirsMeter.loadReservoirsMeter(obj)
    },

    editRowRefs(row, mode) {
      let data = {accessLevel: 1}
      if (mode === 'upd') {
        data = extend(true, {}, row)
      }

      this.$q
        .dialog({
          component: UpdaterReservoirRefs,
          componentProps: {
            mode: mode,
            data: data,
            dte: this.dte,
            periodType: this.periodType
            // ...
          }
        })
        .onOk((r) => {
          //console.log('Ok! updated', r)
          if (mode === 'ins') {
            this.rows.push(r)
            this.selected = []
            this.selected.push(r)
          } else {
            Object.keys(row).forEach((key) => {
              row[key] = null
            })
            for (let key in r) {
              row[key] = r[key]
              /*
              if (r.hasOwnProperty(key)) {
                row[key] = r[key]
              }
*/
            }
          }
          this.updateSelected()
        })
    },

    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message:
            this.$t('deleteRecord') +
            '<div style="color: plum">(' +
            row.name +
            ' - ' +
            row.nameCls +
            ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel'
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1]
            })
            .then(() => {
              this.loadReservors()
              this.selected = []
              this.updateSelected()
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

    infoSelected(row) {
      return row ? ' ' + row.name + ' (' + row.nameCls + ')' : ""
    },

    getColumns() {
      return [
        {
          name: 'name',
          label: this.$t('fldName') + '*',
          field: 'name',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:20%'
        },
/*
        {
          name: 'nameCls',
          label: this.$t('vidWaterObject') + '*',
          field: 'nameCls',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%'
        },
*/

        {
          name: 'nameBranch',
          label: this.$t('struct_enterprise') + '*',
          field: 'nameBranch',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 25%'
        },

        {
          name: 'nameKATO',
          label: this.$t('kato') + '*',
          field: 'nameKATO',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 25%',
        },
        {
          name: 'fvReservoirType',
          label: this.$t('ReservoirType') + '*',
          field: 'fvReservoirType',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%',
          format: (v) => (this.optFvReservoirType ? this.optFvReservoirType[v] : null),
        },

        {
          name: 'fvReservoirStatus',
          label: this.$t('ReservoirStatus') + '*',
          field: 'fvReservoirStatus',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%',
          format: (v) => (this.optFvReservoirStatus ? this.optFvReservoirStatus[v] : null),
        },


      ]
    },

    loadReservors() {
      this.loading = true
      api
        .post('', {
          method: 'data/loadReservors',
          params: [{codTyp: 'Typ_WaterBodies', idObj: 0}]
        })
        .then((response) => {
          let obj = 0
          if (this.selected.length > 0) {
            obj = this.selected[0].obj
          }
          this.rows = response.data.result["records"]
          //console.info("rows", this.rows)
          if (obj > 0) {
            this.selected = []
            let sel = this.rows.filter((item) => {
              return item['obj'] === obj
            })
            //console.info("sel", sel)
            this.selected.push(sel[0])
            this.updateSelected()
          }
        })
        .catch((error) => {
          if (error.response.data.error.message.includes('@')) {
            let msgs = error.response.data.error.message.split('@')
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
            let msg = m1 + m2
            notifyError(msg)
          } else {
            notifyError(this.$t(error.response.data.error.message))
          }
        })
        .finally(() => {
          //setTimeout(()=> {
          this.loading = false
          //}, 3000)

        })
    }
  },

  created() {
    this.cols = this.getColumns()
    //
    api
      .post('', {
        method: 'data/loadPeriodType',
        params: [],
      })
      .then(
        (response) => {
          this.optPeriod = response.data.result["records"]
        })
      .finally(() => {
        this.loading = false
      })
    //
    api
      .post('', {
        method: 'data/loadFvReservoirTypeAsMap',
        params: ['Prop_ReservoirType']
      })
      .then(
        (response) => {
          this.optFvReservoirType = response.data.result
        })
      .finally(() => {
      })
    //
    api
      .post('', {
        method: 'data/loadFvReservoirStatusAsMap',
        params: ['Prop_ReservoirStatus']
      })
      .then(
        (response) => {
          this.optFvReservoirStatus = response.data.result
        })
      .finally(() => {
      })
    //
    api
      .post('', {
        method: 'data/loadFvFishFarmingTypeAsMap',
        params: ['Prop_FishFarmingType']
      })
      .then(
        (response) => {
          this.optFvFishFarmingType = response.data.result
        })
      .finally(() => {
        this.loadReservors()
      })
    //
  },

  setup() {
    return {}
  }
}
</script>

<style lang="sass">
.sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 140px)
  /* bg color is important for th; just specify one #bdbdbd #607d8b */
  background-color: #607d8b

  thead tr th
    position: sticky
    z-index: 1

  thead tr:first-child th
    top: 0

  &.q-table--loading thead tr:last-child th
    /* height of all previous header rows */
    top: 48px

  /* prevent scrolling behind sticky top row on focus */
  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px

</style>

<!--<style scoped>
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
.sticky-header-table .q-table&#45;&#45;bordered {
  border-top: none;
}
</style>-->
