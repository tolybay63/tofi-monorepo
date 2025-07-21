<!--
style="height: calc(100vh - 220px); width: 100%"
-->
<template>
  <div class="q-pa-sm-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[60, 100]"
      horizontal
      separator-class="bg-red"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"

    >
      <template v-slot:before>
        <q-banner dense inline-actions class="bg-orange-1">
          <div style="font-size: 1.2em; font-weight: bold;">{{ $t("clss") }}</div>
          <template v-slot:action>
            <q-btn
              dense
              icon="expand_more"
              color="secondary"
              @click="fnExpand()"
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
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("collapseAll") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:cls:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:cls:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="editRow(selected[0], 'upd')"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel:cls:del')"
              dense
              icon="delete"
              color="secondary"
              class="q-ml-sm"
              @click="removeRow(selected[0])"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:typ:sel')"
              :dense="dense"
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading || selected.length === 0"
              @click="clsSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>

          </template>

          <q-inner-loading :showing="loading" color="secondary"/>
        </q-banner>

        <div class="q-pa-sm-sm">
          <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
          {{ this.nodeInfo() }}
        </div>

        <div class="scroll" style="height: calc(100vh - 500px); width: 100%">

          <div class="q-pa-sm-sm q-table-container q-table--dense wrap bg-orange-1">

            <div class="q-table-middle scroll">
              <table class="q-table q-table--cell-separator q-table--bordered wrap"
              >
                <thead class="text-bold text-white bg-blue-grey-13">
                <tr class style="text-align: left">
                  <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
                  <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
                  <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
                  <th :style="cols[3].headerStyle">{{ cols[3].label }}</th>
                  <th :style="cols[4].headerStyle">{{ cols[4].label }}</th>
                  <th :style="cols[5].headerStyle">{{ cols[5].label }}</th>
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

                        <q-icon
                          name="brightness_1"
                          :color="getIconColor(item)"
                        ></q-icon>
                        {{ item.cod }}
                      </span>
                  </td>
                  <!--name-->
                  <td :data-th="cols[1].name">{{ item.name }}</td>
                  <!--fullName-->
                  <td :data-th="cols[2].name">{{ item.fullName }}</td>
                  <!--accessLevel-->
                  <td :data-th="cols[3].name">
                    {{ fnAL(item.accessLevel) }}
                  </td>
                  <!--db-->
                  <td :data-th="cols[4].name">{{ fnDb(item.dataBase) }}</td>

                  <!--cmt-->
                  <td :data-th="cols[5].name">{{ item.cmt }}</td>
                </tr>
                </tbody>
              </table>
            </div>

        </div>

        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 scroll">
          <div style="font-size: 1.2em; font-weight: bold;">{{ $t("clsFV") }}</div>

          <template v-slot:action>
            <q-btn
              dense
              icon="expand_more"
              color="secondary"
              @click="fnExpand2()"
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
              @click="fnCollapse2()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("collapseAll") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div class="q-table-container q-table--dense wrap bg-orange-1 scroll">

            <table
              class="q-table q-table--cell-separator q-table--bordered wrap "
            >
              <thead class="text-bold text-white bg-blue-grey-13">
              <tr class style="text-align: left">
                <th :style="cols2[0].headerStyle">{{ cols2[0].label }}</th>
                <th :style="cols2[1].headerStyle">{{ cols2[1].label }}</th>
                <th :style="cols2[2].headerStyle">{{ cols2[2].label }}</th>
              </tr>
              </thead>

              <tbody style="background: aliceblue">
              <tr v-for="(item, index) in arrayTreeObj2" :key="index">
                <td :data-th="cols2[0].name" @click="toggle2(item)">
                      <span
                        class="q-tree-link q-tree-label"
                        v-bind:style="setPadding(item)"
                      >
                        <q-icon
                          style="cursor: pointer"
                          :name="iconName(item)"
                          color="secondary"
                        ></q-icon>
                        <q-icon
                          :name="getIcon(item)"
                          :color="getIconColor(item)"
                        ></q-icon>
                        {{ item.name }}
                      </span>
                </td>
                <td :data-th="cols2[1].name">{{ item.fullName }}</td>
                <td :data-th="cols2[2].name">{{ item.cod }}</td>
              </tr>
              </tbody>
            </table>

        </div>

      </template>
    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import UpdateCls from "pages/typ/cls/UpdateCls.vue";
import {
  collapsAll,
  expandAll,
  findNode,
  hasTarget,
  notifyError,
  notifyInfo,
  notifySuccess,
  pack
} from "src/utils/jsutils";


export default {
  name: "ClsPage",

  data() {
    return {
      splitterModel: 60,

      cols: [],
      rows: [],
      FD_AccessLevel: null,
      dataBase: null,
      loading: ref(false),
      separator: ref("cell"),
      selected: ref([]),
      isExpanded: true,
      selectedRowID: {},
      currentNode: null,
      itemId: null,
      dense: true,
      //
      cols2: [],
      rows2: [],
      //loading2: ref(false),
      //
      isExpanded2: true,
      selectedRowID2: {},
      currentNode2: null,
      itemId2: null,
      typId: 0,
      clsId: 0,
    };
  },
  methods: {
    hasTarget,

    clsSelect() {
      this.$router["push"]({
        name: "clsVer",
        params: {
          cls: this.selected[0].ent,
        },
      });
    },

    getIconColor(row) {
      if (row.isOwn === 1) return "green";
      else return "dark-gray";
    },

    getIcon(row) {
      if (row.parent) return "brightness_1";
      else return "square";
    },

    fnAL(val) {
      return this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null;
    },

    fnDb(val) {
      //console.log("fnDb", val);
      return this.dataBase ? this.dataBase.get(val) : null;
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

      this.updSelected(vm.selected);
      //temporary
      //console.log("selected", vm.selected[0])
      //console.log("currentNode", this.currentNode)
    },

    updSelected(cur) {
      if (cur.length === 0) this.fetchData2(0, 0);
      else {
        this.fetchData2(this.typId, cur[0].ent);
      }
    },

    fetchData2(typ, cls) {
      this.loading = ref(false);
      api
        .post(baseURL, {
          method: "typ/loadClsFV",
          params: [typ, cls],
        })
        .then((response) => {
          this.rows2 = pack(response.data.result.records, "ord");
          //console.log("data ClsFV", this.rows2)
          this.fnExpand2();
        })
        .catch((error) => {

          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
          //
        })
        .finally(() => {
          //setTimeout(() => {
          this.loading = ref(false);
          //}, 500)
        });
    },

    fetchData(typ) {
      this.loading = ref(true)

      this.selected = ref([])
      api
        .post(baseURL, {
          method: "typ/loadClsTree",
          params: [{typ: typ, typNodeVisible: false}],
        })
        .then((response) => {
          this.rows = pack(response.data.result.records, "ord");
          this.fnExpand();
        })
        .catch((error) => {

          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;

          notifyError(msg);
          //
        })
        .then(() => {
          if (this.clsId > 0) {
            let res = []
            findNode(this.rows, "ent", this.clsId, res)
            this.selected.push(res[0])
            this.updSelected(this.selected)
          }
        })
        .finally(() => {
          //setTimeout(() => {
          this.loading = ref(false);
          //}, 500)
        });
    },

    removeRow(rec) {
      //console.log("Delete Row:", JSON.stringify(rec));

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
          //let index = this.rows.findIndex((row) => row.ent === rec.ent);
          api
            .post(baseURL, {
              method: "typ/deleteCls",
              params: [{rec: rec}],
            })
            .then(
              () => {
                //console.log("response=>>>", response.data)
                this.fetchData(this.typId);
                this.updSelected([])
                notifySuccess(this.$t("success"));
              },
              (error) => {
                let msg
                if (error.response) msg = error.response.data.error.message;
                else msg = error.message;
                notifyError(msg);
              }
            );
        })
        .onCancel(() => {
          notifyInfo(this.$t("canceled"));
        });
    },

    editRow(rec, mode) {
      let data = {
        id: 0,
        cod: "",
        accessLevel: 1,
        typ: this.typId,
        isOpenness: true,
        dataBase: null,
        lastVer: 1,
        name: "",
        fullName: "",
        cmt: null,
        isOwn: 1,
      };
      if (mode === "upd") {
        data = {
          id: rec.ent,
          cod: rec.cod,
          accessLevel: rec.accessLevel,
          typ: rec.typ,
          isOpenness: rec.isOpenness,
          dataBase: rec.dataBase,
          lastVer: rec.lastVer,
          dbeg: rec.dbeg > tofi_dbeg ? rec.dbeg : null,
          dend: rec.dend < tofi_dend ? rec.dend : null,
          name: rec.name,
          fullName: rec.fullName,
          cmt: rec.cmt,
          isOwn: rec.isOwn,
        };
      }

      const lg = {name: this.lang};

      //console.log("data",data)

      this.$q
        .dialog({
          component: UpdateCls,
          componentProps: {
            data: data,
            mode: mode,
            typ: this.typId,
            lg: lg,
            dense: this.dense,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (r.res) {
            //reload...
            this.fetchData(this.typId);
          }
        })
    },

    nodeInfo() {
      let res = "";
      if (this.currentNode) {
        res = this.currentNode.cod + " - " + this.currentNode.name;
      }
      return res;
    },
    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 10%",
        },

        {
          name: "dataBase",
          label: this.$t("dbNameLabel"),
          field: "dataBase",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 10%",
        },

        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 30%",
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
          headerStyle: "font-size: 1.2em; width: 30%",
          style: "width: 30%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 50%",
          style: "width: 50%",
        },
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
          style: "width: 20%",
        },
      ];
    },
    /////////////////////////////////
    fnExpand() {
      expandAll(this.rows);
    },
    fnCollapse() {
      collapsAll(this.rows);
    },

    fnExpand2() {
      expandAll(this.rows2);
    },
    fnCollapse2() {
      collapsAll(this.rows2);
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
    toggle2(item) {
      let vm = this;
      vm.itemId2 = item.id;

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
        vm.itemId2 = null;
      }
    },
    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },
  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

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

    api
      .post(baseURL, {
        method: "database/loadDbForSelect",
        params: [],
      })
      .then((response) => {
        //console.info("db", response.data.result.records)
        this.dataBase = new Map();
        response.data.result.records.forEach((it) => {
          this.dataBase.set(it["id"], it["name"]);
        });
      });


    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();
  },

  mounted() {
    //console.log("mounted")

    this.typId = parseInt(this.$route["params"].typ, 10);
    this.clsId = parseInt(this.$route["params"].cls, 10);

    this.fetchData(this.typId);
  },

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    },
    arrayTreeObj2() {
      let vm = this;
      let newObj2 = [];
      vm.recursive(vm.rows2, newObj2, 0, vm.itemId2, vm.isExpanded2);
      return newObj2;
    },
  },

  setup() {
  },
};
</script>

<style scoped></style>
