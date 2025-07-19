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
              {{ $t("cubesGr") }}
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

              <q-inner-loading :showing="loading" color="secondary"/>
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

        <q-table
          style="height: calc(100vh - 150px); width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          dense
          :columns="cols2"
          :rows="rows2"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          :rows-per-page-options="[0]"
          selection="single"
          @update:selected="updSelection2"
          v-model:selected="selected2"
        >

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="view_in_ar"></q-avatar>
              {{ $t("cubes") }}
            </div>
            <q-space/>

            <q-space/>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:attr:ins')"
              dense
              icon="post_add"
              color="secondary"
              :disable="loading"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:attr:upd')"
              dense icon="edit"
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected2.length === 0"
              @click="editRow(selected2[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:attr:del')"
              dense icon="delete"
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected2.length === 0"
              @click="removeRow(selected2[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:cgt:sel')"
              dense icon="pan_tool_alt"
              color="secondary"
              class="q-ml-lg"
              :disable="loading || selected2.length === 0"
              @click="cubesSelect"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("chooseRecord") }}
              </q-tooltip>
            </q-btn>


          </template>

          <template #bottom-row>
            <q-td colspan="100%" v-if="selected2.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected2[0]) }} </span>
            </q-td>
            <q-td
              v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
              {{ $t("infoRow") }}
            </q-td>
          </template>

        </q-table>

      </template>


    </q-splitter>

  </div>

</template>


<script>
import QTreeTable from "components/QTreeTable.vue";
import {ref} from "vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios.js";
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  pack
} from "src/utils/jsutils.js";
import UpdateGroup from "pages/group/UpdateGroup.vue";
import {date} from "quasar";
import UpdateCubes from "pages/cubes/UpdateCubes.vue";

export default {
  name: "CubesPage",
  components: {QTreeTable},


  data() {
    return {
      splitterModel: 30,
      cols: [],
      rows: [],
      currentNode: null,
      loading: false,
      //
      cols2: [],
      rows2: [],
      selected2: [],
      loading2: false,
      FD_AccessLevel: new Map(),
      FD_CubeSType: new Map(),
      //
      cubesId: 0,
      cubesGrId: 0


    }
  },

  methods: {
    hasTarget,

    editRow(row, mode) {
      let data = {
        id: 0,
        cod: "",
        cubeSGr: this.currentNode.id,
        name: "",
        fullName: "",
        accessLevel: 1,
        cubeSType: 1,
        dOrg: date.formatDate(new Date(), "YYYY-MM-DD"),
        cmt: null,
      };
      if (mode === "upd") {
        data = {
          id: row.id,
          cod: row.cod,
          cubeSGr: row.cubeSGr,
          name: row.name,
          fullName: row.fullName,
          accessLevel: row.accessLevel,
          cubeSType: row.cubeSType,
          dOrg: row.dOrg,
          cmt: row.cmt,
        };
      }

      //console.log("data",data)

      this.$q
        .dialog({
          component: UpdateCubes,
          componentProps: {
            data: data,
            mode: mode,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (mode === "ins") {
            this.rows2.push(r);
            this.selected2 = [];
            this.selected2.push(r);
            //this.updSelection(this.selected2);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                row[key] = r[key];
              }
            }
          }
        });
    },

    removeRow(rec) {
      this.cubesId = 0

      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            rec.cod + ": " + rec.name + ")</div>",
          html: true,
          cancel: true,
          persistent: true,
          focus: "cancel",
        })
        .onOk(() => {
          //let index = this.rows.findIndex((row) => row.id === rec.id);
          api
            .post(baseURL, {
              method: "cubes/deleteCube",
              params: [rec],
            })
            .then(
              () => {
                this.selected2 = [];
                this.fetchData(this.currentNode.id);
              },
              (error) => {
                let msg = error.message;
                if (error.response)
                  msg = error.response.data.error.message;
                notifyError(msg);
              }
            );
        })


    },

    cubesSelect() {
      this.$router["push"]({
        name: "CubeSelected",
        params: {
          cubesGr: this.currentNode.id,
          cubes: this.selected2[0].id,
        },
      });
    },

    updSelection2(par) {
      console.log("updSelection2", par);
      //if (par[0] === undefined) return;

    },

    onUpdateSelect(item) {
      console.log(item);

      this.cubesGrId = item.id;
      this.cubesId = 0

      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        api
          .post(baseURL, {
            method: "group/loadRec",
            params: [{id: this.currentNode.id, tableName: "CubeSGr"}],
          })
          .then((response) => {
            this.currentNode = response.data.result.records[0];

            this.fetchData(this.currentNode.id);
          });
      } else {
        this.fetchData(0);
      }
    },

    fetchDataGr() {
      this.visible = ref(true)
      this.currentNode = null

      api
        .post(baseURL, {
          method: "group/loadGroup",
          params: [{tableName: "CubeSGr"}],
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
          if (this.cubesGrId > 0 && this.$refs.childComp) {
            let res = []
            findNode(this.rows, "id", this.cubesGrId, res)
            this.currentNode = res[0]
            this.$refs.childComp.restoreSelect(this.currentNode)
            this.fetchData(this.currentNode.id)
          }
        })
        .finally(() => {
          this.visible = ref(false);
        });
    },

    fetchData(cubesGr) {
      this.loading = ref(true)
      //this.currentNode2 = null

      api
        .post(baseURL, {
          method: "cubes/loadCubes",
          params: [cubesGr, 0],
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
          if (this.cubesId > 0) {
            let res = []
            findNode(this.rows2, "id", this.cubesId, res)
            this.selected2.push(res[0])
            //this.$refs.childComp2.restoreSelect(this.currentNode2)
          }
        })
        .finally(() => {
          this.loading = ref(false);
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
            tableName: "CubeSGr",
            parentName: parentName,
            lg: lg,
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
      rec.tableName = "CubeSGr";
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
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "accessLevel",
          label: this.$t("accessLevel"),
          field: "accessLevel",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
          format: (val) => this.FD_AccessLevel.get(val),
        },
        {
          name: "cubeSType",
          label: this.$t("cubesType"),
          field: "cubeSType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
          format: (val) => this.FD_CubeSType.get(val),
        },

        {
          name: "dOrg",
          label: this.$t("dOrg"),
          field: "dOrg",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 5%",
          format: (val) =>
            val <= tofi_dbeg || val >= tofi_dend ? '...' : date.formatDate(val, 'DD.MM.YYYY'),
        },
        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 15%",
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

    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },

  },

  mounted() {
    this.cubesId = parseInt(this.$route["params"].cubes, 10);
    this.cubesGrId = parseInt(this.$route["params"].cubesGr, 10);

    this.fetchDataGr();

    //console.info("mounted params", this.$route["params"])
  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    this.loading = true;
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_AccessLevel"}],
      })
      .then((response) => {
        response.data.result.records.forEach((it) => {
          this.FD_AccessLevel.set(it["id"], it["text"]);
        })
      })
      .catch(error=> {
        console.log(error.message);
        if (error.response && error.response.data && error.response.data.error) {
          notifyError(this.$t(error.response.data.error.message));
        } else {
          notifyError(error.message);
        }
      })
      .finally(()=> {
        this.loading = false;
      })

    this.loading = true;
    api
      .post(baseURL, {
        method: "dict/load",
        params: [{dict: "FD_CubeSType"}],
      })
      .then((response) => {
        response.data.result.records.forEach((it) => {
          this.FD_CubeSType.set(it["id"], it["text"]);
        })
      })
      .finally(()=> {
        this.loading = false;
      })

    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();

  },

  setup() {
  },


}
</script>

<style scoped>

</style>
