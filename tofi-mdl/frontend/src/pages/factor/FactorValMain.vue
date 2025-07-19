<template>
  <q-page class="q-pa-sm-sm bg-green-1">
    <q-tabs v-model="tab" class="text-teal">
      <div style="margin-left: 20px">
        {{ $t("factor") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ this.infoFactor() }}</strong>
        </span>
      </div>

      <q-space/>
      <q-btn
          dense round color="secondary" icon="arrow_back" glossy
          @click="toBack()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
      <q-tab name="val" no-caps icon="pin" :label="$t('factorVals')"/>
      <q-tab
          name="rel"
          no-caps
          icon="task"
          :label="$t('relFactors')"
          style="margin-right: 10px"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel name="val" style="height: calc(100vh - 190px); width: 100%">
        <factor-val/>
      </q-tab-panel>

      <q-tab-panel name="rel" style="height: calc(100vh - 190px); width: 100%">
        <factor-rel/>
      </q-tab-panel>
    </q-tab-panels>
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import FactorVal from "pages/factor/FactorVal.vue";
import FactorRel from "pages/factor/FactorRel.vue";
import {notifyError} from "src/utils/jsutils";

export default defineComponent({
  components: {FactorRel, FactorVal},

  data: function () {
    return {
      factor1_id: 0,
      factor: {},
    };
  },

  methods: {
    toBack() {
      this.$router["push"]({
        name: "FactorPage",
        params: {
          factor: this.factor1_id,
        },
      });
    },

    infoFactor() {
      return this.factor.cod + " - " + this.factor.name;
    },
  },

  mounted() {
    this.factor1_id = parseInt(this.$route["params"].factor, 10);
    //console.info("this.factor1_id", this.factor1_id)


    // load factor
    this.loading = ref(true);
    api
        .post(baseURL, {
          id: "1",
          method: "factor/loadRec",
          params: [{id: this.factor1_id}],
        })
        .then((response) => {
          //console.log("f", response.data.result.records)
          this.factor = response.data.result.records[0];
        })
        .catch((error) => {
          console.log(error);
          notifyError(error.message);
        })
        .finally(() => {
          this.loading = ref(false);
        });
    //
  },

  created() {
    //console.log("<<<<<  created")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
  },

  setup() {
    return {
      tab: ref("val"),
    };
  },
});
</script>

<style></style>
