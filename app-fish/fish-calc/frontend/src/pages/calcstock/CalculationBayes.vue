<template>
  <div class="q-pa-none" style="height: 100%; width: 100%">
    <q-banner class="bg-green-1" dense inline-actions>
      <div class="row">
        <div style="font-size: 1.2em; font-weight: bold">
          <q-avatar dense color="black" icon="code" text-color="white"></q-avatar>
          {{ title }} <span class="text-caption text-orange">({{ proxy?.$t("calcBayes") }})</span>
        </div>
        <q-space/>
        <div class="q-gutter-lg">
          <q-avatar color="secondary text-white cursor-pointer" dense glossy icon="arrow_back" round @click="toBack">
            <q-tooltip transition-hide="rotate" transition-show="rotate">
              {{ $t('back') }}
            </q-tooltip>
          </q-avatar>

          <q-avatar color="red text-white cursor-pointer" dense glossy icon="start" @click="toCalc">
            <q-tooltip transition-hide="rotate" transition-show="rotate">
              Запустить расчет
            </q-tooltip>
          </q-avatar>
        </div>

      </div>
    </q-banner>


      <props-bayes-page :name="title" :own="id"/>

  </div>
</template>

<script setup>
import {getCurrentInstance, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {Notify} from "quasar";
import PropsBayesPage from "@/pages/calcstock/props/bayes/PropsBayesPage.vue";

const router = useRouter();
const route = useRoute()
const {proxy} = getCurrentInstance()


const id = ref(0)
const title = ref("")

const toBack = () => {
  router["push"]({name: 'CalcStockBayes'})
}

const toCalc = () => {
  Notify.create({
    type: "info",
    position: "top",
    timeout: 5000,
    message: "CalcStockBayes...",
  });

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
