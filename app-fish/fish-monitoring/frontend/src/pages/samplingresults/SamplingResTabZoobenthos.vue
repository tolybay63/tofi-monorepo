<template>
  <div class="no-padding no-margin" style="height: calc(100vh - 250px)">
    <q-table
      color="primary"
      card-class="bg-amber-1 text-brown"
      row-key="dimmultipropitem"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      :rows-per-page-options="[0]"
    >
      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" text-color="white" icon="free_cancellation"> </q-avatar>
          {{ $t('researchResults') }}
        </div>
      </template>

      <template #body-cell="props">
        <q-td :props="props">
          <div style="height: 50px">
            <div v-if="fnCell(props.row.name, props.col.name)">
              <q-btn
                v-if="hasTarget('mon:rzp:toc:res:upd')"
                icon="edit"
                dense
                size="10px"
                flat
                round
                color="secondary"
                class="absolute-top-right"
                @click="editObjPropsChild(props.row, props.col)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('update') }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="hasTarget('mon:rzp:toc:res:del')"
                icon="delete"
                dense
                size="10px"
                flat
                round
                color="red"
                class="absolute-bottom-right"
                @click="removeObjPropsChild(props.row, props.col)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t('delete') }}
                </q-tooltip>
              </q-btn>
            </div>
            <div v-if="props.row.name === 'Всего'" style="font-weight: bold; font-size: 1.2em">
              {{ props.value }}
            </div>
            <div v-else style="font-weight: normal">
              {{ props.value }}
            </div>
          </div>
        </q-td>
      </template>
    </q-table>
  </div>
</template>

<script>
import {api} from 'boot/axios'
import UpdaterResZooplankton from 'pages/samplingresults/UpdaterResZooplankton.vue'
import {hasTarget, notifyError} from 'src/utils/jsutils'

export default {
  name: 'SamplingResTabZoobenthos',

  props: [
    'codCls', //cod класса забора
    'cls', //id класса исследования забора
    'obj',
  ],

  data() {
    return {
      cols: [],
      rows: [],
      loading: false,
    }
  },

  methods: {
    hasTarget,
    fnCell(row, col) {
      return col.startsWith('fld_') && row !== 'Всего'
    },

    editObjPropsChild(row, col) {
      let r_dmpi = row["dimmultipropitem"]
      let r_mpd = row["multipropdim"]
      let arr = col.name.split('_')
      let c_dmpi = arr[1]
      let c_mpd = arr[2]
      let id = col.name.replace('fld', 'id')
      this.$q
        .dialog({
          component: UpdaterResZooplankton,
          componentProps: {
            data: {
              dmpcell: row[id],
              codMP: 'MP_Zoobenthos',
              obj: this.obj,
              cls: this.cls,
              r_dmpi: r_dmpi,
              r_mpd: r_mpd,
              c_dmpi: c_dmpi,
              c_mpd: c_mpd,
              val: row[col.name],
            },
            dim1: row.name,
            dim2: col.label,
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) this.loadData(this.obj)
        })
    },

    removeObjPropsChild(row, col) {
      let id = col.name.replace('fld', 'id')
      if (row[id] === 0) return
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '</br>(' + row.name + '/' + col.label + ')',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          api
            .post('', {
              method: 'datamulti/deleteZooplankton',
              params: [row[id]],
            })
            .then(() => {
              this.loadData(this.obj)
            })
            .catch((error) => {
              let msg = error.message
              if (error.response) msg = error.response.data.error.message
              notifyError(msg)
            })
        })
    },

    loadData(obj) {
      this.loading = true
      api
        .post('', {
          method: 'datamulti/loadZooplankton',
          params: ['MP_Zoobenthos', this.cls, obj],
        })
        .then((response) => {
          this.rows = response.data.result.records
          //console.info("rows", this.rows)
        })
        .catch((error) => {
          console.error(error.message)
        })
        .finally(() => {
          this.loading = false
        })
    },
  },

  created() {
    this.loading = true
    api
      .post('', {
        method: 'datamulti/colsZooplankton',
        params: ['MP_Zoobenthos', this.cls],
      })
      .then((response) => {
        this.cols = response.data.result.cols
      })
      .catch((error) => {
        console.error(error.message)
      })
      .finally(() => {
        this.loading = false
      })
  },
}
</script>

<style scoped></style>
