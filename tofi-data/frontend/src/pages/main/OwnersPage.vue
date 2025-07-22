<template>
  <div style="height: calc(100vh - 190px); width: 100%" class="no-scroll">
    <q-bar class="bg-orange-1" style="height: 48px">
      {{
        propName === "prop"
          ? isFlat === 0
            ? $t("ownersStd")
            : $t("ownersFlat")
          : $t("ownersMulti")
      }}
      <q-btn
        v-if="fnTarget()"
        dense
        :disable="selected.length === 0"
        icon="pan_tool_alt"
        color="secondary"
        class="q-mt-sm"
        @click="fnChoose()"
        style="margin-bottom: 5px; margin-left: 10px"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("chooseRecord") }}
        </q-tooltip>
      </q-btn>
      <q-space></q-space>

      <div class="text-purple">
        {{ $t("infoContexMenu") }}
      </div>

    </q-bar>

    <q-inner-loading :showing="loading" color="secondary"/>

    <div class="q-table--dense wrap bg-orange-1 scroll" style="height: 95%">
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
        <tr v-for="(item) in arrayTreeObj" :key="item.cod">
          <td :data-th="cols0[0].name" @click="toggle(item)">
              <span class="q-tree__node" v-bind:style="setPadding(item)">
                <q-icon
                  style="cursor: pointer"
                  :name="iconName(item)"
                  color="secondary"
                />
                <q-btn
                  dense
                  flat
                  :color="'blue'"
                  :icon="getIcon(item)"
                  @click.stop="updateCheck(item)"
                >
                  <q-menu auto-close context-menu>
                    <q-list>
                      <div v-if="item.isObj">
                        <div v-if="item.level===0">
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'ins')">
                              {{ $t("createObj") }}
                            </q-item-section>
                           </q-item>
                        </div>
                        <div v-else>
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'ins')">
                              {{ $t("createObj") }}
                            </q-item-section>
                          </q-item>
                          <q-separator/>
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'upd')">
                              {{ $t("updateObj") }}
                            </q-item-section>
                          </q-item>
                          <q-separator/>
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'del')">
                              {{ $t("deleteObj") }}
                            </q-item-section>
                          </q-item>
                        </div>
                      </div>
                      <div v-else>
                        <div v-if="item.level===0">
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'ins')">
                              {{ $t("createRel") }}
                            </q-item-section>
                          </q-item>
                        </div>
                        <div v-else>
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'ins')">
                              {{ $t("createRel") }}
                            </q-item-section>
                          </q-item>
                          <q-separator/>
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'upd')">
                              {{ $t("updateRel") }}
                            </q-item-section>
                          </q-item>
                          <q-separator/>
                          <q-item clickable>
                            <q-item-section @click="showMenu(item, 'del')">
                              {{ $t("deleteRel") }}
                            </q-item-section>
                          </q-item>

                        </div>

                      </div>

                    </q-list>
                  </q-menu>

                </q-btn>

                {{ item[cols0[0].field] }}
              </span>
          </td>

          <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">
            <div v-if="col.name==='fullName'">
              {{ item[col.field] }}
              <div v-if="item.parent && item.isObj">
                <q-badge
                  floating rounded :color="fnColorCls(item)" text-color="white"
                  v-if="item.parent"
                >
                  {{ fnClsName(item) }}
                  <q-tooltip>
                    {{ $t("cls") }}
                  </q-tooltip>
                </q-badge>
              </div>
              <div v-if="!item.parent && !item.isObj">
                <q-badge
                  floating rounded :color="fnColorCls(item)" text-color="white"
                >
                  {{ fnClsName(item) }}
                  <q-tooltip>
                    {{ $t("reltyp") }}
                  </q-tooltip>
                </q-badge>
              </div>

            </div>
            <div v-else>
              {{ item[col.field] }}
            </div>
          </td>
        </tr>
        </tbody>
      </table>

      <q-bar class="bg-orange-1"/>
    </div>
  </div>
</template>

<script>
import {ref} from "vue";
import UpdateObj from "pages/owners/UpdateObj.vue";
import {api, baseURL} from "boot/axios";
import {findNode, getParentNode, hasTarget, notifyError, notifyInfo} from "src/utils/jsutils";
import {useParamsStore} from "stores/params-store";
import UpdateRelObj from "pages/owners/UpdateRelObj.vue";
import {extend} from "quasar";
import {storeToRefs} from "pinia";

const storeParams = useParamsStore();
const {getStoreParams, getModel, getMetaModel} = storeToRefs(storeParams);

let ARR_COLORS = [
  "primary", "secondary", "accent", "dark",
  "positive", "negative", "warning", "info"
]

export default {
  name: "OwnersPage",
  props: ["propName", "isFlat"],

  data() {
    return {
      loading: false,
      cols: [],
      rows: [],
      isExpanded: false,
      selected: [],
      currentNode: null,
      itemId: null,
      typORrel: 0,
      mapCls: new Map(),
      mapClsName: new Map(),
      mapClsClr: new Map(),
      mapRelTyp: new Map(),
      mapRelTypName: new Map(),
      mapRelTypClr: new Map(),
      //
    };
  },

  methods: {
    loadChilds(node) {
      console.info("node", node);
      this.toggle2(node);
      setTimeout(() => {
        const children = node.children;
        console.info(children.length);
        children.forEach(child => {
          this.loadChilds(child);
        })
      }, 100)
    },

    fnClsName(item) {
      if (item.parent && item.isObj)
        return this.mapClsName.get(item.cls)
      if (!item.parent && !item.isObj)
        return this.mapRelTypName.get(item["relTyp"])
      return "Not in CharGr"
    },

    fnColorCls(item) {
      if (!item.parent && !item.isObj)
        return this.mapRelTypClr.get(item["relTyp"])
      if (item.parent && item.isObj)
        return this.mapClsClr.get(item.cls)
      return "red"
    },

    showMenu(item, mode) {
      console.log("item", item);
      //this.toggle(item)
      if (item.isObj) {
        let parentName = null
        if (item.level > 0) {
          if (mode === "del") {
            this.$q
              .dialog({
                title: this.$t("confirmation"),
                message:
                  this.$t("deleteObj") + "?" +
                  '<div style="color: plum">(' + item.name + ")</div>",
                html: true,
                cancel: true,
                persistent: true,
                focus: "cancel",
              })
              .onOk(() => {
                api
                  .post(baseURL, {
                    method: "data/deleteOwnerWithProperties",
                    params: [item.node, 1, getModel.value, getMetaModel.value],
                  })
                  .then(() => {
                    let parentNode = []
                    getParentNode(this.rows, item["parent"], parentNode)
                    const children = parentNode[0].children
                    let ind = 0
                    for (let i = 0; i < children.length; i++) {
                      if (item["id"] === children[i].id) {
                        ind = i
                        break
                      }
                    }
                    children.splice(ind, 1)
                  })
                  .catch(error => {
                    console.log(error.message)
                    notifyInfo(error.message)
                  })
              })
              .onCancel(() => {
                notifyInfo(this.$t("canceled"));
              })
          } else {
            this.loading = true
            api
              .post(baseURL, {
                method: "data/getParentName",
                params: [item.node, getModel.value, getMetaModel.value],
              })
              .then((response) => {
                parentName = response.data.result
              })
              .then(() => {
                this.createObj(item, parentName, mode);
              })
              .finally(() => {
                this.loading = false
              })
          }
        } else
          this.createObj(item, null, mode)
      } else {
        let parentName = null
        if (item.level > 0) {
          if (mode === "del") {
            this.loading = true
            this.$q
              .dialog({
                title: this.$t("confirmation"),
                message:
                  this.$t("deleteObj") + "?" +
                  '<div style="color: plum">(' + item.name + ")</div>",
                html: true,
                cancel: true,
                persistent: true,
                focus: "cancel",
              })
              .onOk(() => {
                api
                  .post(baseURL, {
                    method: "data/deleteOwnerWithProperties",
                    params: [item.node, 0, getModel.value, getMetaModel.value],
                  })
                  .then(() => {
                    let parentNode = []
                    getParentNode(this.rows, item["parent"], parentNode)
                    const children = parentNode[0].children
                    let ind = 0
                    for (let i = 0; i < children.length; i++) {
                      if (item["id"] === children[i].id) {
                        ind = i
                        break
                      }
                    }
                    children.splice(ind, 1)
                  })
                  .then(()=> {
                    this.loading = false
                  })
                  .catch(error => {
                    console.log(error.message)
                    notifyInfo(error.message)
                    this.loading = false
                  })
              })
              .onCancel(() => {
                notifyInfo(this.$t("canceled"));
              })
          } else {
            this.loading = true
            api
              .post(baseURL, {
                method: "data/getParentNameRO",
                params: [item.relCls],
              })
              .then((response) => {
                parentName = response.data.result
              })
              .then(() => {
                this.createRelObj(item, parentName, mode);
              })
              .finally(() => {
                this.loading = false
              })
          }
        } else
          this.createRelObj(item, item.name, mode);
      }
    },

    createObj(item, parentName, mode) {
      console.info("createObj", item);
      let res1 = []
      findNode(this.rows, "id", `t_${item.typORrel}`, res1);
      this.loadChilds(res1[0])

      let data = {accessLevel: 1}
      if (item.level > 0) {
        data = {cls: item.cls}
      }

      if (mode === "upd") {
        this.loading = true
        api
          .post(baseURL, {
            method: "data/loadRecObjOrRelObj",
            params: [item.node, true, getModel.value, getMetaModel.value],
          })
          .then(
            (response) => {
              data = response.data.result.records[0]
              if (data.dbeg === "1800-01-01") {
                data.dbeg = null
              }
              if (data.dend === "3333-12-31") {
                data.dend = null
              }
            })
          .then(() => {
            this.updateObj(data, item, data.name, mode)
          })
          .catch(error => {
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          })
          .finally(() => {
            this.loading = false
          })
      } else {
        this.updateObj(data, item, parentName, mode)
      }
    },

    updateObj(data, item, parentName, mode) {
      console.info("updateObj", item)
      console.info("data", data)

      this.$q
        .dialog({
          component: UpdateObj,
          componentProps: {
            data: data,
            typ: item.typORrel,
            typOrCls: item.level > 1 ? item.cls : item.typORrel,
            level: item.level,
            parentName: parentName,
            isMultiProp: this.propName !== "prop",
            mode: mode
            // ...
          },
        })
        .onOk((data) => {
          console.info("emit data", data)
          console.info("emit item", data)
          if (mode === "ins") {
            let res1 = []
            findNode(this.rows, "id", `t_${item.typORrel}`, res1)
            console.info("emit2", res1)
            let res2 = []
            findNode(res1, "id", data.parent, res2)
            console.info("emit3", res2)
            const children = res2[0].children;
            children.push(data)
            children.sort((a,b)=> a.cls - b.cls)
            this.selected = []
            this.selected.push(data)
          }
          if (mode === "upd") {
            let res1 = []
            findNode(this.rows, "id", `t_${item.typORrel}`, res1)
            console.info("emit2 parent", res1)
            let res2 = []
            findNode(res1, "id", data.id, res2)
            console.info("emit3 rec", res2)
            extend(true, res2[0], data)
            this.selected = []
            this.selected.push(data)
          }

            //reload...
            //this.loadOwners1lev()

        })
    },

    createRelObj(item, name, mode) {
      console.info("createObj", item);

      this.loadChilds(item)

      let data = {
        accessLevel: 1,
      }
      if (mode === "upd") {
        this.loading = true
        api
          .post(baseURL, {
            method: "data/loadRecObjOrRelObj",
            params: [item.node, false, getModel.value, getMetaModel.value],
          })
          .then(
            (response) => {
              data = response.data.result.records[0]
              if (data.dbeg === "1800-01-01") {
                data.dbeg = null
              }
              if (data.dend === "3333-12-31") {
                data.dend = null
              }
            })
          .then(() => {
            this.updateRelObj(data, item, name, mode)
          })
          .catch(error => {
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message)
            notifyError(msg)
          })
          .finally(() => {
            this.loading = false
          })
      } else {
        this.updateRelObj(data, item, name, mode)
      }
    },

    updateRelObj(data, item, name, mode) {
      this.$q
        .dialog({
          component: UpdateRelObj,
          componentProps: {
            data: data,
            relCls: item.typORrel,
            name: name,
            mode: mode
            // ...
          },
        })
        .onOk((r) => {
          if (mode === "ins") {
            console.info("item", item)
            console.info("data", data)
            console.info("emit", r)
            let res1 = []
            findNode(this.rows, "id", r.parent, res1)
            if (res1[0].children.length===0)
              res1[0].children = []
            let children = res1[0].children;
            children.push(r)
            this.selected = []
            this.selected.push(r)
          } else {
            console.info("item", item)
            console.info("data", data)
            console.info("emit", r)

            let res1 = []
            findNode(this.rows, "id", item.parent, res1)
            console.info("res1", res1)
            let res2 = []
            findNode(res1, "id", r.id, res2)
            console.info("res2", res2)
            extend(true, res2[0], r)
            this.selected = []
            this.selected.push(r)
          }
         //   this.loadOwners1lev()
        })
    },

    updateCheck(item) {
      if (item.level === 0) return;
      //this.selected = ref([])
      if (this.selected.length === 0) {
        this.selected.push(item);
      } else {
        if (this.selected[0].id === item.id) this.selected = ref([]);
        else {
          this.selected = ref([]);
          this.selected.push(item);
        }
      }
      if (this.selected.length > 0) {
        this.typORrel = this.selected[0].typORrel;
      }

      //console.info("typORrel, this.selected:", this.typORrel, this.selected)
    },

    getIcon(item) {
      if (item.id.includes("r_")) {
        return "category";
      } else if (item.id.includes("t_")) {
        return "view_quilt";
      } else {
        if (this.selected.length > 0 && this.selected[0].id === item.id)
          return "check_box";
        else return "check_box_outline_blank";
      }
    },

    loadOwners1lev() {
      this.loading = true
      api
        .post(baseURL, {
          method: "data/loadOwnersParent",
          params: [getStoreParams.value],
        })
        .then(
          (response) => {
            response.data.result.records.forEach(function (o) {
              o.children = []
              o.leaf = false
            });
            this.rows = response.data.result.records
          })
        .then(() => {
          for (let r of this.rows) {
            if (!r.isObj) {
              this.mapRelTypName.set(r["relTyp"], this.mapRelTyp.get(r["relTyp"]).name)
              this.mapRelTypClr.set(r["relTyp"], this.mapRelTyp.get(r["relTyp"]).clr)
            }
          }
        })
        .catch(error => {
          let msg = error.message;
          if (error.response)
            msg = this.$t(error.response.data.error.message)
          notifyError(msg)
        })
        .finally(() => {
          this.loading = false
        })
    },

    fnChoose() {
      if (this.propName === "prop") {
        if (this.isFlat === 1) {
          this.$router["push"]({
            name: "FlatInputPage",
            params: {
              owner: this.selected[0].node,
              isObj: this.selected[0].isObj,
              typORrel: this.typORrel,
            },
          });
        } else {
          this.$router["push"]({
            name: "StdInputPage",
            params: {
              owner: this.selected[0].node,
              isObj: this.selected[0].isObj,
              typORrel: this.typORrel,
            },
          });
        }
      } else if (this.propName === "multiProp") {
        this.$router["push"]({
          name: "MultiInputPage",
          params: {
            owner: this.selected[0].node,
            isObj: this.selected[0].isObj,
            typORrel: this.typORrel,
          },
        });
      }
    },

    recursive(obj, newObj, level, itemId, isExpend) {
      let vm = this;
      obj.forEach(function (o) {
        if (o.children && o.children.length !== 0) {
          o.level = level;
          o.leaf = false;
          //itemId = o.id   //new
          newObj.push(o);
          if (o.id === itemId) {
            o.expend = isExpend;
          }
          if (o.expend) {
            vm.recursive(o.children, newObj, o.level + 1, itemId, isExpend);
          }
        } else {
          o.level = level;
          //o.leaf = true;
          if (o.loaded)
            //new
            o.leaf = true;

          newObj.push(o);
          return false;
        }
      });
    },

    iconName(item) {
      if (!item.loaded) {
        return "control_point";
      }
      if (item.expend) {
        return "remove_circle_outline";
      }
      if (item.children && item.children.length > 0) {
        return "control_point";
      }
      return "";
    },

    toggle(item) {
      //console.info("item", item)
      if (!item.loaded) {
        this.loading = true
        let isMultiProp = this.propName !== "prop";
        api
          .post(baseURL, {
            method: "data/loadOwners",
            params: [isMultiProp, item.id, item.cls ? item.cls : 0, item.relCls ? item.relCls : 0,
              item.isObj, getModel.value, getMetaModel.value, 0],
          })
          .then((response) => {
            item.loaded = !item.loaded
            this.loading = true
            for (let record of response.data.result.records) {
              record.children = [];
              record.loaded = false;
              record.leaf = false;
              record.expend = false;
              if (item.isObj) {
                this.mapClsName.set(record.cls, this.mapCls.get(record.cls).name)
                this.mapClsClr.set(record.cls, this.mapCls.get(record.cls).clr)
              }
              item.children.push(record);
            }
            this.loading = false
          })
          .finally(() => {
            item.loaded = true;
            if (item.children.length === 0) item.leaf = true;
            if (item.children && item.children.length > 0) {
              //item.expend = !item.expend;
              item.expend = true
            }
            this.loading = false
          })
      } else {
        //console.info("item toogle Loaded", item.id, item.loaded, item.expend)
        item.expend = !item.expend;
      }
    },

    toggle2(item) {
      //console.info("item", item)
      if (!item.loaded) {
        this.loading = true
        let isMultiProp = this.propName !== "prop";
        api
          .post(baseURL, {
            method: "data/loadOwners",
            params: [isMultiProp, item.id, item.cls ? item.cls : 0, item.relCls ? item.relCls : 0,
              item.isObj, getModel.value, getMetaModel.value, 0],
          })
          .then((response) => {
            item.loaded = !item.loaded
            this.loading = true
            for (let record of response.data.result.records) {
              record.children = [];
              record.loaded = false;
              record.leaf = false;
              record.expend = false;
              if (item.isObj) {
                this.mapClsName.set(record.cls, this.mapCls.get(record.cls).name)
                this.mapClsClr.set(record.cls, this.mapCls.get(record.cls).clr)
              }
              item.children.push(record);
            }
            this.loading = false
          })
          .finally(() => {
            item.loaded = true;
            if (item.children.length === 0) item.leaf = true;
            if (item.children && item.children.length > 0) {
              //item.expend = !item.expend;
              item.expend = true
            }
            this.loading = false
          })
      } else {
        //console.info("item toogle Loaded", item.id, item.loaded, item.expend)
        item.expend = true//!item.expend;
      }
    },

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },

    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.3em; width:35%;",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.3em; width:50%;",
        },
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.3em; width:15%;",
        },
      ];
    },

    fnTarget() {
      if (this.propName === "prop") {
        if (this.isFlat === 1) {
          return hasTarget("dta:dtaflat");
        } else {
          return hasTarget("dta:dtastd");
        }
      } else {
        return hasTarget("dta:dtamatrix");
      }
    },
  },

  created() {
    this.loading = true
    api
      .post(baseURL, {
        method: "data/loadClsFromChar",
        params: [],
      })
      .then(
        (response) => {
          response.data.result.records.forEach((it, index) => {
            let i = index % 8
            this.mapCls.set(it["id"], {name: it["name"], clr: ARR_COLORS[i]})
          })
          //console.info("Cls", this.mapCls)
        })
      .then(() => {
        api
          .post(baseURL, {
            method: "data/loadRelTypFromChar",
            params: [],
          })
          .then(
            (response) => {
              response.data.result.records.forEach((it, index) => {
                let i = index % 8
                this.mapRelTyp.set(it["id"], {name: it["name"], clr: ARR_COLORS[i]})
              })
              //console.info("RelTyp", this.mapRelTyp)
            })
      })
      .then(() => {
        this.cols = this.getColumns()
      })
      .then(() => {
        this.loadOwners1lev()
      })
      .finally(() => {
        this.loading = false
      })

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
};
</script>

<style scoped></style>
