<template>
  <q-page class="q-pa-sm-sm">
    <div class="bg-green-1">
      <q-tabs dense v-model="tab" class="text-bold text-teal">


        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          <q-avatar color="black" text-color="white" icon="table_chart"></q-avatar>
          {{ $t("chargr") }}
        </div>

        <q-space/>

        <q-tab
            name="typchargr"
            no-caps
            icon="view_quilt"
            :label="$t('chargrtyp')"
        />

        <q-tab
            name="relchargr"
            no-caps
            icon="view_column"
            :label="$t('chargrrel')"
        />

      </q-tabs>

      <q-tab-panels v-model="tab" animated>

        <q-tab-panel name="typchargr" style="height: calc(100vh - 190px); width: 100%">
          <typ-char-gr @updateSelect="onUpdateSelect" ref="childTyp"/>
        </q-tab-panel>

        <q-tab-panel name="relchargr">
          <rel-typ-char-gr @updateSelect="onUpdateSelect2" ref="childRel"/>

        </q-tab-panel>


      </q-tab-panels>
    </div>
  </q-page>
</template>

<script>
import {ref} from "vue";
import TypCharGr from "pages/chargr/typchargr/TypCharGr.vue";
import RelTypCharGr from "pages/chargr/relchargr/RelTypCharGr.vue";

export default {
  name: "CharGrPage",
  components: {
    RelTypCharGr,
    TypCharGr
  },

  data() {
    return {
      tab: ref("typchargr"),
    };
  },
  methods: {

    onUpdateSelect() {
    },

    onUpdateSelect2() {
    }

  },

  created() {
    //console.info("created CharGrPage", this.$route.params)
  },

  mounted() {
    //console.info("mounted CharGrPage", this.$route.params)
    let tab = this.$route["params"].tab
    this.tab = ref(tab)
  },

  setup() {
  },
};
</script>

<style scoped></style>
