<template>
  <div class="q-pa-sm">
    <q-table
      style="height: calc(100vh - 140px); width: 100%"
      class="my-sticky-header-table"
      color="primary"
      dense
      card-class="bg-amber-1 text-brown"
      row-key="relobj"
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
          <q-avatar color="black" text-color="white" icon="tsunami"> </q-avatar>
          {{ $t('piscesInReservoirs') }}
        </div>

        <q-space />
        <q-btn
          v-if="hasTarget('mon:rpv:ins')"
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
          v-if="hasTarget('mon:rpv:upd')"
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
          v-if="hasTarget('mon:rpv:del')"
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
          v-if="hasTarget('mon:rpv:sel')"
          dense="dense"
          :disable="loading || selected.length === 0"
          class="q-ml-lg"
          color="secondary"
          icon="pan_tool_alt"
          @click="ReservoirFishSelect()"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("chooseRecord") }}
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
import {api} from 'boot/axios'
import {hasTarget, notifyError, notifyInfo} from 'src/utils/jsutils'
import {extend} from 'quasar'
import UpdaterPiscesReservoir from 'pages/piscesreservoirs/UpdaterPiscesReservoir.vue'
import FishFecundityPage from "pages/piscesreservoirs/FishFecundityPage.vue";

export default {
  name: 'PiscesReservoirsPage',
  props: [],

  data: function () {
    return {
      cols: [],
      rows: [],
      filter: '',
      selected: [],
      loading: false,

      mapReservoir: new Map(),
      mapTypeOfFish: new Map(),
    }
  },

  methods: {
    hasTarget,

    ReservoirFishSelect() {
      this.$q
        .dialog({
          component: FishFecundityPage,
          componentProps: {
            relobj: this.selected[0].relobj,
            name: this.mapReservoir.get(this.selected[0].reservoir) + " - " + this.mapTypeOfFish.get(this.selected[0].typeOfFish),
            // ...
          },
        })
        .onOk(() => {
        })
    },


    editRow(row, mode) {
      let data = { accessLevel: 1 }
      if (mode === 'upd') {
        data = extend(true, {}, row)
      }

      this.$q
        .dialog({
          component: UpdaterPiscesReservoir,
          componentProps: {
            mode: mode,
            data: data,
            // ...
          },
        })
        .onOk((r) => {
          console.log("Ok! updated", r);
          if (mode === 'ins') {
            this.rows.push(r)
            this.selected = []
            this.selected.push(r)
          } else {
            for (let key in r) {
              //row[key] = r[key]
              if (r.hasOwnProperty(key)) {
                row[key] = r[key]
              }
            }
          }
        })
    },

    removeRow(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message:
            this.$t('deleteRecord') +
            '<div style="color: plum">(' +
            this.mapReservoir.get(row.reservoir) +
            ' - ' +
            this.mapTypeOfFish.get(row.typeoffish) +
            ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          api
            .post('', {
              //method: 'data/deleteFishInResoirvoir',
              method: 'data/deleteOwnerWithProperties',
              params: [row.relobj, 0],
            })
            .then(() => {
              this.loadData()
              this.selected = []
            })
/*
            .catch((error) => {
              console.log(error.message)
              let msg = error.message
              if (error.response) {
                msg = error.response.data.error.message
                if (msg==="existsFishInResoirvoir")
                  msg = "["+this.mapTypeOfFish.get(row.typeoffish)+"] используется в результатах исследования"
              }
              notifyError(msg)
            })
*/
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

    loadData() {
      this.loading = true
      api
        .post('', {
          method: 'data/loadPiscesReservoir',
          params: [{ codRelTyp: 'RelTyp_FishReservoir' }],
        })
        .then(
          (response) => {
            this.rows = response.data.result["records"]

            console.info("rows", this.rows)
          })
        .finally(() => {
          //setTimeout(()=> {
            this.loading = false
          //}, 3000)
        })
    },

    getColumns() {
      return [
        {
          name: 'reservoir',
          label: this.$t('reservoir')+"*",
          field: 'reservoir',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%',
          format: (v) => (this.mapReservoir ? this.mapReservoir.get(v) : null),
        },
        {
          name: 'typeOfFish',
          label: this.$t('typeOfFish')+"*",
          field: 'typeOfFish',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%',
          format: (v) => (this.mapTypeOfFish ? this.mapTypeOfFish.get(v) : null),
        },
        {
          name: 'FishSpawPeriod',
          label: this.$t('FishSpawPeriod'),
          field: 'FishSpawPeriod',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%',
        },
        {
          name: 'FishStartPuberty',
          label: this.$t('FishStartPuberty'),
          field: 'FishStartPuberty',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%',
        },
        {
          name: 'FishEndPuberty',
          label: this.$t('FishEndPuberty'),
          field: 'FishEndPuberty',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%',
        },
        {
          name: 'FishSpawFrequency',
          label: this.$t('FishSpawFrequency'),
          field: 'FishSpawFrequency',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 15%',
        },
      ]
    },

    infoSelected(row) {
      return (
        ' ' +
        this.mapReservoir.get(row.reservoir) +
        ' (' +
        this.mapTypeOfFish.get(row.typeoffish) +
        ')'
      )
    },
  },

  created() {
    this.cols = this.getColumns()
    this.loading = true
    api
      .post('', {
        method: 'data/loadReservoir',
        params: ["Typ_WaterBodies"],
      })
      .then(
        (response) => {
          response.data.result.records.forEach((it) => {
            this.mapReservoir.set(it["id"], it["name"])
          })
        })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadTypeOfFish',
        params: ['Typ_Fish'],
      })
      .then(
        (response) => {
          response.data.result.records.forEach((it) => {
            this.mapTypeOfFish.set(it["id"], it["name"])
          })
        })
      .finally(() => {
        this.loading = false
      })
    //
    setTimeout(()=> {
      this.loadData()
    }, 200)

  },
}
</script>

<style lang="sass">
.my-sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 190px)

  thead tr:first-child th
    /* bg color is important for th; just specify one  #bdbdbd*/
    background-color: #607d8b

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
