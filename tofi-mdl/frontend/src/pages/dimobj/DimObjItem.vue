<template>
  <div class="q-pa-md">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
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
              dense round color="secondary" icon="arrow_back" glossy @click="toBack"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("back") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              dense class="q-ml-sm" icon="expand_more" color="secondary" @click="fnExpand"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("expandAll") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              dense icon="expand_less" color="secondary" class="q-ml-sm" @click="fnCollapse"
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
              @click="deleteNode"
              :disable="loading || selected.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deleteNode") }}
              </q-tooltip>
            </q-btn>

            <q-space />

            <q-btn
              v-if="hasTarget('mdl:mn_dp:dmprop:sel:del')"
              dense
              icon="visibility"
              color="secondary"
              class="q-ml-sm"
              @click="fnView"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("view") }}
              </q-tooltip>
            </q-btn>

            <q-inner-loading :showing="loading" color="secondary" />
          </template>
        </q-banner>

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
              <tr style="text-align: left">
                <th :style="cols[0]?.headerStyle">{{ cols[0]?.label }}</th>
                <th :style="cols[1]?.headerStyle">{{ cols[1]?.label }}</th>
                <th :style="cols[2]?.headerStyle">{{ cols[2]?.label }}</th>
                <th :style="cols[3]?.headerStyle" v-if="visProp">{{ cols[3]?.label }}</th>
                <th :style="cols[4]?.headerStyle">{{ cols[4]?.label }}</th>
              </tr>
              </thead>

              <tbody style="background: aliceblue">
              <tr v-for="(item, index) in arrayTreeObj" :key="index">
                <td :data-th="cols[0]?.name" @click="toggle(item)">
                    <span
                      class="q-tree-link q-tree-label"
                      :style="setPadding(item)"
                    >
                      <q-icon
                        style="cursor: pointer"
                        :name="iconName(item)"
                        color="secondary"
                      />

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
                      />

                      {{ item.name }}
                    </span>
                </td>
                <td :data-th="cols[1]?.name">{{ fnLT(item.linkType) }}</td>
                <td :data-th="cols[2]?.name">
                  {{ fnDOIT(item.dimObjItemType) }}
                </td>
                <td :data-th="cols[3]?.name" v-if="visProp">
                  {{ item.nameProp }}
                </td>

                <td :data-th="cols[4]?.name">
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
                  <span class="text-bold"> {{ infoSelected2(selected2[0]) }} </span>
                </q-td>
                <q-td
                  v-else-if="rows.length > 0" colspan="100%" class="text-bold">
                  {{ $t("infoApp") }}
                </q-td>
              </template>

              <template v-slot:top>
                <div style="font-size: 1.2em; font-weight: bold;" v-if="selected.length > 0">
                  {{ $t("propWithValue") }}
                  <span style="font-size: 0.8em; color: #9C27B0;" class="q-mt-sm">
                    ({{ $t("notRequired") }})
                  </span>
                </div>

                <q-space />
                <q-btn
                  v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:ins')"
                  dense
                  icon="post_add"
                  color="secondary"
                  :disable="loading || loading2 || selected.length === 0 || (selected.length > 0 && selected[0].linkType > 4)"
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
                  color="red"
                  class="q-ml-sm"
                  :disable="loading2 || selected2.length === 0"
                  @click="removeRow2"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>
              </template>

              <template #loading>
                <q-inner-loading :showing="loading2" color="secondary" />
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
                    />
                  </q-td>

                  <q-td key="numberVal" :props="props">
                    {{ props.row.numberVal }}
                  </q-td>

                  <q-td key="strVal" :props="props">
                    {{ props.row.strVal }}
                  </q-td>

                  <q-td key="multiStrVal" :props="props">
                    {{ props.row.multiStrVal }}
                  </q-td>

                  <q-td key="dateTimeVal" :props="props">
                    {{ props.row.dateTimeVal }}
                  </q-td>

                  <q-td key="propVal" :props="props">
                    {{ getValueRef(props.row.propVal) }}
                  </q-td>
                </q-tr>
              </template>

              <template v-slot:top>
                <div style="font-size: 1.2em; font-weight: bold;" v-if="selected2.length > 0 && selected2[0].nameMeasure">
                  {{ $t("measure") }}
                  <span style="font-size: 0.8em; color: #9C27B0;" class="q-mt-sm">
                    ({{ selected2[0].nameMeasure }})
                  </span>
                </div>

                <q-space />
                <q-btn
                  v-if="hasTarget('mdl:mn_ds:reltyp:sel:memb:notcls:ins')"
                  dense icon="post_add" color="secondary"
                  :disable="loading2 || loading3 || selected2.length === 0 || (selected2.length > 0 && rows3.length === 1)"
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
                  dense icon="delete" color="red" class="q-ml-sm"
                  :disable="loading3 || selected3.length === 0"
                  @click="removeRow3"
                >
                  <q-tooltip transition-show="rotate" transition-hide="rotate">
                    {{ $t("deletingRecord") }}
                  </q-tooltip>
                </q-btn>
              </template>

              <template #loading>
                <q-inner-loading :showing="loading3" color="secondary" />
              </template>
            </q-table>
          </template>
        </q-splitter>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar } from "quasar";
import {
  collapsAll,
  expandAll,
  getParentNode,
  hasChild,
  hasTarget,
  notifyError,
  notifyInfo,
  pack,
} from "@/utils/jsutils";
import { api } from "@/boot/axios";
import allConsts from "@/pages/all-consts";
import UpdaterDimObjItem from "@/pages/dimobj/UpdaterDimObjItem.vue";
import DimObjView from "@/pages/dimobj/DimObjView.vue";
import UpdaterDimObjItemPropVal from "@/pages/dimobj/UpdaterDimObjItemPropVal.vue";
import UpdaterDimObjItemPropValValue from "@/pages/dimobj/UpdaterDimObjItemPropValValue.vue";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(70);
const splitterModel2 = ref(50);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const FD_LinkType = reactive(new Map());
const FD_DimObjItemType = reactive(new Map());
const FD_PropType = reactive(new Map());
const isExpanded = ref(true);
const itemId = ref(null);
const selected = ref([]);
const dimObj = ref(null);
const dimObjGr = ref(null);
const dimObjName = ref("");

const cols2 = ref([]);
const rows2 = ref([]);
const loading2 = ref(false);
const selected2 = ref([]);

const cols3 = ref([]);
const rows3 = ref([]);

const optFV = reactive(new Map());
const optMea = reactive(new Map());
const optObj = reactive(new Map());
const optRelObj = reactive(new Map());

const visibleColumns = ref(["strVal"]);
const loading3 = ref(false);
const selected3 = ref([]);
const visProp = ref(false);
const hasChildNode = ref(false);

const afterUPD = ref(false);
const afterINS = ref(false);

const lang = ref(
  localStorage.getItem("curLang") === "en-US" ? "en" : localStorage.getItem("curLang")
);

const toBack = () => {
  router.push({
    name: "DimObj",
    params: {
      dimObjGr: dimObjGr.value,
      dimObj: dimObj.value,
    },
  });
};

const setVisibleColumns = (pt) => {
  visibleColumns.value = [];
  if (pt === 11 || pt === 55 || pt === 66 || pt === 77) {
    visibleColumns.value = ["propVal"];
  } else if (pt === 22 || pt === 33 || pt === 46 || pt === 47) {
    visibleColumns.value = ["numberVal"];
  } else if (pt === 41 || pt === 42) {
    visibleColumns.value = ["strVal"];
  } else if (pt === 43 || pt === 44 || pt === 45) {
    visibleColumns.value = ["dateTimeVal"];
  } else if (pt === 48) {
    visibleColumns.value = ["multiStrVal"];
  } else {
    notifyInfo("Unknown Prop Type!");
  }
};

const selectedCheck2 = () => {
  selected3.value = [];
  if (selected2.value.length > 0 && selected.value.length > 0 && selected.value[0].linkType < 5) {
    setVisibleColumns(selected2.value[0].pt);
    if (
      selected2.value[0].pt === 11 ||
      selected2.value[0].pt === 55 ||
      selected2.value[0].pt === 66 ||
      selected2.value[0].pt === 77
    ) {
      if (selected2.value[0].pt === 11) loadOptRefValues("fv");
      else if (selected2.value[0].pt === 55) loadOptRefValues("obj");
      else if (selected2.value[0].pt === 66) loadOptRefValues("relobj");
      else if (selected2.value[0].pt === 77) loadOptRefValues("measure");
    } else {
      loadDimObjItemPropVal(selected2.value[0].id, selected2.value[0].prop, selected2.value[0].pt);
    }
  } else {
    loadDimObjItemPropVal(0, 0, 0);
  }
};

const selectedCheck = (item) => {
  selected2.value = [];
  selected3.value = [];
  loadDimObjItemPropVal(0, 0, 0);
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    if (!afterUPD.value) {
      selected.value = [];
      splitterModel.value = 100;
      loadDimObjItemProp(0);
    }
  } else {
    selected.value = [];
    selected.value.push(item);

    if (item.linkType > 4) {
      splitterModel.value = 100;
    } else {
      splitterModel.value = 70;
    }

    if (afterINS.value) {
      hasChildNode.value = false;
    } else {
      hasChildNode.value = hasChild(selected.value[0].id, selected.value[0]);
    }

    loadDimObjItemProp(selected.value[0].id);
    if (item.linkType <= 4) {
      cols3.value = getColumns3();
      visibleColumns.value = [];
      visibleColumns.value = ["strVal"];
    }
  }
  afterUPD.value = false;
  afterINS.value = false;
};

const editRow2 = (mode) => {
  let data = {};
  if (mode === "upd") {
    data = selected2.value[0];
  }

  $q.dialog({
    component: UpdaterDimObjItemPropVal,
    componentProps: {
      data: data,
      mode: mode,
      doi: selected.value[0].id,
      linkType: selected.value[0].linkType,
      doiType: selected.value[0].dimObjItemType,
    },
  }).onOk((dataResult) => {
    selected2.value = [];
    loadDimObjItemProp(selected.value[0].id);
    selected2.value.push(dataResult);
    selectedCheck2();
  });
};

const removeRow2 = () => {
  let rec = selected2.value[0];
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "dimobjitem/deleteDOIprop",
        params: [rec.id],
      })
      .then(
        () => {
          loadDimObjItemProp(selected.value[0].id);
        },
        () => {
          notifyInfo(proxy?.$t("hasValue"));
        }
      );
  });
};

const infoSelected2 = (row) => {
  return proxy?.$t("selectedRow") + ": " + row.name;
};

const hasNotChild = () => {
  return !(selected.value.length > 0 && selected.value[0].lev);
};

const fnLT = (val) => {
  if (FD_LinkType.size === 0) return null;
  return FD_LinkType.get(val);
};

const fnDOIT = (val) => {
  if (FD_DimObjItemType.size === 0) return null;
  return FD_DimObjItemType.get(val);
};

const addNode = (isChild) => {
  let data = { dimObj: dimObj.value, lev: 1 };
  data.dimObjItemType = allConsts.FD_DimObjItemType.typ;
  data.linkType = allConsts.FD_LinkType.none;
  let parentDimObjItemType = 0;
  let parentName = "Нет";
  let parent = 0;
  if (isChild) {
    data.parent = selected.value[0].id;
    parentName = selected.value[0].name;
    parent = selected.value[0].id;
    parentDimObjItemType = selected.value[0].dimObjItemType;
  }
  $q.dialog({
    component: UpdaterDimObjItem,
    componentProps: {
      data: data,
      mode: "ins",
      parentName: parentName,
      parent: parent,
      parentDimObjItemType: parentDimObjItemType,
      lg: lang.value,
    },
  }).onOk((dataResult) => {
    afterINS.value = true;
    selected.value = [];
    loadData(dimObj.value);
    selected.value.push(dataResult);
  });
};

const editNode = (rec) => {
  let parentNode = [];
  let parentName = "Нет";
  let parent = 0;
  let parentDimObjItemType = 0;
  if (rec.parent > 0) {
    getParentNode(rows.value, rec.parent, parentNode);
    parentName = parentNode[0].name;
    parent = parentNode[0].id;
    parentDimObjItemType = parentNode[0].dimObjItemType;
  }

  $q.dialog({
    component: UpdaterDimObjItem,
    componentProps: {
      data: rec,
      mode: "upd",
      parentName: parentName,
      parent: parent,
      hasChild: hasChildNode.value,
      parentDimObjItemType: parentDimObjItemType,
      lg: lang.value,
    },
  }).onOk((res) => {
    afterUPD.value = true;
    selected.value = [];
    loadData(dimObj.value);
    selected.value.push(res);
    selectedCheck(res);
  });
};

const deleteNode = () => {
  let rec = selected.value[0];
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "dimobjitem/deleteDOI",
        params: [rec.id],
      })
      .then(
        () => {
          splitterModel.value = 100;
          loadData(dimObj.value);
        },
        () => {
          notifyInfo(proxy?.$t("hasChild"));
        }
      );
  });
};

const loadData = (targetDimObj) => {
  loading.value = true;
  api
    .post("", {
      method: "dimobjitem/loadDimObjItem",
      params: [targetDimObj],
    })
    .then((response) => {
      let recordList = response.data.result.records;
      let maxLT = 1;
      for (let row of recordList) {
        if (row.linkType > 1) maxLT = row.linkType;
      }
      visProp.value = maxLT > 4;
      rows.value = pack(recordList, "ord");
      fnExpand();
    })
    .catch((error) => {
      router.push("/");
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const loadDimObjItemProp = (dimObjItem) => {
  loading.value = true;
  api
    .post("", {
      method: "dimobjitem/loadDimObjItemProp",
      params: [dimObjItem],
    })
    .then((response) => {
      rows2.value = response.data.result.records;
    })
    .catch((error) => {
      router.push("/");
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const loadDimObjItemPropVal = (dimObjItemProp, prop, pt) => {
  loading3.value = true;
  api
    .post("", {
      method: "dimobjitem/loadDimObjItemPropVal",
      params: [dimObjItemProp, prop, pt],
    })
    .then((response) => {
      rows3.value = response.data.result.records;
      if (selected2.value.length > 0 && rows3.value.length === 0) {
        notifyInfo(proxy?.$t("reqPropValue"));
      }
    })
    .catch((error) => {
      router.push("/");
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading3.value = false;
    });
};

const fnView = () => {
  $q.dialog({
    component: DimObjView,
    componentProps: {
      dimobj: dimObj.value,
      lg: lang.value,
      cmdDate: true,
    },
  }).onOk((r) => {
    console.log("Ok!", r);
  });
};

const fnExpand = () => expandAll(rows.value);
const fnCollapse = () => collapsAll(rows.value);

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 35%",
  },
  {
    name: "linkType",
    label: proxy?.$t("linkType"),
    field: "linkType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 25%",
  },
  {
    name: "dimObjItemType",
    label: proxy?.$t("fldCmpType"),
    field: "dimObjItemType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "nameProp",
    label: proxy?.$t("prop"),
    field: "nameProp",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
  {
    name: "lev",
    label: proxy?.$t("level"),
    field: "lev",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
];

const getColumns2 = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "propType",
    label: proxy?.$t("propType"),
    field: "propType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20",
    format: (val) => (val ? FD_PropType.get(val) : null),
  },
  {
    name: "nameStatus",
    label: proxy?.$t("statusFactor"),
    field: "nameStatus",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "nameProvider",
    label: proxy?.$t("providerTyp"),
    field: "nameProvider",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
];

const getColumns3 = () => [
  {
    name: "numberVal",
    label: proxy?.$t("val"),
    field: "numberVal",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 100%",
  },
  {
    name: "strVal",
    label: proxy?.$t("val"),
    field: "strVal",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 100",
    format: (val) => (val ? FD_PropType.get(val) : null),
  },
  {
    name: "multiStrVal",
    label: proxy?.$t("val"),
    field: "multiStrVal",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 100%",
  },
  {
    name: "dateTimeVal",
    label: proxy?.$t("val"),
    field: "dateTimeVal",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 100%",
  },
  {
    name: "propVal",
    label: proxy?.$t("val"),
    field: "propVal",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 100%",
  },
];

const recursive = (obj, newObj, level, targetItemId, isExpend) => {
  obj.forEach((o) => {
    if (o.children && o.children.length !== 0) {
      o.level = level;
      o.leaf = false;
      newObj.push(o);
      if (o.id === targetItemId) {
        o.expend = isExpend;
      }
      if (o.expend) {
        recursive(o.children, newObj, o.level + 1, targetItemId, isExpend);
      }
    } else {
      o.level = level;
      o.leaf = true;
      newObj.push(o);
    }
  });
};

const iconName = (item) => {
  if (item.expend) return "remove_circle_outline";
  if (item.children && item.children.length > 0) return "control_point";
  return "";
};

const toggle = (item) => {
  itemId.value = item.id;
  item.leaf = false;
  if (!item.leaf && item.expend === undefined && item.children !== undefined) {
    if (item.children.length !== 0) {
      recursive(item.children, [], item.level + 1, item.id, true);
    }
  }
  if (item.expend && item.children !== undefined) {
    item.children.forEach((o) => {
      o.expend = undefined;
    });
    item.expend = ref(undefined);
    item.leaf = ref(false);
    itemId.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const editRow3 = (mode) => {
  let data = { dimObjItemProp: selected2.value[0].id };
  if (mode === "upd") {
    data = selected3.value[0];
  }

  $q.dialog({
    component: UpdaterDimObjItemPropValValue,
    componentProps: {
      data: data,
      mode: mode,
      dimObjItemProp: selected2.value[0].id,
      prop: selected2.value[0].prop,
      pt: selected2.value[0].pt,
      kf: selected2.value[0].kfrombase,
    },
  }).onOk(() => {
    loadDimObjItemPropVal(selected2.value[0].id, selected2.value[0].prop, selected2.value[0].pt);
  });
};

const removeRow3 = () => {
  let rec = selected3.value[0];
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message: proxy?.$t("deleteRecord"),
    html: true,
    cancel: true,
    persistent: true,
    focus: "cancel",
  }).onOk(() => {
    api
      .post("", {
        method: "dimobjitem/deleteDOIpropValue",
        params: [rec.id],
      })
      .then(
        () => {
          loadDimObjItemPropVal(selected2.value[0].id, selected2.value[0].prop, selected2.value[0].pt);
        },
        (error) => {
          notifyInfo(error.message);
        }
      );
  });
};

const loadOptRefValues = (par) => {
  loading3.value = true;
  api
    .post("", {
      method: "dimobjitem/loadOptForRefValues",
      params: [par],
    })
    .then((response) => {
      if (par === "fv") {
        optFV.clear();
        response.data.result.records.forEach((it) => {
          optFV.set(it.id, it.name);
        });
      } else if (par === "measure") {
        optMea.clear();
        response.data.result.records.forEach((it) => {
          optMea.set(it.id, it.name);
        });
      } else if (par === "obj") {
        optObj.clear();
        response.data.result.records.forEach((it) => {
          optObj.set(it.id, it.name);
        });
      } else if (par === "relobj") {
        optRelObj.clear();
        response.data.result.records.forEach((it) => {
          optRelObj.set(it.id, it.name);
        });
      }
    })
    .then(() => {
      loading3.value = true;
      api
        .post("", {
          method: "dimobjitem/loadDimObjItemPropVal",
          params: [selected2.value[0].id, selected2.value[0].prop, selected2.value[0].pt],
        })
        .then((response) => {
          rows3.value = response.data.result.records;
          if (selected2.value.length > 0 && rows3.value.length === 0) {
            notifyInfo(proxy?.$t("reqPropValue"));
          }
        })
        .catch((error) => {
          let msg = error.message;
          if (error.response) msg = error.response.data.error.message;
          notifyError(msg);
        })
        .finally(() => {
          loading3.value = false;
        });
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      loading3.value = false;
    });
};

const selected3Row = (row) => {
  if (selected3.value.length > 0) {
    selected3.value = [];
  } else {
    selected3.value.push(row);
  }
};

const getValueRef = (val) => {
  if (selected2.value.length > 0) {
    if (selected2.value[0].pt === 11) {
      return val ? optFV.get(val) : null;
    } else if (selected2.value[0].pt === 55 || selected2.value[0].pt === 66) {
      return val ? optObj.get(val) : null;
    } else if (selected2.value[0].pt === 77) {
      return val ? optMea.get(val) : null;
    }
  }
  return null;
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  dimObjGr.value = parseInt(route.params.dimObjGr, 10);
  dimObj.value = parseInt(route.params.dimObj, 10);
  dimObjName.value = route.params.name;

  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_LinkType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_LinkType.set(it.id, it.text);
      });
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimObjItemType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_DimObjItemType.set(it.id, it.text);
      });
    })
    .finally(() => {
      loading.value = false;
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PropType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_PropType.set(it.id, it.text);
      });
    });

  cols.value = getColumns();
  cols2.value = getColumns2();

  loadData(dimObj.value);
});
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
  -ms-filter: "FlipV";
}
</style>
