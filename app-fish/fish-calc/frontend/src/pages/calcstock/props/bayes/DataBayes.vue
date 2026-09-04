<template>
  <div class="relative-position fit bg-indigo-1">
    <div class="relative-position" style="height: calc(100% - 48px);">
      <q-tab-panels v-model="tab" animated class="absolute-full bg-transparent q-pa-none">

        <q-tab-panel name="reservoir" class="q-pa-none relative-position fit">
          <ReservoirPage class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
        <q-tab-panel name="fish" class="q-pa-none relative-position fit">
          <FishPage class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
        <q-tab-panel name="rand" class="q-pa-none relative-position fit">
          <RandPage class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
        <q-tab-panel name="numbers" class="q-pa-none relative-position fit">
          <NumbersPage class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
        <q-tab-panel name="pdu" class="q-pa-none relative-position fit">
          <PduPage class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
        <q-tab-panel name="weight" class="q-pa-none relative-position fit">
          <WeigthFishPage class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
      </q-tab-panels>
    </div>

    <q-tabs v-model="tab" class="text-teal bg-white shadow-1 absolute-bottom" style="height: 48px; z-index: 2;" dense align="left" no-caps>
      <q-tab name="reservoir" label="Свойства водоема"/>
      <q-tab name="fish" label="Свойства рыбы"/>
      <q-tab name="rand" label="Случайные величины"/>
      <q-tab name="numbers" label="Начальная численность"/>
      <q-tab name="pdu" label="ПДУ"/>
      <q-tab name="weight" label="Средний вес рыбы"/>
    </q-tabs>
  </div>
</template>


<script setup>

import {ref} from "vue";
import ReservoirPage from "./data-bayes/ReservoirPage.vue";
import FishPage from "./data-bayes/FishPage.vue";
import RandPage from "./data-bayes/RandPage.vue";
import NumbersPage from "./data-bayes/NumbersPage.vue";
import PduPage from "./data-bayes/PduPage.vue";
import WeigthFishPage from "./data-bayes/WeigthFishPage.vue";

const props = defineProps({
  own: Number,
  name: String
})

const tab = ref("reservoir")

</script>

<style scoped>

</style>
