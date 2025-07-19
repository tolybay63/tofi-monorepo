<template>
  <q-page class="q-pa-sm-sm">
    <div class="q-gutter-y-md bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal no-scroll">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("multiProp") }}:
        </div>
        <span style="color: black; margin-left: 10px">
          {{ this.infoProp() }}
        </span>

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
          name="MultiPropDim"
          no-caps
          icon="queue"
          :label="$t('multiPropDim')"
        />

        <q-tab
          name="status"
          no-caps
          icon="check_box"
          :label="$t('statusFactor')"
          v-if="multiProp.statusFactor"
        />

        <q-tab
          name="provider"
          no-caps
          icon="download"
          :label="$t('providerTyp')"
          v-if="multiProp.providerTyp"
        />

        <q-tab
          name="MultiPropCond"
          no-caps
          icon="settings"
          :label="$t('multiPropCond')"
        />

        <q-tab
          name="periodType"
          no-caps
          icon="date_range"
          :label="$t('periodTypes')"
          v-if="dependPeriod()"
        />
      </q-tabs>

      <q-tab-panels v-model="tab" animated>

        <q-tab-panel name="status" class="no-scroll">
          <status-page
            :act="'multiProp'"
            :fk="multiProp.id"
            :factor="multiProp.statusFactor"
          />
        </q-tab-panel>

        <q-tab-panel name="provider" class="no-scroll">
          <provider-page
            :act="'multiProp'"
            :fk="multiProp.id"
            :typ="multiProp.providerTyp"
          />
        </q-tab-panel>

        <q-tab-panel name="MultiPropDim" class="no-scroll">
          <multi-prop-dim :multiProp="multiProp.id"/>
        </q-tab-panel>

        <q-tab-panel name="MultiPropCond" class="no-scroll">
          <multi-prop-cond :multiProp="multiProp.id"/>
        </q-tab-panel>

        <q-tab-panel name="periodType">
          <multi-prop-period-type :multiProp="multiProp.id"/>
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import MultiPropDim from "pages/multiprop/MultiPropDim.vue";
import MultiPropPeriodType from "pages/multiprop/MultiPropPeriodType.vue";
import MultiPropCond from "pages/multiprop/MultiPropCond.vue";
import StatusPage from "pages/prop/StatusPage.vue";
import ProviderPage from "pages/prop/ProviderPage.vue";

export default {
  name: "MultiPropSelected",
  components: {
    ProviderPage,
    StatusPage,
    MultiPropCond,
    MultiPropPeriodType,
    MultiPropDim,
  },
  data() {
    return {
      tab: ref("MultiPropDim"),
      multiPropGrId: 0,
      multiPropId: 0,
      multiProp: {},
    };
  },
  methods: {
    toBack() {
      this.$router["push"]({
        name: "MultiProp",
        params: {
          mpGr: this.multiPropGrId,
          mp: this.multiPropId,
        },
      });
    },

    dependPeriod() {
      return this.multiProp["isDependValueOnPeriod"] === true;
    },

    infoProp() {
      return this.multiProp.cod + " - " + this.multiProp.name;
    },

  },

  mounted() {
    //console.log("mounted MultiPropSelected!", this.$route.params);
    this.multiPropId = parseInt(this.$route["params"].mp, 10);
    this.multiPropGrId = parseInt(this.$route["params"].mpGr, 10);

    // load multiProp
    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "multiProp/loadRec",
        params: [this.multiPropId],
      })
      .then((response) => {
        this.multiProp = response.data.result.records[0];
        //console.log("Prop Rec Load", this.prop)
      })
      .catch((error) => {
        console.log(error);
      })
      .finally(() => {

        this.loading = ref(false);
      });
    //
  },
};
</script>

<style scoped></style>
