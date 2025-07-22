<template>
  <span class="q-pa-sm-sm">
    <q-btn
      dense
      icon="expand_more"
      color="secondary"
      @click="fnExpand()"
      style="margin-bottom: 5px"
    >
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
      style="margin-bottom: 5px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("collapseAll") }}
      </q-tooltip>
    </q-btn>
    <span v-if="checked_visible">
      <span style="color: #1976d2; margin-left: 5px">
        {{ $t("selectedNode") }}:
      </span>
      {{ this.nodeInfo() }}
    </span>
  </span>

  <div class="q-pa-sm-sm bg-orange-1">
    <table class="q-table q-table--cell-separator q-table--bordered wrap">
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
        <td :data-th="cols0[0].name" @click="toggle(item, index)">
            <span class="q-tree__node" v-bind:style="setPadding(item)">
              <q-icon
                style="cursor: pointer"
                :name="iconName(item)"
                color="secondary"
              />

              <span v-if="checked_visible">
                <q-btn
                  dense flat color="blue"
                  :icon="
                    selected.length === 1 && item.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click.stop="selectedRow(item)"
                >
                </q-btn>
              </span>

              {{ item[cols0[0].field] }}
            </span>
        </td>
        <!--other cols without 0-->
        <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">
          <span :class="fnCls(item)">
            {{fnClsName(item)}}
          </span>
            <span class="q-mx-sm"/>
          <span :class="fnQTFvCls(item)">
            {{fnQTFV(item)}}
          </span>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll} from "src/utils/jsutils";
import {useUserStore} from "stores/user-store.js";
import {storeToRefs} from "pinia";

const store = useUserStore();
const {getUserObj} = storeToRefs(store)

const expand = (item) => {
  item.expend = ref(true);
  const {children} = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
};

const collaps = (item) => {
  item.expend = ref(false);
  const {children} = item;
  if (children.length > 0) {
    item.leaf = ref(false);
  } else {
    item.leaf = ref(true);
    item.expend = undefined;
  }
};

export default {
  name: "QTreeTable",
  props: [
    "rows",
    "cols",
    "icon_leaf",
    "checked_visible",
    "mapQT",
    "mapQCls",
    "mapConsts",
  ],
  emits: ["updateSelect"],

  setup() {
    //console.log("Tree setup")
    return {
      isExpanded: false,
      currentNode: null,
      itemId: null,
      selected: ref([]),
    };
  },

  methods: {

    fnCls(item) {
      if (item.cls===this.mapConsts["Cls_Question"])
        return "bg-purple text-white rounded-borders"
      else if (item.cls===this.mapConsts["Cls_AnswerOpened"])
        return "bg-green text-white rounded-borders"
      else if (item.cls===this.mapConsts["Cls_AnswerClosed"])
        return "bg-red text-white rounded-borders"
    },

    fnClsName(item) {
      let v = !this.mapQCls ? "" : this.mapQCls.get(item.cls)
      return v==="" ? "" : v.name
    },

    fnQTFvCls(item) {
      if (item["questiontypefv"]===this.mapConsts["FV_IsUniq"])
        return "bg-green text-white rounded-borders"
      else
        return "bg-blue text-white rounded-borders"
    },

    fnQTFV(item) {
      //console.info("fnQTFV", item)
      if (item.parent)
        return null
      else
        return !this.mapQT ? "" : this.mapQT.get(item.questiontypefv)
    },


    clrAny() {
      this.selected = [];
      this.currentNode = null;
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    restoreSelect(item) {
      let vm = this;
      vm.selected = [];
      vm.selected.push(item);
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
      this.$emit("updateSelect", {selected: vm.selected[0]});
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
          if (o.expend === true) {
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
      if (item.expend === true) {
        return "remove_circle_outline";
      }
      if (item.children && item.children.length > 0) {
        return "control_point";
      }
      return this.icon_leaf;
    },

    toggle(item, index) {
      if (item.children && item.children.length > 0) {
        if (item.expend) collaps(item);
        else expand(item);
      }
    },
    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },

    nodeInfo() {
      let res = "";
      if (this.currentNode) {
        res = !this.currentNode.cod
          ? this.currentNode.name
          : this.currentNode.cod;
      }
      return res;
    },
  },

  computed: {
    cols0() {
      return this.cols.slice(0, 1);
    },

    cols_() {
      return this.cols.slice(1);
    },
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
  },

  mounted() {
  },

  created() {
  },
};
</script>

<style scoped></style>
