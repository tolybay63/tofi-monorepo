<template>
  <div class="q-pa-sm-sm bg-green-1">
    <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="view_in_ar" />
        {{ $t("cubes") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ infoCube() }} </strong>
        </span>
      </div>
    </q-banner>

    <q-tabs
      dense
      v-model="tab"
      class="text-teal no-scroll justify-center"
      @update:model-value="tabChange"
    >
      <q-btn
        dense
        glossy
        round
        color="secondary"
        icon="arrow_back"
        class="q-ml-lg"
        @click="toBack"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
      <q-space />
      <q-tab
        name="dims"
        no-caps
        icon="type_specimen"
        :label="$t('dims')"
      />

      <q-tab
        name="edjesprop"
        no-caps
        icon="credit_score"
        :label="$t('edjesprop')"
      />

      <q-tab
        name="edjesobj"
        no-caps
        icon="pattern"
        :label="$t('edjesobj')"
      />

      <q-tab
        name="algs"
        no-caps
        icon="functions"
        :label="$t('algs')"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="dims" class="no-scroll">
        <dims-page :dOrg="cube.dOrg" :cubeS="cube.id" />
      </q-tab-panel>

      <q-tab-panel name="edjesprop" class="no-scroll">
        <edjes-prop-page :dOrg="cube.dOrg" :cubeS="cube.id" />
      </q-tab-panel>

      <q-tab-panel name="edjesobj" class="no-scroll">
        <edjes-obj-page :dOrg="cube.dOrg" :cubeS="cube.id" />
      </q-tab-panel>

      <q-tab-panel name="algs" class="no-scroll">
        <alg-page />
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "@/boot/axios.js";
import DimsPage from "@/pages/cubes/tabs/DimsPage.vue";
import EdjesPropPage from "@/pages/cubes/tabs/EdjesPropPage.vue";
import EdjesObjPage from "@/pages/cubes/tabs/EdjesObjPage.vue";
import AlgPage from "@/pages/cubes/tabs/AlgPage.vue";

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();

const tab = ref("");
const cubesGrId = ref(0);
const cubesId = ref(0);
const cube = reactive({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "CubesPage",
    params: {
      cubesGr: cubesGrId.value,
      cubesId: cubesId.value,
    },
  });
};

const checkDimProp = (cubeId) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/checkDimProp",
      params: [cubeId],
    })
    .then(() => {})
    .catch(() => {
      tab.value = "dims";
    })
    .finally(() => {
      loading.value = false;
    });
};

const checkDimObj = (cubeId) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/checkDimObj",
      params: [cubeId],
    })
    .then(() => {})
    .catch(() => {
      tab.value = "dims";
    })
    .finally(() => {
      loading.value = false;
    });
};

const tabChange = (v) => {
  if (v === "edjesprop") {
    checkDimProp(cubesId.value);
  }
  if (v === "edjesobj") {
    checkDimObj(cubesId.value);
  }
};

const infoCube = () => {
  let inf = (cube.cod || "") + " - " + (cube.name || "");
  if (cube.cubeSType === 1) {
    inf = inf + " (" + proxy?.$t("std") + ")";
  } else {
    inf = inf + " (" + proxy?.$t("calc") + ")";
  }
  return inf;
};

onMounted(() => {
  cubesGrId.value = parseInt(route.params.cubesGr, 10);
  cubesId.value = parseInt(route.params.cubes, 10);

  loading.value = true;
  api
    .post("", {
      method: "cubes/loadRec",
      params: [cubesId.value],
    })
    .then((response) => {
      const rec = response.data.result.records[0];
      if (rec) {
        Object.assign(cube, rec);
      }
    })
    .catch((error) => {
      console.error(error);
    })
    .finally(() => {
      tab.value = "dims";
      loading.value = false;
    });
});
</script>

<style scoped></style>
