<template>
  <div class="column no-wrap fit">

    <h3> ReservoirPage </h3>

  </div>
</template>


<script setup>

</script>

<style scoped>
.custom-table {
  display: flex;
  flex-direction: column;
  height: 100% !important;
}

/* Шапка таблицы фиксируется */
:deep(.q-table__top) {
  padding-left: 0;
  padding-right: 0;
  flex-shrink: 0;
}

/* Контейнер сетки растягивается на всю оставшуюся высоту и получает скролл */
:deep(.q-table__grid-content) {
  padding: 0;
  flex: 1 1 auto;
  overflow-y: auto !important;
}
</style>

