<template>
  <q-page padding>
    <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
      {{ info.cod }} - {{ info.rcgName }}
      <span style="font-size: 0.6em; color: chocolate; font-weight: normal; margin-bottom: 10px ">
        ({{ info.dbTitle }})
      </span>

      <q-space />
      <q-btn
        dense
        glossy
        round
        color="secondary"
        icon="arrow_back"
        @click="toBack"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
    </q-bar>

    <q-splitter
      v-model="splitterModel"
      class="no-padding no-margin"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      style="height: 100%; width: 100%"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <rel-typ-char-gr-prop
          :relTypCharGr="relCharGr"
          :dense="dense"
          ref="childProp"
        />
      </template>

      <template v-slot:after>
        <rel-typ-char-gr-multi-prop
          :relTypCharGr="relCharGr"
          :dense="dense"
          ref="childMultiProp"
        />
      </template>
    </q-splitter>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { api } from "@/boot/axios";
import RelTypCharGrProp from "@/pages/chargr/relchargr/RelTypCharGrProp.vue";
import RelTypCharGrMultiProp from "@/pages/chargr/relchargr/RelTypCharGrMultiProp.vue";

const router = useRouter();
const route = useRoute();

const splitterModel = ref(50);
const relCharGr = ref(0);
const info = ref({});
const dense = ref(true);
const loading = ref(false);

const childProp = ref(null);
const childMultiProp = ref(null);

const toBack = () => {
  router.push({
    name: "chargr",
    params: {
      chargr: relCharGr.value,
      tab: "relchargr",
    },
  });
};

onMounted(() => {
  relCharGr.value = parseInt(route.params.relTypCharGr, 10);

  // Явный вызов загрузки (также поддерживается автоматическая
  // загрузка через watch в дочерних компонентах, которые мы написали ранее)
  if (childProp.value && childProp.value.loadData) {
    childProp.value.loadData(relCharGr.value);
  }
  if (childMultiProp.value && childMultiProp.value.loadData) {
    childMultiProp.value.loadData(relCharGr.value);
  }

  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "reltyp/loadRelTypCharGrInfo",
      params: [relCharGr.value],
    })
    .then((response) => {
      info.value = response.data.result.records[0] || {};
    })
    .catch((error) => {
      console.error("Ошибка при загрузке info:", error);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
