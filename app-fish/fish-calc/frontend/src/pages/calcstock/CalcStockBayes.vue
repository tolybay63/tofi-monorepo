
<script setup>
import {onMounted, ref} from "vue";
import {useRoute} from "vue-router";
import CalcStockPage from "@/pages/calcstock/CalcStockPage.vue";
const route = useRoute()

const loading = ref(false);


onMounted(() => {
  loading.value = false

})

</script>

<template>
  <calc-stock-page codCls="Cls_CalcBayes"></calc-stock-page>
</template>

<style scoped>

</style>
