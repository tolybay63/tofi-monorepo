<template>
  <q-banner class="bg-green-1" dense inline-actions>
    <div class="row">
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" icon="code" text-color="white"></q-avatar>
        {{ title }} <span class="text-caption text-orange">({{ proxy?.$t("calcBayes") }})</span>
      </div>
      <q-space/>
      <q-avatar color="secondary text-white cursor-pointer" dense glossy icon="arrow_back" round @click="toBack">
        <q-tooltip transition-hide="rotate" transition-show="rotate">
          {{ $t('back') }}
        </q-tooltip>
      </q-avatar>
    </div>
  </q-banner>

  <h1 class="absolute-center"> Интерфейс Байесовского расчета</h1>

</template>

<script setup>
import {getCurrentInstance, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";

const router = useRouter();
const route = useRoute()
const {proxy} = getCurrentInstance()


const id = ref(0)
const title = ref("")

const toBack = () => {
  router["push"]({name: 'CalcStackBayes'})
}

onMounted(() => {
  id.value = parseInt(route.params["id"], 10)
  title.value = route.params["title"]
  console.log("id", id);
  console.log("title", title);
})

</script>

<style scoped>

</style>
