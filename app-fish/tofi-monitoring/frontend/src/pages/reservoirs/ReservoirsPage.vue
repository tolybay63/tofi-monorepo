<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[70, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
    >

      <template v-slot:before>
        <q-table
          style="height: calc(100vh - 140px); width: 100%"
          class="my-sticky-header-table"

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

            <q-space />
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
              color="secondary"
              class="q-ml-lg"
              :disable="loading || selected.length === 0"
              @click="removeRow(selected[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('deletingRecord') }}
              </q-tooltip>
            </q-btn>

            <q-input
              v-model="dte"
              :label="$t('date')"
              :model-value="dte"
              class="q-ml-lg"
              dense
              stack-label
              style="width: 100px"
              type="date"
              @update:model-value="fnDt"
            />

            <!--  PeriodType  -->
            <q-select
              class="q-ml-lg"
              v-model="periodType"
              :model-value="periodType"
              dense
              options-dense
              :options="optPeriod"
              :label="fnReqLabel('periodType')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectPeriodType"
              style="width: 100px"
            />


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

          <template v-slot:loading>
            <q-inner-loading showing color="secondary" />
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <q-banner dense class="text-bold text-white bg-grey-13" inline-actions>
          <div class="row">
            {{ $t('other_props') }}
          </div>
          <template v-slot:action>
            <q-btn
              icon="post_add"
              dense
              color="secondary"
              @click="editRowMeters('ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('newRecord') }}
              </q-tooltip>
            </q-btn>



            <q-btn
              icon="edit"
              dense
              color="secondary"
              class="q-ml-sm"
              @click="editRowMeters('upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('editRecord') }}
              </q-tooltip>
            </q-btn>

          </template>

        </q-banner>

        <q-card class="bg-amber-1 text-brown">
          <q-card-section v-if="selected.length > 0">
            <!-- F_ReservoirType -->
            <q-select
              v-model="recUpd.fvReservoirType"
              :model-value="recUpd.fvReservoirType"
              :label="$t('typeReservoir')"
              :options="optFvReservoirType"
              dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-sm"
              readonly
              stack-label
            />
            <!-- F_ReservoirStatus -->
            <q-select
              v-model="recUpd.fvReservoirStatus"
              :model-value="recUpd.fvReservoirStatus"
              :label="$t('statusReservoir')"
              :options="optFvReservoirStatus"
              dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-sm"
              readonly
              stack-label
            />

            <!-- F_FishFarmingType -->
            <q-select
              v-model="recUpd.fvFishFarmingType"
              :model-value="recUpd.fvFishFarmingType"
              :label="$t('fishFarmingType')"
              :options="optFvFishFarmingType"
              dense
              map-options
              option-label="name"
              option-value="id"
              class="q-ma-sm"
              readonly
              stack-label
            />

            <!--Prop_WaterArea-->
            <q-input
              :model-value="recUpd.WaterArea"
              stack-label
              v-model="recUpd.WaterArea"
              readonly
              class="q-ma-sm"
              :label="$t('WaterArea')"
              type="number"
              clearable
              dense
            >
              <template v-slot:after>
                <q-btn round dense flat size="sm" color="purple" icon="help_outline">
                  <q-menu auto-close>
                    <div>
                      <lifi-info :field="'WaterArea'" :rec="recUpd"></lifi-info>
                    </div>
                  </q-menu>
                </q-btn>
              </template>
            </q-input>

            <!--Prop_WaterAreaFishing-->
            <q-input
              :model-value="recUpd.WaterAreaFishing"
              stack-label
              v-model="recUpd.WaterAreaFishing"
              readonly
              class="q-ma-sm"
              :label="$t('WaterAreaFishing')"
              type="number"
              dense
            >
              <template v-slot:after>
                <q-btn round dense flat size="sm" color="purple" icon="help_outline">
                  <q-menu auto-close>
                    <div>
                      <lifi-info :field="'WaterAreaFishing'" :rec="recUpd"></lifi-info>
                    </div>
                  </q-menu>
                </q-btn>
              </template>
            </q-input>

            <!-- Prop_WaterAreaLittoral-->
            <q-input
              :model-value="recUpd.WaterAreaLittoral"
              stack-label
              v-model="recUpd.WaterAreaLittoral"
              readonly
              class="q-ma-sm"
              :label="$t('WaterAreaLittoral')"
              type="number"
              dense
            >
              <template v-slot:after>
                <q-btn round dense flat size="sm" color="purple" icon="help_outline">
                  <q-menu auto-close>
                    <div>
                      <lifi-info :field="'WaterAreaLittoral'" :rec="recUpd"></lifi-info>
                    </div>
                  </q-menu>
                </q-btn>
              </template>
            </q-input>

            <!-- Prop_ReservoirHydroLevel-->
            <q-input
              :model-value="recUpd.ReservoirHydroLevel"
              stack-label
              v-model="recUpd.ReservoirHydroLevel"
              readonly
              class="q-ma-sm"
              :label="$t('ReservoirHydroLevel')"
              type="number"
              dense
            >
              <template v-slot:after>
                <q-btn round dense flat size="sm" color="purple" icon="help_outline">
                  <q-menu auto-close>
                    <div>
                      <lifi-info :field="'ReservoirHydroLevel'" :rec="recUpd"></lifi-info>
                    </div>
                  </q-menu>
                </q-btn>
              </template>
            </q-input>

          </q-card-section>
        </q-card>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import { hasTarget, notifyError, notifyInfo, today } from 'src/utils/jsutils'
import { api, baseURL } from 'boot/axios'
import { extend } from 'quasar'
import { ref } from 'vue'
import LifiInfo from 'pages/reservoirs/LifiInfo.vue'
import UpdaterReservoirRefs from 'pages/reservoirs/UpdaterReservoirRefs.vue'
import UpdaterReservoirMeter from 'pages/reservoirs/UpdaterReservoirMeter.vue'

export default {
  name: 'ReservoirsPage',
  components: { LifiInfo },
  props: [],

  data: function() {
    return {
      splitterModel: 100,
      cols: [],
      rows: [],
      filter: '',
      selected: [],
      recUpd: {},
      loading: true,
      optFvReservoirType: [],
      optFvReservoirStatus: [],
      optFvFishFarmingType: [],
      pagination: ref({
        page: 1,
        rowsPerPage: 15,
        rowsNumber: 0,
        descending: false,
        sortBy: 'name'
      }),
      dte: today(),
      periodType: 71,
      optPeriod: []

    }
  },

  methods: {
    hasTarget,

    fnSelectPeriodType(v) {
      console.log(v, this.periodType)
    },

    fnReqLabel(label) {
      return this.$t(label)+"*"
    },

    editRowMeters(mode) {
      let data

      if (mode==="upd") {
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
          component: UpdaterReservoirMeter,
          componentProps: {
            mode: mode,
            data: data,
            dte: this.dte,
            periodType: this.periodType
            // ...
          }
        })
        .onOk((r) => {

          //console.info("UpdMeter", r)
          this.loadReservors()

/*
          this.recUpd.WaterArea = r.WaterArea
          this.recUpd.WaterAreaFishing = r.WaterAreaFishing
          this.recUpd.WaterAreaLittoral = r.WaterAreaLittoral
          this.recUpd.ReservoirHydroLevel = r.ReservoirHydroLevel
*/

        })
    },

    updateSelected() {
      if (this.selected.length > 0) {
        //console.info(this.selected[0]);
        this.splitterModel = 70
        this.recUpd = extend(true, {}, this.selected[0], {dte: this.dte})
      } else
        this.splitterModel = 100
    },

    fnDt(val) {
      this.dte = val
      this.loadReservors()

      if (this.selected.length > 0) {
        this.recUpd.dte = this.dte
      }
    },

    editRowRefs(row, mode) {
      let data = { accessLevel: 1 }
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
          this.$axios
            .post(baseURL, {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1]
            })
            .then(() => {
              this.loadReservors()
              this.selected = []
              this.updateSelected()
            })
            .catch((error) => {
              console.log(error.message)
              if (error.response.data.error)
                notifyError(error.response.data.error.message)
              else
                notifyError(error.message)
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
        {
          name: 'nameCls',
          label: this.$t('vidWaterObject') + '*',
          field: 'nameCls',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%'
        },

        {
          name: 'nameRegion',
          label: this.$t('region') + '*',
          field: 'nameRegion',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 25%'
        },
        {
          name: 'nameDistrict',
          label: this.$t('district'),
          field: 'nameDistrict',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%'
        },
        {
          name: 'nameBranch',
          label: this.$t('branch') + '*',
          field: 'nameBranch',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%'
        }
      ]
    },

    loadReservors() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadReservors',
          params: [{ codTyp: 'Typ_WaterBodies', isRec: false, idObj: 0, dte: this.dte, periodType: this.periodType }]
        })
        .then((response) => {
          let obj = 0
          if (this.selected.length > 0) {
            obj = this.selected[0].obj
          }
          this.rows = response.data.result.records
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
          //console.info("this.rows", this.rows)
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
      .post(baseURL, {
        method: 'data/loadPeriodType',
        params: [],
      })
      .then(
        (response) => {
          this.optPeriod = response.data.result.records
        })
      .catch(error => {
        notifyError(error.message)
      })
      .finally(() => {
        this.loading = false
      })

    api
      .post(baseURL, {
        method: 'data/loadFvReservoirType',
        params: ['Factor_ReservoirType']
      })
      .then(
        (response) => {
          this.optFvReservoirType = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
      })
    //
    api
      .post(baseURL, {
        method: 'data/loadFvReservoirStatus',
        params: ['Factor_ReservoirStatus']
      })
      .then(
        (response) => {
          this.optFvReservoirStatus = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
      })
    //
    api
      .post(baseURL, {
        method: 'data/loadFvFishFarmingType',
        params: ['Factor_FishFarmingType']
      })
      .then(
        (response) => {
          this.optFvFishFarmingType = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loadReservors()
      })
  },

  setup() {
    return {}
  }
}
</script>

<style lang="sass">
.my-sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 190px)

  .q-table__top,
  .q-table__bottom,
  thead tr:first-child th
    /* bg color is important for th; just specify one #00b4ff #bdbdcb*/
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
