<script setup>

import ChooseFishing from "@/pages/fishing/ChooseFishing.vue";
import {useQuasar} from "quasar";
import FishingPage from "@/pages/fishing/FishingPage.vue";
import {useRouter} from "vue-router";
const router = useRouter()
const $q = useQuasar()

let isFishing = false

$q.dialog({
  component: ChooseFishing,
  componentProps: {
  },
}).onOk((r) => {
  console.log(r)
  isFishing = true
  router.push({ path: "/fishing" })
})
.onCancel(()=> {
  console.log("Cancelled")
  })
</script>

<template>

  <div v-if="isFishing">
    <fishing-page></fishing-page>
  </div>
</template>

<style scoped>

</style>
