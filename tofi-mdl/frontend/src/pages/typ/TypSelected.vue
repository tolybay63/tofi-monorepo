<template>
  <q-page class="q-pa-sm-sm">
    <div class="bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("typ") }}:
        </div>
        <span style="color: black; margin-left: 10px">
            {{ this.infoTyp() }}
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

        <q-tab name="vers" no-caps icon="event_note" :label="$t('vers')"/>
        <q-tab name="typroles" no-caps icon="perm_contact_calendar" :label="$t('roles2')" />
        <q-tab name="clustfactors" no-caps icon="pie_chart_outline" :label="$t('clusterFactors')" />
        <q-tab name="clss" no-caps icon="group_work" :label="$t('clss')"/>
        <q-tab v-if="typ.parent" name="notextended" no-caps icon="unpublished" :label="$t('notextended')"/>

      </q-tabs>

      <q-tab-panels v-model="tab" animated @transition="fnChangePn">
        <q-tab-panel name="vers" style="height: calc(100vh - 220px); width: 100%" >
          <typ-ver @loadTyp="onLoadTyp"/>
        </q-tab-panel>

        <q-tab-panel name="typroles">
          <typ-role/>
        </q-tab-panel>

        <q-tab-panel name="clustfactors">
          <cluster-factor-page/>
        </q-tab-panel>

        <q-tab-panel name="clss">
          <cls-page/>
        </q-tab-panel>

        <q-tab-panel name="notextended" v-if="typ.parent">
          <not-expended :typParent="typ.parent"/>
        </q-tab-panel>

      </q-tab-panels>
    </div>
  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import TypVer from "pages/typ/ver/TypVer.vue";
import TypRole from "pages/typ/typrole/TypRole.vue";
import ClusterFactorPage from "pages/typ/clusterfactor/ClusterFactorPage.vue";
import ClsPage from "pages/typ/cls/ClsPage.vue";
import NotExpended from "pages/typ/notextended/NotExtended.vue";

export default {
  name: "TypSelected",
  components: {
    NotExpended,
    ClsPage,
    ClusterFactorPage,
    TypRole,
    TypVer,
  },
  data() {
    return {
      tab: ref("vers"),
      typId: null,
      typ: {},
    };
  },
  methods: {
    toBack() {
      //$router.push('/typ')
      this.$router["push"]({
        name: "typPage",
        params: {
          typ: this.typId,
        },
      });

    },

    infoTyp() {
      return this.typ.cod + " - " + this.typ.name;
    },

    fnChangePn() {


    },

    onLoadTyp(typ) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            id: "1",
            method: "typ/loadRec",
            params: [typ],
          })
          .then((response) => {
            //console.log("typ", response.data.result.records)
            this.typ = response.data.result.records[0];
          })
          .catch((error) => {
            console.log(error);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    }

  },
  mounted() {
    //console.log("mounted! TypSelected", this.$route.params);
    this.typId = parseInt(this.$route["params"].typ, 10);
    this.tab = this.$route["params"].tab;

    // load typ
    this.onLoadTyp(this.typId)
    //
  },

  setup() {
  }

};
</script>

<style scoped>

</style>
