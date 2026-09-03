<template>
  <div class="column no-wrap fit bg-indigo-1 q-pa-sm">
    <q-tabs v-model="tab" class="text-teal col-auto bg-white shadow-1" dense align="left">
      <q-tab name="props" label="Свойства" no-caps/>
      <q-tab name="data" label="Исходные данные" no-caps/>
      <q-tab name="res" label="Результат расчета" no-caps/>
    </q-tabs>

    <q-tab-panels v-model="tab" animated class="col bg-transparent q-mt-sm">
      <q-tab-panel name="props" class="q-pa-none column no-wrap fit">
        <props-calc-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>

      <q-tab-panel name="data" class="q-py-md q-px-none column no-wrap fit">
        <props-data-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>

      <q-tab-panel name="res" class="q-py-md q-px-none column no-wrap fit">
        <props-res-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script setup>
import {ref} from "vue";
import PropsCalcBayes from "@/pages/calcstock/props/bayes/PropsCalcBayes.vue";
import PropsResBayes from "@/pages/calcstock/props/bayes/PropsResBayes.vue";
import PropsDataBayes from "@/pages/calcstock/props/bayes/PropsDataBayes.vue";

const props = defineProps({
  name: String,
  own: Number
})

const tab = ref("props")
</script>

<style scoped>
</style>
