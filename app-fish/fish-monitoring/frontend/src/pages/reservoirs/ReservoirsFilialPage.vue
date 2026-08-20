<template>
  <div class="q-pa-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[40, 100]"
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
                {{ $t('reservoirs') }} ( {{nameBranch}} )
              </div>

              <q-space/>

              <q-btn
                dense round color="secondary" icon="arrow_back" glossy
                @click="toBack()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("back") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mon:vod:ins')"
                icon="post_add"
                dense class="q-ml-sm"
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

              <q-btn
                icon="insert_chart_outlined"
                color="secondary"
                class="q-ml-sm" dense
                :disable="loading || selected.length === 0"
                @click="showChart(selected[0])"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('charts') }}
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
import {hasTarget, notifyError, notifyInfo} from 'src/utils/jsutils'
import {api} from 'boot/axios'
import {extend, useQuasar} from 'quasar'
import {ref} from 'vue'
import ReservoirsMeter from "pages/reservoirs/ReservoirsMeter.vue";
import ChartViewPage from "components/ChartViewPage.vue";
import UpdaterReservoirFilialRefs from "pages/reservoirs/UpdaterReservoirFilialRefs.vue";

export default {
  name: 'ReservoirsFilialPage',
  components: {ReservoirsMeter},
  props: ["filial"],

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
      nameBranch: "",
      pvBranch: null,
      objBranch: null,
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

    toBack() {
      this.$router["push"]({
        name: "FilialsPage",
      });
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

    showChart(row) {
      this.$q.dialog({
        component: ChartViewPage,
        componentProps: {
          owner: row.obj,
          ownerName: row.name,
          meter: 1007
        }
      }).onOk(() => {
        console.log('Диалог закрыт с OK');
      }).onCancel(() => {
        console.log('Диалог отменен');
      });
    },

    editRowRefs(row, mode) {
      let data = {accessLevel: 1, objBranch: this.objBranch, pvBranch: this.pvBranch}
      if (mode === 'upd') {
        data = extend(true, {}, row)
      }

      this.$q
        .dialog({
          component: UpdaterReservoirFilialRefs,
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
            row.name + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel'
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: 'data/deleteReservoir',
              params: [row.obj]
            })
            .then(() => {
              this.loadReservoirs(this.objBranch)
              this.selected = []
              this.updateSelected()
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

    infoSelected(row) {
      return row ? ' ' + row.name + ' (' + row.nameBranch + ')' : ""
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
          headerStyle: 'font-size: 1.2em; width:30%'
        },

        {
          name: 'nameKATO',
          label: this.$t('kato2') + '*',
          field: 'nameKATO',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 35%',
        },
        {
          name: 'fvReservoirType',
          label: this.$t('ReservoirType') + '*',
          field: 'fvReservoirType',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 18%',
          format: (v) => (this.optFvReservoirType ? this.optFvReservoirType[v] : null),
        },

        {
          name: 'fvReservoirStatus',
          label: this.$t('ReservoirStatus') + '*',
          field: 'fvReservoirStatus',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 17%',
          format: (v) => (this.optFvReservoirStatus ? this.optFvReservoirStatus[v] : null),
        },


      ]
    },

    loadReservoirs(filial) {
      this.loading = true
      api
        .post('', {
          method: 'data/loadReservoirsFilial',
          params: [{codTyp: 'Typ_WaterBodies', idObj: 0, filial: filial}]
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
          console.error(error.response?.data?.error.message)
        })
        .finally(() => {
          //setTimeout(()=> {
          this.loading = false
          //}, 3000)

        })
    }
  },

  mounted() {
    const filial = parseInt(this.$route["params"].filial, 10);
    this.loadReservoirs(filial);
    api
      .post('', {
        method: 'data/getBranchInfo',
        params: [filial],
      })
      .then(
        (response) => {
          this.nameBranch = response.data.result.name
          this.pvBranch = response.data.result.pv
          this.objBranch = filial
        })
    //
  },



  created() {
    this.loading = true
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
    this.loading = true
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
        this.loading = false
      })
    //
    this.loading = true
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
        this.loading = false
      })
    //
    this.loading = true
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
        this.loading = false
      })
    //
  },

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

