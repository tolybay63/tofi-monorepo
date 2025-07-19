<template>
  <q-page padding>

    <q-bar class="bg-green-1" style="font-size: 1.2em; font-weight: bold;">
      {{ info.cod }} - {{ info.tcgName }}
      <span style="font-size: 0.6em; color: chocolate; font-weight: normal; margin-bottom: 10px ">
        ({{ info.dbTitle }})
      </span>

      <q-space/>
      <q-btn
          dense round color="secondary" glossy
          icon="arrow_back" @click="toBack()"
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
        <typ-char-gr-prop
            :typCharGr="typCharGr"
            :dense="dense"
            ref="childProp"
        />
      </template>

      <template v-slot:after>
        <typ-char-gr-multi-prop
            :typCharGr="typCharGr"
            :dense="dense"
            ref="childMultiProp"
        />
      </template>
    </q-splitter>


  </q-page>
</template>

<script>
import TypCharGrMultiProp from "pages/chargr/typchargr/TypCharGrMultiProp.vue";
import TypCharGrProp from "pages/chargr/typchargr/TypCharGrProp.vue";
import {ref} from "vue";
import {api, baseURL} from "boot/axios";

export default {
  name: 'TypCharGrProps',
  components: {TypCharGrMultiProp, TypCharGrProp},

  data: function () {
    return {
      splitterModel: ref(50),
      typCharGr: 0,
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
          chargr: this.typCharGr,
          tab: "typchargr",
        },
      });
    },


  },

  created() {
    //console.info("created TypCharGrProps")
  },

  mounted() {
    //console.info("mounted TypCharGrProps", this.$route.params)

    this.typCharGr = parseInt(this.$route["params"].typCharGr, 10);
    this.tab = this.$route["params"].tab;

    this.$refs.childProp.loadData(this.typCharGr);
    this.$refs.childMultiProp.loadData(this.typCharGr);

    // load info
    this.loading = ref(true);
    api
        .post(baseURL, {
          id: "1",
          method: "typ/loadTypCharGrInfo",
          params: [this.typCharGr],
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
