<template>
  <div class="relative-position fit bg-indigo-1">

    <div class="relative-position" style="height: calc(100% - 48px);">
      <q-tab-panels v-model="tab" animated class="absolute-full bg-transparent q-pa-none">

        <q-tab-panel name="numbers" class="q-pa-none relative-position fit">
          <NumbersPage :name="props.name" :own="props.own"/>
        </q-tab-panel>

        <q-tab-panel name="mass" class="q-pa-none relative-position fit">
          <MassPage :name="props.name" :own="props.own"/>
        </q-tab-panel>

        <q-tab-panel name="mortality" class="q-pa-none relative-position fit">
          <MortalityPage :name="props.name" :own="props.own"/>
        </q-tab-panel>

      </q-tab-panels>
    </div>


    <q-tabs v-model="tab" active-bg-color="red-1" align="left"
            class="text-teal bg-white shadow-1 absolute-bottom" dense indicator-color="red" no-caps style="height: 48px; z-index: 2;">
      <q-tab name="numbers" label="Показатели численности"/>
      <q-tab name="mass" label="Показатели ихтиомассы"/>
      <q-tab name="mortality" label="Показатели естественной смертности"/>
    </q-tabs>
  </div>
</template>

<script setup>

import {ref} from "vue";
import NumbersPage from "@/pages/calcstock/props/bayes/result-bayes/NumbersPage.vue";
import MassPage from "@/pages/calcstock/props/bayes/result-bayes/MassPage.vue";
import MortalityPage from "@/pages/calcstock/props/bayes/result-bayes/MortalityPage.vue";

const props = defineProps({
  own: Number,
  name: String
})

const tab = ref("numbers")

</script>

<style scoped>

</style>
