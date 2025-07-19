<template>

  <q-page  class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("scale") }}:
        <span style="color: black; margin-left: 10px">
          <strong> {{info}} </strong>
        </span>
      </div>

      <q-space/>
      <q-btn
          dense round color="secondary" glossy
          icon="arrow_back" @click="toBack()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
      <q-tab name="val" no-caps icon="pin" :label="$t('val')"/>
      <q-tab
          name="cnf"
          no-caps icon="task" :label="$t('settings')"
          style="margin-right: 10px"
      />
    </q-tabs>
    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="val" style="height: calc(100vh - 190px); width: 100%">
        <scale-val-page ref="childVal"/>
      </q-tab-panel>

      <q-tab-panel name="cnf" style="height: calc(100vh - 190px); width: 100%">
        <scale-asgn-page ref="childAsgn"/>
      </q-tab-panel>
    </q-tab-panels>

  </q-page>

</template>

<script>

import {ref} from "vue";
import ScaleAsgnPage from "pages/scale/ScaleAsgnPage.vue";
import ScaleValPage from "pages/scale/ScaleValPage.vue";

export default {
  name: 'ScaleSelectedPage',
  components: {ScaleValPage, ScaleAsgnPage},
  data() {
    return {
      currentNode: null,
      tab: ref("val"),
      info: ""
    };
  },

  methods: {
    toBack() {
      this.$router["push"]({
        name: "ScalePage",
        params: {
          scale: this.scale,
        },
      });

    },

/*
    onUpdateSelect(data) {
      console.log("currentNode onUpdateSelect", this.currentNode)
      this.currentNode = data.selected !== undefined ? data.selected : null;
    },
*/

  },


  mounted() {
    this.info = this.$route["params"].info;
  },

  setup() {
  }
}
</script>
