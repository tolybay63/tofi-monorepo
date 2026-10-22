<template>
  <h6> Не определено</h6>
</template>

<script setup>
const props = defineProps({
  dimMultiPropItem: [Number, String],
});

const load = (id) => {
  console.log(id);
};

defineExpose({
  load,
});
</script>

<style scoped></style>
