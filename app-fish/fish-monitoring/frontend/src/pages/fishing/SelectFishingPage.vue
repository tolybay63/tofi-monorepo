<script setup>

import ChooseFishing from "@/pages/fishing/ChooseFishing.vue";
import {useQuasar} from "quasar";
import {useRouter} from "vue-router";
import {useI18n} from "vue-i18n";
import {notifyInfo} from "@/utils/jsutils.js";

const router = useRouter()
const $q = useQuasar()
const {t} = useI18n()

$q.dialog({
  component: ChooseFishing,
  componentProps: {},
})
  .onOk((r) => {
    //console.log("onOk", r)
    const reservoirs = r.reservoirs.join(",")
    const dbeg = r.dbeg
    const dend = r.dend

    router.push({
      name: "FishingPage",
      params: {
        reservoirs: reservoirs,
        dbeg: dbeg,
        dend: dend
      }
    })
  })
  .onCancel(() => {
    notifyInfo(t("canceled"))
  })
</script>

<template>

</template>

<style scoped>

</style>
