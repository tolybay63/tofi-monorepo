<template>
  <div class="no-padding no-margin" style="height: calc(100vh - 250px)">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[50, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <q-table
          style="max-height: 900px; width: 100%"
          color="primary"
          dense
          card-class="bg-amber-1 text-brown"
          row-key="obj"
          :columns="cols"
          :rows="rows"
          :filter="filter"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          selection="single"
          v-model:selected="selected"
          @update:selected="updateSelect"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t('selectedRow') }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
              {{ $t('infoRowExt') }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold">
              <q-avatar color="black" text-color="white" icon="biotech"> </q-avatar>
              {{ $t('research') }}
            </div>

            <q-space />
            <q-btn
              v-if="hasTarget(tgIns)"
              icon="post_add"
              dense
              color="secondary"
              :disable="loading"
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
              :disable="loading || selected.length === 0"
              @click="editObjProps(selected[0], 'upd')"
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
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="removeObjProps(selected[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t('deletingRecord') }}
              </q-tooltip>
            </q-btn>

            <q-space />
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <div v-if="codCls === 'Cls_SamplingFishing'">
          <SamplingResTabFishing
            :codCls="codCls"
            :cls="cls"
            :obj="selected.length > 0 ? selected[0].obj : 0"
            ref="refSamplingFishing"
          />
        </div>

        <div v-if="codCls === 'Cls_SamplingZooplankton'">
          <SamplingResTabZooplankton
            :codCls="codCls"
            :cls="cls"
            :obj="selected.length > 0 ? selected[0].obj : 0"
            ref="refSamplingZooplankton"
          />
        </div>

        <div v-if="codCls === 'Cls_SamplingZoobenthos'">
          <SamplingResTabZoobenthos
            :codCls="codCls"
            :cls="cls"
            :obj="selected.length > 0 ? selected[0].obj : 0"
            ref="refSamplingZoobenthos"
          />
        </div>

        <div v-if="codCls === 'Cls_SamplingHydrochemistry'">
          <SamplingResTabHydrochemistry
            :codCls="codCls"
            :cls="cls"
            :obj="selected.length > 0 ? selected[0].obj : 0"
            ref="refSamplingHydrochemistry"
          />
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import { api, baseURL, tofi_dbeg, tofi_dend } from 'boot/axios'
import { hasTarget, notifyError } from 'src/utils/jsutils'
import { date } from 'quasar'
import { extend } from 'quasar'
import UpdaterResFishing from 'pages/samplingresults/UpdaterResFishing.vue'
import SamplingResTabFishing from 'pages/samplingresults/SamplingResTabFishing.vue'
import SamplingResTabZoobenthos from 'pages/samplingresults/SamplingResTabZoobenthos.vue'
import SamplingResTabZooplankton from 'pages/samplingresults/SamplingResTabZooplankton.vue'
import SamplingResTabHydrochemistry from 'pages/samplingresults/SamplingResTabHydrochemistry.vue'

export default {
  name: 'SamplingResTabs',
  components: {
    SamplingResTabHydrochemistry,
    SamplingResTabZooplankton,
    SamplingResTabZoobenthos,
    SamplingResTabFishing,
  },

  props: [
    'codCls', //cod класса забора
    'cls', //id класса исследования забора
    'title',
  ],

  data() {
    return {
      splitterModel: 100,
      cols: [],
      rows: [],
      selected: [],
      loading: false,
      filter: '',
      mapExecutor: new Map(),
      tgIns: '',
      tgUpd: '',
      tgDel: '',
    }
  },

  methods: {
    hasTarget,

    editObjProps(row, mode) {
      let data = { cls: this.cls, ResearchDate: date.formatDate(Date.now(), 'YYYY-MM-DD') }
      if (mode === 'upd') {
        extend(true, data, row)
      }
      if (data.ResearchDate <= tofi_dbeg || data.ResearchDate >= tofi_dend) data.ResearchDate = null
      this.$q
        .dialog({
          component: UpdaterResFishing,
          componentProps: {
            data: data,
            mode: mode,
            codCls: this.codCls,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === 'ins') {
            this.rows.push(r)
            this.selected = []
            this.selected.push(r)
            this.updateSelect()
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

    removeObjProps(row) {
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message:
            this.$t('deleteRecord') +
            '</br>(' +
            row["SampleNumber"] +
            '; ' +
            row.ResearchNumber +
            '; ' +
            date.formatDate(row.ResearchDate, 'DD.MM.YYYY') +
            ')',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          this.$axios
            .post(baseURL, {
              method: 'data/deleteOwnerWithProperties',
              params: [row.obj, 1],
            })
            .then(
              () => {
                this.splitterModel = 100
                this.selected = []
                this.loadResultSampling()
              },
              (error) => {
                let msg = error.message
                if (error.response) {
                  msg = error.response.data.error.message
                  if (msg==="existsResSampling")
                    msg = "Существуют результаты исследования"
                }
                notifyError(msg)
              }
            )
        })
    },

    updateSelect() {
      if (this.selected.length <= 0) {
        this.splitterModel = 100
      } else {
        console.info("row", this.selected[0]);
        if (
          this.codCls === 'Cls_SamplingHydrophysical' ||
          this.codCls === 'Cls_SamplingHydrological'
        )
          return
        this.splitterModel = 50
        if (this.codCls === 'Cls_SamplingFishing')
          this.$refs.refSamplingFishing.loadChildProps(this.selected[0].obj)
        if (this.codCls === 'Cls_SamplingZooplankton')
          this.$refs.refSamplingZooplankton.loadData(this.selected[0].obj)
        if (this.codCls === 'Cls_SamplingZoobenthos')
          this.$refs.refSamplingZoobenthos.loadData(this.selected[0].obj)
        if (this.codCls === 'Cls_SamplingHydrochemistry')
          this.$refs.refSamplingHydrochemistry.loadHydrochemistry(this.selected[0].obj)
      }
    },

    loadResultSampling() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadResultSampling',
          params: [this.cls, false, 0],
        })
        .then((response) => {
          this.rows = response.data.result.records
          //console.info("rows", this.rows)
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
          this.loading = false
        })
    },

    loadExecutor() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadExecutor',
          params: ['Typ_Users', 'Prop_ResearchExecutor'],
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

    infoSelected(row) {
      return (
        ' ' +
        row["SampleNumber"] +
        ' (' +
        row.ResearchNumber +
        ') от ' +
        date.formatDate(row.ResearchDate, 'DD.MM.YYYY')
      )
    },

    getColumns() {
      return [
        {
          name: 'SampleNumber',
          label: this.$t('sampleNumber'),
          field: 'SampleNumber',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:30%',
        },
        {
          name: 'ResearchNumber',
          label: this.$t('ResearchNumber'),
          field: 'ResearchNumber',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 30%',
        },
        {
          name: 'ResearchDate',
          label: this.$t('ResearchDate'),
          field: 'ResearchDate',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:10%',
          format: (val) =>
            val <= tofi_dbeg || val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY'),
        },
        {
          name: 'objResearchExecutor',
          label: this.$t('executor'),
          field: 'objResearchExecutor',
          align: 'left',
          sortable: true,
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width: 30%',
          format: (val) => (this.mapExecutor ? this.mapExecutor.get(val) : null),
        },
      ]
    },
  },

  created() {
    if (this.codCls === 'Cls_SamplingFishing') {
      this.tgIns = 'mon:rzp:bio:is:ins'
      this.tgUpd = 'mon:rzp:bio:is:upd'
      this.tgDel = 'mon:rzp:bio:is:del'
    }
    if (this.codCls === 'Cls_SamplingZooplankton') {
      this.tgIns = 'mon:rzp:ton:is:ins'
      this.tgUpd = 'mon:rzp:ton:is:upd'
      this.tgDel = 'mon:rzp:ton:is:del'
    }
    if (this.codCls === 'Cls_SamplingZoobenthos') {
      this.tgIns = 'mon:rzp:toc:is:ins'
      this.tgUpd = 'mon:rzp:toc:is:upd'
      this.tgDel = 'mon:rzp:toc:is:del'
    }
    if (this.codCls === 'Cls_SamplingHydrophysical') {
      this.tgIns = 'mon:rzp:viz:is:ins'
      this.tgUpd = 'mon:rzp:viz:is:upd'
      this.tgDel = 'mon:rzp:viz:is:del'
    }
    if (this.codCls === 'Cls_SamplingHydrological') {
      this.tgIns = 'mon:rzp:log:is:ins'
      this.tgUpd = 'mon:rzp:log:is:upd'
      this.tgDel = 'mon:rzp:log:is:del'
    }
    if (this.codCls === 'Cls_SamplingHydrochemistry') {
      this.tgIns = 'mon:rzp:him:is:ins'
      this.tgUpd = 'mon:rzp:him:is:upd'
      this.tgDel = 'mon:rzp:him:is:del'
    }

    this.cols = this.getColumns()
    this.loadResultSampling()
  },
}
</script>

<style scoped></style>
