<template>
  <q-page class="q-pa-sm-sm bg-green-1">
    <div v-if="meter_struct === 2">
      <q-tabs v-model="tab" class="text-teal">
        <div style="margin-left: 20px">
          {{ $t("meter") }}:
          <span style="color: black; margin-left: 10px">
            <strong>{{ infoMeter() }}</strong>
          </span>
        </div>

        <q-space />
        <q-btn
          dense
          round
          color="secondary"
          icon="arrow_back"
          glossy
          @click="toBack"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>
        <q-tab name="factors" no-caps icon="pin" :label="$t('meterFactors')" />
        <q-tab
          name="rates"
          no-caps
          icon="speed"
          :label="$t('rates')"
          style="margin-right: 10px"
        />
      </q-tabs>
      <q-tab-panels v-model="tab" animated class="no-scroll" @beforeTransition="fnChangeTab">
        <q-tab-panel
          name="factors"
          style="height: calc(100vh - 200px); width: 100%"
        >
          <meter-factors ref="refFactors" />
        </q-tab-panel>

        <q-tab-panel
          name="rates"
          style="height: calc(100vh - 200px); width: 100%"
        >
          <meter-soft-rates ref="refRates" />
        </q-tab-panel>
      </q-tab-panels>
    </div>
    <div v-else>
      <q-tabs v-model="tab2" class="text-teal">
        <div style="margin-left: 20px">
          {{ $t("meter") }}:
          <span style="color: black; margin-left: 10px">
            <strong>{{ infoMeter() }}</strong>
          </span>
        </div>

        <q-space />
        <q-btn
          dense
          round
          color="secondary"
          icon="arrow_back"
          glossy
          @click="toBack"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>
        <q-tab
          name="hardrates"
          no-caps
          icon="speed"
          :label="$t('rates')"
          style="margin-right: 10px; color: #c10015"
        />
      </q-tabs>
      <q-tab-panels
        style="height: calc(100vh - 190px); width: 100%"
        v-model="tab2"
        animated
        swipeable
        vertical
        transition-prev="jump"
        transition-next="jump"
      >
        <q-tab-panel name="hardrates">
          <meter-hard-rates />
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "@/boot/axios";
import MeterFactors from "@/pages/meter/MeterFactors.vue";
import MeterSoftRates from "@/pages/meter/MeterSoftRates.vue";
import MeterHardRates from "@/pages/meter/MeterHardRates.vue";
import { notifyError } from "@/utils/jsutils.js";

const router = useRouter();
const route = useRoute();

const tab = ref("factors");
const tab2 = ref("hardrates");
const meter_id = ref(0);
const meter_struct = ref(2);
const meter = ref({});
const loading = ref(false);
const lang = ref("");

const refFactors = ref(null);
const refRates = ref(null);

const fnChangeTab = (n) => {
  if (n === "rates") {
    if (refFactors.value?.isEmpty?.()) {
      notifyError("Для данного измерителя не указаны факторы");
      tab.value = "factors";
    }
  }
};

const toBack = () => {
  router.push({
    name: "meterPage",
    params: {
      meter: meter_id.value,
    },
  });
};

const infoMeter = () => {
  return (meter.value.cod || "") + " - " + (meter.value.name || "");
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

  meter_id.value = parseInt(route.params.meter, 10) || 0;
  meter_struct.value = parseInt(route.params.meterStruct, 10) || 2;

  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "meter/loadRec",
      params: [{ id: meter_id.value }],
    })
    .then((response) => {
      meter.value = response.data.result.records[0] || {};
    })
    .catch((error) => {
      console.log(error);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style></style>
