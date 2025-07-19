<template>
  <div class="q-pa-md">
    <q-splitter
        v-model="splitterModel"
        :model-value="splitterModel"
        :limits="[0, 100]"
        before-class="overflow-hidden q-mr-sm"
        after-class="overflow-hidden q-ml-sm"
        separator-class="bg-red"
        class="bg-amber-1"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="folder"></q-avatar>
              {{ $t("multiPropGr") }}
            </div>
            <template v-slot:action>
              <q-btn
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
                  v-if="hasTarget('mdl:mn_ds:pm:insgr')"
                  dense
                  icon="post_add"
                  color="secondary"
                  @click="fnInsGr('ins', true)"
                  class="q-ml-sm img-vert"
                  :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                  v-if="hasTarget('mdl:mn_ds:pm:updgr')"
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
                  v-if="hasTarget('mdl:mn_ds:pm:delgr')"
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
            <div style="height: calc(100vh - 220px); width: 100%">
              <QTreeTable
                  :cols="cols"
                  :rows="rows"
                  :icon_leaf="''"
                  @updateSelect="onUpdateSelect"
                  ref="childComp"
                  checked_visible="true"
              />
            </div>
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="view_in_ar"></q-avatar>
            {{ $t("multiProps") }}
          </div>

          <template v-slot:action>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:pm:ins')"
                dense icon="post_add" color="secondary" class="q-ml-sm" @click="fnIns('ins')"
                :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:pm:upd')"
                dense icon="edit" color="secondary" class="q-ml-sm" @click="fnIns('upd')"
                :disable="loading2 || selected2.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:pm:del')"
                dense icon="delete" color="secondary" class="q-ml-sm" @click="fnDel(selected2[0])"
                :disable="loading2 || selected2.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

            <q-space></q-space>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:pm:sel')"
                :dense="dense"
                icon="pan_tool_alt"
                color="secondary"
                class="q-ml-lg"
                :disable="loading2 || selected2.length === 0"
                @click="multiPropSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <q-table
            style="height: 100%; width: 100%"
            :wrap-cells="true"
            color="primary"
            card-class="bg-amber-1 text-brown"
            row-key="id"
            :dense="dense"
            :columns="cols2"
            :rows="rows2"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2.value"
            selection="single"
            @update:selected="onUpdateSelect2"
            v-model:selected="selected2"
            :rows-per-page-options="[0]"
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

          <template #loading>
            <q-inner-loading showing color="secondary"></q-inner-loading>
          </template>
        </q-table>
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
import UpdaterMultiProp from "pages/multiprop/UpdaterMultiProp.vue";


export default {
  name: "MultiProp",
  components: {QTreeTable},

  data() {
    return {
      splitterModel: ref(30),
      cols: [],
      rows: [],
      currentNode: null,
      visible: ref(false),
      dense: true,
      //
      cols2: [],
      rows2: [],
      FD_AccessLevel: null,
      loading2: ref(false),
      selected2: ref([]),
      //
      mpGr: 0,
      mp: 0,

    };
  },
  methods: {
    hasTarget,

    multiPropSelect() {
      this.$router["push"]({
        name: "MultiPropSelected",
        params: {
          mpGr: this.currentNode.id,
          mp: this.selected2[0].id,
        },
      });
    },

    onUpdateSelect2(par) {
      this.mp = 0
      this.mpGr = 0
      if (par.length > 0) {
        if (par[0].statusFactor) this.checkStatus(par[0].id);
        if (par[0].providerTyp) this.checkProvider(par[0].id);
        if (!par[0].isUniq) this.checkCondition(par[0].id);
      }
    },

    checkCondition(prop) {
      api
          .post(baseURL, {
            method: "multiProp/checkCondition",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notMultiPropCond"));
            }
          });
    },

    checkStatus(prop) {
      api
          .post(baseURL, {
            method: "multiProp/checkStatus",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notStatus"));
            }
          });
    },

    checkProvider(prop) {
      api
          .post(baseURL, {
            method: "multiProp/checkProvider",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notProvider"));
            }
          });
    },

    fnSelection() {
    },


    fnAL(val) {
      return this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null;
    },

    onUpdateSelect(item) {
      this.mp = 0
      this.mpGr = 0
      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        api
            .post(baseURL, {
              method: "group/loadRec",
              params: [{id: this.currentNode.id, tableName: "MultiPropGr"}],
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
      this.visible = ref(true);
      this.currentNode = null

      api
          .post(baseURL, {
            method: "group/loadGroup",
            params: [{tableName: "MultiPropGr"}],
          })
          .then(
              (response) => {
                this.rows = pack(response.data.result.records, "id");
                this.fnExpand();
              },
              (error) => {

                this.$router["push"]("/");
                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .then(() => {
            if (this.mpGr > 0) {
              let res = []
              findNode(this.rows, "id", this.mpGr, res)
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
              tableName: "MultiPropGr",
              parentName: parentName,
              lg: lg,
              dense: true,
            },
          })
          .onOk((data) => {
            this.$refs.childComp.clrAny();
            this.fetchDataGr();
            this.currentNode = data
            this.$refs.childComp.restoreSelect(this.currentNode)
            this.onUpdateSelect({selected: data})
          });
    },

    fnDelGr(rec) {
      rec.tableName = "MultiPropGr";
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
            component: UpdaterMultiProp,
            componentProps: {
              rec: data,
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
              this.onUpdateSelect2(this.selected2);
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
              method: "multiProp/newRecMultiProp",
              params: [this.currentNode.id],
            })
            .then((response) => {
              this.edit(response.data.result.records[0], mode);
            });
      } else {
        //console.log("this.selected2[0]", this.selected2[0])
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
            api
                .post(baseURL, {
                  method: "multiProp/deleteMultiProp",
                  params: [{id: rec.id}],
                })
                .then(
                    () => {
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
            method: "multiProp/loadMultiProp",
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
            if (this.mp > 0) {
              let index = this.rows2.findIndex((row) => row.id === this.mp);
              if (index > -1)
                this.selected2.push( this.rows2[index] );
            }
          })
          .finally(() => {
            this.visible = ref(false);
          });
    },

    /////////////////////////////////
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
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 30%",
        },

        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
          format: (val) =>
              this.FD_AccessLevel ? this.FD_AccessLevel.get(val) : null,
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

    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();

    this.fetchDataGr();
  },

  mounted() {
    this.mpGr = parseInt(this.$route["params"].mpGr, 10);
    this.mp = parseInt(this.$route["params"].mp, 10);
    //console.info("mounted params", this.$route.params)
  },

  computed: {},

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
