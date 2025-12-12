<template>
  <div class="no-padding no-margin" style="height: calc(100vh - 190px)">
    <q-splitter
      v-model="splitterModel2"
      :model-value="splitterModel2"
      :limits="[80, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <q-table
          style="max-height: 900px; width: 100%"
          class="my-sticky-header-table"
          color="primary"
          dense
          card-class="bg-amber-1 text-brown"
          row-key="obj"
          :columns="cols2"
          :rows="rows2"
          :filter="filter"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading2"
          selection="single"
          v-model:selected="selected2"
          @update:selected="updateSelect2"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected2.length > 0">
              <span class="text-blue"> {{ $t('selectedRow') }}: </span>
              <span class="text-bold"> {{ this.infoSelected2(selected2[0]) }} </span>
            </q-td>
            <q-td colspan="100%" v-else-if="this.rows2.length > 0" class="text-bold">
              {{ $t('infoRowExt') }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold">
              <div v-if="gridMode==='count'">
                {{ $t("gridModeCount") }}
              </div>
              <div v-else>
                {{ $t("gridModeMassa") }}
              </div>

            </div>

            <q-space />
            <q-btn
              v-if="hasTarget(tgIns)"
              icon="post_add"
              dense
              color="secondary"
              :disable="loading2"
              @click="editObjProps(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('newRecord') }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget(tgUpd)"
              icon="edit"
              dense
              color="secondary"
              class="q-ml-sm"
              :disable="loading2 || selected2.length === 0"
              @click="editObjProps(selected2[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('editRecord') }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget(tgDel)"
              icon="delete"
              dense
              color="secondary"
              class="q-ml-sm q-mr-xl"
              :disable="loading2 || selected2.length === 0"
              @click="removeObjProps(selected2[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('deletingRecord') }}
              </q-tooltip>
            </q-btn>

            <q-radio v-model="gridMode" checked-icon="task_alt" unchecked-icon="panorama_fish_eye" val="count"
                     label="Количественное соотношение" @update:model-value="fnRadio"/>
            <q-radio v-model="gridMode" checked-icon="task_alt" unchecked-icon="panorama_fish_eye" val="massa"
                     label="Весовое соотношение" @update:model-value="fnRadio"/>

            <q-space />

          </template>

          <template v-slot:body="props">
<!--            <q-tr :props="props" :key="`e_${props.row.index}`">
              <q-td colspan="100%">
                <span> {{$t("executor")}}: </span>
                <span class="text-bold">{{ props.row.nameSampleExecutor }}</span>
              </q-td>
            </q-tr>-->
            <q-tr :props="props" :key="`m_${props.row.index}`">
              <q-td>
                <q-btn
                  dense
                  flat
                  color="blue"
                  :icon="
                      selected2.length === 1 && props.row.obj === selected2[0].obj
                        ? 'check_box'
                        : 'check_box_outline_blank'
                    "
                  @click.stop="selectedRow(props.row)"
                >
                </q-btn>
              </q-td>
              <q-td
                v-for="col in props.cols"
                :key="col.name"
                :props="props"
              >
                <div v-if="fnCell(props.row, col.name)">

                  <q-btn
                    color="primary" round size="sm" flat dense icon="more_vert" class="absolute-right"
                  >
                    <q-menu auto-close>
                      <q-btn
                        round size="sm" icon="edit" color="blue" flat dense
                        @click="fnEditCell(props.row, col)" class="no-padding no-margin"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("update") }}
                        </q-tooltip>
                      </q-btn>

                      <q-btn
                        round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                        @click="fnDeleteCell(props.row, col)"
                        :disable="props.row[col.name]===undefined"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("deletingRecord") }}
                        </q-tooltip>
                      </q-btn>
                    </q-menu>
                  </q-btn>

                  {{ props.row[col.name] }}
                </div>

                <div v-else>
                  <div v-if="col.field==='objFishGear'">
                  {{ mapFishGear.get(col.value) }}
                  </div>
                  <div v-else>
                    {{ col.value }}
                  </div>
                </div>

              </q-td>
            </q-tr>
          </template>

        </q-table>
      </template>

      <template v-slot:after>
        <q-banner dense class="text-bold text-white bg-blue-grey-13">
          <div class="row">
            {{ $t('other_props') }}
          </div>
        </q-banner>

        <q-card class="bg-amber-1 text-brown">
          <q-card-section v-if="selected2.length > 0">
            <!-- Branch -->
            <q-select
              v-model="recUpd.objBranch"
              :model-value="recUpd.objBranch"
              :label="fmReqLabel('branch')"
              :options="optBranch"
              dense
              map-options
              option-label="name"
              option-value="id"
              class="q-mb-md"
              readonly
            />

            <!-- Reservoir -->
            <q-input
              v-model="recUpd['nameReservoir']"
              :model-value="recUpd['nameReservoir']"
              :label="fmReqLabel('reservoir')"
              class="q-mb-md"
              readonly
            />

            <!-- Executor -->
            <q-input
              v-model="recUpd['nameSampleExecutor']"
              :model-value="recUpd['nameSampleExecutor']"
              :label="$t('executor')"
              class="q-mb-md"
              readonly
            />

            <!-- Comment -->
            <div style="max-height: 160px">
              <q-input
                :model-value="recUpd['cmt']"
                v-model="recUpd['cmt']"
                type="textarea"
                class="q-mb-md"
                dense
                :label="$t('fldCmt')"
                readonly
              />
            </div>
          </q-card-section>
        </q-card>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import {api} from 'boot/axios'
import UpdaterSampling from 'pages/sampling/UpdaterSampling.vue'
import {hasTarget, notifyError} from 'src/utils/jsutils'
import {date, extend} from 'quasar'
import {ref} from 'vue'
import UpdaterSamplingTab1 from 'pages/sampling/UpdaterSamplingTab1.vue'

export default {
  name: 'SamplingPageTab',
  props: ['cls', 'title'],

  data() {
    return {
      splitterModel2: 100,
      optBranch: [],
      cols2: [],
      rows2: [],
      selected2: [],
      loading2: false,
      filter: '',
      mapExecutor: new Map(),
      recUpd: {},
      mapMeasure: new Map(),
      mapFishGear: new Map(),
      tgIns: '',
      tgUpd: '',
      tgDel: '',
      codCls: "",
      gridMode: ref("count")
    }
  },

  methods: {
    hasTarget,

    fmReqLabel(label) {
      return this.$t(label) + "*";
    },

    fnCell(row, col) {
      //console.info(col)
      return col.startsWith('p_')
    },

    fnEditCell(row, col) {
      //console.info("fnEditCell", row, col)
      //console.info("fnEditCell val", row[col.name])

      let mode = row[col.name] === undefined ? "ins" : "upd"

      this.$q
        .dialog({
          component: UpdaterSamplingTab1,
          componentProps: {
            mode: mode,
            data: {
              codProp: this.gridMode==="count" ? "Prop_FishRatio" : "Prop_FishRatioWeight",
              prop: parseInt(col.name.split("_")[1], 10),
              colName: col.label,
              periodName: row["periodName"],
              periodType: row["periodType"],
              dbeg: row["dbeg"],
              dend: row["dend"],
              idVal: row["id_" + col.name],
              objOrRelObj: row["obj"],
              isObj: 1,
              val: row[col.name],
            },
            // ...
          },
        })
        .onOk(() => {
          //row[col.name] = r
          this.loadSampling()
        })


    },

    fnDeleteCell(row, col) {
      //console.info("fnDeleteCell row", row)
      //console.info("fnDeleteCell col", col)
      let msg = col.label + ", " + row["periodName"]
      if (this.gridMode === "count")
        msg = msg + ", количество - " + row[col.name]
      else
        msg = msg + ", масса - " + row[col.name]

      let dta= {
        codProp: this.gridMode==="count" ? "Prop_FishRatio" : "Prop_FishRatioWeight",
        prop: parseInt(col.name.split("_")[1], 10),
        periodType: row["periodType"],
        dbeg: row["dbeg"],
        dend: row["dend"],
        idVal: row["id_" + col.name],
        objOrRelObj: row["obj"],
        isObj: 1,
        val: row[col.name],
      }

      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '<div style="color: plum">(' + msg + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: 'data/deleteSamplingTab1',
              params: [row["id_" + col.name], dta],
            })
            .then(() => {
              //row[col.name] = null
              this.loadSampling()
            })
            .catch((error) => {
              console.log(error.message)
              notifyError(error.message)
            })
        })
    },

    fnRadio(v) {
      console.info(v)
      this.loadSampling()
    },

    selectedRow(item) {
      if (this.selected2.length > 0 && item.obj === this.selected2[0].obj)
        this.selected2 = [];
      else {
        this.selected2 = [];
        this.selected2.push(item);
      }
      this.updateSelect2()
    },

    editObjProps(row, mode) {
      let data = { cls: this.cls, dte: date.formatDate(Date.now(), 'YYYY-MM-DD'),
        gridMode: this.gridMode, periodType: 71 }
      if (mode === 'upd') {
        extend(true, data, row)
        data.dte = data.dbeg
      }

      this.$q
        .dialog({
          component: UpdaterSampling,
          componentProps: {
            data: data,
            mode: mode,
            // ...
          },
        })
        .onOk((r) => {
          console.log("Ok! updated", r);
          if (mode === 'ins') {
            this.rows2.push(r)
            this.selected2 = []
            this.selected2.push(r)
            this.updateSelect2()
          } else {
            if (!r.hasOwnProperty("AreaOfTon"))
              r["AreaOfTon"] = ""
            for (let key in r) {
              row[key] = r[key]
              if (r.hasOwnProperty(key)) {
                row[key] = r[key]
              }
            }
            this.updateSelect2()
          }
        })
    },

    removeObjProps(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message:
            this.$t('deleteRecord') + '</br>(' + row["SampleNumber"] + ' - ' + row["SampleDate"] + ')',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          api
            .post('', {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1],
            })
            .then(
              () => {
                this.splitterModel2 = 100
                this.selected2 = []
                this.loadSampling()
              },
              (error) => {
                if (error.response.data.error.message.includes('@')) {
                  let msgs = error.response.data.error.message.split('@')
                  let m1 = msgs[0]
                  let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
                  let msg = ''
                  if (m1 === 'existsSampling') {
                    msg = `
                      Результаты забора проб:
                      Существует - ${m2}
                      `
                  }
                  notifyError(msg)
                } else {
                  notifyError(this.$t(error.response.data.error.message))
                }
              }
            )
        })
    },

    updateSelect2() {
      if (this.selected2.length > 0) {
        this.splitterModel2 = 85
        this.recUpd = extend(true, {}, this.selected2[0])
      } else this.splitterModel2 = 100
    },

    loadSampling() {
      this.loading2 = true
      api
        .post('', {
          method: 'data/loadSampling',
          params: [this.cls, false, 0, this.gridMode],
        })
        .then((response) => {
          this.cols2 = []
          this.getColumns1().forEach((col) => {
            this.cols2.push(col)
          })
          response.data.result.cols.forEach((col) => {
            this.cols2.push(col)
          })
          this.rows2 = response.data.result.rows.records
          //console.info("rows2", this.rows2)
        })
        .then(() => {
          this.loadExecutor()
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
          this.loading2 = false
        })
    },

    loadExecutor() {
      this.loading = true
      api
        .post('', {
          method: 'data/loadExecutor',
          params: ['Typ_Users', 'Prop_SampleExecutor'],
        })
        .then(
          (response) => {
            response.data.result.records.forEach((it) => {
              this.mapExecutor.set(it["id"], it["name"])
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
    },

    infoSelected2(row) {
      return ' ' + row["nameFishZone"] + ' (' + row["periodName"]+ ')'
    },

    getColumns1() {
      return [
        {
          name: 'periodName',
          label: this.$t('periodType')+"*",
          field: 'periodName',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:12%',
        },
        {
          name: 'nameFishZone',
          label: this.$t('fishArea')+"*",
          field: 'nameFishZone',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
        },
        {
          name: 'objFishGear',
          label: this.$t('FishGear')+"*",
          field: 'objFishGear',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 10%',
        },
        {
          name: 'AreaOfTon',
          label: this.$t('PropAreaOfTonShort'),
          field: 'AreaOfTon',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 8%',
        },
      ]
    },
  },

  mounted() {},

  created() {
    this.loading2 = true

    api
      .post('', {
        method: 'data/CodCls',
        params: [this.cls],
      })
      .then((response) => {
        this.codCls = response.data.result
        //console.log('codCls', this.codCls)
      })
      .then(()=> {
        if (this.codCls === "Cls_SamplingFishing") {
          this.tgIns = 'mon:zp:vr:ins'
          this.tgUpd = 'mon:zp:vr:upd'
          this.tgDel = 'mon:zp:vr:del'
        }
        if (this.codCls === "Cls_SamplingZooplankton") {
          this.tgIns = 'mon:zp:ton:ins'
          this.tgUpd = 'mon:zp:ton:upd'
          this.tgDel = 'mon:zp:ton:del'
        }
        if (this.codCls === "Cls_SamplingZoobenthos") {
          this.tgIns = 'mon:zp:toc:ins'
          this.tgUpd = 'mon:zp:toc:upd'
          this.tgDel = 'mon:zp:toc:del'
        }
        if (this.codCls === "Cls_SamplingHydrophysical") {
          this.tgIns = 'mon:zp:viz:ins'
          this.tgUpd = 'mon:zp:viz:upd'
          this.tgDel = 'mon:zp:viz:del'
        }
        if (this.codCls === "Cls_SamplingHydrological") {
          this.tgIns = 'mon:zp:log:ins'
          this.tgUpd = 'mon:zp:log:upd'
          this.tgDel = 'mon:zp:log:del'
        }
        if (this.codCls === "Cls_SamplingHydrochemistry") {
          this.tgIns = 'mon:zp:him:ins'
          this.tgUpd = 'mon:zp:him:upd'
          this.tgDel = 'mon:zp:him:del'
        }
      })
      .then(()=> {
        api
          .post('', {
            method: 'data/measureInfo',
            params: [],
          })
          .then((response) => {
            this.mapMeasure = response.data.result
            //console.info("this.mapMeasure", this.mapMeasure)
          })
          .then(() => {
            api
              .post('', {
                method: 'data/loadBranchName',
                params: ['Cls_Branch'],
              })
              .then((response) => {
                this.optBranch = response.data.result.records
              })
              .then(() => {
                api
                  .post('', {
                    method: 'data/loadFishGearName',
                    params: ['Cls_FishGearTrade'],
                  })
                  .then((response) => {
                    response.data.result.records.forEach((it) => {
                      this.mapFishGear.set(it["id"], it["name"])
                    })
                    //console.info("this.mapFishGear", this.mapFishGear)
                  })
              })
              .then(()=> {
                  this.loadSampling()
                })
              })
      })
      .catch((error) => {
        console.log(error)
        console.log(error.message)
        if (error.response) {
          if (error.response.data.error.message.includes('@')) {
            let msgs = error.response.data.error.message.split('@')
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
            let msg = m1 + m2
            notifyError(msg)
          } else {
            notifyError(this.$t(error.response.data.error.message))
          }
        }
      })
      .finally(() => {
        this.loading2 = false
      })
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
    /* bg color is important for th; just specify one #00b4ff*/
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
