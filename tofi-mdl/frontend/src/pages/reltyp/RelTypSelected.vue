<template>
  <q-page class="q-pa-sm-sm">
    <div class="bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("reltyp") }}:
        </div>
        <div style="color: black; margin-left: 10px"> {{ infoRelTyp() }}</div>
        <q-space />

        <q-btn
          dense round color="secondary" icon="arrow_back" glossy
          @click="toBack"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>
        <q-tab name="vers" no-caps icon="event_note" :label="$t('vers')" />
        <q-tab name="reltyproles" no-caps icon="perm_contact_calendar" :label="$t('roles2')" />
        <q-tab name="members" no-caps icon="group_work" :label="$t('members')" />
        <q-tab name="relcls" no-caps icon="settings_applications" :label="$t('relcls')" />
      </q-tabs>

      <q-tab-panels v-model="tab" animated>
        <q-tab-panel
          name="vers"
          style="height: calc(100vh - 220px); width: 100%"
        >
          <rel-typ-ver @loadRelTyp="onLoadRelTyp" />
        </q-tab-panel>

        <q-tab-panel name="reltyproles">
          <rel-typ-role />
        </q-tab-panel>

        <q-tab-panel name="members">
          <rel-typ-member />
        </q-tab-panel>

        <q-tab-panel name="relcls">
          <rel-cls />
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "@/boot/axios";
import RelTypVer from "@/pages/reltyp/RelTypVer.vue";
import RelTypRole from "@/pages/reltyp/reltyprole/RelTypRole.vue";
import RelTypMember from "@/pages/reltyp/members/RelTypMember.vue";
import { notifyError } from "@/utils/jsutils";
import RelCls from "@/pages/reltyp/relcls/RelCls.vue";

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const tab = ref("vers");
const reltypId = ref(0);
const relclsId = ref(0);
const reltyp = ref({});
const loading = ref(false);

const toBack = () => {
  router.push({
    name: "reltypPage",
    params: {
      reltyp: reltypId.value,
    },
  });
};

const infoRelTyp = () => {
  return (reltyp.value.cod || "") + " - " + (reltyp.value.name || "");
};

const onLoadRelTyp = (reltypVal) => {
  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "reltyp/loadRec",
      params: [{ id: reltypVal }],
    })
    .then((response) => {
      reltyp.value = response.data.result.records[0];
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
  onLoadRelTyp(reltypId.value);
});
</script>

<style scoped></style>
