<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1 no-scroll"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="folder"></q-avatar>
              {{ $t("dimMultiPropGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_ds:pmdim:insgr')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('ins', false)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:pmdim:insgr')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm img-vert"
                @click="fnInsGr('ins', true)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:pmdim:updgr')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnInsGr('upd', null)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_ds:pmdim:delgr')"
                dense
                icon="delete"
                color="secondary"
                class="q-ml-sm"
                @click="fnDelGr(currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary"/>
            </template>
          </q-banner>

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

        <q-splitter
          horizontal
          v-model="splitterModel2"
          :model-value="splitterModel2"
          before-class="overflow-hidden q-mr-sm"
          after-class="overflow-hidden q-ml-sm"
          separator-class="bg-red"
          class="bg-amber-1 no-scroll"
        >
          <template v-slot:before>

            <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
              <div style="font-size: 1.2em; font-weight: bold;">
                <q-avatar color="black" text-color="white" icon="type_specimen"></q-avatar>
                {{ $t("dimMultiProp") }}
              </div>


              <template v-slot:action>
                <q-btn
                  v-if="hasTarget('mdl:mn_ds:pmdim:ins')"
                  dense
                  icon="post_add"
                  color="secondary"
                  class="q-ml-sm"
                  @click="fnIns('ins')"
                  :disable="currentNode == null"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("newRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  v-if="hasTarget('mdl:mn_ds:pmdim:upd')"
                  dense
                  icon="edit"
                  color="secondary"
                  class="q-ml-sm"
                  @click="fnIns('upd')"
                  :disable="loading2 || selected2.length === 0"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("editRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  v-if="hasTarget('mdl:mn_ds:pmdim:del')"
                  dense
                  icon="delete"
                  color="secondary"
                  class="q-ml-sm"
                  @click="fnDel(selected2[0])"
                  :disable="loading2 || selected2.length === 0"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-space></q-space>

                <q-btn
                  dense icon="pan_tool_alt"
                  color="secondary" class="q-ml-lg"
                  :disable="loading2 || selected2.length === 0"
                  @click="propSelect()"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("chooseRecord") }}
                  </q-tooltip>
                </q-btn>
              </template>
            </q-banner>
            <q-table
              style="height: calc(100vh - 500px); width: 100%"
              color="primary"
              card-class="bg-amber-1 text-brown"
              row-key="id" dense
              :columns="cols2"
              :rows="rows2"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading2"
              selection="single"
              wrap-cells
              v-model:selected="selected2"
              :rows-per-page-options="[0]"
              @update:selected="onUpdateSelect2"
            >

              <template #bottom-row>
                <q-td colspan="100%" v-if="selected2.length > 0">
                  <span class="text-blue"> {{ $t("selectedRow") }}: </span>
                  <span class="text-bold"> {{ this.infoSelected(selected2[0]) }} </span>
                </q-td>
                <q-td colspan="100%" v-else-if="this.rows2.length > 0" class="text-bold">
                  {{ $t("infoRow") }}
                </q-td>
              </template>


            </q-table>

          </template>

          <template v-slot:after>

            <q-table
              color="primary"
              card-class="bg-amber-1 text-brown"
              row-key="id" dense
              :columns="cols3"
              :rows="rows3"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading3"
              selection="single"
              wrap-cells
              v-model:selected="selected3"
              :rows-per-page-options="[0]"
            >

              <template v-slot:top>
                <div style="font-size: 1.2em; font-weight: bold;">
                  {{ $t("titleDop") }}
                </div>
                <q-space/>
                <q-btn
                  dense icon="post_add" color="secondary"
                  :disable="loading3 || selected2.length === 0"
                  @click="editRow3('ins')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("newRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  dense
                  icon="edit" color="secondary" class="q-ml-sm"
                  :disable="loading3 || selected3.length === 0"
                  @click="editRow3('upd')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("editRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
                  dense icon="delete" color="secondary"
                  class="q-ml-sm"
                  :disable="loading3 || selected3.length === 0"
                  @click="removeRow3()"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>

                <q-space/>

                <q-btn
                  dense
                  icon="swipe_up_alt"
                  color="secondary"
                  class="q-ml-lg"
                  @click="fnUp(true)"
                  :disable="onoffUp()"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("up") }}
                  </q-tooltip>
                </q-btn>

                <q-btn
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

              </template>

              <template #loading>
                <q-inner-loading :showing="loading3" color="secondary"></q-inner-loading>
              </template>

            </q-table>


          </template>

        </q-splitter>

      </template>
    </q-splitter>
  </div>
</template>

<script>
import {ref} from "vue";
import {api, baseURL} from "boot/axios";
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  pack,
} from "src/utils/jsutils";
import QTreeTable from "components/QTreeTable.vue";
import UpdateGroup from "pages/group/UpdateGroup.vue";
import UpdateDimMultiProp from "pages/multiprop/dim/UpdateDimMultiProp.vue";
import UpdateDimMultiPropMoreCols from "pages/multiprop/dim/UpdateDimMultiPropMoreCols.vue";


export default {
  name: "DimMultiProp",
  components: {QTreeTable},

  data() {
    return {
      splitterModel: ref(30),
      splitterModel2: ref(60),

      cols: [],
      rows: [],
      currentNode: null,
      visible: ref(false),
      //
      cols2: [],
      rows2: [],
      FD_AccessLevel: null,
      FD_DimMultiPropType: null,
      loading2: ref(false),
      selected2: ref([]),
      dmpGr: 0,
      dmp: 0,
      //

      cols3: [],
      rows3: [],
      loading3: ref(false),
      selected3: ref([]),
      maxLen: 0,
    };
  },
  methods: {
    hasTarget,
    propSelect() {
      this.$router["push"]({
        name: "DimMultiPropItem",
        params: {
          dimMultiPropGr: this.currentNode.id,
          dimMultiProp: this.selected2[0].id,
          dimMultiPropName: this.selected2[0].name,
          dimMultiPropType: this.selected2[0].dimMultiPropType,
        },
      });
    },

    onUpdateSelect(item) {
      this.dmp = 0
      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        api
          .post(baseURL, {
            method: "group/loadRec",
            params: [{id: this.currentNode.id, tableName: "DimMultiPropGr"}],
          })
          .then((response) => {
            this.currentNode = response.data.result.records[0];

            this.fetchData(this.currentNode.id);
          });
      } else {
        this.selected2 = [];
        this.fetchData(0);
      }
    },

    fetchDataGr() {
      this.visible = ref(true)
      this.currentNode = null
      api
        .post(baseURL, {
          method: "group/loadGroup",
          params: [{tableName: "DimMultiPropGr"}],
        })
        .then(
          (response) => {
            this.rows = pack(response.data.result.records, "id");
            this.fnExpand();
          },
          (error) => {

            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            notifyError(msg);
          }
        )
        .then(() => {
          if (this.dmpGr > 0) {
            let res = []
            findNode(this.rows, "id", this.dmpGr, res)
            this.currentNode = res[0]
            this.$refs.childComp.restoreSelect(this.currentNode)
            this.fetchData(this.currentNode.id)
          }
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fnInsGr(mode, isChild) {
      let data = {
        id: 0,
        cod: "",
        name: "",
        fullName: "",
        accessLevel: 1,
        cmt: null,
      };
      const lg = this.lang;
      let parent = null;
      let parentName = null;
      if (isChild) {
        parent = this.currentNode.id;
        parentName = this.currentNode.name;
      }
      if (mode === "ins") {
        data.parent = parent;
      } else if (mode === "upd") {
        data = {
          id: this.currentNode.id,
          cod: this.currentNode.cod,
          name: this.currentNode.name,
          fullName: this.currentNode.fullName,
          accessLevel: this.currentNode.accessLevel,
          parent: this.currentNode.parent,
          cmt: this.currentNode.cmt,
        };
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          //console.log("ParentNode-----", parentNode)
          parentName = parentNode[0].fullName;

          //parentName = findParentNode(this.currentNode, this.rows).name
          isChild = true;
        }
      }
      //
      this.$q
        .dialog({
          component: UpdateGroup,
          componentProps: {
            data: data,
            mode: mode,
            isChild: isChild,
            tableName: "DimMultiPropGr",
            parentName: parentName,
            lg: lg,
            dense: true,
          },
        })
        .onOk((data) => {
          this.$refs.childComp.clrAny();
          this.fetchDataGr()
          this.currentNode = data
          this.$refs.childComp.restoreSelect(this.currentNode)
          this.onUpdateSelect({selected: data})
        });
    },

    fnDelGr(rec) {
      rec.tableName = "DimMultiPropGr";
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
          focus: "cancel",
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "group/delete",
              params: [rec],
            })
            .then(
              () => {
                this.fetchDataGr();
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

    /*---------------------------------*/

    edit(data, mode) {
      const lg = this.lang;
      this.$q
        .dialog({
          component: UpdateDimMultiProp,
          componentProps: {
            data: data,
            mode: mode,
            lg: lg,
            dense: true,
          },
        })
        .onOk((r) => {
          if (mode === "ins") {
            this.rows2.push(r);
            this.selected2 = [];
            this.selected2.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                data[key] = r[key];
              }
            }
          }
        });
    },

    fnIns(mode) {
      if (mode === "ins") {
        api
          .post(baseURL, {
            method: "dimMultiProp/newRec",
            params: [this.currentNode.id],
          })
          .then((response) => {
            this.edit(response.data.result.records[0], mode);
          });
      } else {
        this.edit(this.selected2[0], mode);
      }
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
          focus: "cancel",
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "dimMultiProp/delete",
              params: [{rec: rec}],
            })
            .then(
              () => {
                //this.$refs.childComp2.clrAny()
                this.selected2 = [];
                this.fetchData(this.currentNode.id);
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

    fetchData(propGr) {
      this.visible = ref(true)
      this.selected2 = []
      api
        .post(baseURL, {
          method: "dimMultiProp/loadDimMultiProp",
          params: [propGr],
        })
        .then((response) => {
          this.rows2 = response.data.result.records;
        })
        .catch((error) => {

          let msg = error.message;
          if (error.response) msg = this.$t(error.response.data.error.message);

          notifyError(msg);
        })
        .then(() => {
          if (this.dmp > 0) {
            const cur = this.rows2.filter((it) => {
              return it.id === this.dmp
            })
            if (cur.length > 0) {
              this.selected2.push(cur[0])
              this.onUpdateSelect2(this.selected2)
            }
          }
        })
        .finally(() => {
          this.visible = ref(false);
        })
    },

    /////////////////////////////////

    fnUp(up) {
      api
        .post(baseURL, {
          method: "dimMultiProp/changeOrdMoreCols",
          params: [{rec: this.selected3[0], up: up, dimMultiProp: this.selected2[0].id}],
        })
        .then(
          () => {
            //reload...
            this.loadDimMultiPropMoreCols(this.selected2[0].id)
          },
          (error) => {
            let msg = error.response.data.error.message
              ? error.response.data.error.message
              : error.message;
            notifyError(msg);
          }
        );
    },

    indexOf(id) {
      let rez = -1;
      this.rows3.forEach((row, index) => {
        if (row.id === id) {
          rez = index;
        }
      });
      return rez;
    },

    onoffUp() {
      //console.log("selected[0]", this.selected[0])

      if (this.selected3[0] === undefined) return true;
      else {
        return this.indexOf(this.selected3[0].id) <= 0;
      }
    },

    onoffDown() {
      if (this.selected3[0] === undefined) return true;
      else {
        return this.indexOf(this.selected3[0].id) >= this.maxLen - 1;
      }
    },

    onUpdateSelect2(item) {
      //console.info(item)
      const id = item.length===0 ? 0 : item[0].id
      this.loadDimMultiPropMoreCols(id)
    },

    loadDimMultiPropMoreCols(dimMultiProp) {
      this.loading3 = ref(true)
      this.selected3 = []
      api
        .post(baseURL, {
          method: "dimMultiProp/loadDimMultiPropMoreCols",
          params: [dimMultiProp],
        })
        .then((response) => {
          this.rows3 = response.data.result.records
          this.maxLen = this.rows3.length;
        })
        .catch((error) => {

          let msg = error.message;
          if (error.response) msg = this.$t(error.response.data.error.message);

          notifyError(msg);
        })
        .finally(() => {
          this.loading3 = ref(false);
        })

    },


    removeRow3() {
      let rec = this.selected3[0];
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' + rec.title +
            ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          this.loading3 = ref(true)
          api
            .post(baseURL, {
              method: "dimMultiProp/deleteMoreCols",
              params: [rec.id],
            })
            .then(
              () => {
                this.loadDimMultiPropMoreCols(this.selected2[0].id)
                this.selected3 = []
              },
              (error) => {
                notifyError(error.response.data.error.message)
              }
            )
            .finally(()=> {
              this.loading3 = ref(false)
            })
        })

    },

    editRow3(mode) {
      let data = {dimMultiProp: this.selected2[0].id};
      if (mode === "upd") {
        data = this.selected3[0];
      }

      this.$q
        .dialog({
          component: UpdateDimMultiPropMoreCols,
          componentProps: {
            data: data,
            mode: mode,
          },
        })
        .onOk((data) => {
          this.selected3 = []
          this.loadDimMultiPropMoreCols(this.selected2[0].id)
          this.selected3.push(data)
        })

    },


    getColumns() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 30%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 70%",
        },
      ];
    },

    getColumns2() {
      return [
        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
          format: (val) =>
            this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
        },
        {
          name: "dimMultiPropType",
          label: this.$t("dimMultiPropType"),
          field: "dimMultiPropType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
          format: (val) =>
            this.FD_DimMultiPropType ? this.FD_DimMultiPropType.get(val) : null,
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
      ];
    },

    getColumns3() {
      return [
        {
          name: "title",
          label: this.$t("titleDop"),
          field: "title",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 100%",
        }
      ];
    },

    fnExpand() {
      expandAll(this.rows);
    },
    fnCollapse() {
      collapsAll(this.rows);
    },

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },

  },

  created() {
    console.log("create")
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
          this.FD_AccessLevel.set(["id"], it["text"]);
        })
      })

    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_DimMultiPropType"}],
      })
      .then((response) => {
        this.FD_DimMultiPropType = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_DimMultiPropType.set(["id"], it["text"]);
        })
      })


    this.cols = this.getColumns()
    this.cols2 = this.getColumns2()
    this.cols3 = this.getColumns3()
  },

  mounted() {
    this.dmpGr = parseInt(this.$route["params"].dmpGr, 10);
    this.dmp = parseInt(this.$route["params"].dmp, 10);
    this.fetchDataGr();
  },

  setup() {
  },
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
