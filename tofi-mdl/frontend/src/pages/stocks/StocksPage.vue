<template>
  <div class="q-pa-md">

    <q-splitter
      v-model="splitterModel"
      :limits="[0, 100]"
      after-class="overflow-hidden q-ml-sm"
      before-class="overflow-hidden q-mr-sm"
      class="bg-amber-1"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >

      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner class="bg-orange-1 q-mb-sm" dense inline-actions>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" icon="folder" text-color="white"></q-avatar>
              {{ $t("stocksGr") }}
            </div>
            <template v-slot:action>
              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:insgr')"
                class="q-ml-sm"
                color="secondary"
                dense
                icon="post_add"
                @click="fnInsGr('ins', false)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:insgr')"
                :disable="currentNode == null"
                class="q-ml-sm img-vert"
                color="secondary"
                dense
                icon="post_add"
                @click="fnInsGr('ins', true)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:updgr')"
                :disable="currentNode == null"
                class="q-ml-sm"
                color="secondary"
                dense
                icon="edit"
                @click="fnInsGr('upd', null)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dop:stocks:delgr')"
                :disable="currentNode == null"
                class="q-ml-sm"
                color="red"
                dense
                icon="delete"
                @click="fnDelGr(currentNode)"
              >
                <q-tooltip transition-hide="rotate" transition-show="rotate">
                  {{ $t("deletingRecord") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="loading" color="secondary"/>
            </template>
          </q-banner>

          <QTreeTable
            ref="childComp"
            :cols="cols"
            :icon_leaf="''"
            :rows="rows"
            checked_visible="true"
            @updateSelect="onUpdateSelect"
          />
        </div>
      </template>

      <template v-slot:after>

        <q-table
          style="height: 100%; width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="cod"
          :columns="cols2"
          :rows="rows2"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          dense
          selection="single"
          v-model:selected="selected"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ this.infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td colspan="100%" v-else-if="this.rows.length > 0" class="text-bold">
              {{ $t("infoApp") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              <q-avatar color="black" text-color="white" icon="storage"></q-avatar>
              {{ $t("stocks") }}
            </div>

            <q-space/>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:ins')"
              icon="post_add"
              color="secondary"
              dense
              :disable="loading || currentNode == null"
              @click="editRow(null, 'ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="editRow(selected[0], 'upd')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              :disable="loading || selected.length === 0"
              @click="removeRow(selected[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>

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
import QTreeTable from "components/QTreeTable.vue";
import {
  collapsAll,
  expandAll,
  findNode,
  getParentNode,
  hasTarget,
  notifyError,
  notifyInfo,
  notifySuccess,
  pack
} from "src/utils/jsutils.js";
import {api} from "boot/axios.js";
import UpdateGroup from "pages/group/UpdateGroup.vue";
import UpdateStock from "pages/stocks/UpdateStock.vue";

export default {
  name: "StocksPage",
  components: {QTreeTable},


  data() {
    return {
      splitterModel: 30,
      cols: [],
      rows: [],
      currentNode: null,
      loading: false,
      stockId: 0,
      stockGrId: 0,
      cols2: [],
      rows2: [],
      selected: [],
    }
  },

  methods: {
    hasTarget,

    onUpdateSelect(item) {


      //this.propId = 0
      //if (this.currentNode2)
        //this.$refs.childComp2.selectedRow(this.currentNode2);

      this.currentNode = item.selected !== undefined ? item.selected : null;
      if (this.currentNode) {
        console.log("ITEM GR", this.currentNode.id);
        this.stockGrId = this.currentNode.id;
        api
          .post('', {
            method: "group/loadRec",
            params: [{id: this.currentNode.id, tableName: "SourceStockGr"}],
          })
          .then((response) => {
            this.currentNode = response.data.result.records[0];

            this.fetchData(this.currentNode.id);
          });
      } else {
        this.stockGrId = 0;
        this.fetchData(0);
      }
    },

    fetchDataGr() {
      this.loading = true
      this.currentNode = null

      api
        .post('', {
          method: "group/loadGroup",
          params: [{tableName: "SourceStockGr"}],
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
          if (this.stockGrId > 0 && this.$refs.childComp) {
            let res = []
            findNode(this.rows, "id", this.stockGrId, res)
            this.currentNode = res[0]
            this.$refs.childComp.restoreSelect(this.currentNode)
            this.fetchData(this.currentNode.id)
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },

    fnInsGr(mode, isChild) {
      let data = {
        accessLevel: 1,
      };
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
            tableName: "SourceStockGr",
            parentName: parentName,
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
      rec.tableName = "SourceStockGr";
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
            .post('', {
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
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "fullName",
          label: this.$t("fldFullName"),
          field: "fullName",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 35%",
        },

        {
          name: "cmt",
          label: this.$t("fldCmt"),
          field: "cmt",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
      ];
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    fetchData(stockGr) {
      this.loading = true
      //this.currentNode2 = null
      api
        .post('', {
          method: "stock/loadStocks",
          params: [stockGr],
        })
        .then(
          (response) => {
            this.rows2 = response.data.result.records;
          },
          (error) => {
            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);

            notifyError(msg);
          }
        )
        .finally(() => {
          this.loading = false;
        });
    },

    editRow(rec, mode) {
      let data = {parent: this.stockGrId};
      if (mode === "ins") {
        this.loading = true;
        api
          .post('', {
            method: "stock/newRec",
            params: [this.stockGrId],
          })
          .then(
            (response) => {
              data = response.data.result.records[0];
            },
            (error) => {
              //const store = useUserStore();
              //let {setUserName} = store;
              //setUserName("");

              let msg = error.message;
              if (error.response)
                msg = this.$t(error.response.data.error.message);
              notifyError(msg);
            }
          )
          .finally(() => {
            this.loading = false;
          });
      } else {
        data = {
          id: rec.id,
          cod: rec.cod,
          parent: rec.parent,
          accessLevel: rec.accessLevel,
          sourceStockType: rec.sourceStockType,
          name: rec.name,
          fullName: rec.fullName,
          cmt: rec.cmt,
        };
      }

      this.$q
        .dialog({
          component: UpdateStock,
          componentProps: {
            data: data,
            mode: mode,
            // ...
          },
        })
        .onOk((r) => {
          if (mode === "ins") {
            this.rows2.push(r);
            this.selected = [];
            this.selected.push(r);
          } else {
            for (let key in r) {
              if (r.hasOwnProperty(key)) {
                rec[key] = r[key];
              }
            }
          }
        });
    },

    removeRow(rec) {
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
          let index = this.rows2.findIndex((row) => row.id === rec.id);
          api
            .post('', {
              method: "stock/delete",
              params: [rec],
            })
            .then(
              () => {
                this.rows2.splice(index, 1);
                this.selected = [];
                notifySuccess(this.$t("success"));
              },
              (error) => {
                notifyInfo(error.message);
              }
            );
        });
    },


    infoSelected(row) {
      return " " + row.cod + " - " + row.name;
    },
  },


  mounted() {
    this.stockId = parseInt(this.$route["params"]["stock"], 10);
    this.stockGrId = parseInt(this.$route["params"]["stockGr"], 10);

    this.fetchDataGr();

    //console.info("mounted params", this.$route.params)

  },

  created() {
    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();
  }


}


</script>

<style scoped>

</style>
