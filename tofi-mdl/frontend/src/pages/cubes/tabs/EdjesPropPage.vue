<template>

  <div class="q-pa-sm-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[20, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
    >

      <template v-slot:before>
        <div style="height: calc(100vh - 310px); width: 100%">

          <div v-for="(dimProp, index) in dimProps" class="q-ma-sm">

            <q-item-label class="text-grey-7"
            >{{ dimProp.name }}
            </q-item-label>

            <treeselect
              :options="optDP[index]"
              v-model="dp[index]"
              :model-value="dp[index]"
              :normalizer="normalizerDP"
              :placeholder="$t('select')"
              :noResultsText="$t('noResult')"
              :noChildrenText="$t('noChilds')"
              :noOptionsText="$t('noResult')"
              DefaultExpandLevel=1
              @select="fnSelectDP"
              :disabled="fixedDim"
            />
          </div>

          <q-toggle
            v-model="fixedDim"
            color="green"
            label="Зафиксировать"
            :disable="validAll()"
            @update:model-value="updValue"
          />

          <div class="q-ma-sm">
            {{ fnMapping() }}
          </div>

        </div>
      </template>

      <q-inner-loading :showing="loading" color="secondary"></q-inner-loading>

      <template v-slot:after>
        <div v-if="!showGrid">
          <h3> Грани прокуба свойств</h3>
        </div>
        <div v-else>
          <edjes-prop :cols="cols" :rows="rows" :propField="propField" :cubeS="cubeS" @updateDelete="onUpdateDelete"
                      @updateBind="onUpdateBind" :showAction="showAction" :showSelect="showSelect"
                      :mapping="fnMapping()"/>

        </div>

      </template>


    </q-splitter>

  </div>

</template>

<script>
import {api, baseURL} from "boot/axios.js";
import treeselect from "vue3-treeselect";
import "vue3-treeselect/dist/vue3-treeselect.css";
import {notifyError, pack} from "src/utils/jsutils.js";
import EdjesProp from "pages/cubes/tabs/prop/EdjesProp.vue";


export default {
  name: "EdjesPropPage",
  components: {EdjesProp, treeselect},
  props: ["cubeS", "dOrg"],

  data() {
    return {
      splitterModel: 30,
      loading: false,
      dimProps: [],
      dp: [],
      optDP: [],
      cols: [],
      rows: [],
      propField: "prop",
      showAction: true,
      showSelect: true,
      fixedDim: false,
      showGrid: false,
    }
  },

  methods: {

    onUpdateDelete(data) {
      //console.log("onUpdateDelete", data)
      this.fixedDim = false
      this.rows = []
      setTimeout(() => {
        this.fixedDim = true
        this.updValue(this.fixedDim)
      }, 100)
    },

    onUpdateBind() {
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/binding",
          params: [this.cubeS, this.propField, this.fnMapping()],
        })
        .then((response) => {
          this.rows = []
          let rr = response.data.result.store
          this.rows = pack(rr.records, "id")
          console.info("rr", rr)
          console.info("rows", this.rows)
        })
        .then(() => {
          this.updValue(false)
          setTimeout(() => {
            this.fixedDim = true
            this.updValue(this.fixedDim)
          }, 100)
        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
          console.error(msg)
        })
        .finally(() => {
          this.loading = false
        })

    },


    updValue(v) {
      if (v) {
        this.getGrid(this.fnMapping())
      } else {
        this.showGrid = false
        this.cols = []
        this.rows = []
      }

    },

    getGrid(par) {
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/getGrid",
          params: [this.cubeS, par],
        })
        .then((response) => {
          this.cols = response.data.result.cols
          this.propField = response.data.result.propField
          this.showAction = response.data.result.showAction
          this.showSelect = response.data.result.showSelect
          this.rows = pack(response.data.result.rows.records, "id")
          this.showGrid = true
          //console.info(`this.propField`, this.propField)
          console.info(`response`, response.data.result)
        })
        .then(()=> {
          this.showGrid = true
        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message);
          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })

    },

    fnMapping() {
      let map = new Map
      this.dimProps.forEach((e, index) => {
        map.set(e.id, this.dp[index])
      })
      //console.info("map", Array.from(map.entries()))
      return Array.from(map.entries())
    },

    fnSelectDP(v) {
      //console.log("index", v);
    },


    normalizerDP(node) {
      //console.log("normalizerDP", node);
      return {
        id: node.id,
        label: node.name,
      };
    },

    validAll() {
      this.dp.forEach((r, index) => {
        if (r === undefined || !r) {
          this.dp[index] = 0
        }
      })

      let d1 = this.dp.filter(r => {
        return r === 1
      })
      let d2 = this.dp.filter(r => {
        return r > 1
      })

      if (this.dp.length === 1)
        return !(d1.length === 1)
      else
        return !(d1.length === 1 && d2.length > 0)

    },

    loadDimPropItem(dimProp, index) {
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/loadDimPropItem",
          params: [dimProp],
        })
        .then((response) => {
          let dta = pack(response.data.result.records, "id")
          //this.optDP[index] = [{id: 0, parent: null, name: "Нет"}, {id: 1, parent: null, name: "Все"}, ...dta];
          this.optDP[index] = [{id: 1, parent: null, name: "Все"}, ...dta];
          //console.info(`this.optDP[${index}]`, this.optDP[index])
        })
        .then(() => {
          this.dp[index] = this.dp[index] ? this.dp[index] : 0;
        })
        .finally(() => {
          this.loading = false
        })
    },

    loadDimPropItemFV(dimProp, index) {
      this.loading = true
      api
        .post(baseURL, {
          method: "cubes/loadDimPropItemFV",
          params: [dimProp],
        })
        .then((response) => {
          this.optDP[index] = pack(response.data.result.records, "id")
          //let dta = pack(response.data.result.records, "ord")
          //this.optDP[index] = [ {id: 1, parent: null, name: "Все"}, ...dta];
        })
        .then(() => {
          this.dp[index] = this.dp[index] ? this.dp[index] : 0;
        })
        .finally(() => {
          //console.info(`this.optDP[${index}]`, this.optDP[index])
          //console.info(`this.dp[${index}]`, this.dp[index])
          this.loading = false
        })
    },


  },

  mounted() {
    console.info("mounted")
  },

  created() {
    console.info("created")

    this.loading = true
    api
      .post(baseURL, {
        method: "cubes/loadDimProp",
        params: [this.cubeS],
      })
      .then((response) => {
        this.dimProps = response.data.result.records;
      })
      .then(() => {
        this.dimProps.forEach((dim, index) => {
          if (dim["dimproptype"] === 2)
            this.loadDimPropItemFV(dim.id, index)
          else
            this.loadDimPropItem(dim.id, index)
        })
      })
      .finally(() => {
        this.loading = false
      })

  }

}
</script>

<style scoped>

</style>
