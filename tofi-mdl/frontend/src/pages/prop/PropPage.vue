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
              {{ $t("propGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                  v-if="hasTarget('mdl:mn_ds:prop:insgr')"
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
                  v-if="hasTarget('mdl:mn_ds:prop:insgr')"
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
                  v-if="hasTarget('mdl:mn_ds:prop:updgr')"
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
                  v-if="hasTarget('mdl:mn_ds:prop:delgr')"
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
              :FD_PropType="FD_PropType"
              @updateSelect="onUpdateSelect"
              ref="childComp"
              checked_visible="true"
          />
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="credit_score"></q-avatar>
            {{ $t("props") }}
          </div>
          <template v-slot:action>
            <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnIns('ins')"
                :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("create1level") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:upd')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnIns('upd')"
                :disable="currentNode2 == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:del')"
                dense
                icon="delete"
                color="secondary"
                class="q-ml-sm"
                @click="fnDel(currentNode2)"
                :disable="currentNode2 == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
            <q-space/>

            <q-btn
                v-if="hasTarget('mdl:mn_ds:prop:val')"
                :dense="dense"
                icon="pan_tool_alt"
                color="secondary"
                class="q-ml-lg"
                :disable="currentNode2 == null"
                @click="propSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
          <QTreeTable
              :cols="cols2"
              :rows="rows2"
              :icon_leaf="''"
              @updateSelect="onUpdateSelect2"
              :meterStruct="meterStruct"
              :FD_PropType="FD_PropType"
              ref="childComp2"
              checked_visible="true"
          />
        </div>
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
import UpdateProp from "pages/prop/UpdateProp.vue";
import allConsts from "pages/all-consts";


export default {
  name: "PropPage",
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
      FD_PropType: null,
      loading2: ref(false),
      //
      currentNode2: null,
      meterStruct: 0,
      propId: 0,
      propGrId: 0
    };
  },

  methods: {
    hasTarget,

    propSelect() {
      this.$router["push"]({
        name: "propSelected",
        params: {
          propGr: this.currentNode.id,
          prop: this.currentNode2.id,
        },
      });
    },

    onUpdateSelect(item) {

      this.propId = 0
      if (this.currentNode2)
        this.$refs.childComp2.selectedRow(this.currentNode2);

      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        api
            .post(baseURL, {
              method: "group/loadRec",
              params: [{id: this.currentNode.id, tableName: "PropGr"}],
            })
            .then((response) => {
              this.currentNode = response.data.result.records[0];

              this.fetchData(this.currentNode.id);
            });
      } else {
        this.currentNode2 = null;
        this.fetchData(0);
      }
    },

    onUpdateSelect2(item) {

      this.propId = 0
      this.currentNode2 = item.selected !== undefined ? item.selected : null;
      if (this.currentNode2) {

        //console.info("this.currentNode2", this.currentNode2)

        api
            .post(baseURL, {
              method: "prop/loadRec",
              params: [this.currentNode2.id],
            })
            .then((response) => {
              this.currentNode2 = response.data.result.records[0]

              if (this.currentNode2.statusFactor)
                this.checkStatus(this.currentNode2)

              if (this.currentNode2.providerTyp)
                this.checkProvider(this.currentNode2)

              let field = ""
              if (this.currentNode2.propType===1)
                field = "factorVal"
              else if (this.currentNode2.propType===5)
                field = "cls"
              else if (this.currentNode2.propType===6)
                field = "relCls"
              else if (this.currentNode2.propType===7)
                field = "measure"
              if (field !== "")
                this.checkRefValue(this.currentNode2.id, field)

              //console.info("this.currentNode2-sel", this.currentNode2)
            })
            .finally(() => {
              if (
                  this.currentNode2.propType === allConsts.FD_PropType.meter ||
                  this.currentNode2.propType === allConsts.FD_PropType.rate
              ) {
                api
                    .post(baseURL, {
                      method: "meter/loadRec",
                      params: [{id: this.currentNode2.meter}],
                    })
                    .then((response) => {
                      this.meterStruct =
                          response.data.result.records[0].meterStruct;
                    });
              }

            });
      }
    },

    checkRefValue(prop, field) {
      api
          .post(baseURL, {
            method: "prop/checkRefValue",
            params: [prop, field],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notPossibleValue"));
            }
          });
    },

    checkStatus(item) {
      let prop = item.id;
      api
          .post(baseURL, {
            method: "prop/checkStatus",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notStatus"));
            }
          });
    },

    checkProvider(item) {
      let prop = item.id;
      api
          .post(baseURL, {
            method: "prop/checkProvider",
            params: [prop],
          })
          .then((response) => {
            if (!response.data.result) {
              notifyInfo(this.$t("notProvider"));
            }
          });
    },

    /*    fnDbeg(val) {
      return val <= tofi_dbeg ? '...' : date.formatDate(val, 'DD.MM.YYYY')
    },

    fnDend(val) {
      return val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY')
    },*/

    fetchDataGr() {
      this.visible = ref(true)
      this.currentNode = null

      api
          .post(baseURL, {
            method: "group/loadGroup",
            params: [{tableName: "PropGr"}],
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
            if (this.propGrId > 0 && this.$refs.childComp) {
              let res = []
              findNode(this.rows, "id", this.propGrId, res)
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
              tableName: "PropGr",
              parentName: parentName,
              lg: lg,
              dense: true,
            },
          })
          .onOk((data) => {
            this.fetchDataGr();
            this.currentNode = data
            this.$refs.childComp.restoreSelect(data)
            this.onUpdateSelect({selected: data})
          });
    },

    fnDelGr(rec) {
      rec.tableName = "PropGr";
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
                      let msg = error.message;
                      if (error.response) msg = error.response.data.error.message;

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
      //console.info("data", data)
      if (
          mode === "ins" &&
          (data.propType === allConsts.FD_PropType.typ ||
              data.propType === allConsts.FD_PropType.reltyp ||
              data.propType === allConsts.FD_PropType.factor ||
              data.propType === allConsts.FD_PropType.attr ||
              data.propType === allConsts.FD_PropType.complex)
      ) {
        data.isUniq = true;
        data.allItem = false;
        data.visualFormat = allConsts.FD_VisualFormat.shortt;
      }
      const lg = this.lang;
      this.$q
          .dialog({
            component: UpdateProp,
            componentProps: {
              rec: data,
              mode: mode,
              lg: lg,
              dense: true,
            },
          })
          .onOk((r) => {
            console.log("Update", r)
            console.log("this.currentNode", this.currentNode)
            this.fetchData(this.currentNode.id)
            this.currentNode2 = r
            this.$refs.childComp2.restoreSelect(this.currentNode2)
          })
    },

    fnIns(mode) {
      this.propId = 0

      if (mode === "ins") {
        this.currentNode2 = null
        api
            .post(baseURL, {
              method: "prop/newRec",
              params: [this.currentNode.id],
            })
            .then((response) => {
              this.edit(response.data.result.records[0], mode);
            });
      } else {
        this.edit(this.currentNode2, mode);
      }
    },

    fnDel(rec) {
      this.propId = 0

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
                  method: "prop/delete",
                  params: [{rec: rec}],
                })
                .then(
                    () => {
                      this.$refs.childComp2.clrAny();
                      this.fetchData(this.currentNode.id);
                    },
                    (error) => {
                      let msg = error.message;
                      if (error.response) msg = error.response.data.error.message;
                      if (msg === "NotChangeStructComplexProp")
                        msg = this.$t("NotChangeStructComplexProp");

                      if (msg.includes('propperiodtype'))
                        msg = "зависит от Периода"
                      else if (msg.includes('propstatus'))
                        msg = "зависит от Статуса"
                      else if (msg.includes('proppovider'))
                        msg = "зависит от Поставщика"
                      else if (msg === "NotChangeStructComplexProp")
                        msg = this.$t("NotChangeStructComplexProp")
                      else
                        msg = ":-("

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
      this.currentNode2 = null

      api
          .post(baseURL, {
            method: "prop/loadPropTree",
            params: [propGr],
          })
          .then(
              (response) => {
                this.rows2 = pack(response.data.result.records, "ord");
                this.fnExpand2();
              },
              (error) => {

                let msg = error.message;
                if (error.response)
                  msg = this.$t(error.response.data.error.message);

                notifyError(msg);
              }
          )
          .then(() => {
            if (this.propId > 0) {
              let res = []
              findNode(this.rows2, "id", this.propId, res)
              this.currentNode2 = res[0]
              this.$refs.childComp2.restoreSelect(this.currentNode2)
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
          headerStyle: "font-size: 1.2em; width: 25%",
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
          name: "propType",
          label: this.$t("propType"),
          field: "propType",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
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

    fnExpand2() {
      expandAll(this.rows2);
    },

  },

  created() {
    //console.log("create")
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

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

    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();

  },

  mounted() {
    this.propId = parseInt(this.$route["params"].prop, 10);
    this.propGrId = parseInt(this.$route["params"].propGr, 10);

    this.fetchDataGr();

    //console.info("mounted params", this.$route.params)

  },

  setup() {
  }

};
</script>

<style scoped>

.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  /*-ms-filter: "FlipV";*/
}

</style>
