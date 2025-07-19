<template>
  <div class="q-pa-md">
    <q-splitter
        v-model="splitterModel"
        :model-value ="splitterModel"
        :limits="[60, 100]"
        before-class="overflow-hidden q-mr-sm"
        after-class="overflow-hidden q-ml-sm"
        separator-class="bg-red"

    >
      <template v-slot:before>
        <q-banner dense inline-actions class="bg-amber-1">
          <div class="row">
            <div style="font-size: 1.2em; font-weight: bold;">{{ $t("compObjRel") }}:</div>
            <span style="color: black; margin-left: 10px; margin-top: 2px">
              {{ dimObjName }}
            </span>
          </div>

          <template v-slot:action>
            <q-btn
                dense round color="secondary" icon="arrow_back" glossy @click="toBack()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("back") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                dense class="q-ml-sm" icon="expand_more" color="secondary" @click="fnExpand()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("expandAll") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                dense icon="expand_less" color="secondary" class="q-ml-sm" @click="fnCollapse()"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("collapseAll") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:sel:ins')"
                dense icon="post_add" color="secondary" class="q-ml-sm" :disable="loading" @click="addNode(false)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("create1level") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:sel:ins')"
                dense icon="post_add" color="secondary" class="q-ml-sm img-vert"
                :disable="loading || selected.length === 0 || hasNotChild()"
                @click="addNode(true)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("createChild") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:sel:upd')"
                dense
                icon="edit"
                color="secondary"
                class="q-ml-sm"
                :disable="loading || selected.length === 0"
                @click="editNode(selected[0])"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editNode") }}
              </q-tooltip>
            </q-btn>

            <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:sel:del')"
                dense
                icon="delete"
                color="secondary"
                class="q-ml-sm"
                @click="deleteNode()"
                :disable="loading || selected.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deleteNode") }}
              </q-tooltip>
            </q-btn>

            <q-space/>

            <q-btn
                v-if="hasTarget('mdl:mn_dp:dmprop:sel:del')"
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

            <q-inner-loading :showing="loading" color="secondary"/>
          </template>
        </q-banner>
        <!-- style="height: calc(100vh - 210px); width: 100%"-->
        <div
          style="height: calc(100vh - 210px); width: 100%"
            class="scroll bg-amber-1"
        >
          <div
              class="q-table-container q-table--dense scroll"
              style="height: 100%"
          >
            <table
                class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle"
            >
              <thead class="text-bold text-white bg-blue-grey-13">
              <tr class style="text-align: left">
                <th :style="cols[0].headerStyle">{{ cols[0].label }}</th>
                <th :style="cols[1].headerStyle">{{ cols[1].label }}</th>
                <th :style="cols[2].headerStyle">{{ cols[2].label }}</th>
                <th :style="cols[3].headerStyle" v-if="visProp">{{ cols[3].label }}</th>
                <th :style="cols[4].headerStyle">{{ cols[4].label }}</th>
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
                          :color="item.propType === undefined ? 'gray' : 'blue'"
                          :icon="
                          selected.length > 0 && item.id === selected[0].id
                            ? 'check_box'
                            : 'check_box_outline_blank'
                        "
                          @click.stop="selectedCheck(item)"
                      >
                      </q-btn>

<!--                      <q-icon :name="getIcon(item)"></q-icon>-->
                      {{ item.name }}
                    </span>
                </td>
                <td :data-th="cols[1].name">{{ fnLT(item.linkType) }}</td>
                <td :data-th="cols[2].name">
                  {{ fnDOIT(item.dimObjItemType) }}
                </td>
                <td :data-th="cols[3].name" v-if="visProp">
                  {{ item["nameProp"] }}
                </td>

                <td :data-th="cols[4].name">
                  {{ item.lev }}
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>

      <template v-slot:after>
        <q-splitter
            v-model="splitterModel2"
            :model-value="splitterModel2"
            class="no-padding no-margin"
            horizontal
            separator-class="bg-red"
            style="height: calc(100vh - 210px); width: 100%"
        >
          <!-- style="height: calc(100vh - 162px); width: 100%" -->
          <template v-slot:before>
            <q-table
                style="height: 100%; width: 100%"
                color="primary"
                card-class="bg-amber-1"
                row-key="id"
                :columns="cols2"
                :rows="rows2"
                :wrap-cells="true"
                table-header-class="text-bold text-white bg-blue-grey-13"
                separator="cell"
                :loading="loading2"
                dense
                selection="single"
                v-model:selected="selected2"
                :rows-per-page-options="[0]"
                @update:selected="selectedCheck2"
            >
              <template #bottom-row>
                <q-td colspan="100%" v-if="selected2.length > 0">
                  <span class="text-blue"> {{ $t("selectedRow") }}: </span>
                  <span class="text-bold"> {{ this.infoSelected2(selected2[0]) }} </span>
                </q-td>
                <q-td
                    v-else-if="this.rows.length > 0" colspan="100%" class="text-bold">
                  {{ $t("infoApp") }}
                </q-td>
              </template>

              <template v-slot:top>
                <!-- && selected[0].linkType<5" -->
                <div style="font-size: 1.2em; font-weight: bold;" v-if="selected.length>0">
                  {{ $t("propWithValue") }}
                  <span style="font-size: 0.8em; color: #9C27B0;" class="q-mt-sm">
                    ({{ $t("notRequired") }})
                  </span>
                </div>

<!--
                <div style="font-size: 1.2em; font-weight: bold;" v-else>
                  {{ $t("prop") }}
                </div>
-->


                <q-space/>
<!--                && rows3.length===0-->
                <q-btn
                    v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:ins')"
                    dense
                    icon="post_add"
                    color="secondary"
                    :disable="loading || loading2 || selected.length === 0 || (selected.length>0 && selected[0].linkType > 4)"
                    @click="editRow2('ins')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("newRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-btn
                    v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:upd')"
                    dense
                    icon="edit"
                    color="secondary"
                    class="q-ml-sm"
                    :disable="loading2 || selected2.length === 0"
                    @click="editRow2('upd')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("editRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-btn
                    v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:del')"
                    dense
                    icon="delete"
                    color="secondary"
                    class="q-ml-sm"
                    :disable="loading2 || selected2.length === 0"
                    @click="removeRow2()"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>
              </template>

              <template #loading>
                <q-inner-loading :showing="loading2" color="secondary"></q-inner-loading>
              </template>
            </q-table>
          </template>

          <template v-slot:after>

            <q-table
                style="height: 100%; width: 100%"
                color="primary"
                card-class="bg-amber-1"
                row-key="id"
                :columns="cols3"
                :rows="rows3"
                table-header-class="text-bold text-white bg-blue-grey-13"
                separator="cell"
                :loading="loading3"
                dense
                selection="single"
                v-model:selected="selected3"
                :rows-per-page-options="[0]"
                :visible-columns="visibleColumns"
            >

              <template v-slot:body="props">
                <q-tr :props="props">
                  <q-td>
                    <q-btn
                        dense flat color="blue"
                        :icon="selected3.length > 0 ? 'check_box' : 'check_box_outline_blank'"
                        @click="selected3Row(props.row)"
                    >
                    </q-btn>
                  </q-td>

                  <q-td key="numberVal" :props="props">
                    {{ props.row.numberVal }}
                  </q-td>

                  <q-td key="strVal" :props="props">
                    {{ props.row["strVal"] }}
                  </q-td>

                  <q-td key="multiStrVal" :props="props">
                    {{ props.row["multiStrVal"] }}
                  </q-td>

                  <q-td key="dateTimeVal" :props="props">
                    {{ props.row["dateTimeVal"] }}
                  </q-td>

                  <q-td key="propVal" :props="props">
                    {{ getValueRef(props.row.propVal) }}
                  </q-td>

                </q-tr>
              </template>


              <template v-slot:top>
                <div style="font-size: 1.2em; font-weight: bold;" v-if="selected2.length>0 && selected2[0]['nameMeasure']">
                  {{ $t("measure") }}
                  <span style="font-size: 0.8em; color: #9C27B0;" class="q-mt-sm">
                    ({{ selected2[0]["nameMeasure"] }})
                  </span>
                </div>

                <q-space/>
                <q-btn
                    v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:ins')"
                    dense icon="post_add" color="secondary"
                    :disable="loading2 || loading3 || selected2.length === 0 || (selected2.length>0 && rows3.length===1)"
                    @click="editRow3('ins')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("newRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-btn
                    v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:upd')"
                    dense icon="edit" color="secondary" class="q-ml-sm"
                    :disable="loading3 || selected3.length === 0"
                    @click="editRow3('upd')"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("editRecord") }}
                  </q-tooltip>
                </q-btn>
                <q-btn
                    v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:del')"
                    dense icon="delete" color="secondary" class="q-ml-sm"
                    :disable="loading3 || selected3.length === 0"
                    @click="removeRow3()"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
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
import {
  collapsAll,
  expandAll,
  getParentNode,
  hasChild,
  hasTarget,
  notifyError,
  notifyInfo,
  pack,
} from "src/utils/jsutils";
import {api, baseURL} from "boot/axios";
import allConsts from "pages/all-consts";
import UpdaterDimObjItem from "pages/dimobj/UpdaterDimObjItem.vue";
import DimObjView from "pages/dimobj/DimObjView.vue";
import UpdaterDimObjItemPropVal from "pages/dimobj/UpdaterDimObjItemPropVal.vue";
import UpdaterDimObjItemPropValValue from "pages/dimobj/UpdaterDimObjItemPropValValue.vue";

export default {
  name: "DimPropItem",

  data() {
    return {
      splitterModel: 70,
      splitterModel2: 50,
      cols: [],
      rows: [],
      loading: false,
      FD_LinkType: [],
      FD_DimObjItemType: [],
      FD_PropType: [],
      isExpanded: true,
      itemId: null,
      selected: [],
      dimObj: null,
      dimObjName: "",
      //
      cols2: [],
      rows2: [],
      loading2: false,
      selected2: [],
      //
      cols3: [],
      rows3: [],

      optFV: [],
      optMea: [],
      optObj: [],
      optRelObj: [],

      visibleColumns: ["strVal"],
      loading3: false,
      selected3: [],
      visProp: false,
      hasChild: false,

      afterUPD: false,
      afterINS: false
    };
  },

  methods: {
    hasTarget,

    toBack() {
      this.$router["push"]({
        name: "DimObj",
        params: {
          dimObjGr: this.dimObjGr,
          dimObj: this.dimObj,
        },
      });
    },

    setVisibleColumns(pt) {

      this.visibleColumns = []
      if (pt === 11 || pt === 55 || pt === 66 || pt === 77)
        this.visibleColumns = ["propVal"]
      else if (pt === 22 || pt === 33 || pt === 46 || pt === 47)
        this.visibleColumns = ["numberVal"]
      else if (pt === 41 || pt === 42)
        this.visibleColumns = ["strVal"]
      else if (pt === 43 || pt === 44 || pt === 45)
        this.visibleColumns = ["dateTimeVal"]
      else if (pt === 48)
        this.visibleColumns = ["multiStrVal"]
      else
        notifyInfo("Unknown Prop Type!")

    },

    selectedCheck2() {
      this.selected3 = [];
      if (this.selected2.length > 0 && this.selected.length > 0 && this.selected[0].linkType < 5) {
        //
        this.setVisibleColumns(this.selected2[0].pt)
        if (this.selected2[0].pt === 11 || this.selected2[0].pt === 55 ||
            this.selected2[0].pt === 66 || this.selected2[0].pt === 77) {
          if (this.selected2[0].pt === 11)
            this.loadOptRefValues('fv');
          else if (this.selected2[0].pt === 55)
            this.loadOptRefValues('obj');
          else if (this.selected2[0].pt === 66)
            this.loadOptRefValues('relobj');
          else if (this.selected2[0].pt === 77)
            this.loadOptRefValues('measure');
        } else {
          this.loadDimObjItemPropVal(this.selected2[0].id, this.selected2[0].prop, this.selected2[0].pt)
        }
      } else {
        this.loadDimObjItemPropVal(0, 0, 0)
      }
    },

    selectedCheck(item) {
      this.selected2 = [];
      this.selected3 = [];
      this.loadDimObjItemPropVal(0, 0, 0)
      if (this.selected.length > 0 && item.id === this.selected[0].id) {
        if (!this.afterUPD) {
          this.selected = [];
          this.splitterModel = 100;
          this.loadDimObjItemProp(0);
        }
      } else {
        this.selected = [];
        this.selected.push(item);

        if (item.linkType > 4) {
          this.splitterModel = 100;

        } else
          this.splitterModel = 70;

        if (this.afterINS)
          this.hasChild = false
        else
          this.hasChild = hasChild(this.selected[0].id, this.selected[0])

        this.loadDimObjItemProp(this.selected[0].id);
        if (item.linkType > 4) {
          //this.splitterModel2 = ref(100);
        } else {
          this.cols3 = this.getColumns3()
          this.visibleColumns = []
          this.visibleColumns = ["strVal"]
        }
      }
      this.afterUPD = false
      this.afterINS = false
    },

    editRow2(mode) {
      let data = {};
      if (mode === "upd") {
        data = this.selected2[0];
      }

      this.$q
          .dialog({
            component: UpdaterDimObjItemPropVal,
            componentProps: {
              data: data,
              mode: mode,
              doi: this.selected[0].id,
              linkType: this.selected[0].linkType,
              doiType: this.selected[0].dimObjItemType,
            },
          })
          .onOk((data) => {
            this.selected2 = []
            this.loadDimObjItemProp(this.selected[0].id);
            this.selected2.push(data)
            this.selectedCheck2()
          });
    },

    removeRow2() {
      let rec = this.selected2[0];
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
            api
                .post(baseURL, {
                  method: "dimobj/deleteDOIprop",
                  params: [rec.id],
                })
                .then(
                    () => {
                      this.loadDimObjItemProp(this.selected[0].id);
                    },
                    () => {
                      notifyInfo(this.$t("hasValue"));
                    }
                );
          });
    },

    infoSelected2(row) {
      return this.$t("selectedRow") + ": " + row.name;
    },

    hasNotChild() {
      return !(this.selected.length > 0 && this.selected[0].lev);
    },

    fnLT(val) {
      if (this.FD_LinkType.length === 0) return null;
      return this.FD_LinkType.get(val);
    },

    fnDOIT(val) {
      if (this.FD_DimObjItemType.length === 0) return null;
      return this.FD_DimObjItemType.get(val);
    },

    addNode(isChild) {
      let data = {dimObj: this.dimObj, lev: 1};
      data.dimObjItemType = allConsts.FD_DimObjItemType.typ;
      data.linkType = allConsts.FD_LinkType.none;
      let parentDimObjItemType = 0; //allConsts.FD_DimObjItemType.typ;
      let parentName = "Нет";
      let parent = 0;
      if (isChild) {
        data.parent = this.selected[0].id;
        parentName = this.selected[0].name;
        parent = this.selected[0].id;
        parentDimObjItemType = this.selected[0].dimObjItemType;
      }
      this.$q
          .dialog({
            component: UpdaterDimObjItem,
            componentProps: {
              data: data,
              mode: "ins",
              parentName: parentName,
              parent: parent,
              parentDimObjItemType: parentDimObjItemType,
              lg: this.lang,
            },
          })
          .onOk((data) => {
            this.afterINS = true
            this.selected = []
            this.loadData(this.dimObj);
            this.selected.push(data)
            //this.selectedCheck(data)
          });
    },

    editNode(rec) {
      let parentNode = [];
      let parentName = "Нет";
      let parent = 0;
      let parentDimObjItemType = 0
      if (rec.parent > 0) {
        getParentNode(this.rows, rec.parent, parentNode);
        parentName = parentNode[0].name;
        parent = parentNode[0].id;
        parentDimObjItemType = parentNode[0].dimObjItemType
      }

      this.$q
          .dialog({
            component: UpdaterDimObjItem,
            componentProps: {
              data: rec,
              mode: "upd",
              parentName: parentName,
              parent: parent,
              hasChild: this.hasChild,
              parentDimObjItemType: parentDimObjItemType,
              lg: this.lang,
            },
          })
          .onOk((res) => {
            this.afterUPD = true
            this.selected = [];
            this.loadData(this.dimObj);
            this.selected.push(res)
            this.selectedCheck(res)
          });
    },

    deleteNode() {
      let rec = this.selected[0];
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
            api
                .post(baseURL, {
                  method: "dimobj/deleteDOI",
                  params: [rec.id],
                })
                .then(
                    () => {
                      this.splitterModel = 100;
                      this.loadData(this.dimObj);
                    },
                    () => {
                      notifyInfo(this.$t("hasChild"));
                    }
                );
          });
    },

    loadData(dimobj) {
      this.loading = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadDimObjItem",
            params: [dimobj],
          })
          .then((response) => {
            let rows = response.data.result.records
            let maxLT = 1
            for (let row of rows) {
              if (row["linkType"] > 1)
                maxLT = row["linkType"]
            }
            this.visProp = maxLT > 4
            this.rows = pack(rows, "ord");
            this.fnExpand();
          })
          .catch((error) => {
            this.$router["push"]("/");

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

            notifyError(msg);
            //
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    loadDimObjItemProp(dimObjItem) {
      this.loading = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadDimObjItemProp",
            params: [dimObjItem],
          })
          .then((response) => {
            this.rows2 = response.data.result.records;

          })
          .catch((error) => {
            this.$router["push"]("/");

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

            notifyError(msg);
            //
          })
          .finally(() => {
            this.loading = ref(false);
          });
    },

    loadDimObjItemPropVal(dimObjItemProp, prop, pt) {
      this.loading3 = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadDimObjItemPropVal",
            params: [dimObjItemProp, prop, pt],
          })
          .then((response) => {
            this.rows3 = response.data.result.records;
            if (this.selected2.length > 0 && this.rows3.length === 0)
              notifyInfo(this.$t("reqPropValue"))
          })
          .catch((error) => {
            this.$router["push"]("/");

            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;

            notifyError(msg);
            //
          })
          .finally(() => {
            this.loading3 = ref(false);
          });
    },

    fnView() {
      this.$q
          .dialog({
            component: DimObjView,
            componentProps: {
              dimobj: this.dimObj,
              lg: this.lg,
              cmdDate: true,
              // ...
            },
          })
          .onOk((r) => {
            console.log("Ok!", r);

          });
    },

    fnExpand() {
      expandAll(this.rows);
    },

    fnCollapse() {
      collapsAll(this.rows);
    },

    getColumns() {
      //["name", "linkType", "dimObjItem", "nameProp", "lev"]
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 35%",
        },
        {
          name: "linkType",
          label: this.$t("linkType"),
          field: "linkType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 25%",
        },
        {
          name: "dimObjItemType",
          label: this.$t("fldCmpType"),
          field: "dimObjItemType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "nameProp",
          label: this.$t("prop"),
          field: "nameProp",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 15%",
        },
        {
          name: "lev",
          label: this.$t("level"),
          field: "lev",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 5%",
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
          sortable: true,
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 40%",
        },
        {
          name: "propType",
          label: this.$t("propType"),
          field: "propType",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20",
          format: (val) => (val) ? this.FD_PropType.get(val) : null
        },
        {
          name: "nameStatus",
          label: this.$t("statusFactor"),
          field: "nameStatus",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 20%",
        },
        {
          name: "nameProvider",
          label: this.$t("providerTyp"),
          field: "nameProvider",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 20%",
        },
      ];
    },

    getColumns3() {
      return [
        {
          name: "numberVal",
          label: this.$t("val"),
          field: "numberVal",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 100%",
        },
        {
          name: "strVal",
          label: this.$t("val"),
          field: "strVal",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 100",
          format: (val) => (val) ? this.FD_PropType.get(val) : null
        },
        {
          name: "multiStrVal",
          label: this.$t("val"),
          field: "multiStrVal",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width: 100%",
        },
        {
          name: "dateTimeVal",
          label: this.$t("val"),
          field: "dateTimeVal",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 100%",
        },
        {
          name: "propVal",
          label: this.$t("val"),
          field: "propVal",
          align: "center",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em",
          style: "width: 100%",
        },
      ];
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

    setPadding(item) {
      return `padding-left: ${item.level * 30}px;`;
    },

    editRow3(mode) {
      let data = {dimObjItemProp: this.selected2[0].id};
      if (mode === "upd") {
        data = this.selected3[0];
      }

      this.$q
          .dialog({
            component: UpdaterDimObjItemPropValValue,
            componentProps: {
              data: data,
              mode: mode,
              dimObjItemProp: this.selected2[0].id,
              prop: this.selected2[0].prop,
              pt: this.selected2[0].pt,
              kf: this.selected2[0]["kfrombase"],
            },
          })
          .onOk(() => {
            this.loadDimObjItemPropVal(this.selected2[0].id, this.selected2[0].prop, this.selected2[0].pt);
          });
    },

    removeRow3() {
      let rec = this.selected3[0];
      this.$q
          .dialog({
            title: this.$t("confirmation"),
            message:
                this.$t("deleteRecord"),
            html: true,
            cancel: true,
            persistent: true,
            focus: "cancel",
          })
          .onOk(() => {
            api
                .post(baseURL, {
                  method: "dimobj/deleteDOIpropValue",
                  params: [rec.id],
                })
                .then(
                    () => {
                      this.loadDimObjItemPropVal(this.selected2[0].id, this.selected2[0].prop, this.selected2[0].pt);
                    },
                    (error) => {
                      notifyInfo(error.message);
                    }
                );
          });
    },

    loadOptRefValues(par) {
      this.loading3 = ref(true);
      //
      api
          .post(baseURL, {
            method: "dimobj/loadOptForRefValues",
            params: [par],
          })
          .then((response) => {
            if (par === 'fv') {
              this.optFV = new Map();
              response.data.result.records.forEach((it) => {
                this.optFV.set(it["id"], it["name"]);
              });
            } else if (par === 'measure') {
              this.optMea = new Map();
              response.data.result.records.forEach((it) => {
                this.optMea.set(it["id"], it["name"]);
              });
            } else if (par === 'obj') {
              this.optObj = new Map();
              response.data.result.records.forEach((it) => {
                this.optObj.set(it["id"], it["name"]);
              });
            } else if (par === 'relobj') {
              this.optRelObj = new Map();
              response.data.result.records.forEach((it) => {
                this.optRelObj.set(it["id"], it["name"]);
              });
            }
          })
          .then(() => {
            this.loading3 = ref(true);
            //
            api
                .post(baseURL, {
                  method: "dimobj/loadDimObjItemPropVal",
                  params: [this.selected2[0].id, this.selected2[0].prop, this.selected2[0].pt],
                })
                .then((response) => {
                  this.rows3 = response.data.result.records;
                  if (this.selected2.length > 0 && this.rows3.length === 0)
                    notifyInfo(this.$t("reqPropValue"))
                })
                .catch((error) => {
                  let msg = error.message;
                  if (error.response) msg = error.response.data.error.message;
                  notifyError(msg);
                })
                .finally(() => {
                  this.loading3 = ref(false);
                });
          })
          .catch((error) => {
            let msg = error.message;
            if (error.response) msg = error.response.data.error.message;
            notifyError(msg);
          })
          .finally(() => {
            this.loading3 = ref(false);
          });
    },

    selected3Row(row) {
      if (this.selected3.length > 0) {
        this.selected3 = []
      } else {
        this.selected3.push(row)
      }
    },

    getValueRef(val) {
      if (this.selected2.length > 0) {
        if (this.selected2[0].pt === 11) {
          return (val) ? this.optFV.get(val) : null
        } else if (this.selected2[0].pt === 55) {
          return (val) ? this.optObj.get(val) : null
        } else if (this.selected2[0].pt === 55) {
          return (val) ? this.optObj.get(val) : null
        } else if (this.selected2[0].pt === 66) {
          return (val) ? this.optObj.get(val) : null
        } else if (this.selected2[0].pt === 77) {
          return (val) ? this.optMea.get(val) : null
        }
      } else
        return null
    },

  },

  created() {
    this.lang = localStorage.getItem("curLang");
    this.lang = this.lang === "en-US" ? "en" : this.lang;
    //
    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_LinkType"}],
        })
        .then((response) => {
          this.FD_LinkType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_LinkType.set(it["id"], it["text"]);
          });
        })
        .finally(() => {
          this.loading = ref(false);
        });
    //
    this.loading = ref(true);
    api
        .post(baseURL, {
          method: "dict/load",
          params: [{dict: "FD_DimObjItemType"}],
        })
        .then((response) => {
          this.FD_DimObjItemType = new Map();
          response.data.result.records.forEach((it) => {
            this.FD_DimObjItemType.set(it["id"], it["text"]);
          });
        })
        .finally(() => {
          this.loading = ref(false);
        });

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
  }
  ,

  computed: {
    arrayTreeObj() {
      let vm = this;
      let newObj = [];
      vm.recursive(vm.rows, newObj, 0, vm.itemId, vm.isExpanded);
      return newObj;
    }
    ,
  },

  mounted() {
    this.dimObjGr = parseInt(this.$route["params"].dimObjGr, 10);
    this.dimObj = parseInt(this.$route["params"].dimObj, 10);
    this.dimObjName = this.$route["params"].name;
    this.loadData(this.dimObj);
  }
  ,

  setup() {}

}
;
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
