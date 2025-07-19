<template>
  <div class="q-pa-sm-sm" style="height: calc(100vh - 290px)">
    <q-banner dense inline-actions class="bg-amber-1">
      <div style="font-size: 1.2em; font-weight: bold;">{{ $t("mrHardMeter") }}</div>
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
            v-if="hasTarget('mdl:mn_ds:meter:sel:hard:ins')"
            icon="post_add"
            color="secondary"
            class="q-ml-sm"
            @click="fnIns('ins', false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("create1level") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:meter:sel:hard:ins')"
            icon="post_add"
            color="secondary"
            class="q-ml-sm img-vert"
            @click="fnIns('ins', true)"
            :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("createChild") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:meter:sel:hard:upd')"
            icon="edit"
            color="secondary"
            class="q-ml-sm"
            @click="fnIns('upd', null)"
            :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_ds:meter:sel:hard:del')"
            icon="delete"
            color="secondary"
            class="q-ml-sm"
            @click="fnDel(currentNode)"
            :disable="currentNode == null"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
      </template>
    </q-banner>

    <div class="q-pa-md-md">
      <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
      {{ this.nodeInfo() }}
    </div>

    <div
        class="q-table-container q-table--dense wrap bg-amber-1"
        style="height: 100%; width: 100%"
    >
      <div class="q-pa-sm-sm">
        <div class="q-table-middle scroll">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr class style="text-align: left">
              <th style="font-size: 1.3em">{{ columns[0].label }}</th>
              <th style="font-size: 1.3em">{{ columns[1].label }}</th>
              <th style="font-size: 1.3em">{{ columns[2].label }}</th>
              <th style="font-size: 1.3em">{{ columns[3].label }}</th>
              <th style="font-size: 1.3em">{{ columns[4].label }}</th>
            </tr>
            </thead>

            <tbody style="background: aliceblue; height: 100%">
            <tr v-for="(item, index) in arrayTreeObj" :key="index">
              <td
                  :data-th="columns[0].name"
                  style="width: 20%"
                  @click="toggle(item)"
              >
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
              <td :data-th="columns[1].name" style="width: 20%">
                {{ item.name }}
              </td>
              <!--fullName-->
              <td :data-th="columns[2].name" style="width: 20%">
                {{ item.fullName }}
              </td>
              <!--al-->
              <td :data-th="columns[3].name" style="width: 10%">
                {{ fnAL(item.accessLevel) }}
              </td>
              <!--cmt-->
              <td :data-th="columns[4].name" style="width: 25%">
                {{ item.cmt }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import {collapsAll, expand, expandAll, getParentNode, hasTarget, notifyInfo, pack,} from "src/utils/jsutils";
import UpdateHardMeterRate from "pages/meter/UpdateHardMeterRate.vue";

export default defineComponent({
  name: "MeterHardRates",

  data: function () {
    return {
      isExpanded: true,
      selected: ref([]),
      currentNode: null,
      itemId: null,
      FD_AccessLevel: null,
      columns: [],
      table: [],

      separator: "cell",
      meter_id: null,
      loading: ref(false),
      //bgBody: 'background: aliceblue',
    };
  },
  mounted() {
    //console.info("params meter", this.$route.params.meter)
    //console.info("params meterStruct", this.$route.params.meterStruct)
    this.meter_id = parseInt(this.$route["params"].meter, 10);
    this.fetchData(this.meter_id);
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    this.columns = this.getColumns();

    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_AccessLevel"}],
        })
        .then((response) => {
          this.FD_AccessLevel = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_AccessLevel.set(it["id"], it["text"]);
          });
        });
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
    //
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

    fnIns(mode, isChild) {
      let data = {
        id: 0,
        cod: "",
        meter: this.meter_id,
        accessLevel: 1,
        name: "",
        fullName: "",
        cmt: null,
      };

      let parent = null;
      let parentName = null;
      if (isChild) {
        parent = this.currentNode.id;
        parentName = this.currentNode.fullName;
      }
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          parent: this.currentNode.parent,
          meter: this.meter_id,
          cod: this.currentNode.cod,
          accessLevel: this.currentNode.accessLevel,
          name: this.currentNode.name,
          fullName: this.currentNode.fullName,
          cmt: this.currentNode.cmt,
        };
        if (this.currentNode.parent > 0) {
          //let pn = ref("")
          //getParentName(this.table, this.currentNode.parent, pn)
          //parentName = pn.value

          let parentNode = [];
          getParentNode(this.table, this.currentNode.parent, parentNode);
          //console.log("ParentNode-----", parentNode);
          parentName = parentNode[0].fullName;

          isChild = true;
        }
      }

      this.$q
          .dialog({
            component: UpdateHardMeterRate,
            componentProps: {
              mode: mode,
              isChild: isChild,
              parentName: parentName,
              data: data,
              // ...
            },
          })
          .onOk((data) => {
            //console.log("Ok! updated", data);
            if (data.res) {
              //reload...
              this.fetchData();
              expand(this.currentNode)

            }
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
                  method: "meterrate/deleteHardMR",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      //this.selected = ref([]);
                      this.fetchData();
                      //notifySuccess("Успешно!")
                    },
                    () => {
                      /*
                                      let msg = "";
                                      if (error.response) msg = error.response.data.error.message;
                                      else msg = error.message;
                                      notifyError(msg)
                      */
                      notifyInfo(this.$t("hasChild"));
                    }
                );
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          });
    },

    fetchData() {
      this.loading = ref(true);
      api
          .post(baseURL, {
            method: "meterrate/loadHardMR",
            params: [this.meter_id],
          })
          .then((response) => {
            //console.log("fd_al", response.data.result["dictdata"]["FD_AccessLevel"])
            //console.log("records", response.data.result.records)
            this.table = pack(response.data.result.records, "ord");
            //this.FD_AccessLevel = response.data.result["dictdata"]["FD_AccessLevel"]
            //console.log("FD_AccessLevel 1", this.FD_AccessLevel[1].text)
          })
          .finally(() => {
            //test

            /*
          console.log("")
          console.log("")
          console.log("")
          console.log("TREE")

          for (let i=0; i < this.table.length; i++) {
            dfs(this.table[i])
          }
  */

            //
            this.loading = ref(false);
          });
    },

    fnAL(val) {
      return this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null;
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          headerStyle:
              "font-size: 1.2em; color: #1976d2; background: antiquewhite",
          style: "text-align: left; width:5%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle:
              "font-size: 1.2em; color: #1976d2; background: antiquewhite",
          style: "text-align: left; width:15%",
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
          style: "text-align: left; width:25%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          classes: "bg-blue-grey-1",
          headerStyle:
              "font-size: 1.2em; color: #1976d2; background: antiquewhite",
          style: "text-align: left; width:15%",
          format: (val) => this.FD_AccessLevel.get(val),
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          classes: "bg-blue-grey-1",
          headerStyle:
              "font-size: 1.2em; color: #1976d2; background: antiquewhite",
          style: "text-align: left; width:40%",
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
  },
  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.table, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  setup() {}

});
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}

</style>
