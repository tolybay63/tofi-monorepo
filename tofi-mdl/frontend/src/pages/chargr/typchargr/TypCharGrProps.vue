<template>
  <q-page padding>
    <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
      {{ info.cod }} - {{ info.tcgName }}
      <span style="font-size: 0.6em; color: chocolate; font-weight: normal; margin-bottom: 10px ">
        ({{ info.dbTitle }})
      </span>

      <q-space />
      <q-btn
        dense
        round
        color="secondary"
        glossy
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
        <typ-char-gr-prop
          :typCharGr="typCharGr"
          :dense="dense"
          ref="childProp"
        />
      </template>

      <template v-slot:after>
        <typ-char-gr-multi-prop
          :typCharGr="typCharGr"
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
import TypCharGrProp from "@/pages/chargr/typchargr/TypCharGrProp.vue";
import TypCharGrMultiProp from "@/pages/chargr/typchargr/TypCharGrMultiProp.vue";

const router = useRouter();
const route = useRoute();

const splitterModel = ref(50);
const typCharGr = ref(0);
const info = ref({});
const dense = ref(true);
const loading = ref(false);

const childProp = ref(null);
const childMultiProp = ref(null);

const toBack = () => {
  router.push({
    name: "chargr",
    params: {
      chargr: typCharGr.value,
      tab: "typchargr",
    },
  });
};

onMounted(() => {
  typCharGr.value = parseInt(route.params.typCharGr, 10);

  // Вызываем метод загрузки у дочерних компонентов
  if (childProp.value && childProp.value.loadData) {
    childProp.value.loadData(typCharGr.value);
  }
  if (childMultiProp.value && childMultiProp.value.loadData) {
    childMultiProp.value.loadData(typCharGr.value);
  }

  loading.value = true;
  api
    .post("", {
      id: "1",
      method: "typ/loadTypCharGrInfo",
      params: [typCharGr.value],
    })
    .then((response) => {
      info.value = response.data.result.records[0] || {};
    })
    .catch((error) => {
      console.error(error);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
