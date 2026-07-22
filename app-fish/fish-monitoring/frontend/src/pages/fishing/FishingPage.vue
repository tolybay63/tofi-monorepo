<template>
  <div class="q-pa-sm">

    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[70, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 135px); width: 100%"
    >

      <template v-slot:before>
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
              <q-avatar color="black" text-color="white" icon="location_on"></q-avatar>
              {{ $t('fishing') }}
            </div>

            <q-space/>
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

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <FishFecundityPage ref="FishFecundity" :name="name"></FishFecundityPage>

      </template>

    </q-splitter>
  </div>


</template>

<script>
import {api} from 'boot/axios'
import {hasTarget, notifyInfo, today} from 'src/utils/jsutils'
import {date, extend} from 'quasar'
import FishFecundityPage from "pages/piscesreservoirs/FishFecundityPage.vue";
import UpdaterFishingRefs from "pages/fishing/UpdaterFishingRefs.vue";

export default {
  name: 'PiscesReservoirsPage',
  components: {FishFecundityPage},
  props: [],

  data: function () {
    return {

      splitterModel: 100,
      cols: [],
      rows: [],
      filter: '',
      selected: [],
      loading: false,
      name: "",

      mapFishLocation: new Map(),
      mapFishGear: new Map(),
      mapFishManager: new Map(),
      mapFishParticipants: new Map(),

    }
  },

  methods: {
    hasTarget,
    updateSelected() {
      let obj = 0

      if (this.selected.length > 0) {
        //console.info(this.selected[0]);
        this.splitterModel = 70
        obj = this.selected[0].obj
        this.name = this.selected[0].nameCls + " - " + this.selected[0].StartDate
      } else {
        this.splitterModel = 100
        obj = 0
        this.name = ""
        this.$refs.FishFecundity.clearData()
      }
      this.$refs.FishFecundity.loadFishFecundity(obj)

    },

    editRow(row, mode) {
      let data = {accessLevel: 1, StartDate: new Date().toLocaleDateString()}
      if (mode === 'upd') {
        data = extend(true, {}, row)
      }

      this.$q
        .dialog({
          component: UpdaterFishingRefs,
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
              if (r.hasOwnProperty(key)) {
                row[key] = r[key]
              }
            }
            if (r["FishSpawPeriod"] !== null)
              row["FishSpawPeriod"] = r["FishSpawPeriod"]
            if (r["FishStartPuberty"] !== null)
              row["FishStartPuberty"] = r["FishStartPuberty"]
            if (r["FishEndPuberty"] !== null)
              row["FishEndPuberty"] = r["FishEndPuberty"]
            if (r["FishSpawFrequency"] !== null)
              row["FishSpawFrequency"] = r["FishSpawFrequency"]
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
              method: 'data/deletePiscesReservoir',
              params: [row.relobj],
            })
            .then(() => {
              this.loadData()
              this.selected = []
            })
            .catch((error) => {
              console.log(error.message)
            })
        })
        .onCancel(() => {
          notifyInfo(this.$t('canceled'))
        })
    },

    loadData() {
      this.loading = true
      api
        .post('', {
          method: 'data/loadFishing',
          params: [0],
        })
        .then(
          (response) => {
            this.rows = response.data.result["records"]
          })
        .finally(() => {
          //setTimeout(()=> {
          this.loading = false
          //}, 3000)
        })
    },

    //fishingType: "Тип ловли",
    //StartDate: "Дата начала вылова",
    //FishLocation: "Место вылова",
    //AreaOfTon: "Площадь тони, га",
    //FishGear: 'Орудие лова',
    //fishManager: "Руководитель вылова",
    //FishParticipants: "Участники вылова",
    getColumns() {
      return [
        {
          name: 'fishingType',
          label: this.$t('fishingType') + "*",
          field: 'fishingType',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
          //format: (v) => (this.mapReservoir ? this.mapReservoir.get(v) : null),
        },
        {
          name: 'StartDate',
          label: this.$t('StartDate') + "*",
          field: 'StartDate',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
          //format: (v) => (this.mapTypeOfFish ? this.mapTypeOfFish.get(v) : null),
        },
        {
          name: 'FishLocation',
          label: this.$t('FishLocation'),
          field: 'FishLocation',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
        },
        {
          name: 'AreaOfTon',
          label: this.$t('AreaOfTon'),
          field: 'AreaOfTon',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
        },
        {
          name: 'FishGear',
          label: this.$t('FishGear'),
          field: 'FishGear',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
        },
        {
          name: 'fishManager',
          label: this.$t('fishManager'),
          field: 'fishManager',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 20%',
        },
        {
          name: 'FishParticipants',
          label: this.$t('FishParticipants'),
          field: 'FishParticipants',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 30%',
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
        method: 'data/loadFishLocationForSelect',
        params: ["Prop_FishLocation"],
      })
      .then(
        (response) => {
          response.data.result.records.forEach((it) => {
            this.mapFishLocation.set(it["id"], it["name"])
          })
          console.info("mapFishLocation", this.mapFishLocation)
        })
      .finally(() => {
        this.loading = false
      })
    //



    //
    setTimeout(() => {
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
