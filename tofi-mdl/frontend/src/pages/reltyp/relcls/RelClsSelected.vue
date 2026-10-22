<template>
  <q-page class="q-pa-sm-sm">
    <div class="bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("relcls") }}:
        </div>
        <div style="color: black; margin-left: 10px">
          {{ infoRelCls() }}
        </div>

        <q-space />

        <q-btn
          dense round color="secondary" glossy
          icon="arrow_back" @click="toBack"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>
        <q-tab
          name="vers"
          no-caps
          icon="event_note"
          :label="$t('vers')"
        />
      </q-tabs>

      <q-tab-panels v-model="tab" animated>
        <q-tab-panel
          name="vers"
          style="height: calc(100vh - 220px); width: 100%"
        >
          <rel-cls-ver @loadRelCls="onLoadRelCls" />
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";
import RelClsVer from "@/pages/reltyp/relcls/RelClsVer.vue";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const tab = ref("vers");
const reltypId = ref(0);
const relclsId = ref(0);
const relcls = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "reltypSelected",
    params: {
      reltyp: reltypId.value,
      relcls: relclsId.value,
      tab: "relcls",
    },
  });
};

const infoRelCls = () => {
  return (relcls.value.cod || "") + " - " + (relcls.value.name || "");
};

const onLoadRelCls = (idVal) => {
  loading.value = true;
  api
    .post("", {
      method: "relcls/loadRec",
      params: [idVal],
    })
    .then((response) => {
      relcls.value = response.data.result.records[0];
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  reltypId.value = parseInt(route.params.reltyp, 10);
  relclsId.value = parseInt(route.params.relcls, 10);
  tab.value = route.params.tab || "vers";
  onLoadRelCls(relclsId.value);
});
</script>

<style scoped></style>
