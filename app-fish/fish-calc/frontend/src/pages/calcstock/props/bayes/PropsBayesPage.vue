<template>
  <div class="relative-position fit bg-indigo-1">
    <q-tabs v-model="tab" class="text-teal bg-white shadow-1" style="height: 48px;" dense align="left">
      <q-tab name="props" label="Свойства" no-caps/>
      <q-tab name="data" label="Исходные данные" no-caps/>
      <q-tab name="res" label="Результат расчета" no-caps/>
    </q-tabs>

    <div class="relative-position" style="height: calc(100% - 48px);">
      <q-tab-panels v-model="tab" animated class="absolute-full bg-transparent q-pa-none">
        <q-tab-panel name="props" class="q-pa-none relative-position fit">
          <props-calc-bayes class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>

        <q-tab-panel name="data" class="q-pa-none relative-position fit">
          <props-data-bayes class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>

        <q-tab-panel name="res" class="q-pa-none relative-position fit">
          <props-res-bayes class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
      </q-tab-panels>
    </div>
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
