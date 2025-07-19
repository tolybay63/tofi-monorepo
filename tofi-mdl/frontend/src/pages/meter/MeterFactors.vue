<template>
  <q-page class="q-pa-sm-sm" style="height: auto">
    <q-banner dense inline-actions class="bg-orange-1">
      <div style="font-size: 1.2em; font-weight: bold;">{{ $t("meterFactors") }}</div>
      <template v-slot:action>
        <q-btn dense icon="expand_more" color="secondary" @click="fnExpand()">
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          dense
          icon="expand_less"
          color="secondary"
          class="q-ml-sm"
          @click="fnCollapse()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          @click="fnIns()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addFactor") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:del')"
          dense
          icon="delete"
          color="secondary"
          class="q-ml-sm"
          @click="fnDel(currentNode)"
          :disable="
            currentNode == null || (currentNode.id < 0)
          "
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("delFactor") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:ord')"
          dense
          icon="swipe_up_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(true)"
          :disable="onoffUp()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("up") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:ord')"
          dense
          icon="swipe_down_alt"
          color="secondary"
          class="q-ml-sm"
          @click="fnUp(false)"
          :disable="onoffDown()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("down") }}
          </q-tooltip>
        </q-btn>
        <q-space/>
        <q-btn
          v-if="hasTarget('mdl:mn_ds:meter:sel:fac:dim')"
          dense
          icon="vertical_split"
          color="secondary"
          class="q-ml-sm"
          @click="fnNewDim()"
          :disable="onoffDim()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("newDimension") }}
          </q-tooltip>
        </q-btn>
        <q-space/>
      </template>
    </q-banner>

    <div class="q-pa-md-md">
      <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
      {{ this.nodeInfo() }}
    </div>

    <div
      class="q-table-container q-table--dense wrap bg-orange-1"
      style="height: calc(100vh - 300px); width: 100%"
    >
      <div class="q-pa-sm-sm">
        <div class="q-table-middle scroll">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr class style="text-align: left">
              <th :style="columns[0].style">{{ columns[0].label }}</th>
              <th :style="columns[1].style">{{ columns[1].label }}</th>
              <th :style="columns[2].style">{{ columns[2].label }}</th>
            </tr>
            </thead>

            <tbody style="background: aliceblue; height: 100%">
            <tr v-for="(item, index) in arrayTreeObj" :key="index">
              <td :data-th="columns[0].name" @click="toggle(item)">
                  <span
                    class="q-tree-link q-tree-label"
                    v-bind:style="setPadding(item)"
                  >
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    ></q-icon>

                    <q-btn
                      dense
                      flat
                      color="blue"
                      :icon="
                        selected.length === 1 && item.id === selected[0].id
                          ? 'check_box'
                          : 'check_box_outline_blank'
                      "
                      @click.stop="selectedRow(item)"
                    >
                    </q-btn>

                    {{ item.cod }}
                  </span>
              </td>
              <!--name-->
              <td :data-th="columns[1].name">{{ item.name }}</td>
              <!--fullName-->
              <td :data-th="columns[2].name">{{ item.fullName }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import {collapsAll, expandAll, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import UpdateMeterFactors from "pages/meter/UpdateMeterFactors.vue";

export default defineComponent({
  name: "MeterFactors",

  data: function () {
    return {
      isExpanded: true,
      selectedRowID: {},
      selected: ref([]),
      currentNode: null,
      itemId: null,
      FD_AccessLevel: null,
      columns: [],
      table: [],

      separator: "cell",
      meter_id: null,
      maxDim: 1,
    };
  },

  methods: {
    hasTarget,
    recursive(obj, newObj, level, itemId, isExpend) {
      let vm = this;
      obj.forEach(function (o) {
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
      });
    },
    iconName(item) {
      if (item.expend) {
        return "remove_circle_outline";
      }

      if (item.children && item.children.length > 0) {
        return "control_point";
      }

      return "";
    },
    toggle(item) {
      let vm = this;
      vm.itemId = item.id;
      item.leaf = false;
      //show  sub items after click on + (more)
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

    selectedRow(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id)
        vm.selected = [];
      else {
        vm.selected = [];
        vm.selected.push(item);
      }
      this.currentNode = vm.selected[0] !== undefined ? vm.selected[0] : null;
      //temporary
      //console.log("selected", vm.selected[0])
      //console.log("currentNode", this.currentNode)
    },

    fnExpand() {
      expandAll(this.table);
    },
    fnCollapse() {
      collapsAll(this.table);
    },

    onoffUp() {
      if (this.currentNode === null) return true;
      else if (this.currentNode.id < 0) {
        return true;
      } else {
        if (this.currentNode["ordFactorInDim"] > 1) return false;
        if (this.currentNode.ordDim > 1) return false;
      }
    },

    onoffDown() {
      //console.log("maxDim", this.maxDim)
      if (this.currentNode === null) return true;
      else if (this.currentNode.id < 0) {
        return true;
      } else {
        if (this.currentNode.ordDim < this.maxDim - 1) return false;
        else {
          let cnt = 1;
          let i = 0;
          while (i < this.table.length) {
            let nd = this.table[i];
            if (nd.id === this.currentNode.parent) {
              const {children} = nd;
              cnt = children.length;
              break;
            }
            i++;
          }
          return this.currentNode["ordFactorInDim"] >= cnt;
        }
      }
    },

    onoffDim() {
      if (this.currentNode === null) return true;
      else return this.currentNode.id < 0;
    },

    fnUp(up) {
      api
        .post(baseURL, {
          method: "meterfactor/changeOrd",
          params: [{rec: this.currentNode, up: up}],
        })
        .then(
          () => {
            //reload...
            this.fetchData(this.meter_id);
          },
          (error) => {
            let msg = error.response.data.error.message
              ? error.response.data.error.message
              : error.message;
            notifyError(msg);
          }
        );
    },

    fnNewDim() {
      api
        .post(baseURL, {
          method: "meterfactor/newDimension",
          params: [{rec: this.currentNode, maxDim: this.maxDim}],
        })
        .then(
          () => {
            //reload...
            this.fetchData(this.meter_id);
          },
          (error) => {
            let msg = error.response.data.error.message
              ? error.response.data.error.message
              : error.message;
            notifyError(msg);
          }
        );
    },

    fnIns() {
      let data = null;
      api
        .post(baseURL, {
          method: "meterfactor/newRec",
          params: [{}],
        })
        .then((response) => {
          //console.log("rec", response.data)
          data = response.data.result.records[0];
          data.ordDim = this.maxDim;
          this.$q
            .dialog({
              component: UpdateMeterFactors,
              componentProps: {
                meterId: this.meter_id,
                data: data,
                dense: true, //???????????
                maxDim: this.maxDim,
                // ...
              },
            })
            .onOk((data) => {
              //console.log("Ok! updated", data);
              if (data.res) {
                //reload...
                this.fetchData(this.meter_id);
              }
            });
        });
    },

    fnDel(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.cod +
            ": " +
            rec.name +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "meterfactor/delFactor",
              params: [{rec: rec}],
            })
            .then(
              () => {
                //this.selected = ref([]);
                this.fetchData(this.meter_id);
                //notifySuccess("Успешно!")
              },
              () => {
                notifyInfo(this.$t("hasValue"));
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    fetchData(meter) {
      api
        .post(baseURL, {
          method: "meterfactor/load",
          params: [{meter: meter}],
        })
        .then((response) => {
          this.table = pack(response.data.result.records, "ordDim");
          if (this.table.length > 0)
            this.maxDim = this.table[this.table.length - 1].ordDim + 1;
          //console.log("data", this.table)
          //console.log("maxDim", this.maxDim)
        });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          headerStyle:
            "font-size: 1.2em; color: #1976d2; background: antiquewhite",
          style: "font-size: 1.2em; text-align: left; width:20%",
        },

        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.2em; color: #1976d2; background: antiquewhite;",
          style: "font-size: 1.2em;text-align: left; width:35%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle:
            "font-size: 1.2em; color: #1976d2; background: antiquewhite",
          style: "font-size: 1.2em;text-align: left; width:45%",
        },
      ];
    },

    nodeInfo() {
      let res = "";
      if (this.currentNode) {
        res = this.currentNode.cod;
      }
      return res;
    },

    isEmpty() {
      return !this.table.length>0
    }
  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.table, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  mounted() {
    //console.log("params", this.$route.params.meterStruct)
    this.meter_id = parseInt(this.$route["params"].meter, 10);
    let ms = parseInt(this.$route["params"].meterStruct, 10)
    if (ms === 2) {
      this.fetchData(this.meter_id);
    }
  },

  created() {
    //this.lang = localStorage.getItem("curLang")
    //this.lang = this.lang === "en-US" ? "en" : this.lang
    this.columns = this.getColumns();

    //
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        this.FD_AccessLevel = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_AccessLevel.set(it["id"], it["text"]);
        })
      })
      .catch((error) => {
        let msg = error.message;
        if (error.response) msg = this.$t(error.response.data.error.message);
        notifyError(msg);
      })
  },

  setup() {}

});
</script>

<style></style>
