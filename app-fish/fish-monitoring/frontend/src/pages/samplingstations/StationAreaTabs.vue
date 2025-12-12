<template>
  <div class="q-pa-sm-sm bg-green-1">
    <div>
      <q-inner-loading :showing="loading" color="secondary" />
    </div>

    <q-tabs dense v-model="tab" active-color="purple" class="text-teal no-scroll">
      <q-tab name="station" no-caps :label="$t('samplingStations')" />
      <q-tab name="area" no-caps :label="$t('fishingArea')" />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="station" class="no-scroll">
        <SamplingStationsPage />
      </q-tab-panel>

      <q-tab-panel name="area" class="no-scroll">
        <FishingAreaPage />
      </q-tab-panel>

    </q-tab-panels>
  </div>
</template>

<script>

import {ref} from 'vue'

import SamplingStationsPage from 'pages/samplingstations/SamplingStationsPage.vue'
import FishingAreaPage from 'pages/samplingstations/FishingAreaPage.vue'

export default {
  name: 'StationAreaTabs',
  components: { FishingAreaPage, SamplingStationsPage },

  data: function () {
    return {
      loading: false,
      tab: ref('station'),
    }
  },

  methods: {


  },

  created() {

  },
}
</script>

<style scoped></style>
