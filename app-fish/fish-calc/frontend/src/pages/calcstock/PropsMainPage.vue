<template>
  <q-page class="column no-wrap fit bg-green-1 q-pa-sm">
    <!-- Табы фиксированной высоты сверху -->
    <q-tabs v-model="tab" class="text-teal col-auto bg-white shadow-1" dense align="left"
            active-bg-color="red-1" indicator-color="red">
      <q-tab name="desc" label="Описание" no-caps/>
      <q-tab name="props" label="Основные свойства" no-caps/>
    </q-tabs>

    <!-- Панели занимают всё оставшееся пространство (col) -->
    <q-tab-panels v-model="tab" animated class="col bg-transparent q-mt-sm column no-wrap">

      <q-tab-panel name="desc" class="q-pa-none column no-wrap fit">
        <h3 v-if="!props.data">
          Расчет не выбран
        </h3>
        <div v-else class="column no-wrap fit">
          <desc-page :name="props.data.name" :own="props.data.id"/>
        </div>
      </q-tab-panel>

      <q-tab-panel name="props" class="q-pa-none column no-wrap fit">
        <h3 v-if="!props.data">
          Расчет не выбран
        </h3>
        <div v-else class="column no-wrap fit">
          <div v-if="codCalc==='Cls_CalcBayes'" class="column no-wrap fit">
            <props-page-bayes :name="props.data.name" :own="props.data.id"/>
          </div>
          <div v-else-if="codCalc==='Cls_CalcDeterm'" class="column no-wrap fit">
            <props-page-determ :name="props.data.name" :own="props.data.id"/>
          </div>
        </div>
      </q-tab-panel>

    </q-tab-panels>
  </q-page>
</template>

<script setup>
import {ref} from "vue";
import DescPage from "@/pages/calcstock/props/main/DescPage.vue";
import PropsPageBayes from "@/pages/calcstock/props/main/PropsPageBayes.vue";
import PropsPageDeterm from "@/pages/calcstock/props/main/PropsPageDeterm.vue";

const props = defineProps({
  data: Object,
  codCalc: String
})

const tab = ref("desc")
</script>

<style scoped>
</style>
