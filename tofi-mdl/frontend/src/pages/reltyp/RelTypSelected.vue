<template>
  <q-page class="q-pa-sm-sm">
    <div class=" bg-green-1">
      <q-tabs dense v-model="tab" class="text-teal">
        <div style="margin-left: 20px; font-size: 1.2em; font-weight: bold;">
          {{ $t("reltyp") }}:
        </div>
        <div style="color: black; margin-left: 10px"> {{ this.infoRelTyp() }}</div>
        <q-space/>

        <q-btn
            dense round color="secondary" icon="arrow_back" glossy
            @click="toBack()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>
        <q-tab name="vers" no-caps icon="event_note" :label="$t('vers')"/>
        <q-tab name="reltyproles" no-caps icon="perm_contact_calendar" :label="$t('roles2')"/>
        <q-tab name="members" no-caps icon="group_work" :label="$t('members')"/>
        <q-tab name="relcls" no-caps icon="settings_applications" :label="$t('relcls')"/>
      </q-tabs>

      <q-tab-panels v-model="tab" animated>
        <q-tab-panel
            name="vers"
            style="height: calc(100vh - 220px); width: 100%"
        >
          <rel-typ-ver @loadRelTyp="onLoadRelTyp"/>
        </q-tab-panel>

        <q-tab-panel name="reltyproles">
          <rel-typ-role/>
        </q-tab-panel>

        <q-tab-panel name="members">
          <rel-typ-member/>
        </q-tab-panel>

        <q-tab-panel name="relcls">
          <rel-cls/>
        </q-tab-panel>

      </q-tab-panels>
    </div>
  </q-page>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import RelTypVer from "pages/reltyp/RelTypVer.vue";
import RelTypRole from "pages/reltyp/reltyprole/RelTypRole.vue";
import RelTypMember from "pages/reltyp/members/RelTypMember.vue";
import {notifyError} from "src/utils/jsutils";
import RelCls from "pages/reltyp/relcls/RelCls.vue";

export default {
  name: "RelTypSelected",
  components: {RelCls, RelTypMember, RelTypRole, RelTypVer},
  data() {
    return {
      tab: ref("vers"),
      reltypId: 0,
      relclsId: 0,
      reltyp: {},
    };
  },
  methods: {

    toBack() {
      //$router.push('/reltyp')
      this.$router["push"]({
        name: "reltypPage",
        params: {
          reltyp: this.reltypId,
        },
      });
    },

    infoRelTyp() {
      return this.reltyp.cod + " - " + this.reltyp.name;
    },

    onLoadRelTyp(reltyp) {
      this.loading = ref(true);
      api
          .post(baseURL, {
            id: "1",
            method: "reltyp/loadRec",
            params: [{id: reltyp}],
          })
          .then((response) => {
            this.reltyp = response.data.result.records[0];
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
    this.reltypId = parseInt(this.$route["params"].reltyp, 10);
    this.relclsId = parseInt(this.$route["params"].relcls, 10);
    this.tab = this.$route["params"].tab;
    // load reltyp
    this.onLoadRelTyp(this.reltypId)
  },

  setup() {
  }

};
</script>

<style scoped></style>
