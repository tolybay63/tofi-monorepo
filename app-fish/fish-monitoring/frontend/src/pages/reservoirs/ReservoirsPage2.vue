<template>
  <div class="q-pa-sm">

    <q-table
      :columns="cols"
      :rows="rows"
      :loading="loading"
      card-class="bg-blue-grey-1 text-brown"
      row-key="id"
      table-header-class="text-bold text-white bg-blue-grey-13"
      hide-header
      grid
      flat
      bordered
      dense
      :rows-per-page-options="[0]"
    >

      <template v-slot:top>
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar color="black" icon="sailing" text-color="white"></q-avatar>
          {{ $t('reservoirs') }}
        </div>
      </template>

      <template v-slot:item="props">
        <div class="q-pa-xs col-xs-12 col-sm-6 col-md-4 cursor-pointer" @click="fnChoose(props.row)">

          <q-card flat bordered class="bg-blue-grey-1 text-brown">
            <q-card-section class="text-center" >
              Филиал:
              <br />
              <strong>{{ props.row.name }}</strong>
            </q-card-section>

            <q-card-section
              class="flex flex-center"
              :style="{fontSize: 20 + 'px'} "
            >
              <div class="row">Количество водоемов:
                <div class="text-blue text-bold">
                  {{ props.row.cnt }}
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </template>






      <template v-slot:loading>
        <q-inner-loading color="secondary" showing/>
      </template>

    </q-table>

  </div>
</template>

<script>
import {hasTarget, notifyError} from 'src/utils/jsutils'
import {api} from 'boot/axios'


export default {
  name: 'ReservoirsPage',
  props: [],

  data: function () {
    return {
      cols: [],
      rows: [],
      loading: true,
      maxCnt: 4

    }
  },

  methods: {
    hasTarget,

    fnChoose(row) {
      console.log(row)
    },


    getColumns() {
      return [
        {
          name: 'name',
          label: this.$t('filial'),
          field: 'name',
          align: 'left',
          width: '150px',
        },

        {
          name: 'cnt',
          label: this.$t('countReservoirs'),
          field: 'cnt',
          align: 'left',
          width: '150px',
        },

      ]
    },

    loadFilials() {
      this.loading = true
      api
        .post('', {
          method: 'data/loadFilials',
          params: []
        })
        .then((response) => {
          this.rows = response.data.result["records"]
        })
        .catch((error) => {

           let msg = error.response?.data?.error?.message
           notifyError(msg)
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
    this.loadFilials()
  },
}

</script>

<style lang="sass">

</style>

