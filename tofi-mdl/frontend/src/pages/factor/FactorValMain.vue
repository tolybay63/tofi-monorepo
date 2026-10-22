<template>
  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("factor") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ infoFactor() }}</strong>
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
      <q-tab name="val" no-caps icon="pin" :label="$t('factorVals')" />
      <q-tab
        name="rel"
        no-caps
        icon="task"
        :label="$t('relFactors')"
        style="margin-right: 10px"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="val" style="height: calc(100vh - 190px); width: 100%">
        <factor-val />
      </q-tab-panel>

      <q-tab-panel name="rel" style="height: calc(100vh - 190px); width: 100%">
        <factor-rel />
      </q-tab-panel>
    </q-tab-panels>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "@/boot/axios";
import FactorVal from "@/pages/factor/FactorVal.vue";
import FactorRel from "@/pages/factor/FactorRel.vue";
import { notifyError } from "@/utils/jsutils";

const router = useRouter();
const route = useRoute();

const tab = ref("val");
const factor1_id = ref(0);
const factor = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "FactorPage",
    params: {
      factor: factor1_id.value,
    },
  });
};

const infoFactor = () => {
  return (factor.value.cod || "") + " - " + (factor.value.name || "");
};

onMounted(() => {
  factor1_id.value = parseInt(route.params.factor, 10) || 0;

  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "factor/loadRec",
      params: [{ id: factor1_id.value }],
    })
    .then((response) => {
      factor.value = response.data.result.records[0] || {};
    })
    .catch((error) => {
      console.log(error);
      notifyError(error.message);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style></style>
