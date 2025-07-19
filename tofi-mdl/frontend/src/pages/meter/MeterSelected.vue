<template>
  <q-page class="q-pa-sm-sm bg-green-1">
    <div v-if="meter_struct === 2">
      <q-tabs v-model="tab" class="text-teal">
        <div style="margin-left: 20px">
          {{ $t("meter") }}:
          <span style="color: black; margin-left: 10px">
            <strong>{{ this.infoMeter() }}</strong>
          </span>
        </div>

        <q-space/>
        <q-btn
            dense round color="secondary" icon="arrow_back" glossy
            @click="toBack()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>
        <q-tab name="factors" no-caps icon="pin" :label="$t('meterFactors')"/>
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
          <meter-factors ref="refFactors"/>
        </q-tab-panel>

        <q-tab-panel
            name="rates"
            style="height: calc(100vh - 200px); width: 100%"
        >
          <meter-soft-rates ref="refRates"/>
        </q-tab-panel>
      </q-tab-panels>
    </div>
    <div v-else>
      <q-tabs v-model="tab2" class="text-teal">
        <div style="margin-left: 20px">
          {{ $t("meter") }}:
          <span style="color: black; margin-left: 10px">
            <strong>{{ this.infoMeter() }}</strong>
          </span>
        </div>

        <q-space/>
        <q-btn
            dense round color="secondary" icon="arrow_back" glossy
            @click="toBack()"
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
          <meter-hard-rates/>
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import MeterFactors from "pages/meter/MeterFactors.vue";
import MeterSoftRates from "pages/meter/MeterSoftRates.vue";
import MeterHardRates from "pages/meter/MeterHardRates.vue";
import {notifyError} from "src/utils/jsutils.js";

export default defineComponent({
  components: {MeterFactors, MeterSoftRates, MeterHardRates},

  data: function () {
    return {
      meter_id: 0,
      meter_struct: 2,
      meter: {},
    };
  },

  methods: {
    fnChangeTab(n,o) {
      if (n==="rates") {
        if (this.$refs.refFactors.isEmpty()) {
          notifyError("Для данного измерителя не указаны факторы")
          this.tab = "factors"
        }
      }
    },

    toBack() {
      //console.info("this.meter_id", this.meter_id)
      this.$router["push"]({
        name: "meterPage",
        params: {
          meter: this.meter_id,
        },
      });
    },

    infoMeter() {
      return this.meter.cod + " - " + this.meter.name;
    },
  },

  mounted() {
    console.log("mounted!");
    this.meter_id = parseInt(this.$route["params"].meter, 10);
    this.meter_struct = parseInt(this.$route["params"].meterStruct, 10);

    // load meter
    this.loading = ref(true);
    api
        .post(baseURL, {
          id: "1",
          method: "meter/loadRec",
          params: [{id: this.meter_id}],
        })
        .then((response) => {
          //console.log("f", response.data.result.records)
          this.meter = response.data.result.records[0];
        })
        .catch((error) => {
          console.log(error);
        })
        .finally(() => {
          this.loading = ref(false);
        });
    //
  },

  created() {
    //console.log("<<<<<  created")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
  },

  setup() {
    return {
      tab: ref("factors"),
      tab2: ref("hardrates"),
    };
  },
});
</script>

<style></style>
