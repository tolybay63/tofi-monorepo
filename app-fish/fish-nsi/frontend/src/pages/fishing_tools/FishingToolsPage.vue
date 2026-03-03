<template>
  <obj-page-list :title="$t('fishing_tools')" :typCod="'Typ_FishGear'"/>
</template>


<script>


import {defineComponent} from "vue";
import ObjPageList from "pages/objlist/ObjPageList.vue";

export default defineComponent ({
  name: "FishingToolsPage",
  components: {ObjPageList},

  data() {
  },

  methods: {
  },

  created() {
  },

  setup() {
  },

})

</script>

<style scoped>
.sticky-header-table {
  /* Ограничиваем высоту контейнера, чтобы появилась прокрутка */
  max-height: 100%;
  overflow: auto;
}

.sticky-header-table table {
  /* Убираем схлопывание границ, чтобы sticky работал корректно в некоторых браузерах */
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  /* Делаем заголовок липким */
  position: sticky;
  top: 0;
  /* Z-index нужен, чтобы содержимое body не перекрывало заголовок */
  z-index: 1;
  /* Фон обязателен, иначе заголовок будет прозрачным */
  background-color: #607d8b; /* Аналог bg-blue-grey-13 */
}

/* Опционально: если у таблицы есть границы, фиксируем их отображение */
.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>
