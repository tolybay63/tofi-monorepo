<template>
  <div class="q-pa-sm-sm bg-indigo-1"  style="height: calc(100vh - 230px); width: 100%">

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel class="q-py-md q-px-none" name="desc">
        <desc-page :name="props.name" :own="props.own"/>
      </q-tab-panel>

      <q-tab-panel class="q-py-md q-px-none" name="props">
        <props-page-bayes :name="props.name" :own="props.own"/>
      </q-tab-panel>
    </q-tab-panels>

    <q-tabs v-model="tab" class="text-teal" style="align-items: start">
      <q-tab name="desc" label="Описание" no-caps/>
      <q-tab name="props" label="Основные свойства" no-caps
      />
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

const splitterModel = ref(20)
const tab = ref("desc")

</script>


<style scoped>

</style>
