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
              {{ $t("dimObjGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:insgr')"
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
                v-if="hasTarget('mdl:mn_dp:dmprop:insgr')"
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
                v-if="hasTarget('mdl:mn_dp:dmprop:updgr')"
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
                v-if="hasTarget('mdl:mn_dp:dmprop:delgr')"
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

              <q-inner-loading :showing="visible" color="secondary" />
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
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div style="font-size: 1.2em; font-weight: bold;">
            <q-avatar color="black" text-color="white" icon="pattern"></q-avatar>
            {{ $t("dimsObj") }}
          </div>
          <template v-slot:action>
            <q-btn
              v-if="hasTarget('mdl:mn_dp:dmprop:ins')"
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
              v-if="hasTarget('mdl:mn_dp:dmprop:upd')"
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
              v-if="hasTarget('mdl:mn_dp:dmprop:del')"
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
              v-if="hasTarget('mdl:mn_dp:dmprop:sel')"
              dense
              icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading2 || selected2.length === 0"
              @click="dimObjSelect()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
          <q-table
            style="height: 100%; width: 100%"
            color="primary"
            card-class="bg-amber-1"
            row-key="id"
            :columns="cols2"
            :rows="rows2"
            dense
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2.value"
            v-model:selected="selected2"
            selection="single"
            :rows-per-page-options="[0]"
          >

            <template #bottom-row>
              <q-td colspan="100%" v-if="selected2.length > 0">
                <span class="text-blue"> {{ $t("selectedRow") }}: </span>
                <span class="text-bold"> {{ this.infoSelected(selected2[0]) }} </span>
              </q-td>
              <q-td
                  v-else-if="this.rows2.length > 0" colspan="100%" class="text-bold">
                {{ $t("infoRow") }}
              </q-td>
            </template>

          </q-table>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import QTreeTable from "components/QTreeTable.vue";
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
import UpdateGroup from "pages/group/UpdateGroup.vue";
import UpdaterDimObj from "pages/dimobj/UpdaterDimObj.vue";

export default {
  name: "DimObjPage",

  components: { QTreeTable },

  data() {
    return {
      splitterModel: 30,
      cols: [],
      rows: [],
      currentNode: null,
      visible: ref(false),
      //
      cols2: [],
      rows2: [],
      loading2: ref(false),
      //
      selected2: [],
      dimObjGr: 0,
      dimObj: 0
    };
  },

  methods: {
    hasTarget,
    dimObjSelect() {
      this.$router["push"]({
        name: "DimObjItem",
        params: {
          dimObjGr: this.currentNode.id,
          dimObj: this.selected2[0].id,
          name: this.selected2[0].name,
        },
      });

    },

    onUpdateSelect(item) {
      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        api
          .post(baseURL, {
            method: "group/loadRec",
            params: [{ id: this.currentNode.id, tableName: "DimObjGr" }],
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
          params: [{ tableName: "DimObjGr" }],
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
        .then(()=> {
          if (this.dimObjGr > 0) {
            let res = []
            findNode(this.rows, "id", this.dimObjGr, res)
            this.currentNode = res[0]
            this.$refs.childComp.restoreSelect(this.currentNode)
            this.fetchData(this.currentNode.id)
          }
        })
        .finally(() => {
          //setTimeout(()=> {
            this.visible = ref(false);
          //}, 5000)

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
            tableName: "DimObjGr",
            parentName: parentName,
            lg: lg,
          },
        })
        .onOk((r) => {
          this.fetchDataGr();
          this.currentNode = r
          this.$refs.childComp.restoreSelect(this.currentNode)
          this.onUpdateSelect({selected: r})
        });
    },

    fnDelGr(rec) {
      rec.tableName = "DimObjGr";
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

                this.$router["push"]("/");
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

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    ////////////////////////

    fetchData(dimObjGr) {
      this.visible = ref(true);
      this.selected2 = []
      api
        .post(baseURL, {
          method: "dimobj/loadDimObj",
          params: [dimObjGr],
        })
        .then(
          (response) => {
            this.rows2 = response.data.result.records;
          },
          (error) => {

            this.$router["push"]("/");

            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          }
        )
        .then(()=> {
          if (this.dimObj > 0) {
            const cur = this.rows2.filter((it) => {
              return it.id === this.dimObj
            })
            if (cur.length > 0) {
              this.selected2.push(cur[0])
            }
          }
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    edit(data, mode) {
      const lg = this.lang;
      this.$q
        .dialog({
          component: UpdaterDimObj,
          componentProps: {
            data: data,
            mode: mode,
            lg: lg,
          },
        })
        .onOk((r) => {
          this.dimObj = 0
          this.fetchData(this.currentNode.id);
          this.selected2 = []
          this.selected2.push(r)
        });
    },

    fnIns(mode) {
      if (mode === "ins") {
        api
          .post(baseURL, {
            method: "dimobj/newRec",
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
          api
            .post(baseURL, {
              method: "dimobj/delete",
              params: [rec],
            })
            .then(
              () => {
                this.fetchData(this.currentNode.id);
              },
              (error) => {
                this.$router["push"]("/");
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
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
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

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();
  },

  mounted() {
    //console.info("Mounted")
    this.dimObjGr = parseInt(this.$route["params"].dimObjGr, 10);
    this.dimObj = parseInt(this.$route["params"].dimObj, 10);
    this.fetchDataGr()
  },

  setup() {},
};
</script>

<style scoped></style>
