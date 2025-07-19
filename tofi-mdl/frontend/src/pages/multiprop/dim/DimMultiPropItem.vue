<template>
  <div class="q-pa-md">

    <q-splitter
        v-model="splitterModel"
        :model-value="splitterModel"
        :limits="[60, 100]"
        before-class="overflow-hidden q-mr-sm"
        after-class="overflow-hidden q-ml-sm"
        separator-class="bg-red"
    >

      <template v-slot:before>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div class="row">
            <div style="font-size: 1.2em; font-weight: bold;">
              {{ $t("dimMultiPropItem") }}
              <span v-if="dimMultiPropType===1" class="text-blue">({{$t("dimStatic")}}):</span>
              <span v-else class="text-red">({{$t("dimDynamic")}}):</span>
            </div>
            <span style="color: black; margin-left: 10px; margin-top: 2px">
          {{ dimMultiPropName }}
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
                v-if="hasTarget('mdl:mn_ds:pmdim:sel:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                :disable="dimMultiPropType===2 && rows.length > 0"
                @click="fnIns('ins', false)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("create1level") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="dimMultiPropType===1 && hasTarget('mdl:mn_ds:pmdim:sel:ins')"
                dense
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
                v-if="hasTarget('mdl:mn_ds:pmdim:sel:upd')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnIns('upd', false)"
                :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:pmdim:sel:del')"
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

        <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
        <QTreeTable
            :cols="cols"
            :rows="rows"
            :icon_leaf="''"
            @updateSelect="onUpdateSelect"
            ref="childComp"
            checked_visible="true"
        />
        </div>

      </template>

      <template v-slot:after>
        <div v-if="entityType()==='meter'">
          <dim-multi-prop-item-meter :dimMultiPropItem="entityId()" ref="childCompMeter"/>
        </div>
        <div v-else-if="entityType()==='attr_str' || entityType()==='attr_mask' || entityType()==='attr_date'">
          <dim-multi-prop-item-attrib :dimMultiPropItem="entityId()" ref="childCompAttr"/>
        </div>
        <div v-else-if="entityType()==='factorval'">
          <dim-multi-prop-item-factor :dimMultiPropItem="entityId()" ref="childCompFactor"/>
        </div>
        <div v-else-if="entityType()==='obj'">
          <dim-multi-prop-item-cls :dimMultiPropItem="entityId()" ref="childCompCls"/>
        </div>
        <div v-else-if="entityType()==='relobj'">
          <dim-multi-prop-item-rel-cls :dimMultiPropItem="entityId()" ref="childCompRelCls"/>
        </div>
        <div v-else-if="entityType()==='measure'">
          <dim-multi-prop-item-measure :dimMultiPropItem="entityId()" ref="childCompMeasure"/>
        </div>
        <div v-else>
          <dim-multi-prop-item-none :dimMultiPropItem="entityId()" ref="childCompNone"/>
        </div>


      </template>

    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import QTreeTable from "components/QTreeTable.vue";
import UpdateDimMultiPropItem from "pages/multiprop/dim/UpdateDimMultiPropItem.vue";
import DimMultiPropItemAttrib from "pages/multiprop/dim/DimMultiPropItemAttrib.vue";
import DimMultiPropItemMeter from "pages/multiprop/dim/DimMultiPropItemMeter.vue";
import DimMultiPropItemFactor from "pages/multiprop/dim/DimMultiPropItemFactor.vue";
import DimMultiPropItemCls from "pages/multiprop/dim/DimMultiPropItemCls.vue";
import DimMultiPropItemRelCls from "pages/multiprop/dim/DimMultiPropItemRelCls.vue";
import DimMultiPropItemMeasure from "pages/multiprop/dim/DimMultiPropItemMeasure.vue";
import DimMultiPropItemNone from "pages/multiprop/dim/DimMultiPropItemNone.vue";
import allConsts from "pages/all-consts.js";

export default {
  name: "DimMultiPropItem",
  components: {
    DimMultiPropItemNone,
    DimMultiPropItemMeasure,
    DimMultiPropItemRelCls,
    DimMultiPropItemCls, DimMultiPropItemMeter, DimMultiPropItemAttrib, DimMultiPropItemFactor, QTreeTable},

  data() {
    return {
      splitterModel: 100,
      visible: ref(false),
      dense: true,
      cols: [],
      rows: [],
      currentNode: null,
      dimMultiPropGr: 0,
      dimMultiProp: 0,
      dimMultiPropType: 0,
      dimMultiPropName: null,
      titles: [],
    };
  },
  methods: {
    hasTarget,
    entityType() {
      if (this.currentNode) {
        return this.currentNode.entityType
      } else
        return ""
    },

    entityId() {
      if (this.currentNode) {
        //console.info("currentNode", this.currentNode)
        return this.currentNode.id
      } else
        return 0
    },

    toBack() {
      this.$router["push"]({
        name: "DimMultiProp",
        params: {
          dmpGr: this.dimMultiPropGr,
          dmp: this.dimMultiProp,
        },
      });
    },

    onUpdateSelect(item) {
      this.currentNode = item.selected !== undefined ? item.selected : null
      let id = item.selected !== undefined ? item.selected.id : 0
      if (this.currentNode) {
        this.splitterModel = 60
      } else {
        this.splitterModel = 100
      }

//console.info("onUpd", this.currentNode.entityType, this.currentNode.multiEntityType)
      if (id > 0) {
        if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.meter)
          this.$refs.childCompMeter?.loadDimMultiPropItemMeter(id)
        else if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.attr_str ||
                  this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.attr_mask ||
                    this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.attr_date)
          this.$refs.childCompAttr?.loadDimMultiPropItemAttrib(id)
        else if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.fv)
          this.$refs.childCompFactor?.loadDimMultiPropItemFactor(id)
        else if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.measure)
          this.$refs.childCompMeasure?.loadDimMultiPropItemMeasure(id)
        else if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.obj)
          this.$refs.childCompCls?.loadDimMultiPropItemCls(id)
        else if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.relobj)
          this.$refs.childCompRelCls?.loadDimMultiPropItemRelCls(id)
        else if (this.currentNode.multiEntityType===allConsts.FD_MultiValEntityType.none)
          this.$refs.childCompNone?.load(id)
        else
          notifyError("Не известный тип сущности")

      }

    },

    edit(data, mode, isChild, parentName) {
      const lg = this.lang;
      this.$q
          .dialog({
            component: UpdateDimMultiPropItem,
            componentProps: {
              data: data,
              mode: mode,
              isChild: isChild,
              dimMultiPropName: this.dimMultiPropName,
              dimMultiPropType: this.dimMultiPropType,
              lg: lg,
              parentName: parentName,
              titles: this.titles
            },
          })
          .onOk((r) => {
            this.currentNode = null
            this.fetchData(this.dimMultiProp);
            this.$refs.childComp.restoreSelect(r)
            this.onUpdateSelect({selected: r})
          });
    },

    fnIns(mode, isChild) {

      let parent = null;
      let parentName = null;

      if (mode === "ins") {
        api
            .post(baseURL, {
              method: "dimMultiProp/newRecDimMultiPropItem",
              params: [this.dimMultiProp],
            })
            .then((response) => {
              if (isChild) {
                parent = this.currentNode.id;
                let parentNode = [];
                getParentNode(this.rows, parent, parentNode);
                parentName = parentNode[0].fullName;
                response.data.result.records[0].parent = parent;
              }

              this.edit(
                  response.data.result.records[0],
                  mode,
                  isChild,
                  parentName
              );
            });
      } else {
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          parentName = parentNode[0].fullName;
          isChild = true;
        }
        this.edit(this.currentNode, mode, isChild, parentName);
      }
    },

    fnDel(rec) {
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord") +
                '<div style="color: plum">(' +
                rec.name +
                ")</div>",
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            //let index = this.rows.findIndex((row) => row.id === rec.id);
            api
                .post(baseURL, {
                  method: "dimMultiProp/deleteDimItem",
                  params: [rec],
                })
                .then(
                    () => {
                      this.$refs.childComp.clrAny();
                      this.fetchData(this.dimMultiProp);
                    },
                    (error) => {
                      let msg = error.message;
                      if (error.response)
                        msg = error.response.data.error.message;
                      notifyError(msg);
                    }
                );
          })
          .onCancel(() => {
            notifyInfo(this.$t("canceled"));
          });
    },

    fetchData(propDim) {
      this.visible = ref(true);
      api
          .post(baseURL, {
            method: "dimMultiProp/loadDimMultiPropItem",
            params: [propDim],
          })
          .then((response) => {
            //console.info("rows", response.data.result.records)
            this.rows = pack(response.data.result.rows.records, "ord")
            //this.fnExpand()
            this.titles = response.data.result.titles.records
          })
        .then(() => {
          let b = false
          this.cols.forEach(c=> {
            if (c.name.substring(0,4)==="col_")
              b = true
          })
          let cnt = this.titles.length
          if (b) {
            this.cols.splice(2, cnt)
          }
          let k = 2
          this.titles.forEach(r=> {
            this.cols.splice(k++, 0, {
              name: "col_"+r.id,
              label: r.title,
              field: "col_"+r.id,
              align: "left",
              headerStyle: "font-size: 1.2em; width: 15%"
            })
          })


        })
          .catch((error) => {

            let msg = error.message;
            if (error.response) msg = this.$t(error.response.data.error.message);
            notifyError(msg);
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },

        {
          name: "entityType",
          label: this.$t("multiValEntityType"),
          field: "entityType",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 15%",
        }
      ];
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang")
    this.lang = this.lang === "en-US" ? "en" : this.lang
    this.cols = this.getColumns()
  },

  mounted() {
    //console.log("mounted!", this.$route.params);
    this.dimMultiPropGr = parseInt(this.$route["params"].dimMultiPropGr, 10);
    this.dimMultiProp = parseInt(this.$route["params"].dimMultiProp, 10);
    this.dimMultiPropName = this.$route["params"].dimMultiPropName;
    this.dimMultiPropType = parseInt(this.$route["params"].dimMultiPropType, 10);
    //
    this.fetchData(this.dimMultiProp);
    //

  },

  computed: {},

  setup() {},
};
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
}
/*  -ms-filter: "FlipV";*/
</style>
