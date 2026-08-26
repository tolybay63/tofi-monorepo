<template>

  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">

      <q-tab name="desc" no-caps icon="task" label="Описание"/>
      <q-tab
        name="prop1"
        no-caps
        icon="task"
        label="Group of Prop 1"
        style="margin-right: 10px"
      />

      <q-tab
        name="prop2"
        no-caps
        icon="task"
        label="Group of Prop 2"
        style="margin-right: 10px"
      />

    </q-tabs>

    <q-tab-panels v-model="tab" animated>

      <q-tab-panel name="desc" style="height: calc(100vh - 190px); width: 100%">
      </q-tab-panel>

      <q-tab-panel name="prop1" style="height: calc(100vh - 190px); width: 100%">
      </q-tab-panel>

      <q-tab-panel name="prop2" style="height: calc(100vh - 190px); width: 100%">
      </q-tab-panel>


    </q-tab-panels>
  </q-page>


</template>


<script setup>

import {ref} from "vue";

const tab = ref("desc")


</script>

<style scoped>

</style>
