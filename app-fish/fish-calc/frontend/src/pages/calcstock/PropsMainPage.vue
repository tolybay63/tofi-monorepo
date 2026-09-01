<template>

  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">

      <q-tab name="desc" no-caps icon="task" label="Описание"/>
      <q-tab
        name="props"
        no-caps
        icon="task"
        label="Основные свойства"
        style="margin-right: 10px"
      />

    </q-tabs>

    <q-tab-panels v-model="tab" animated>

      <q-tab-panel name="desc" class="q-py-md q-px-none" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data" >
          Расчет не выбран
        </h3>
        <div v-else>
          <desc-page :own="props.data.id" :name="props.data.name"/>
        </div>
      </q-tab-panel>

      <q-tab-panel name="props" class="q-py-md q-px-none" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data" >
          Расчет не выбран
        </h3>
        <div v-else>
          <props-page :own="props.data.id" :name="props.data.name"/>
        </div>
      </q-tab-panel>


    </q-tab-panels>
  </q-page>


</template>


<script setup>
import {ref} from "vue";
import DescPage from "@/pages/calcstock/props/main/DescPage.vue";
import PropsPage from "@/pages/calcstock/props/main/PropsPage.vue";

const props = defineProps({
  data: ref({}),
})

const tab = ref("desc")


</script>

<style scoped>

</style>
