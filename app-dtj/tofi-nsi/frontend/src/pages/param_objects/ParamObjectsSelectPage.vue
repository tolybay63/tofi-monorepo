<template>
  <div class="q-pa-md bg-green-1">
    <q-banner dense inline-actions class="bg-orange-1">

      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="dashboard"></q-avatar>
        {{ $t("param_objects") }}
      </div>

    </q-banner>

    <div>
      <q-inner-loading :showing="loading" color="secondary" />
    </div>

    <q-tabs dense v-model="tab" active-color="purple" class="text-teal no-scroll">
      <q-tab name="params" no-caps :label="$t('params')" />
      <q-tab name="params_component" no-caps :label="$t('params_component')" />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>

      <q-tab-panel name="params" class="no-scroll">
        <params-page></params-page>
      </q-tab-panel>

      <q-tab-panel name="params_component" class="no-scroll">
        <param-component-page codRelTyp="RT_ParamsComponent"></param-component-page>
      </q-tab-panel>

    </q-tab-panels>

  </div>
</template>

<script>


import ParamsPage from "pages/param_objects/ParamsPage.vue";
import ParamComponentPage from "pages/param_objects/ParamComponentPage.vue";

export default {
  name: "ParamObjectsSelectPage",
  components: {ParamComponentPage, ParamsPage },

  data: function () {
    return {
      loading: false,
      tab: "params"
    }
  },



}
</script>

<style scoped>

</style>
