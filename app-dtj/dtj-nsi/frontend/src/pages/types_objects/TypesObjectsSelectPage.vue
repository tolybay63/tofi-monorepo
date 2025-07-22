<template>
  <div class="q-pa-md bg-green-1">
    <q-banner dense inline-actions class="bg-orange-1">

      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="dashboard"></q-avatar>
        {{ $t("types_objects_comps") }}
      </div>

    </q-banner>

    <div>
      <q-inner-loading :showing="loading" color="secondary" />
    </div>

    <q-tabs dense v-model="tab" active-color="purple" class="text-teal no-scroll">
      <q-tab name="types" no-caps :label="$t('types_objects')" />
      <q-tab name="compenents" no-caps :label="$t('components')" />
      <q-tab name="comp_objects" no-caps :label="$t('comp_objects')" />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="types" class="no-scroll">
        <types-objects-page></types-objects-page>
      </q-tab-panel>

      <q-tab-panel name="compenents" class="no-scroll">
        <components-page></components-page>
      </q-tab-panel>

      <q-tab-panel name="comp_objects" class="no-scroll">
        <component-objects-page codRelTyp="RT_Components"></component-objects-page>
      </q-tab-panel>

    </q-tab-panels>

  </div>
</template>

<script>
import TypesObjectsPage from "pages/types_objects/TypesObjectsPage.vue";
import ComponentsPage from "pages/types_objects/ComponentsPage.vue";
import ComponentObjectsPage from "pages/types_objects/ComponentObjectsPage.vue";

export default {
  name: "TypesObjectsSelectPage",
  components: {ComponentObjectsPage, ComponentsPage, TypesObjectsPage},

  data: function () {
    return {
      loading: false,
      tab: "types"
    }
  },



}
</script>

<style scoped>

</style>
