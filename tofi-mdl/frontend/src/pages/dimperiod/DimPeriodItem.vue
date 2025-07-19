<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value ="splitterModel"
      :limits="[0, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
    >
      <template v-slot:before>
        <div class="q-pa-sm-sm">
          <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">

            <div class="row no-margin no-padding" style="font-size: 1.2em; font-weight: bold;">
              {{ $t("leveles") }}
              <span style="color: black; margin-left: 10px">{{ dpName }}</span>
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
                v-if="hasTarget('mdl:mn_dp:dmper:sel:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm"
                @click="fnEdit('ins', false, null)"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("create1level") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:ins')"
                dense
                icon="post_add"
                color="secondary"
                class="q-ml-sm img-vert"
                @click="fnEdit('ins', true, currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("createChild") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:upd')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                @click="fnEdit('upd', null, currentNode)"
                :disable="currentNode == null"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("editRecord") }}
                </q-tooltip>
              </q-btn>

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:sel:del')"
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
              <q-space />

              <q-btn
                v-if="hasTarget('mdl:mn_dp:dmper:view')"
                dense
                icon="visibility"
                color="secondary"
                class="q-ml-sm"
                @click="fnView()"
              >
                <q-tooltip transition-show="rotate" transition-hide="rotate">
                  {{ $t("view") }}
                </q-tooltip>
              </q-btn>

              <q-inner-loading :showing="visible" color="secondary" />
            </template>
          </q-banner>

          <QTreeTable
            :cols="cols"
            :rows="rows"
            :icon_leaf="''"
            :FD_PeriodType="FD_PeriodType"
            :emptydate="''"
            @updateSelect="onUpdateSelect"
            checked_visible="true"
            ref="childComp"
          />
        </div>
      </template>

      <template v-slot:after>
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">

          <div style="font-size: 1.2em; font-weight: bold;">
            {{ $t("exclItems") }}
          </div>


          <template v-slot:action>
            <q-btn
              v-if="hasTarget('mdl:mn_ds:prop:s:edit')"
              dense
              icon="edit_note"
              color="secondary"
              class="q-ml-sm"
              @click="fnExcl()"
              :disable="currentNode === null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("update") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 215px); width: 100%" class="scroll">
          <q-table
            style="height: 100%; width: 100%"
            color="primary"
            card-class="bg-white"
            row-key="name"
            :columns="cols2"
            :rows="rows2"
            :dense="dense"
            :wrap-cells="true"
            table-header-class="text-bold text-white bg-blue-grey-13"
            separator="cell"
            :loading="loading2.value"
            :rows-per-page-options="[0]"
          >
          </q-table>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script>
import QTreeTable from "components/QTreeTable.vue";
import {api, baseURL, tofi_dbeg, tofi_dend} from "boot/axios";
import {getParentNode, hasTarget, notifyError, notifyInfo, pack,} from "src/utils/jsutils";
import {date, extend} from "quasar";
import UpdaterDimPeriodItem from "pages/dimperiod/UpdaterDimPeriodItem.vue";
import UpdaterNotIn from "pages/dimperiod/UpdaterNotIn.vue";
import DimPeriodView from "pages/dimperiod/DimPeriodView.vue";

export default {
  name: "DimPeriodItem",
  components: { QTreeTable },

  data() {
    return {
      splitterModel: 70,
      FD_PeriodType: null,
      cols: [],
      rows: [],
      currentNode: null,
      visible: false,
      dense: true,

      cols2: [],
      rows2: [],
      loading2: false,
      //
      dimPeriod: null,
      dpName: "",
    };
  },

  methods: {
    hasTarget,
    toBack() {
      this.$router["push"]({
        name: "DimPeriod",
        params: {
          dimperiod: this.dimPeriod,
        },
      })
    },

    fnView() {
      this.$q
        .dialog({
          component: DimPeriodView,
          componentProps: {
            dimperiod: this.dimPeriod,
            dense: true,
            lg: this.lg,
            cmdDate: false,
            // ...
          },
        })
        .onOk(() => {
          //console.log("Ok! updated", r);
          //this.fetchData(this.dimPeriod)
        });
    },

    onUpdateSelect(item) {
      this.currentNode = item.selected !== undefined ? item.selected : null;
      //console.info(this.currentNode);

      if (this.currentNode) {
        api
          .post(baseURL, {
            method: "dimperiod/loadNotIn",
            params: [this.currentNode.id],
          })
          .then((response) => {
            this.rows2 = response.data.result.records;
          })
          .catch((error) => {
            notifyError(error.message);
          });
      } else {
        this.rows2 = [];
      }
    },

    fetchData(dimperiod) {
      this.visible = true;

      api
        .post(baseURL, {
          method: "dimperiod/loadDPI",
          params: [dimperiod],
        })
        .then(
          (response) => {
            console.info("rows", response.data.result.records)
            this.rows = pack(response.data.result.records, "ord");
          },
          (error) => {

            let msg = error.message;
            if (error.response)
              msg = this.$t(error.response.data.error.message);
            notifyError(msg);
          }
        )
        .finally(() => {
          //setTimeout(() => {
          this.$refs.childComp.fnExpand();
          this.visible = false;
          //}, 500)
        });
    },

    sendQuery(data, mode, isChild, parentName, parentPeriodType, dense) {
      this.$q
        .dialog({
          component: UpdaterDimPeriodItem,
          componentProps: {
            data: data,
            mode: mode,
            isChild: isChild,
            parentName: parentName,
            parentPeriodType: parentPeriodType,
            dense: dense,
            // ...
          },
        })
        .onOk(() => {
          //console.log("Ok! updated", r);
          this.fetchData(this.dimPeriod);
        });
    },

    fnEdit(mode, isChild, rec) {
      let data = {};
      let parentName = null;
      let parentPeriodType = null;

      if (mode === "ins") {
        if (rec === null) rec = { dimperiod: this.dimPeriod };
        api
          .post(baseURL, {
            method: "dimperiod/newRecDPI",
            params: [isChild, rec],
          })
          .then((response) => {
            //console.log("new rec", response.data.result.records[0])
            data = response.data.result.records[0];
            data.dimperiod = this.dimPeriod;
            if (isChild) {
              parentName = this.FD_PeriodType.get(this.currentNode["priodType"]);
              parentPeriodType = this.currentNode.periodType;
            }

            //
            this.sendQuery(
              data,
              mode,
              isChild,
              parentName,
              parentPeriodType,
              this.dense
            );
          });
      } else if (mode === "upd") {
        extend(data, this.currentNode);
        if (data["dbeg"] <= tofi_dbeg) data["dbeg"] = null;
        if (data["dend"] >= tofi_dend) data["dend"] = null;
        if (this.currentNode.parent > 0) {
          let parentNode = [];
          getParentNode(this.rows, this.currentNode.parent, parentNode);
          parentName = parentNode[0].name;
          isChild = true;
        }
        this.sendQuery(
          data,
          mode,
          isChild,
          parentName,
          parentPeriodType,
          this.dense
        );
      }
    },

    fnDel(rec) {
      this.$q
        .dialog({
          title: this.$t("confirmation"),
          message:
            this.$t("deleteRecord") +
            '<div style="color: plum">(' +
            this.FD_PeriodType.get(rec.periodType) +
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
              method: "dimperiod/deleteDPI",
              params: [rec],
            })
            .then(
              () => {
                this.fetchData(this.dimPeriod);
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
    //////////////////////////////////////////////////////////
    fnExcl() {
      this.$q
        .dialog({
          component: UpdaterNotIn,
          componentProps: {
            dimPeriodItem: this.currentNode.id,
            dense: this.dense,
            // ...
          },
        })
        .onOk((r) => {
          //console.log("Ok! updated", r);
          if (r.res) {
            api
              .post(baseURL, {
                method: "dimperiod/loadNotIn",
                params: [this.currentNode.id],
              })
              .then((response) => {
                this.rows2 = response.data.result.records;
              })
              .catch((error) => {
                notifyError(error.message);
              });
          }
        });
    },

    getColumns() {
      return [
        {
          name: "periodType",
          label: this.$t("fldName"),
          field: "periodType",
          align: "left",
          headerStyle: "font-size: 1.2em; width: 20%",
        },

        {
          name: "dbeg",
          label: this.$t("fldDbeg"),
          field: "dbeg",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 10%",
          format: (val) =>
            val <= tofi_dbeg ? "" : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "countPeriod",
          label: this.$t("countPeriod"),
          field: "countPeriod",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "dend",
          label: this.$t("fldDend"),
          field: "dend",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em;",
          style: "width: 10%",
          format: (val) =>
            val >= tofi_dend ? "" : date.formatDate(val, "DD.MM.YYYY"),
        },
        {
          name: "lagCurrentDate",
          label: this.$t("lagCurrentDate"),
          field: "lagCurrentDate",
          headerStyle: "font-size: 1.2em; width: 20%",
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
          headerStyle: "font-size: 1.2em; width: 100%",
        },
      ];
    },
  },

  created() {
    console.log("create");
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;

    api
      .post(baseURL, {
        method: "dict/load",
        params: [{ dict: "FD_PeriodType" }],
      })
      .then((response) => {
        this.FD_PeriodType = new Map();
        response.data.result.records.forEach((it) => {
          this.FD_PeriodType.set(it["id"], it["text"]);
        });
      });

    this.cols = this.getColumns();
    this.cols2 = this.getColumns2();
  },

  mounted() {
    //console.info("mounted", this.$route.params);
    this.dimPeriod = this.$route["params"].dimperiod;
    this.dpName = "(" + this.$route["params"].name + ")";
    this.fetchData(this.dimPeriod);
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
