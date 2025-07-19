<template>
  <q-banner dense inline-actions class="bg-orange-1">
    <div style="font-size: 1.2em; font-weight: bold;">{{ $t("mrSoftMeter") }}</div>
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
        v-if="hasTarget('mdl:mn_ds:meter:sel:soft:edit')"
        dense
        icon="edit_note"
        color="secondary"
        class="q-ml-sm"
        @click="fnEdit()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:meter:sel:soft:upd')"
        dense
        icon="edit"
        color="secondary"
        class="q-ml-sm"
        @click="fnUpd()"
        :disable="currentNode == null"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:meter:sel:soft:del')"
        dense
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

  <div class="q-table--dense wrap bg-orange-1">
    <div class="q-pa-sm-sm scroll">
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr class style="text-align: left">
          <th :style="columns[0].style">{{ columns[0].label }}</th>
          <th :style="columns[1].style">{{ columns[1].label }}</th>
          <th :style="columns[2].style">{{ columns[2].label }}</th>
          <th :style="columns[3].style">{{ columns[3].label }}</th>
          <th :style="columns[4].style">{{ columns[4].label }}</th>
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
          <td :data-th="columns[1].name">{{ item.name }}</td>
          <!--fullName-->
          <td :data-th="columns[2].name">{{ item.fullName }}</td>
          <!--al-->
          <td :data-th="columns[3].name">{{ fnAL(item.accessLevel) }}</td>
          <!--cmt-->
          <td :data-th="columns[4].name">{{ item.cmt }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import {defineComponent, ref} from "vue";
import {api, baseURL} from "boot/axios";
import {collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import UpdateSoftMeterRate from "pages/meter/UpdateSoftMeterRate.vue";
import UpdaterMeterSoftRatesUpd from "pages/meter/UpdaterMeterSoftRatesUpd.vue";

export default defineComponent({
  name: "MeterSoftRates",

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
      loading: ref(false),
    };
  },
  mounted() {
    this.meter_id = parseInt(this.$route["params"].meter, 10);
    this.fetchData(this.meter_id);
    //console.info("this.meter_id", this.meter_id)
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
        })
      })
      .catch((error) => {
        let msg = error.message;
        if (error.response) msg = this.$t(error.response.data.error.message);
        notifyError(msg);
      })
  },

  methods: {
    hasTarget,
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

    fnEdit() {
      this.$q
        .dialog({
          component: UpdaterMeterSoftRatesUpd,
          componentProps: {
            meter: this.meter_id,
            lg: this.lang,
            dense: true,
          },
        })
        .onOk((data) => {
          if (data.res) {
            this.fetchData(this.propId);
          }
        });
    },

    fnUpd() {
      let parentName = null;
      let isChild = false;
      let data = {
        id: this.currentNode.id,
        parent: this.currentNode.parent,
        meter: this.meter_id,
        cod: this.currentNode.cod,
        accessLevel: this.currentNode.accessLevel,
        name: this.currentNode.name,
        fullName: this.currentNode.fullName,
        cmt: this.currentNode.cmt,
        //accessLevel_text: this.currentNode.accessLevel_text,
      };
      if (this.currentNode.parent > 0) {
        let parentNode = [];
        getParentNode(this.table, this.currentNode.parent, parentNode);
        console.log("ParentNode-----", parentNode);
        parentName = parentNode[0].fullName;

        isChild = true;
      }
      this.$q
        .dialog({
          component: UpdateSoftMeterRate,
          componentProps: {
            mode: "upd",
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
              method: "meterrate/deleteSoftMR",
              params: [{rec: rec}],
            })
            .then(
              () => {
                this.fetchData();
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
      api
        .post(baseURL, {
          method: "meterrate/loadSoftMR",
          params: [this.meter_id],
        })
        .then((response) => {
          //console.log("fd_al", response.data.result["dictdata"]["FD_AccessLevel"])
          //console.log("records", response.data.result.records)
          this.table = pack(response.data.result.records, "ord");
          //this.FD_AccessLevel = response.data.result["dictdata"]["FD_AccessLevel"]
          //console.log("FD_AccessLevel 1", this.FD_AccessLevel[1].text)
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
    fnExpand() {
      expandAll(this.table);
    },
    fnCollapse() {
      collapsAll(this.table);
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

</style>
