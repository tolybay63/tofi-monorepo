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

    <q-tab-panels v-model="tab" animated @transition="fnChange($event)">

      <q-tab-panel name="desc" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data" >
          Расчет не выбран
        </h3>
        <div v-else>
          <h3>Описание</h3>
          {{props.data}}
        </div>
      </q-tab-panel>

      <q-tab-panel name="prop1" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data" >
          Расчет не выбран
        </h3>
        <div v-else>
          <h3>Prop 1</h3>
          {{props.data}}
        </div>
      </q-tab-panel>

      <q-tab-panel name="prop2" style="height: calc(100vh - 190px); width: 100%">
        <h3 v-if="!props.data" >
          Расчет не выбран
        </h3>
        <div v-else>
          <h3>Prop 2</h3>
          {{props.data}}
        </div>

      </q-tab-panel>


    </q-tab-panels>
  </q-page>


</template>


<script setup>
import {ref} from "vue";
import {data} from "autoprefixer";

const props = defineProps({
  data: ref(Object),
})

const tab = ref("desc")

const fnChange = (e) => {
  console.log("fnChange", e)
  console.log("fnChange", props.data)
}


fnChange("desc")
</script>

<style scoped>

</style>
