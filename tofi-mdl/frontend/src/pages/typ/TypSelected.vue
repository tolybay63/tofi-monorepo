<template>
  <q-page class="q-pa-sm-sm">
    <div class="bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("typ") }}:
        </div>
        <span style="color: black; margin-left: 10px">
          {{ infoTyp() }}
        </span>

        <q-space />

        <q-btn
          dense round color="secondary" glossy
          icon="arrow_back" @click="toBack"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>

        <q-tab name="vers" no-caps icon="event_note" :label="$t('vers')" />
        <q-tab name="typroles" no-caps icon="perm_contact_calendar" :label="$t('roles2')" />
        <q-tab name="clustfactors" no-caps icon="pie_chart_outline" :label="$t('clusterFactors')" />
        <q-tab name="clss" no-caps icon="group_work" :label="$t('clss')" />
        <q-tab v-if="typ.parent" name="notextended" no-caps icon="unpublished" :label="$t('notextended')" />
      </q-tabs>

      <q-tab-panels v-model="tab" animated @transition="fnChangePn">
        <q-tab-panel name="vers" style="height: calc(100vh - 220px); width: 100%">
          <typ-ver @loadTyp="onLoadTyp" />
        </q-tab-panel>

        <q-tab-panel name="typroles">
          <typ-role />
        </q-tab-panel>

        <q-tab-panel name="clustfactors">
          <cluster-factor-page />
        </q-tab-panel>

        <q-tab-panel name="clss">
          <cls-page />
        </q-tab-panel>

        <q-tab-panel name="notextended" v-if="typ.parent">
          <not-expended :typParent="typ.parent" />
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "@/boot/axios";
import TypVer from "@/pages/typ/ver/TypVer.vue";
import TypRole from "@/pages/typ/typrole/TypRole.vue";
import ClusterFactorPage from "@/pages/typ/clusterfactor/ClusterFactorPage.vue";
import ClsPage from "@/pages/typ/cls/ClsPage.vue";
import NotExpended from "@/pages/typ/notextended/NotExtended.vue";

const route = useRoute();
const router = useRouter();

const tab = ref("vers");
const typId = ref(null);
const typ = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "typPage",
    params: {
      typ: typId.value,
    },
  });
};

const infoTyp = () => {
  return typ.value.cod + " - " + typ.value.name;
};

const fnChangePn = () => {};

const onLoadTyp = (typVal) => {
  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "typ/loadRec",
      params: [typVal],
    })
    .then((response) => {
      typ.value = response.data.result.records[0];
    })
    .catch((error) => {
      console.log(error);
    })
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  typId.value = parseInt(route.params.typ, 10);
  tab.value = route.params.tab || "vers";

  onLoadTyp(typId.value);
});
</script>

<style scoped></style>
