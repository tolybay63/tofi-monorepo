<template>
  <div class="q-pa-md">
    <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
      <div style="font-size: 1.2em; font-weight: bold;">
        <q-avatar color="black" text-color="white" icon="article" />
        {{ $t("dimsPropItem") }}
      </div>
    </q-banner>

    <q-banner dense inline-actions class="bg-orange-1">
      <div class="row">
        <div style="font-size: 1.2em; font-weight: bold;">{{ $t("dimProp") }}:</div>
        <span style="color: black; margin-left: 10px; margin-top: 2px">
          {{ dimPropName }}
        </span>
      </div>

      <div class="row">
        <div style="font-size: 1.0em; color: #1976d2; margin-top: 2px">{{ $t("dimPropType") }}:</div>
        <span style="color: #9C27B0; margin-left: 10px; margin-top: 2px">
          {{ getDimPropType(dimPropType) }}
        </span>
      </div>

      <template v-slot:action>
        <q-btn
          dense
          round
          color="secondary"
          icon="arrow_back"
          glossy
          @click="toBack"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("back") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="isTree()"
          dense
          class="q-ml-sm"
          icon="expand_more"
          color="secondary"
          @click="fnExpand"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>
        <q-btn
          v-if="isTree()"
          dense
          icon="expand_less"
          color="secondary"
          class="q-ml-sm"
          @click="fnCollapse"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmprop:sel:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm"
          :disable="loading"
          @click="addNode(false)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="isTree() && hasTarget('mdl:mn_dp:dmprop:sel:ins')"
          dense
          icon="post_add"
          color="secondary"
          class="q-ml-sm img-vert"
          :disable="loading || selected.length === 0"
          @click="addNode(true)"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("addSubNode") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('mdl:mn_dp:dmprop:sel:upd')"
          dense
          icon="edit"
          color="secondary"
          class="q-ml-sm"
          :disable="loading || selected.length === 0"
          @click="editNode"
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

        <q-inner-loading :showing="loading" color="secondary" />
      </template>
    </q-banner>

    <div style="height: calc(100vh - 510px); width: 100%" class="no-scroll">
      <div
        v-if="dimPropType === constDimPropTypeProp"
        class="q-table-container q-table--dense scroll"
        style="height: 100%"
      >
        <table class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle">
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr style="text-align: left">
            <th :style="cols[0]?.headerStyle">{{ cols[0]?.label }}</th>
            <th :style="cols[1]?.headerStyle">{{ cols[1]?.label }}</th>
            <th :style="cols[2]?.headerStyle">{{ cols[2]?.label }}</th>
            <th :style="cols[3]?.headerStyle">{{ cols[3]?.label }}</th>
          </tr>
          </thead>

          <tbody style="background: aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="cols[0]?.name" @click="toggle(item)">
                <span class="q-tree-link q-tree-label" :style="setPadding(item)">
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

                  <q-icon :name="getIcon(item)" size="24px" />
                  {{ item.name }}
                </span>
            </td>
            <td :data-th="cols[1]?.name">{{ item.fullName }}</td>
            <td :data-th="cols[2]?.name">{{ fnPropType(item.propType) }}</td>
            <td :data-th="cols[3]?.name">{{ item.cmt }}</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div
        v-else
        class="q-table-container q-table--dense scroll"
        style="height: 100%"
      >
        <table class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle">
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr style="text-align: left">
            <th :style="cols[0]?.headerStyle">{{ cols[0]?.label }}</th>
            <th :style="cols[1]?.headerStyle">{{ cols[1]?.label }}</th>
            <th :style="cols[2]?.headerStyle">{{ cols[2]?.label }}</th>
          </tr>
          </thead>

          <tbody style="background: aliceblue">
          <tr v-for="(item, index) in arrayTreeObj" :key="index">
            <td :data-th="cols[0]?.name" @click="toggle(item)">
                <span class="q-tree-link q-tree-label" :style="setPadding(item)">
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

                  <q-icon :name="getIcon(item)" />
                  {{ item.name }}
                </span>
            </td>
            <td :data-th="cols[1]?.name">{{ item.fullName }}</td>
            <td :data-th="cols[2]?.name">{{ item.cmt }}</td>
          </tr>
          </tbody>
        </table>

        <div>
          <q-td colspan="100%" v-if="selected.length > 0">
            <span class="text-blue"> {{ $t("selectedRow") }}: </span>
            <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
          </q-td>
          <q-td v-else-if="rows.length > 0" colspan="100%" class="text-bold">
            {{ $t("infoApp") }}
          </q-td>
        </div>
      </div>
    </div>
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
  hasTarget,
  notifyError,
  notifyInfo,
  pack,
} from "@/utils/jsutils";
import { api } from "@/boot/axios";
import allConsts from "@/pages/all-consts";
import UpdaterDimPropItemProp from "@/pages/dimprop/UpdaterDimPropItemProp.vue";
import UpdaterDimPropItemFactor from "@/pages/dimprop/UpdaterDimPropItemFactor.vue";
import UpdaterDimPropItemMultiProp from "@/pages/dimprop/UpdaterDimPropItemMultiProp.vue";
import UpdaterDimPropItemDimMultiProp from "@/pages/dimprop/UpdaterDimPropItemDimMultiProp.vue";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const FD_PropType = reactive(new Map());
const FD_DimPropType = reactive(new Map());
const isExpanded = ref(true);
const itemId = ref(null);
const selected = ref([]);

const dimPropGr = ref(0);
const dimProp = ref(0);
const dimPropType = ref(0);
const dimPropName = ref("");

const constDimPropTypeProp = allConsts.FD_DimPropType.prop;
const consDimPropTypeFactor = allConsts.FD_DimPropType.factor;
const consDimPropTypeMultiList = allConsts.FD_DimPropType.multiList;
const consDimPropTypeDMP = allConsts.FD_DimPropType.dimMultiProp;

const countRec = ref(0);
const dimMultiPropType = ref(0);
const dimMultiProp = ref(0);
const lang = ref("");

const toBack = () => {
  router.push({
    name: "DimProp",
    params: {
      dimPropGr: dimPropGr.value,
      dimProp: dimProp.value,
    },
  });
};

const fnPropType = (val) => {
  return FD_PropType.size > 0 ? FD_PropType.get(val) : null;
};

const getDimPropType = (val) => {
  return FD_DimPropType.size > 0 ? FD_DimPropType.get(val) : null;
};

const addNode = (isChild) => {
  let data = { dimProp: dimProp.value };
  let parentName = "Нет";
  if (isChild) {
    data.parent = selected.value[0].id;
    parentName = selected.value[0].name;
  } else {
    if (selected.value.length > 0) {
      let parentNode = [];
      data.parent = selected.value[0].parent;
      getParentNode(rows.value, selected.value[0].parent, parentNode);
      parentName = parentNode.length > 0 ? parentNode[0].name : "Нет";
    }
  }

  let cmp = null;
  if (dimPropType.value === allConsts.FD_DimPropType.prop) cmp = UpdaterDimPropItemProp;
  if (dimPropType.value === allConsts.FD_DimPropType.factor) cmp = UpdaterDimPropItemFactor;
  if (dimPropType.value === allConsts.FD_DimPropType.multiList) cmp = UpdaterDimPropItemMultiProp;
  if (dimPropType.value === allConsts.FD_DimPropType.dimMultiProp) cmp = UpdaterDimPropItemDimMultiProp;

  $q.dialog({
    component: cmp,
    componentProps: {
      data: data,
      dimPropType: dimPropType.value,
      dimMultiProp: dimMultiProp.value,
      dimMultiPropType: dimMultiPropType.value,
      mode: "ins",
      dimProp: dimProp.value,
      dimPropName: dimPropName.value,
      parentName: parentName,
      lg: lang.value,
      dense: true,
    },
  }).onOk((r) => {
    loadData(dimProp.value);
    selected.value = [];
    selected.value.push(r);
  });
};

const editNode = () => {
  let parentNode = [];
  let parentName = "Нет";
  if (selected.value[0].parent > 0) {
    getParentNode(rows.value, selected.value[0].parent, parentNode);
    parentName = parentNode[0].name;
  }

  let cmp = null;
  if (dimPropType.value === allConsts.FD_DimPropType.prop) cmp = UpdaterDimPropItemProp;
  if (dimPropType.value === allConsts.FD_DimPropType.factor) cmp = UpdaterDimPropItemFactor;
  if (dimPropType.value === allConsts.FD_DimPropType.multiList) cmp = UpdaterDimPropItemMultiProp;
  if (dimPropType.value === allConsts.FD_DimPropType.dimMultiProp) cmp = UpdaterDimPropItemDimMultiProp;

  $q.dialog({
    component: cmp,
    componentProps: {
      data: selected.value[0],
      dimPropType: dimPropType.value,
      mode: "upd",
      dimProp: dimProp.value,
      dimMultiProp: dimMultiProp.value,
      dimMultiPropType: dimMultiPropType.value,
      dimPropName: dimPropName.value,
      parentName: parentName,
      lg: lang.value,
      dense: true,
    },
  }).onOk((data) => {
    selected.value = [];
    loadData(dimProp.value);
    selected.value.push(data);
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
        method: "dimprop/deleteDPI",
        params: [rec.id],
      })
      .then(
        () => {
          selected.value = [];
          loadData(dimProp.value);
        },
        () => {
          notifyInfo(proxy?.$t("hasChild"));
        }
      );
  });
};

const selectedCheck = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
  }
};

const loadData = (dimpropVal) => {
  loading.value = true;
  api
    .post("", {
      method: "dimprop/loadDimPropItemProp",
      params: [dimpropVal, dimPropType.value],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
      countRec.value = rows.value.length;
      fnExpand();
    })
    .then(() => {
      if (dimPropType.value === allConsts.FD_DimPropType.dimMultiProp) {
        api
          .post("", {
            method: "dimprop/getDimMultiPropType",
            params: [dimpropVal],
          })
          .then((response) => {
            dimMultiProp.value = response.data.result.dmp;
            dimMultiPropType.value = response.data.result.dmpt;
          });
      }
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const getIcon = (row) => {
  if (dimPropType.value === allConsts.FD_DimPropType.factor) return "";
  if (row.propType === undefined) return "folder";
  if (allConsts.FD_PropType.factor === row.propType) {
    return "account_tree";
  } else if (allConsts.FD_PropType.attr === row.propType) {
    return "format_shapes";
  } else if (allConsts.FD_PropType.meter === row.propType) {
    return "scale";
  } else if (allConsts.FD_PropType.rate === row.propType) {
    return "speed";
  } else if (allConsts.FD_PropType.typ === row.propType) {
    return "view_quilt";
  } else if (allConsts.FD_PropType.reltyp === row.propType) {
    return "view_column";
  } else if (allConsts.FD_PropType.measure === row.propType) {
    return "square_foot";
  } else if (allConsts.FD_PropType.complex === row.propType) {
    return "category";
  }
  return "";
};

const fnExpand = () => expandAll(rows.value);
const fnCollapse = () => collapsAll(rows.value);

const getColumnsProp = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width: 30%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width: 35%",
  },
  {
    name: "propType",
    label: proxy?.$t("propType"),
    field: "propType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width: 15%",
    format: (val) => (FD_PropType.size > 0 ? FD_PropType.get(val) : null),
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const getColumnsFactor = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width: 35%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width: 45%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt"),
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
];

const recursive = (obj, newObj, level, targetItemId, isExpend) => {
  if (!obj) return;
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

const isTree = () => {
  return (
    dimPropType.value === allConsts.FD_DimPropType.prop ||
    dimPropType.value === allConsts.FD_DimPropType.factor ||
    dimPropType.value === allConsts.FD_DimPropType.dimMultiProp
  );
};

const infoSelected = (row) => {
  return " " + row.name;
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;

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

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimPropType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_DimPropType.set(it.id, it.text);
      });
    });

  dimPropGr.value = parseInt(route.params.dimPropGr, 10) || 0;
  dimProp.value = parseInt(route.params.dimProp, 10) || 0;
  dimPropType.value = parseInt(route.params.dimPropType, 10) || 0;
  dimPropName.value = route.params.name;

  if (dimPropType.value === allConsts.FD_DimPropType.prop) {
    cols.value = getColumnsProp();
  } else {
    cols.value = getColumnsFactor();
  }

  loadData(dimProp.value);
});
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
