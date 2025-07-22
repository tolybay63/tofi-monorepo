<template>

  <div class="q-pa-sm">

    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
      style="height: calc(100vh - 135px); width: 100%"
    >

      <template v-slot:before>

        <q-banner dense inline-actions class="bg-orange-1">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="view_in_ar"></q-avatar>
            {{ $t("lstCube") }}
          </div>
          <q-inner-loading :showing="loading" color="secondary"/>
        </q-banner>

        <table
          class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle"
        >

          <thead class="text-bold text-white bg-blue-grey-13">
          <tr class style="text-align: left">
            <th
              v-for="(col, index) in cols"
              :key="index"
              :class="col.headerClass"
              :style="col.headerStyle"
            >
              {{ col.label }}
            </th>
          </tr>
          </thead>
          <tbody style="background: aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="cols[0].name" @click="toggle(item)">
                  <span
                    class="q-tree-link q-tree-label"
                    v-bind:style="setPadding(item)"
                  >
                    <q-icon
                      :style="fnStyleCursor(item)"
                      :name="iconName(item)"
                      color="secondary"
                    ></q-icon>

                    <q-btn
                      dense flat
                      :color="item.id < 0 ? 'black' : 'blue'"
                      :icon="item.id < 0 ? 'source'
                        : selected.length > 0 && item.id === selected[0].id
                        ? 'check_box'
                        : 'check_box_outline_blank'
                      "
                      @click.stop="selectedCheck(item)"
                    >
                    </q-btn>

                    {{ item.name }} {{ item.id }}
                  </span>
            </td>
          </tr>
          </tbody>
        </table>
        <div>
          <q-td colspan="100%" v-if="selected.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
          </q-td>
          <q-td
            v-else-if="rows.length > 0" colspan="100%" class="text-bold">
            {{ $t("infoViewRow") }}
          </q-td>
        </div>

        <q-space/>
        <q-banner dense class="text-bold text-white bg-blue-grey-13">
          Связь с приложениями
        </q-banner>
        <div class="q-py-sm">
          <div v-if="selected.length > 0">
            <q-list v-for="item in rows3" :key="item.id">
              <q-item
                class="q-table--bordered bg-blue-1"
              >
                <q-item-section avatar>
                  <q-icon name="cloud_sync" class="text-blue" size="32px" />
                </q-item-section>

                <q-item-section>
                  <q-item-label>{{ item.name }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </div>
        </div>

      </template>

      <template v-slot:after>
        <div v-if="errorCube || selected.length===0">
          <h3>
            Данные по формированию куба
          </h3>
        </div>
        <div v-else>
          <q-table
            style="height: calc(100vh - 150px); width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            dense
            :columns="cols2"
            :rows="rows2"
            :wrap-cells="true"
            :table-colspan="4"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading"
            :rows-per-page-options="[0]"
          >

            <template v-slot:top>

              <div style="font-size: 1.2em; font-weight: bold;">
                {{ selected[0].cod }} - {{ selected[0].name }}
              </div>
              <q-space/>

              <q-space/>
              <q-btn
                v-if="hasTarget('mdl:mn_ds:attr:ins')"
                dense no-caps icon="dashboard_customize"
                :label="$t('formCube')"
                color="secondary"
                @click="fnForm()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("formCube") }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="false"
                dense no-caps icon="grid_on"
                label="Очистить куб"
                color="secondary"
                class="q-ml-sm"
                @click="fnClear()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>
              <q-btn
                v-if="false"
                dense no-caps icon="settings"
                label="Настройка обновления куба"
                color="secondary"
                class="q-ml-sm"
                @click="fnConf()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>
            </template>
          </q-table>

        </div>
      </template>

    </q-splitter>

  </div>

</template>

<script>

import {ref} from "vue";
import {hasTarget, notifyError, pack} from "src/utils/jsutils.js";
import {api, baseURL} from "boot/axios.js";
import {QSpinnerCube, useQuasar} from "quasar";

export default {
  name: "CubesTreePage",

  data() {
    return {
      splitterModel: 30,
      rows: [],
      cols: [],
      selected: [],
      isExpanded: true,
      itemId: null,
      loading: false,
      rows2: [],
      cols2: [],
      rows3: [],
      errorCube: false,
      dimsCube: new Map(),
      uq: useQuasar()
    }
  },

  methods: {

    hasTarget,

    loadAllCubes() {
      this.loading = true
      api
        .post(baseURL, {
          method: "cube/loadAllCubes",
          params: [],
        })
        .then((response) => {
          this.rows = pack(response.data.result["records"], "ord")
          //console.info("rows", this.rows)
        })
        .catch((error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })
    },

    loadCubeDb(cube) {
      this.loading = true
      this.errorCube = false
      this.rows3 = []
      api
        .post(baseURL, {
          method: "cube/loadCubeDb",
          params: [cube],
        })
        .then((response) => {
          this.rows3 = response.data.result["records"]
          //console.info("rows3", this.rows3)

        })
        .catch((error) => {
          this.errorCube = true
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })
    },

    loadCubeInfo(cube) {
      this.loading = true
      api
        .post(baseURL, {
          method: "cube/loadDataCubeInfo",
          params: [cube],
        })
        .then((response) => {
          this.rows2 = response.data.result["records"]
        })
        .catch((error) => {
          this.errorCube = true
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
        })
        .finally(() => {
          this.loading = false
        })
    },

    fnStyleCursor(item) {
      if (item.level === 0)
        return "cursor: default"
      else
        return "cursor: pointer"
    },

    /*    fnColStyle(ind) {
          return this.cols[ind] ? this.cols[ind].headerStyle : ""
        },

        fnColLabel(ind) {
          return this.cols[ind] ? this.cols[ind].label : ""
        },

        fnExpand() {
          expandAll(this.rows)
        },

        fnCollapse() {
          collapsAll(this.rows)
        },*/

    recursive(obj, newObj, level, itemId, isExpend) {
      let vm = this;
      obj.forEach(o => {
        if (o.children && o.children.length !== 0) {
          o.level = level;
          o.leaf = false;
          newObj.push(o);
          if (o.id === itemId) {
            o.expend = isExpend;
          }
          if (o.expend) {
            vm.recursive(o.children, newObj, o.level + 1, itemId, isExpend);
          }
        } else {
          o.level = level;
          o.leaf = true;
          newObj.push(o);
          return false;
        }
      })
    },

    iconName(item) {
      if (item.expend) {
        return "remove_circle_outline";
      }
      if (item.children && item.children.length > 0) {
        return "control_point";
      }
      return ""
    },

    toggle(item) {
      let vm = this;
      vm.itemId = item.id;
      item.leaf = false;
      if (
        !item.leaf &&
        item.expend === undefined &&
        item.children !== undefined
      ) {
        if (item.children.length !== 0) {
          vm.recursive(item.children, [], item.level + 1, item.id, true);
        }
      }
      if (item.expend && item.children !== undefined) {
        item.children.forEach(function (o) {
          o.expend = undefined;
        });

        item["expend"] = ref(undefined);
        item["leaf"] = ref(false);
        vm.itemId = null;
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },

    infoSelected(row) {
      return " " + row.name;
    },

    selectedCheck(item) {
      //console.log("selectedCheck", item)
      if (item.id < 0) {
        this.toggle(item)
      } else {
        if (this.selected.length > 0 && item.id === this.selected[0].id) {
          this.selected = []
          //this.loadCubeDb(0)
          this.rows3 = []
          this.loadCubeInfo(0)
        } else {
          this.selected = []
          this.selected.push(item)
          this.loadCubeDb(this.selected[0].id)
          this.loadCubeInfo(this.selected[0].id)
        }
      }
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 100%",
        },
      ];
    },

    getColumns2() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "dtBegStr",
          label: this.$t("dtBegStr"),
          field: "dtBegStr",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "dtEndStr",
          label: this.$t("dtEndStr"),
          field: "dtEndStr",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "dt",
          label: this.$t("tm"),
          field: "dt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 10%",
        },
        {
          name: "countProCubeOwn",
          label: this.$t("countProCubeOwn"),
          field: "countProCubeOwn",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "countProCubeProp",
          label: this.$t("countProCubeProp"),
          field: "countProCubeProp",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "countProCubePeriod",
          label: this.$t("countProCubePeriod"),
          field: "countProCubePeriod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "countCells",
          label: this.$t("countCells"),
          field: "countCells",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "countDataCells",
          label: this.$t("countDataCells"),
          field: "countDataCells",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
        },
        {
          name: "isComplete",
          label: this.$t("isComplete"),
          field: "isComplete",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 8%",
          format: (val) => val ? this.$t("success") : this.$t("failed"),
        },
        {
          name: "errorMessage",
          label: this.$t("errorMessage"),
          field: "errorMessage",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 12%",
        },
      ]
    },

    loaderShow(msg) {
      this.uq.loading.show({
        boxClass: 'bg-grey-5 text-grey-9',
        spinner: QSpinnerCube,
        spinnerColor: 'yellow',
        spinnerSize: 140,
        backgroundColor: 'purple',
        message: msg,
        messageColor: 'blue'
      })
    },

    loaderHide() {
      this.uq.loading.hide()
    },

    fnForm() {
      this.loaderShow("Проверка...")
      api
        .post(baseURL, {
          method: "cube/checkCreateCube",
          params: [this.selected[0].id],
        })
        .then((response) => {
          this.dimsCube = response.data.result
          //this.loaderHide()
          this.loaderShow("Измерение периодов...")
          api
            .post(baseURL, {
              method: "cube/createDimPeriod",
              params: [this.selected[0].id, this.dimsCube["dimPeriod"]],
            })
            .then(()=> {
              //this.loaderHide()
              this.loaderShow("Измерение свойств...")
              let mapParam = new Map()
              for (let key in this.dimsCube) {
                if (key.substring(0, 7) === "dimProp") {
                  mapParam.set(key, this.dimsCube[key]);
                  console.info(key, this.dimsCube[key]);
                }
              }
              api
                .post(baseURL, {
                  method: "cube/createDimProp",
                  params: [this.selected[0].id, mapParam],
                })
                .then(()=> {
                  //this.loaderHide()
                  this.loaderShow("Измерение объектов...")
                  api
                    .post(baseURL, {
                      method: "cube/createDimObj",
                      params: [this.selected[0].id, this.dimsCube],
                    })
                    .then(()=> {
                      //this.loaderHide()
                      this.loaderShow("Таблица фактов...")
                      api
                        .post(baseURL, {
                          method: "cube/createFactTable",
                          params: [this.selected[0].id, this.dimsCube],
                        })
                        .then(()=> {
                          //this.loaderHide()
                        })
                        .finally(()=> {
                          this.loadCubeInfo(this.selected[0].id)
                          this.loaderHide()
                        })
                    })
                })
                .catch((error=> {
                  this.loaderHide()
                  let msg = error.message;
                  if (error.response) msg = error.response.data.error.message;
                  notifyError(msg);
                }))
            })
            .catch((error)=> {
              this.loaderHide()
              let msg = error.message;
              if (error.response) msg = error.response.data.error.message;
              notifyError(msg);
            })
        })
        .catch((error) => {
          this.loaderHide()
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        })
        .finally(() => {
            //this.loaderHide()
        })


    },

    fnClear() {

    },

    fnConf() {

    },


  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  mounted() {
    //console.info("mounted")
    this.cols = this.getColumns()
    this.loadAllCubes()
    this.cols2 = this.getColumns2()
  },

  created() {
    //console.info("created")
  }


}

</script>

<style scoped>

</style>
