<template>
<!--icon="task" icon="check_circle"-->
  <div class="q-pa-sm-sm bg-indigo-1"  style="height: calc(100vh - 550px); width: 100%">
    <q-tabs v-model="tab" class="text-teal" style="height: 50px">
      <q-tab name="props" label="Свойства" no-caps/>
      <q-tab name="data" label="Исходные данные" no-caps/>
      <q-tab name="res" label="Результат расчета" no-caps/>
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="props" class="q-pa-none">
        <props-calc-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>

      <q-tab-panel name="data" class="q-py-md q-px-none">
        <props-data-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>

      <q-tab-panel name="res" class="q-py-md q-px-none">
        <props-res-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>

    </q-tab-panels>
  </div>


</template>


<script setup>
import {ref} from "vue";
import DescPage from "@/pages/calcstock/props/main/DescPage.vue";
import PropsPageBayes from "@/pages/calcstock/props/main/PropsPageBayes.vue";
import PropsPageDeterm from "@/pages/calcstock/props/main/PropsPageDeterm.vue";
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
