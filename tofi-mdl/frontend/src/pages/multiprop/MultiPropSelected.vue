<template>
  <q-page class="q-pa-sm-sm">
    <div class="q-gutter-y-md bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal no-scroll">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("multiProp") }}:
        </div>
        <span style="color: black; margin-left: 10px">
          {{ infoProp() }}
        </span>

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
          <multi-prop-dim :multiProp="multiProp.id" />
        </q-tab-panel>

        <q-tab-panel name="MultiPropCond" class="no-scroll">
          <multi-prop-cond :multiProp="multiProp.id" />
        </q-tab-panel>

        <q-tab-panel name="periodType">
          <multi-prop-period-type :multiProp="multiProp.id" />
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "@/boot/axios";
import MultiPropDim from "@/pages/multiprop/MultiPropDim.vue";
import MultiPropPeriodType from "@/pages/multiprop/MultiPropPeriodType.vue";
import MultiPropCond from "@/pages/multiprop/MultiPropCond.vue";
import StatusPage from "@/pages/prop/StatusPage.vue";
import ProviderPage from "@/pages/prop/ProviderPage.vue";

const router = useRouter();
const route = useRoute();

const tab = ref("MultiPropDim");
const multiPropGrId = ref(0);
const multiPropId = ref(0);
const multiProp = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "MultiProp",
    params: {
      mpGr: multiPropGrId.value,
      mp: multiPropId.value,
    },
  });
};

const dependPeriod = () => {
  return multiProp.value["isDependValueOnPeriod"] === true;
};

const infoProp = () => {
  return (multiProp.value.cod || "") + " - " + (multiProp.value.name || "");
};

onMounted(() => {
  multiPropId.value = parseInt(route.params.mp, 10) || 0;
  multiPropGrId.value = parseInt(route.params.mpGr, 10) || 0;

  loading.value = true;
  api
    .post("", {
      method: "multiProp/loadRec",
      params: [multiPropId.value],
    })
    .then((response) => {
      multiProp.value = response.data.result.records[0] || {};
    })
    .catch((error) => {
      console.log(error);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
