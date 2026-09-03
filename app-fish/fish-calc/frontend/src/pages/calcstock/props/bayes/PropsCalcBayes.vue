<template>
  <!-- Корневой контейнер на всю высоту с flex-колонкой -->
  <div class="column no-wrap fit bg-indigo-1">

    <!-- Панели занимают всё гибкое пространство сверху (class="col") -->
    <q-tab-panels v-model="tab" animated class="col bg-transparent q-pa-none">
      <q-tab-panel class="q-py-none q-px-none column no-wrap fit" name="desc">
        <desc-page :name="props.name" :own="props.own"/>
      </q-tab-panel>

      <q-tab-panel class="q-py-none q-px-none column no-wrap fit" name="props">
        <props-page-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>
    </q-tab-panels>

    <!-- Табы располагаются в самом низу и фиксируют свой размер (class="col-auto") -->
    <q-tabs v-model="tab" class="text-teal col-auto bg-white shadow-1" dense align="left" no-caps>
      <q-tab name="desc" label="Описание"/>
      <q-tab name="props" label="Основные свойства"/>
    </q-tabs>

  </div>
</template>

<script setup>
import DescPage from "@/pages/calcstock/props/main/DescPage.vue";
import PropsPageBayes from "@/pages/calcstock/props/main/PropsPageBayes.vue";
import {ref} from "vue";

const props = defineProps({
  own: Number,
  name: String
})

const tab = ref("desc")
</script>

<style scoped>
</style>
