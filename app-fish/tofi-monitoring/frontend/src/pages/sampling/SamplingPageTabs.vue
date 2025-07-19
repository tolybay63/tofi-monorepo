<template>
  <div class="q-pa-sm-sm bg-green-1">
    <div>
      <q-inner-loading :showing="loading" color="secondary" />
    </div>

    <q-tabs dense v-model="tab" active-color="purple" class="text-teal no-scroll">
      <q-tab :name="fnNameTab(0)" no-caps :label="fnLabelTab(0)" />
      <q-tab :name="fnNameTab(1)" no-caps :label="fnLabelTab(1)" />
      <q-tab :name="fnNameTab(2)" no-caps :label="fnLabelTab(2)" />
      <q-tab :name="fnNameTab(3)" no-caps :label="fnLabelTab(3)" />
      <q-tab :name="fnNameTab(4)" no-caps :label="fnLabelTab(4)" />
      <q-tab :name="fnNameTab(5)" no-caps :label="fnLabelTab(5)" />
    </q-tabs>

    <q-tab-panels v-model="tab" animated>
      <q-tab-panel :name="fnNameTab(0)" class="no-scroll">
        <SamplingPageTab :cls="fnClsTab(0)" :title="rows[0].name" />
      </q-tab-panel>

      <q-tab-panel :name="fnNameTab(1)" class="no-scroll">
        <SamplingPageTab :cls="fnClsTab(1)" :title="rows[1].name" />
      </q-tab-panel>

      <q-tab-panel :name="fnNameTab(2)" class="no-scroll">
        <SamplingPageTab :cls="fnClsTab(2)" :title="rows[2].name" />
      </q-tab-panel>

      <q-tab-panel :name="fnNameTab(3)" class="no-scroll">
        <SamplingPageTab :cls="fnClsTab(3)" :title="rows[3].name" />
      </q-tab-panel>

      <q-tab-panel :name="fnNameTab(4)" class="no-scroll">
        <SamplingPageTab :cls="fnClsTab(4)" :title="rows[4].name" />
      </q-tab-panel>

      <q-tab-panel :name="fnNameTab(5)" class="no-scroll">
        <SamplingPageTab :cls="fnClsTab(5)" :title="rows[5].name" />
      </q-tab-panel>
    </q-tab-panels>
  </div>
</template>

<script>
import { api, baseURL } from 'boot/axios'
import { notifyError } from 'src/utils/jsutils'
import { ref } from 'vue'
import SamplingPageTab from 'pages/sampling/SamplingPageTab.vue'

export default {
  name: 'SamplingPageTabs',
  components: { SamplingPageTab },

  data: function () {
    return {
      loading: false,
      rows: [],
      tab: ref('Cls_SamplingFishing'), //"Cls_SamplingFishing"
    }
  },

  methods: {
    fnNameTab(ind) {
      return this.rows.length > 0 ? this.rows[ind].cod : null
    },

    fnLabelTab(ind) {
      return this.rows.length > 0 ? this.rows[ind].name : null
    },

    fnClsTab(ind) {
      return this.rows.length > 0 ? this.rows[ind].id : null
    },

    loadSamplingCls() {
      this.loading = true
      api
        .post(baseURL, {
          method: 'data/loadCls',
          params: ['Typ_Sampling'],
        })
        .then((response) => {
          this.rows = response.data.result.records
          //console.info("this.rows", this.rows)
        })
        .then(() => {
          this.tab = ref(this.rows[0].cod)
        })
        .catch((error) => {
          //console.info(error.message);
          if (error.response.data.error.message.includes('@')) {
            let msgs = error.response.data.error.message.split('@')
            let m1 = this.$t(`${msgs[0]}`)
            let m2 = msgs.length > 1 ? ' [' + msgs[1] + ']' : ''
            let msg = m1 + m2
            notifyError(msg)
          } else {
            notifyError(this.$t(error.response.data.error.message))
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
  },

  created() {
    this.loadSamplingCls()
  },
}
</script>

<style scoped></style>
