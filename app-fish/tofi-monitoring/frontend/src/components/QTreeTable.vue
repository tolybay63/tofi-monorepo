<template>

<!--
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
-->

  <div class="q-pa-sm-sm bg-orange-1">

    <q-inner-loading :showing="visible" color="secondary"/>


    <table class="q-table q-table--cell-separator q-table--bordered q-table--dense wrap my-sticky-header-table">
      <thead class="text-bold text-white bg-blue-grey-13" >
        <tr style="text-align: left">
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
          <td :data-th="cols0[0]['name']" @click="toggle(item)">
              <span class="q-tree__node" v-bind:style="setPadding(item)">
                <q-icon
                    style="cursor: pointer"
                    :name="iconName(item)"
                    color="secondary"
                ></q-icon>

                <span v-if="checked_visible">
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
                </span>

                {{
                  cols0[0].field === "periodType"
                      ? fnPeriodType(item[cols0[0].field])
                      : item[cols0[0].field]
                }}
              </span>
          </td>
          <!--other cols without 0-->
          <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">
              <span v-if="col['checked']">
                <q-btn
                    dense
                    flat
                    color="blue"
                    :icon="
                    item[col.field] ? 'check_box' : 'check_box_outline_blank'
                  "
                >
                </q-btn>
              </span>
              <span v-else>
                  <span v-if="col['field'] === 'propType'">
                    <q-icon
                        size="24px"
                        :color="getColor(item)"
                        :name="getIcon(item[col['field']])"
                    ></q-icon>
                  </span>
                  {{
                  col['field'] === "accessLevel"
                      ? fnAL(item[col['field']])
                      : col['field'] === "propType"
                          ? fnPT(item)
                          : col['field'] === "fvFishFamily"
                              ? fnFishType(item)
                              : col['field'] === "dbeg"
                                  ? fnDbeg(item[col['field']])
                                  : col['field'] === "dend"
                                      ? fnDend(item[col['field']])
                                      : item[col['field']]
                  }}
              </span>
          </td>
        </tr>
      </tbody>

    </table>

  </div>
</template>

<script>
import {ref} from "vue";
import {collapsAll, expandAll} from "src/utils/jsutils.js";
import {tofi_dbeg, tofi_dend} from "boot/axios.js";
import {date} from "quasar";
import allConsts from "pages/all-consts.js";

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
    "meterStruct",
    "FD_PropType",
    "FD_AccessLevel",
    "FD_PeriodType",
    "FD_FishType",
    "emptydate",
  ],
  emits: [
    "updateSelect"
  ],

  setup() {
    //console.log("Tree setup")
    return {
      isExpanded: false,
      currentNode: null,
      itemId: null,
      selected: ref([]),
      visible: false,
    };
  },
  methods: {

    getColor(item) {
      if (item['propType'] === 2 || item['propType'] === 3) {
        if (item['meterStruct'] === allConsts.FD_MeterStruct.hard) return "orange";
        else return "green";
      } else {
        if (item['propType'] === allConsts.FD_PropType.complex) return "red";
        else return "orange";
      }
    },

    getIcon(val) {
      if (val === allConsts.FD_PropType.factor) return "account_tree";
      else if (val === allConsts.FD_PropType.meter) return "scale";
      else if (val === allConsts.FD_PropType.rate) return "speed";
      else if (val === allConsts.FD_PropType.attr) return "format_shapes";
      else if (val === allConsts.FD_PropType.typ) return "view_quilt";
      else if (val === allConsts.FD_PropType.reltyp) return "view_column";
      else if (val === allConsts.FD_PropType.measure) return "square_foot";
      else if (val === allConsts.FD_PropType.complex) return "category";
    },

    fnAL(val) {
      return this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null;
    },

    fnPeriodType(val) {
      return this.FD_PeriodType ? this.FD_PeriodType.get(val) : null;
    },

    fnFishType(item) {
      //console.info("item", item)
      return this.FD_FishType
          ? this.FD_FishType[item.fvFishFamily]
          : null;
    },

    fnPT(item) {
      let at = "";
      if (item['propType'] === allConsts.FD_PropType.attr) {
        if (item['attribValType'] === allConsts.FD_AttribValType.str)
          at = " (" + this.$t("str") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.mask)
          at = " (" + this.$t("mask") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.dt)
          at = " (" + this.$t("dt") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.dttm)
          at = " (" + this.$t("dttm") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.tm)
          at = " (" + this.$t("tm") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.file)
          at = " (" + this.$t("file") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.multistr)
          at = " (" + this.$t("multistr") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.period)
          at = " (" + this.$t("period") + ")";
        else if (item['attribValType'] === allConsts.FD_AttribValType.entity)
          at = " (" + this.$t("entity") + ")";
      }
      return this.FD_PropType ? this.FD_PropType.get(item['propType']) + at : null;
    },

    fnDbeg(val) {
      //console.log("dbeg", val)
      let fmt = "...";
      if (this.emptydate !== undefined) fmt = this.emptydate;
      return val <= tofi_dbeg ? fmt : date.formatDate(val, "DD.MM.YYYY");
    },

    fnDend(val) {
      let fmt = this.emptydate === undefined ? "..." : this.emptydate;
      return val >= tofi_dend ? fmt : date.formatDate(val, "DD.MM.YYYY");
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
      return this.icon_leaf;
    },

    toggle(item) {
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
        res = this.currentNode.name
            ? this.currentNode.name
            : this.currentNode.cod ? this.currentNode.cod : "";
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
    this.visible= false
  },

  created() {
    this.visible= true
  },
};
</script>

<style lang="sass">
.my-sticky-header-table
  /* height or max-height is important */
  height: calc(100vh - 190px)

  .q-table__top,
  .q-table__bottom,
  thead tr:first-child th
    /* bg color is important for th; just specify one #00b4ff  #546e7a #bdbdcb*/
    background-color: #bdbdbd

  thead tr th
    position: sticky
    z-index: 1
  thead tr:first-child th
    top: 0

  /* this is when the loading indicator appears */
  &.q-table--loading thead tr:last-child th
    /* height of all previous header rows */
    top: 52px

  /* prevent scrolling behind sticky top row on focus */
  tbody
    /* height of all previous header rows */
    scroll-margin-top: 48px
</style>
