<template>

    <q-banner class="bg-purple-1">
      <div class="row">
        <!-- Branch -->
        <q-select
          style="width: 300px"
          v-model="form.branch"
          :model-value="form.branch"
          :label="fmReqLabel('branch')"
          :options="optBranch"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectBranch"
          @filter="filterBranch"
        />

        <!-- Reservoir -->
        <q-select
          style="width: 300px"
          class="q-ml-lg"
          v-model="form.reservoir"
          :model-value="form.reservoir"
          :label="fmReqLabel('reservoir')"
          :options="optReservoir"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectReservoir"
          @filter="filterReservoir"
        />

        <!-- TypeOfFish -->
        <q-select
          style="width: 220px"
          class="q-ml-lg"
          v-model="form.typeoffish"
          :model-value="form.typeoffish"
          :label="fmReqLabel('typeOfFish')"
          :options="optTypeOfFish"
          dense
          map-options
          option-label="name"
          option-value="id"
          use-input
          @update:model-value="fnSelectTypeOfFish"
          @filter="filterTypeOfFish"
        />
        <!--  Date  -->
        <q-input
          class="q-ml-lg"
          v-model="form.dte"
          :model-value="form.dte"
          type="date"
          dense
          style="width: 100px; margin-right: 20px"
          stack-label
          :label="$t('date')"
        />
        <!--  PeriodType  -->
        <q-select
          class="q-ml-lg"
          v-model="form.pt"
          :model-value="form.pt"
          dense
          options-dense
          :options="optPeriod"
          :label="$t('periodType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectPeriodType"
          style="width: 120px"
        />

        <q-input class="q-ml-lg" dense v-model="form.count" :model-value="form.count"
                 type="number" :label="$t('countPeriod')" style="width: 130px"/>

        <q-btn class="q-ml-lg" color="primary" dense label="Показать"
               :disable="disableBtn()" no-caps icon="pageview" @click="fnShow()"
        >

        </q-btn>


      </div>

    </q-banner>

    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[30, 90]"
      class="no-padding no-margin"
      horizontal
      separator-class="bg-red"
      style="height: calc(100vh - 240px)"
    >

      <template v-slot:before>

        <h3 v-if="rows1.length===0" class="flex flex-center">
          {{ $t("countFish") }}
        </h3>
        <div v-else>
          <q-table
            color="primary" dense
            card-class="bg-amber-1 text-brown"
            row-key="namePeriod"
            :columns="cols1"
            :rows="rows1"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading"
            :rows-per-page-options="[0]"
          >
            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                {{ $t("countFishFull") }}
              </div>
            </template>

            <template #body-cell="props">
              <q-td :props="props">
                <div v-if="fnCell(props.col.name)">

                  <q-btn
                    color="primary" round size="sm" flat dense icon="more_vert" class="absolute-right"
                  >
                    <q-menu auto-close>
                      <q-btn
                        round size="sm" icon="edit" color="blue" flat dense
                        @click="fnEdit(props.row, props.col, 'Prop_FishCount')" class="no-padding no-margin"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("update") }}
                        </q-tooltip>
                      </q-btn>

                      <q-btn
                        round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                        @click="fnDelete(props, 'Prop_FishCount')"
                        :disable="props.row[props.col.name]===undefined"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("deletingRecord") }}
                        </q-tooltip>
                      </q-btn>
                    </q-menu>
                  </q-btn>
                </div>
                <div>
                  {{ props.value }}
                </div>
              </q-td>
            </template>

          </q-table>
        </div>

        <h3 v-if="rows2.length===0" class="flex flex-center">
          {{ $t("massaFish") }}
        </h3>
        <div v-else>
          <q-table
            color="primary" dense
            card-class="bg-amber-1 text-brown"
            row-key="namePeriod"
            :columns="cols2"
            :rows="rows2"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2"
            :rows-per-page-options="[0]"
          >
            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                {{ $t("massaFishFull") }}
              </div>
            </template>

            <template #body-cell="props">
              <q-td :props="props">
                <div v-if="fnCell(props.col.name)">

                  <q-btn
                    color="primary" round size="sm" flat dense icon="more_vert" class="absolute-right"
                  >
                    <q-menu auto-close>
                      <q-btn
                        round size="sm" icon="edit" color="blue" flat dense
                        @click="fnEdit(props.row, props.col, 'Prop_FishAverageWeight')" class="no-padding no-margin"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("update") }}
                        </q-tooltip>
                      </q-btn>

                      <q-btn
                        round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                        @click="fnDelete(props, 'Prop_FishAverageWeight')"
                        :disable="props.row[props.col.name]===undefined"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("deletingRecord") }}
                        </q-tooltip>
                      </q-btn>
                    </q-menu>
                  </q-btn>
                </div>
                <div>
                  {{ props.value }}
                </div>
              </q-td>
            </template>


          </q-table>
        </div>

      </template>

      <template v-slot:after>

        <h3 v-if="rows3.length===0" class="flex-center text-center">
          {{ $t("sexFishRatio") }}
        </h3>
        <div v-else>
          <q-table
            color="primary" dense
            card-class="bg-amber-1 text-brown"
            row-key="name"
            :columns="cols3"
            :rows="rows3"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading3"
            :rows-per-page-options="[0]"
          >
            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                {{ $t("countFishFull") }}
              </div>
            </template>

            <template #body-cell="props">
              <q-td :props="props">
                <div v-if="fnCell3(props.row, props.col.name)">

                  <q-btn
                    color="primary" round size="sm" flat dense icon="more_vert" class="absolute-right"
                  >
                    <q-menu auto-close>
                      <q-btn
                        round size="sm" icon="edit" color="blue" flat dense
                        @click="fnEdit3(props.row, props.col)" class="no-padding no-margin"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("update") }}
                        </q-tooltip>
                      </q-btn>

                      <q-btn
                        round size="sm" icon="delete" color="red" flat dense class="no-padding no-margin"
                        @click="fnDelete3(props)"
                        :disable="props.row[props.col.name]===undefined"
                      >
                        <q-tooltip
                          transition-show="rotate" transition-hide="rotate"
                        >
                          {{ $t("deletingRecord") }}
                        </q-tooltip>
                      </q-btn>
                    </q-menu>
                  </q-btn>
                </div>
                <div>
                  {{ props.value }}
                </div>
              </q-td>
            </template>
          </q-table>
        </div>

        <div v-if="rows4.length>0">
          <q-table
            color="primary" dense
            card-class="bg-amber-1 text-brown"
            row-key="name"
            :columns="cols4"
            :rows="rows4"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading4"
            :rows-per-page-options="[0]"
          >
            <template v-slot:top>
              <div style="font-size: 1.2em; font-weight: bold">
                {{ $t("sexFishRatio") }}
              </div>
            </template>

          </q-table>
        </div>


      </template>


    </q-splitter>

</template>

<script>
import {notifyError, today} from 'src/utils/jsutils.js'
import {api} from 'boot/axios.js'
import UpdaterFishingAnalise from 'pages/piscesreservoirs/UpdaterFishingAnalise.vue'
import UpdaterFishingSex from "pages/piscesreservoirs/UpdaterFishingSex.vue";

export default {
  name: 'FishingAnalisePage',

  data: function () {
    return {
      form: {branch: null, reservoir: null, typeoffish: null, dte: today(), pt: 11, count: 10},
      optBranch: [],
      optBranchOrg: [],
      optReservoir: [],
      optReservoirOrg: [],
      optTypeOfFish: [],
      optTypeOfFishOrg: [],
      optPeriod: [],

      splitterModel: 70,
      cols1: [],
      rows1: [],
      loading: false,

      cols2: [],
      rows2: [],
      loading2: false,

      cols3: [],
      rows3: [],
      loading3: false,

      cols4: [],
      rows4: [],
      loading4: false
    }
  },

  methods: {
    fnEdit(row, col, codProp) {
      let mode = row[col.name] === undefined ? "ins" : "upd"

      this.$q
        .dialog({
          component: UpdaterFishingAnalise,
          componentProps: {
            mode: mode,
            data: {
              codProp: codProp,
              colName: col.label,
              periodName: row["namePeriod"],
              periodType: this.form.pt,
              dbeg: row["dbeg"],
              dend: row["dend"],
              idVal: row["id_" + col.name],
              objOrRelObj: parseInt(col.name.split("_")[1], 10),
              isObj: 0,
              val: row[col.name],
            },
            // ...
          },
        })
        .onOk((r) => {
          row[col.name] = r
        })

    },

    fnDelete(props, prop) {
      let msg = props.col.label + ", " + props.row["namePeriod"]
      if (prop === "Prop_FishCount")
        msg = msg + ", количество - " + props.row[props.col.name]
      else
        msg = msg + ", масса - " + props.row[props.col.name]


      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '<div style="color: plum">(' + msg + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: 'data/deleteAnalise',
              params: [props.row["id_" + props.col.name]],
            })
            .then(() => {
              props.row[props.col.name] = null
            })
            .catch((error) => {
              console.log(error.message)
              notifyError(error.message)
            })
        })
    },

    fnCell(col) {
      return col.startsWith('ro_')
    },

    fnCell3(row, col) {
      return col.startsWith('p_') && row["cod"] !== "Prop_FishCount"
    },

    fnEdit3(row, col) {
      let mode = row[col.name] === undefined ? "ins" : "upd"

      this.$q
        .dialog({
          component: UpdaterFishingSex,
          componentProps: {
            mode: mode,
            data: {
              prop: row["prop"],
              name: row["name"],
              periodName: col.label,
              periodType: this.form.pt,
              dbeg: col.name.split("_")[1],
              dend: col.name.split("_")[2],
              idVal: row["id_" + col.name],
              objOrRelObj: row["relobj"],
              isObj: 0,
              val: row[col.name],
            },
            // ...
          },
        })
        .onOk((r) => {
          if (r.res) {
            this.loadFishSex(this.form)
          }
        })

    },

    fnDelete3(props) {
      console.info(props.row["name"])
      console.info(props.col.label)
      console.info(props.row[props.col.name])
      console.info(props.row["id_"+props.col.name])
      console.info(props.col.name.split("_")[1], props.col.name.split("_")[2])

      let dta= {
        prop: props.row["prop"],
        isObj: 0,
        periodType: this.form.pt,
        dbeg: props.col.name.split("_")[1],
        dend: props.col.name.split("_")[2],
        idVal: props.row["id_" + props.col.name],
        objOrRelObj: props.row["relobj"],
      }

      let msg = props.row["name"] + ", " + props.col.label + ", количество - " + props.row[props.col.name]
      this.$q
        .dialog({
          title: this.$t('confirmation'),
          message: this.$t('deleteRecord') + '<div style="color: plum">(' + msg + ')</div>',
          html: true,
          cancel: true,
          persistent: true,
          focus: 'cancel',
        })
        .onOk(() => {
          api
            .post('', {
              method: 'data/deleteFishSex',
              params: [props.row["id_" + props.col.name], dta],
            })
            .then(() => {
              this.loadFishSex(this.form)
            })
            .catch((error) => {
              console.log(error.message)
              notifyError(error.message)
            })
        })

    },

    fnShow() {
      this.loading = true
      api
        .post('', {
          method: 'data/loadAnalisys',
          params: [this.form],
        })
        .then(
          (response) => {
            //console.info("response", response.data.result)
            this.cols1 = response.data.result["cols"]
            this.cols2 = response.data.result["cols"]
            this.rows1 = response.data.result["rows1"].records
            this.rows2 = response.data.result["rows2"].records
          },
          (error) => {
            let msg = error.message
            if (error.response) msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .then(() => {
          this.loadFishSex(this.form)
        })
        .finally(() => {
          this.loading = false
        })

    },

    loadFishSex(params) {
      this.loading3 = true
      api
        .post('', {
          method: 'data/loadFishSex',
          params: [params],
        })
        .then((response) => {
          this.cols3 = response.data.result["cols"]
          this.cols4 = response.data.result["cols"]
          this.rows3 = response.data.result["rows3"].records
          this.rows4 = response.data.result["rows4"].records
        })
        .catch(error => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        })
      .finally(() => {
        this.loading3 = false
      })
    },

    disableBtn() {
      return !(this.form.branch && this.form.reservoir && this.form.typeoffish
        && this.form.dte && this.form.pt && this.form.count > 0)

    },

    fmReqLabel(label) {
      return this.$t(label) + '*'
    },

    fnSelectBranch(v) {
      if (v) {
        this.form.branch = v.id
        this.form.reservoir = null
        this.form.typeoffish = null
        this.rows1 = []
        this.rows2 = []
        this.loadReservoir(v.id)
      }
    },

    loadReservoir(branch) {
      this.loading = true
      api
        .post('', {
          method: 'data/loadReservoir',
          params: [branch],
        })
        .then(
          (response) => {
            this.optReservoir = response.data.result.records
            this.optReservoirOrg = response.data.result.records
          },
          (error) => {
            let msg = error.message
            if (error.response) msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          }
        )
        .finally(() => {
          this.loading = false
        })
    },

    filterBranch(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optBranch = this.optBranchOrg
        })
        return
      }
      update(() => {
        if (this.optBranchOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optBranch = this.optBranchOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectReservoir(v) {
      this.form.reservoir = v.id
      this.form.cls1 = v.cls
    },

    filterReservoir(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optReservoir = this.optReservoirOrg
        })
        return
      }
      update(() => {
        if (this.optReservoirOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optReservoir = this.optReservoirOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectTypeOfFish(v) {
      this.form.typeoffish = v.id
      this.form.cls2 = v.cls
    },

    filterTypeOfFish(val, update) {
      if (val === null || val === '') {
        update(() => {
          this.optTypeOfFish = this.optTypeOfFishOrg
        })
        return
      }
      update(() => {
        if (this.optTypeOfFishOrg.length < 2) return
        const needle = val.toLowerCase()
        let name = 'name'
        this.optTypeOfFish = this.optTypeOfFishOrg.filter((v) => {
          return v[name].toLowerCase().indexOf(needle) > -1
        })
      })
    },

    fnSelectPeriodType(v) {
      this.form.pt = v.id
    }

  },

  created() {
    this.loading = true
    api
      .post('', {
        method: 'data/loadPeriodType',
        params: [],
      })
      .then(
        (response) => {
          this.optPeriod = response.data.result.records
        })
      .catch(error => {
        notifyError(error.message)
      })
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadBranchName',
        params: ['Cls_Branch'],
      })
      .then(
        (response) => {
          this.optBranch = response.data.result.records
          this.optBranchOrg = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })
    //
    this.loading = true
    api
      .post('', {
        method: 'data/loadTypeOfFishFirstLevel',
        params: ['Cls_FishTypes'],
      })
      .then(
        (response) => {
          this.optTypeOfFish = response.data.result.records
          this.optTypeOfFishOrg = response.data.result.records
        },
        (error) => {
          let msg = error.message
          if (error.response) msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        }
      )
      .finally(() => {
        this.loading = false
      })

  }


}
</script>

<style scoped>

</style>
