<template>
  <q-table
    color="primary"
    dense
    card-class="bg-amber-3 text-brown"
    row-key="obj"
    :columns="cols"
    :rows="rows"
    :wrap-cells="true"
    :table-colspan="4"
    table-header-class="text-bold text-white bg-blue-grey-13"
    separator="horizontal"
    :loading="loading"
    :rows-per-page-options="[0]"
  >
    <template #body-cell="props">
      <q-td :props="props">
        <div :class="fnClass(props.row)">
          {{ props.value }}
        </div>
      </q-td>
    </template>
  </q-table>
</template>

<script>
import { api, baseURL, tofi_dbeg, tofi_dend } from 'boot/axios'
import { notifyError } from 'src/utils/jsutils'
import { date } from 'quasar'

export default {
  name: 'LifiInfo',
  props: ['field', 'rec'],

  data() {
    return {
      loading: false,
      rows: [],
      cols: [],
      idv: null,
    }
  },

  methods: {
    fnClass(rec) {
      let id = rec['id']
      return this.idv === id ? 'text-green' : 'text-black'
    },

    load() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadReservorLife',
          params: [{ codProp: 'Prop_' + this.field, obj: this.rec['obj'] }],
        })
        .then((response) => {
          this.rows = response.data.result.records
          //console.info("rows life", this.rows)
        })
        .catch((error) => {
          notifyError(error.message)
        })
        .finally(() => {
          this.loading = false
        })
    },

    getColumn() {
      return [
        {
          name: 'val',
          label: this.$t('val'),
          field: 'val',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:30%',
        },
        {
          name: 'name',
          label: this.$t('period'),
          field: 'name',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:70%',
        },
/*
        {
          name: 'dbeg',
          label: this.$t('fldDbegShort'),
          field: 'dbeg',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:35%',
          format: (val) =>
            val <= tofi_dbeg || val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY'),
        },
        {
          name: 'dend',
          label: this.$t('fldDendShort'),
          field: 'dend',
          align: 'left',
          classes: 'bg-blue-grey-1',
          headerStyle: 'font-size: 1.2em; width:35%',
          format: (val) =>
            val <= tofi_dbeg || val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY'),
        },
*/

      ]
    },
  },

  created() {
    //console.info("rec", this.rec)
    let kProp = 'id' + this.field
    this.idv = this.rec[kProp]
    this.cols = this.getColumn()
    this.load()
  },
}
</script>

<style scoped></style>
