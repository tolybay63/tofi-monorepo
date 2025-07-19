<template>
  <q-page padding>

    <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
      {{ info.cod }} - {{ info.rcgName }}
      <span style="font-size: 0.6em; color: chocolate; font-weight: normal; margin-bottom: 10px ">
        ({{ info.dbTitle }})
      </span>

      <q-space/>
      <q-btn
          dense glossy round color="secondary" icon="arrow_back" @click="toBack()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
    </q-bar>

    <q-splitter
        v-model="splitterModel"
        :model-value ="splitterModel"
        class="no-padding no-margin"
        before-class="overflow-hidden q-mr-sm"
        after-class="overflow-hidden q-ml-sm"
        style="height: 100%; width: 100%"
        separator-class="bg-red"
    >
      <template v-slot:before>
        <rel-typ-char-gr-prop
            :relTypCharGr="relCharGr"
            :dense="dense"
            ref="childProp"
        />
      </template>

      <template v-slot:after>
        <rel-typ-char-gr-multi-prop
            :relTypCharGr="relCharGr"
            :dense="dense"
            ref="childMultiProp"
        />
      </template>
    </q-splitter>


  </q-page>
</template>

<script>

import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import RelTypCharGrProp from "pages/chargr/relchargr/RelTypCharGrProp.vue";
import RelTypCharGrMultiProp from "pages/chargr/relchargr/RelTypCharGrMultiProp.vue";

export default {
  name: 'RelTypCharGrProps',
  components: {RelTypCharGrMultiProp, RelTypCharGrProp},

  data: function () {
    return {
      splitterModel: ref(50),
      relCharGr: 0,
      info: ref({}),
      dense: true,
    };
  },

  methods: {

    //$router.push('/chargr')
    toBack() {
      this.$router["push"]({
        name: "chargr",
        params: {
          chargr: this.relCharGr,
          tab: "relchargr",
        },
      });

    },


  },

  mounted() {
    //console.info("mounted RelTypCharGrProps", this.$route.params)
    this.relCharGr = parseInt(this.$route["params"].relTypCharGr, 10);
    this.$refs.childProp.loadData(this.relCharGr);
    this.$refs.childMultiProp.loadData(this.relCharGr);

    // load info
    this.loading = ref(true);
    api
        .post(baseURL, {
          id: "1",
          method: "reltyp/loadRelTypCharGrInfo",
          params: [this.relCharGr],
        })
        .then((response) => {
          this.info = response.data.result.records[0];
        })
        .catch((error) => {
          console.log(error);
        })
        .finally(() => {
          this.loading = ref(false);
        });

  }


}
</script>
