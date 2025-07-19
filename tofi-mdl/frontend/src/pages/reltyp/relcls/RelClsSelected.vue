<template>
  <q-page class="q-pa-sm-sm">
    <div class="bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("relcls") }}:
        </div>
        <div style="color: black; margin-left: 10px">
          {{ this.infoRelCls() }}
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
        <q-tab
            name="vers"
            no-caps
            icon="event_note"
            :label="$t('vers')"
        />
      </q-tabs>

      <q-tab-panels v-model="tab" animated >
        <q-tab-panel
            name="vers"
            style="height: calc(100vh - 220px); width: 100%"
        >
          <rel-cls-ver @loadRelCls="onLoadRelCls"/>
        </q-tab-panel>

      </q-tab-panels>

    </div>
  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {notifyError} from "src/utils/jsutils";
import RelClsVer from "pages/reltyp/relcls/RelClsVer.vue";

export default {
  name: "RelClsSelected",
  components: {RelClsVer},
  data() {
    return {
      tab: ref("vers"),
      reltypId: 0,
      relclsId: 0,
      relcls: {},
    };
  },
  methods: {
    toBack() {
      //$router.push('/reltyp')
      this.$router["push"]({
        name: "reltypSelected",
        params: {
          reltyp: this.reltypId,
          relcls: this.relclsId,
          tab: 'relcls'
        },
      });

    },

    infoRelCls() {
      return this.relcls.cod + " - " + this.relcls.name;
    },

    onLoadRelCls(relclsId) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "relcls/loadRec",
            params: [relclsId],
          })
          .then((response) => {
            //console.log("f", response.data.result.records)
            this.relcls = response.data.result.records[0];
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          })
          .finally(() => {
            this.loading = ref(false);
          });
    }
  },

  mounted() {
    this.reltypId = parseInt(this.$route["params"].reltyp, 10)
    this.relclsId = parseInt(this.$route["params"].relcls, 10)
    this.tab = this.$route["params"].tab
    // load relcls
    this.onLoadRelCls(this.relclsId)
    //
  },

  setup() {
  }

};
</script>

<style scoped></style>
