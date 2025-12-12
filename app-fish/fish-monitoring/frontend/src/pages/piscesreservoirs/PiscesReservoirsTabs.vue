<template>
  <div class="q-pa-sm-sm bg-green-1">
    <div>
      <q-inner-loading :showing="loading" color="secondary" />
    </div>

    <q-tabs dense v-model="tab" active-color="purple" class="text-teal no-scroll">
      <q-tab name="reservoirs" no-caps :label="$t('piscesInReservoirs')" />
      <q-tab name="analise" no-caps :label="$t('analysis')" />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="reservoirs" class="no-scroll">
        <PiscesReservoirsPage />
      </q-tab-panel>

      <q-tab-panel name="analise" class="no-scroll">
        <FishingAnalisePage />
      </q-tab-panel>

    </q-tab-panels>
  </div>
</template>

<script>

import { ref } from 'vue'

import PiscesReservoirsPage from 'pages/piscesreservoirs/PiscesReservoirsPage.vue'
import FishingAnalisePage from 'pages/piscesreservoirs/FishingAnalisePage.vue'

export default {
  name: 'PiscesReservoirsTabs',
  components: { FishingAnalisePage, PiscesReservoirsPage },

  data: function () {
    return {
      loading: false,
      tab: ref('reservoirs'),
    }
  },

  methods: {


  },

  created() {

  },
}
</script>

<style scoped></style>
