<template>

  <obj-page-list :title="$t('fishing_tools')" :typCod="'Typ_FishGear'"></obj-page-list>

</template>


<script>


import {defineComponent} from "vue";
import ObjPageList from "pages/objlist/ObjPageList.vue";

export default defineComponent ({
  name: "FishingToolsPage",
  components: {ObjPageList},

  data() {
    return {}
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

</style>
