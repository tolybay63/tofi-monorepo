<template>
  <div class="relative-position fit bg-indigo-1">
    <!-- Панели сверху -->
    <div class="relative-position" style="height: calc(100% - 48px);">
      <q-tab-panels v-model="tab" animated class="absolute-full bg-transparent q-pa-none">
        <q-tab-panel name="desc" class="q-pa-none relative-position fit">
          <desc-page class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>

        <q-tab-panel name="props" class="q-pa-none relative-position fit">
          <props-page-bayes class="absolute-full" :name="props.name" :own="props.own"/>
        </q-tab-panel>
      </q-tab-panels>
    </div>

    <!-- Табы строго внизу -->
    <q-tabs v-model="tab" class="text-teal bg-white shadow-1 absolute-bottom" indicator-color="red"
            active-bg-color="red-1" style="height: 48px; z-index: 2;" dense align="left" no-caps>
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
