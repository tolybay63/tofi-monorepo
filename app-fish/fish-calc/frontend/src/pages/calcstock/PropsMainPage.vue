<template>

  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">

      <q-tab icon="task" label="Описание" name="desc" no-caps/>
      <q-tab
        icon="task"
        label="Основные свойства"
        name="props"
        no-caps
        style="margin-right: 10px"
      />

    </q-tabs>

    <q-tab-panels v-model="tab" animated>

      <q-tab-panel class="q-py-md q-px-none" name="desc" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data">
          Расчет не выбран
        </h3>
        <div v-else>
          <desc-page :name="props.data.name" :own="props.data.id"/>
        </div>
      </q-tab-panel>

      <q-tab-panel class="q-py-md q-px-none" name="props" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data">
          Расчет не выбран
        </h3>
        <div v-else>
          <div v-if="codCalc==='Cls_CalcBayes'">
            <props-page-bayes :name="props.data.name" :own="props.data.id"/>
          </div>
          <div v-else-if="codCalc==='Cls_CalcDeterm'">
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
  data: ref({}),
  codCalc: String
})

const tab = ref("desc")


</script>

<style scoped>

</style>
