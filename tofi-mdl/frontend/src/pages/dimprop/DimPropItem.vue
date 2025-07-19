<template>
  <div class="q-pa-md">

    <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="article"></q-avatar>
        {{ $t("dimsPropItem") }}
      </div>
    </q-banner>


    <q-banner dense inline-actions class="bg-orange-1">
      <div class="row">
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("dimProp") }}:</div>
        <span style="color: black; margin-left: 10px; margin-top: 2px">
          {{ dimPropName }}
        </span>
      </div>

      <div class="row">
        <div style="font-size: 1.0em; color: #1976d2;  margin-top: 2px">{{ $t("dimPropType") }}:</div>
        <span style="color: #9C27B0; margin-left: 10px; margin-top: 2px">
          {{ getDimPropType(dimPropType) }}
        </span>
      </div>

      <template v-slot:action>
        <q-btn
            dense round color="secondary" icon="arrow_back" glossy
            @click="toBack()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="isTree()"
            dense class="q-ml-sm" icon="expand_more" color="secondary" @click="fnExpand()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>
        <q-btn
            v-if="isTree()"
            dense icon="expand_less" color="secondary" class="q-ml-sm" @click="fnCollapse()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn

            v-if="hasTarget('mdl:mn_dp:dmprop:sel:ins')"
            dense
            icon="post_add"
            color="secondary"
            class="q-ml-sm"
            :disable="loading"
            @click="addNode(false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="isTree() && hasTarget('mdl:mn_dp:dmprop:sel:ins')"
            dense
            icon="post_add"
            color="secondary"
            class="q-ml-sm img-vert"
            :disable="loading || selected.length === 0"
            @click="addNode(true)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addSubNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_dp:dmprop:sel:upd')"
            dense
            icon="edit"
            color="secondary"
            class="q-ml-sm"
            :disable="loading || selected.length === 0"
            @click="editNode()"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("editNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
            v-if="hasTarget('mdl:mn_dp:dmprop:sel:del')"
            dense
            icon="delete"
            color="secondary"
            class="q-ml-sm"
            @click="deleteNode()"
            :disable="loading || selected.length === 0"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("deleteNode") }}
          </q-tooltip>
        </q-btn>

        <q-inner-loading :showing="loading" color="secondary"/>
      </template>
    </q-banner>

    <div style="height: calc(100vh - 510px); width: 100%" class="no-scroll">
      <div
          v-if="dimPropType === constDimPropTypeProp"
          class="q-table-container q-table--dense scroll"
          style="height: 100%"
      >
        <table
            class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle"
        >
          <thead class="text-bold text-white bg-blue-grey-13">
            <tr class style="text-align: left">
              <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
              <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
              <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
              <th :style="cols[3].headerStyle">{{ cols[3].label }}</th>
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
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                  ></q-icon>

                  <q-btn
                      dense flat
                      :color="item.propType === undefined ? 'gray' : 'blue'"
                      :icon="
                      selected.length > 0 && item.id === selected[0].id
                        ? 'check_box'
                        : 'check_box_outline_blank'
                    "
                      @click.stop="selectedCheck(item)"
                  >
                  </q-btn>

                  <q-icon :name="getIcon(item)" size="24px"></q-icon>
                  {{ item.name }}
                </span>
            </td>
            <td :data-th="cols[1].name">{{ item.fullName }}</td>
            <td :data-th="cols[2].name">{{ fnPropType(item.propType) }}</td>
            <td :data-th="cols[3].name">{{ item.cmt }}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div
          v-else
          class="q-table-container q-table--dense scroll"
          style="height: 100%"
      >
        <table
            class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle"
        >
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr class style="text-align: left">
            <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
            <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
            <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
          </tr>
          </thead>

          <tbody style="background: aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="cols[0].name" @click="toggle(item, index)">
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
                      :color="item.propType === undefined ? 'gray' : 'blue'"
                      :icon="
                      selected.length > 0 && item.id === selected[0].id
                        ? 'check_box'
                        : 'check_box_outline_blank'
                    "
                      @click.stop="selectedCheck(item)"
                  >
                  </q-btn>

                  <q-icon :name="getIcon(item)"></q-icon>
                  {{ item.name }}
                </span>
            </td>
            <td :data-th="cols[1].name">{{ item.fullName }}</td>
            <td :data-th="cols[2].name">{{ item.cmt }}</td>
          </tr>
          </tbody>
        </table>

        <div>
          <q-td colspan="100%" v-if="selected.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
          </q-td>
          <q-td
              v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
            {{ $t("infoApp") }}
          </q-td>
        </div>

      </div>
    </div>
  </div>
</template>

<script>

import {ref} from "vue";
import {collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import allConsts from "pages/all-consts";
import UpdaterDimPropItemProp from "pages/dimprop/UpdaterDimPropItemProp.vue";
import UpdaterDimPropItemFactor from "pages/dimprop/UpdaterDimPropItemFactor.vue";
import UpdaterDimPropItemMultiProp from "pages/dimprop/UpdaterDimPropItemMultiProp.vue";
import UpdaterDimPropItemDimMultiProp from "pages/dimprop/UpdaterDimPropItemDimMultiProp.vue";


export default {
  name: "DimPropItem",

  data() {
    return {
      cols: [],
      rows: [],
      loading: ref(false),
      FD_PropType: null,
      FD_DimPropType: null,
      isExpanded: true,
      itemId: null,
      selected: ref([]),
      dimPropGr: 0,
      dimProp: 0,
      dimPropType: 0,
      dimPropName: "",
      constDimPropTypeProp: allConsts.FD_DimPropType.prop,
      consDimPropTypeFactor: allConsts.FD_DimPropType.factor,
      consDimPropTypeMultiList: allConsts.FD_DimPropType.multiList,
      consDimPropTypeDMP: allConsts.FD_DimPropType.dimMultiProp,
      countRec: 0,
      dimMultiPropType: 0,
      dimMultiProp: 0
    };
  },

  methods: {
    hasTarget,
    //$router.push('/dimsprop')
    toBack() {
      this.$router["push"]({
        name: "DimProp",
        params: {
          dimPropGr: this.dimPropGr,
          dimProp: this.dimProp,
        },
      });
    },

    fnPropType(val) {
      return this.FD_PropType ? this.FD_PropType.get(val) : null;
    },

    getDimPropType(val) {
      return this.FD_DimPropType ? this.FD_DimPropType.get(val) : null;
    },

    addNode(isChild) {
      let data = {dimProp: this.dimProp};
      let parentName = "Нет";
      if (isChild) {
        data.parent = this.selected[0].id;
        parentName = this.selected[0].name;
      } else {
        if (this.selected.length > 0) {
          let parentNode = [];
          data.parent = this.selected[0].parent;
          getParentNode(this.rows, this.selected[0].parent, parentNode);
          parentName = parentNode.length > 0 ? parentNode[0].name : "Нет";
        }
      }

      let cmp = null
      if (this.dimPropType === allConsts.FD_DimPropType.prop)
        cmp = UpdaterDimPropItemProp
      if (this.dimPropType === allConsts.FD_DimPropType.factor)
        cmp = UpdaterDimPropItemFactor
      if (this.dimPropType === allConsts.FD_DimPropType.multiList)
        cmp = UpdaterDimPropItemMultiProp
      if (this.dimPropType === allConsts.FD_DimPropType.dimMultiProp)
        cmp = UpdaterDimPropItemDimMultiProp

      this.$q
          .dialog({
            component: cmp,
            componentProps: {
              data: data,
              dimPropType: this.dimPropType,
              dimMultiProp: this.dimMultiProp,
              dimMultiPropType: this.dimMultiPropType,
              mode: "ins",
              dimProp: this.dimProp,
              dimPropName: this.dimPropName,
              parentName: parentName,
              lg: this.lang,
              dense: true,
            },
          })
          .onOk((r) => {
            //if (data.res) {
            this.loadData(this.dimProp);
            this.selected = []
            this.selected.push(r)
            //}
          });
    },

    editNode() {
      let parentNode = [];
      let parentName = "Нет";
      if (this.selected[0].parent > 0) {
        getParentNode(this.rows, this.selected[0].parent, parentNode);
        parentName = parentNode[0].name;
      }

      let cmp = null
      if (this.dimPropType === allConsts.FD_DimPropType.prop)
        cmp = UpdaterDimPropItemProp
      if (this.dimPropType === allConsts.FD_DimPropType.factor)
        cmp = UpdaterDimPropItemFactor
      if (this.dimPropType === allConsts.FD_DimPropType.multiList)
        cmp = UpdaterDimPropItemMultiProp
      if (this.dimPropType === allConsts.FD_DimPropType.dimMultiProp)
        cmp = UpdaterDimPropItemDimMultiProp

      //console.info("data!", this.selected[0])

      this.$q
          .dialog({
            component: cmp,
            componentProps: {
              data: this.selected[0],
              dimPropType: this.dimPropType,
              mode: "upd",
              dimProp: this.dimProp,
              dimMultiProp: this.dimMultiProp,
              dimMultiPropType: this.dimMultiPropType,
              dimPropName: this.dimPropName,
              parentName: parentName,
              lg: this.lang,
              dense: true,
            },
          })
          .onOk((data) => {
            this.selected = [];
            this.loadData(this.dimProp);
            this.selected.push(data)
          });
    },

    deleteNode() {
      let rec = this.selected[0];
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' + rec.name + ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            api
                .post(baseURL, {
                  method: "dimprop/deleteDPI",
                  params: [rec.id],
                })
                .then(
                    () => {
                      this.selected = []
                      this.loadData(this.dimProp);
                    },
                    () => {
                      notifyInfo(this.$t("hasChild"));
                    }
                );
          });
    },

    selectedCheck(item) {
      let vm = this;
      if (vm.selected.length > 0 && item.id === vm.selected[0].id)
        vm.selected = [];
      else {
        vm.selected = [];
        vm.selected.push(item);
      }
    },

    loadData(dimprop) {
      this.loading = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimprop/loadDimPropItemProp",
            params: [dimprop, this.dimPropType],
          })
          .then((response) => {
            this.rows = pack(response.data.result.records, "ord");
            this.countRec = this.rows.length
            this.fnExpand();
            //console.info("countRec", this.countRec)
            //console.info("rows", this.rows)
          })
        .then(()=> {
          if (this.dimPropType === allConsts.FD_DimPropType.dimMultiProp) {
            api
              .post(baseURL, {
                method: "dimprop/getDimMultiPropType",
                params: [dimprop],
              })
              .then((response) => {
                this.dimMultiProp = response.data.result.dmp
                this.dimMultiPropType = response.data.result.dmpt
                //console.info("dmp, dmpt", this.dimMultiProp, this.dimMultiPropType)
              })
          }
        })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

            notifyError(msg);
            //
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    getIcon(row) {
      if (this.dimPropType === allConsts.FD_DimPropType.factor)
        return "";
      if (row.propType === undefined)
        return "folder";
      if (allConsts.FD_PropType.factor === row.propType) {
        return "account_tree";
      } else if (allConsts.FD_PropType.attr === row.propType) {
        return "format_shapes";
      } else if (allConsts.FD_PropType.meter === row.propType) {
        return "scale";
      } else if (allConsts.FD_PropType.rate === row.propType) {
        return "speed";
      } else if (allConsts.FD_PropType.typ === row.propType) {
        return "view_quilt";
      } else if (allConsts.FD_PropType.reltyp === row.propType) {
        return "view_column";
      } else if (allConsts.FD_PropType.measure === row.propType) {
        return "square_foot";
      } else if (allConsts.FD_PropType.complex === row.propType) {
        return "category";
      }
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    getColumnsProp() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 30%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 35%",
        },
        {
          name: "propType",
          label: this.$t("propType"),
          field: "propType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 15%",
          format: (val) =>
              this.FD_PropType ? this.FD_PropType.get(val) : null,
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
      ];
    },

    getColumnsFactor() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 35%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.3em; width: 45%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
      ];
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

    isTree() {
      return this.dimPropType === allConsts.FD_DimPropType.prop
        || this.dimPropType === allConsts.FD_DimPropType.factor
        || this.dimPropType === allConsts.FD_DimPropType.dimMultiProp;
    },

    infoSelected(row) {
      return " " + row.name
    }

  },

  created() {
    console.log("create DimPropItem");
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    //
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_PropType"}],
        })
        .then((response) => {
          this.FD_PropType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_PropType.set(it["id"], it["text"]);
          });
        });
    //
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{ dict: "FD_DimPropType" }],
        })
        .then((response) => {
          this.FD_DimPropType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_DimPropType.set(it["id"], it["text"]);
          });
        });
    //


    if (this.dimPropType === allConsts.FD_DimPropType.prop) {
      this.cols = this.getColumnsProp();
      //console.log("prop", this.dimPropType);
    } else {
      //console.log("!prop", this.dimPropType);
      this.cols = this.getColumnsFactor();
    }


    //this.loadData(this.dimProp);

    console.log("end DimPropItem");

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
    //console.info("DimPropItem mounted", this.$route.params)

    this.dimPropGr = parseInt(this.$route["params"].dimPropGr, 10);
    this.dimProp = parseInt(this.$route["params"].dimProp, 10);
    this.dimPropType = parseInt(this.$route["params"].dimPropType, 10);
    this.dimPropName = this.$route["params"].name;

    //console.info("dimPropType", this.dimPropType)
    //console.info("dimProp", this.dimProp)

    if (this.dimPropType === allConsts.FD_DimPropType.prop) {
      this.cols = this.getColumnsProp();
      //console.log("prop", this.dimPropType);
    } else {
      //console.log("!prop", this.dimPropType);
      this.cols = this.getColumnsFactor();
    }

    this.loadData(this.dimProp);

  },

  setup() {}

};
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
