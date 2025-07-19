<template>

  <div class="q-pa-sm-sm bg-green-1">

    <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="view_in_ar"></q-avatar>
        {{ $t("cubes") }}:
        <span style="color: black; margin-left: 10px">
          <strong>{{ this.infoCube() }} </strong>
        </span>
      </div>

    </q-banner>

    <q-tabs dense v-model="tab" class="text-teal no-scroll justify-center"
            @update:model-value="tabChange"
    >

      <q-btn dense glossy round color="secondary" icon="arrow_back"
             class="q-ml-lg" @click="toBack()">
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("back") }}
        </q-tooltip>
      </q-btn>
      <q-space/>
      <q-tab
        name="dims"
        no-caps
        icon="type_specimen"
        :label="$t('dims')"
      />

      <q-tab
        name="edjesprop"
        no-caps
        icon="credit_score"
        :label="$t('edjesprop')"
      />

      <q-tab
        name="edjesobj"
        no-caps
        icon="pattern"
        :label="$t('edjesobj')"
      />

      <!--      <q-tab
              name="dependobj"
              no-caps
              icon="webhook"
              :label="$t('dependobj')"
            />-->

      <q-tab
        name="algs"
        no-caps
        icon="functions"
        :label="$t('algs')"
      />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>

      <q-tab-panel name="dims" class="no-scroll">
        <dims-page :dOrg="cube['dOrg']" :cubeS="cube['id']"/>
      </q-tab-panel>

      <q-tab-panel name="edjesprop" class="no-scroll">
        <edjes-prop-page :dOrg="cube['dOrg']" :cubeS="cube['id']"/>
      </q-tab-panel>

      <q-tab-panel name="edjesobj" class="no-scroll">
        <edjes-obj-page :dOrg="cube['dOrg']" :cubeS="cube['id']"></edjes-obj-page>
      </q-tab-panel>

      <!--      <q-tab-panel name="dependobj" class="no-scroll">
              <depend-obj-page></depend-obj-page>
            </q-tab-panel>-->

      <q-tab-panel name="algs" class="no-scroll">
        <alg-page></alg-page>
      </q-tab-panel>

    </q-tab-panels>

  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios.js";
import DimsPage from "pages/cubes/tabs/DimsPage.vue";
import EdjesPropPage from "pages/cubes/tabs/EdjesPropPage.vue";
import EdjesPropFltPage from "pages/cubes/tabs/EdjesPropFltPage.vue";
import EdjesObjPage from "pages/cubes/tabs/EdjesObjPage.vue";
import DependObjPage from "pages/cubes/tabs/DependObjPage.vue";
import AlgPage from "pages/cubes/tabs/AlgPage.vue";
import {notifyError} from "src/utils/jsutils.js";

export default {
  name: "CubeSelected",
  components: {AlgPage, DependObjPage, EdjesObjPage, EdjesPropFltPage, EdjesPropPage, DimsPage},

  data() {
    return {
      tab: ref(""),
      cubesGrId: 0,
      cubesId: 0,
      cube: {},
    }
  },

  methods: {
    toBack() {
      this.$router["push"]({
        name: "CubesPage",
        params: {
          cubesGr: this.cubesGrId,
          cubesId: this.cubesId,
        },
      });
    },

    tabChange(v) {
      //console.log("tabChange", v);
      if (v === "edjesprop") {
        this.checkDimProp(this.cubesId);
      }
      if (v === "edjesobj") {
        this.checkDimObj(this.cubesId);
      }
    },

    checkDimProp(cubeId) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "cubes/checkDimProp",
          params: [cubeId],
        })
        .then(() => {
        })
        .catch((error) => {
          notifyError(this.$t(error.response.data.error.message));
          this.tab = ref("dims");
        })
        .finally(() => {
          this.loading = ref(false);
        });
    },

    checkDimObj(cubeId) {
      this.loading = ref(true);
      api
        .post(baseURL, {
          method: "cubes/checkDimObj",
          params: [cubeId],
        })
        .then(() => {
        })
        .catch((error) => {
          notifyError(this.$t(error.response.data.error.message));
          this.tab = ref("dims");
        })
        .finally(() => {
          this.loading = ref(false);
        });
    },

    infoCube() {
      let inf = this.cube.cod + " - " + this.cube.name;
      if (this.cube.cubeSType === 1)
        inf = inf + " (" + this.$t("std") + ")"
      else
        inf = inf + " (" + this.$t("calc") + ")"

      return inf;
    }

  },

  mounted() {
    //console.log("mounted!", this.$route.params.prop);
    this.cubesGrId = parseInt(this.$route["params"].cubesGr, 10);
    this.cubesId = parseInt(this.$route["params"].cubes, 10);

    this.loading = ref(true);
    api
      .post(baseURL, {
        method: "cubes/loadRec",
        params: [this.cubesId],
      })
      .then((response) => {
        //console.log("cube info", response.data.result.records)
        this.cube = response.data.result.records[0];
      })
      .catch((error) => {
        console.log(error);
      })
      .finally(() => {
        this.tab = ref("dims");
        this.loading = ref(false);
      });

  }
}
</script>

<style scoped>

</style>
