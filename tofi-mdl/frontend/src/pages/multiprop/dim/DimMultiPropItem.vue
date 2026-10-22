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
        <q-banner dense inline-actions class="bg-orange-1 q-mb-sm">
          <div class="row">
            <div style="font-size: 1.2em; font-weight: bold;">
              {{ $t("dimMultiPropItem") }}
              <span v-if="dimMultiPropType === 1" class="text-blue">({{ $t("dimStatic") }}):</span>
              <span v-else class="text-red">({{ $t("dimDynamic") }}):</span>
            </div>
            <span style="color: black; margin-left: 10px; margin-top: 2px">
              {{ dimMultiPropName }}
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
              v-if="hasTarget('mdl:mn_ds:pmdim:sel:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm"
              :disable="dimMultiPropType === 2 && rows.length > 0"
              @click="fnIns('ins', false)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("create1level") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="dimMultiPropType === 1 && hasTarget('mdl:mn_ds:pmdim:sel:ins')"
              dense
              icon="post_add"
              color="secondary"
              class="q-ml-sm img-vert"
              @click="fnIns('ins', true)"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("createChild") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:pmdim:sel:upd')"
              dense
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="fnIns('upd', false)"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              v-if="hasTarget('mdl:mn_ds:pmdim:sel:del')"
              dense
              icon="delete"
              color="red"
              class="q-ml-sm"
              @click="fnDel(currentNode)"
              :disable="currentNode == null"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("deletingRecord") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 220px); width: 100%" class="scroll">
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
        <div v-if="entityType() === 'meter'">
          <dim-multi-prop-item-meter :dimMultiPropItem="entityId()" ref="childCompMeter" />
        </div>
        <div v-else-if="entityType() === 'attr_str' || entityType() === 'attr_mask' || entityType() === 'attr_date'">
          <dim-multi-prop-item-attrib :dimMultiPropItem="entityId()" ref="childCompAttr" />
        </div>
        <div v-else-if="entityType() === 'factorval'">
          <dim-multi-prop-item-factor :dimMultiPropItem="entityId()" ref="childCompFactor" />
        </div>
        <div v-else-if="entityType() === 'obj'">
          <dim-multi-prop-item-cls :dimMultiPropItem="entityId()" ref="childCompCls" />
        </div>
        <div v-else-if="entityType() === 'relobj'">
          <dim-multi-prop-item-rel-cls :dimMultiPropItem="entityId()" ref="childCompRelCls" />
        </div>
        <div v-else-if="entityType() === 'measure'">
          <dim-multi-prop-item-measure :dimMultiPropItem="entityId()" ref="childCompMeasure" />
        </div>
        <div v-else>
          <dim-multi-prop-item-none :dimMultiPropItem="entityId()" ref="childCompNone" />
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack } from "@/utils/jsutils";
import QTreeTable from "@/components/QTreeTable.vue";
import UpdateDimMultiPropItem from "@/pages/multiprop/dim/UpdateDimMultiPropItem.vue";
import DimMultiPropItemAttrib from "@/pages/multiprop/dim/DimMultiPropItemAttrib.vue";
import DimMultiPropItemMeter from "@/pages/multiprop/dim/DimMultiPropItemMeter.vue";
import DimMultiPropItemFactor from "@/pages/multiprop/dim/DimMultiPropItemFactor.vue";
import DimMultiPropItemCls from "@/pages/multiprop/dim/DimMultiPropItemCls.vue";
import DimMultiPropItemRelCls from "@/pages/multiprop/dim/DimMultiPropItemRelCls.vue";
import DimMultiPropItemMeasure from "@/pages/multiprop/dim/DimMultiPropItemMeasure.vue";
import DimMultiPropItemNone from "@/pages/multiprop/dim/DimMultiPropItemNone.vue";
import allConsts from "@/pages/all-consts.js";

const router = useRouter();
const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const splitterModel = ref(100);
const visible = ref(false);
const dense = ref(true);
const cols = ref([]);
const rows = ref([]);
const currentNode = ref(null);
const dimMultiPropGr = ref(0);
const dimMultiProp = ref(0);
const dimMultiPropType = ref(0);
const dimMultiPropName = ref(null);
const titles = ref([]);
const lang = ref("");

const childComp = ref(null);
const childCompMeter = ref(null);
const childCompAttr = ref(null);
const childCompFactor = ref(null);
const childCompCls = ref(null);
const childCompRelCls = ref(null);
const childCompMeasure = ref(null);
const childCompNone = ref(null);

const entityType = () => {
  if (currentNode.value) {
    return currentNode.value.entityType;
  }
  return "";
};

const entityId = () => {
  if (currentNode.value) {
    return currentNode.value.id;
  }
  return 0;
};

const toBack = () => {
  router.push({
    name: "DimMultiProp",
    params: {
      dmpGr: dimMultiPropGr.value,
      dmp: dimMultiProp.value,
    },
  });
};

const onUpdateSelect = (item) => {
  currentNode.value = item.selected !== undefined ? item.selected : null;
  let id = item.selected !== undefined ? item.selected.id : 0;
  if (currentNode.value) {
    splitterModel.value = 60;
  } else {
    splitterModel.value = 100;
  }

  if (id > 0) {
    if (currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.meter)
      childCompMeter.value?.loadDimMultiPropItemMeter(id);
    else if (
      currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.attr_str ||
      currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.attr_mask ||
      currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.attr_date
    )
      childCompAttr.value?.loadDimMultiPropItemAttrib(id);
    else if (currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.fv)
      childCompFactor.value?.loadDimMultiPropItemFactor(id);
    else if (currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.measure)
      childCompMeasure.value?.loadDimMultiPropItemMeasure(id);
    else if (currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.obj)
      childCompCls.value?.loadDimMultiPropItemCls(id);
    else if (currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.relobj)
      childCompRelCls.value?.loadDimMultiPropItemRelCls(id);
    else if (currentNode.value.multiEntityType === allConsts.FD_MultiValEntityType.none)
      childCompNone.value?.load(id);
    else
      notifyError("Не известный тип сущности");
  }
};

const edit = (data, mode, isChild, parentName) => {
  const lg = lang.value;
  $q.dialog({
    component: UpdateDimMultiPropItem,
    componentProps: {
      data: data,
      mode: mode,
      isChild: isChild,
      dimMultiPropName: dimMultiPropName.value,
      dimMultiPropType: dimMultiPropType.value,
      lg: lg,
      parentName: parentName,
      titles: titles.value,
    },
  }).onOk((r) => {
    currentNode.value = null;
    fetchData(dimMultiProp.value);
    childComp.value?.restoreSelect(r);
    onUpdateSelect({ selected: r });
  });
};

const fnIns = (mode, isChild) => {
  let parent = null;
  let parentName = null;

  if (mode === "ins") {
    api
      .post("", {
        method: "dimMultiProp/newRecDimMultiPropItem",
        params: [dimMultiProp.value],
      })
      .then((response) => {
        if (isChild) {
          parent = currentNode.value.id;
          let parentNode = [];
          getParentNode(rows.value, parent, parentNode);
          parentName = parentNode[0].fullName;
          response.data.result.records[0].parent = parent;
        }

        edit(
          response.data.result.records[0],
          mode,
          isChild,
          parentName
        );
      });
  } else {
    if (currentNode.value.parent > 0) {
      let parentNode = [];
      getParentNode(rows.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0].fullName;
      isChild = true;
    }
    edit(currentNode.value, mode, isChild, parentName);
  }
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation") || "confirmation",
    message:
      (proxy?.$t("deleteRecord") || "deleteRecord") +
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
        .post("", {
          method: "dimMultiProp/deleteDimItem",
          params: [rec],
        })
        .then(
          () => {
            childComp.value?.clrAny();
            fetchData(dimMultiProp.value);
          },
          (error) => {
            let msg = error.message;
            if (error.response?.data?.error?.message)
              msg = error.response.data.error.message;
            notifyError(msg);
          }
        );
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled") || "canceled");
    });
};

const fetchData = (propDim) => {
  visible.value = true;
  api
    .post("", {
      method: "dimMultiProp/loadDimMultiPropItem",
      params: [propDim],
    })
    .then((response) => {
      rows.value = pack(response.data.result.rows.records, "ord");
      titles.value = response.data.result.titles.records;
    })
    .then(() => {
      let b = false;
      cols.value.forEach((c) => {
        if (c.name.substring(0, 4) === "col_") b = true;
      });
      let cnt = titles.value.length;
      if (b) {
        cols.value.splice(2, cnt);
      }
      let k = 2;
      titles.value.forEach((r) => {
        cols.value.splice(k++, 0, {
          name: "col_" + r.id,
          label: r.title,
          field: "col_" + r.id,
          align: "left",
          headerStyle: "font-size: 1.2em; width: 15%",
        });
      });
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      visible.value = false;
    });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "entityType",
    label: proxy?.$t("multiValEntityType") || "multiValEntityType",
    field: "entityType",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 15%",
  },
];

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  dimMultiPropGr.value = parseInt(route.params.dimMultiPropGr, 10) || 0;
  dimMultiProp.value = parseInt(route.params.dimMultiProp, 10) || 0;
  dimMultiPropName.value = route.params.dimMultiPropName;
  dimMultiPropType.value = parseInt(route.params.dimMultiPropType, 10) || 0;

  fetchData(dimMultiProp.value);
});
</script>

<style scoped>
.img-vert {
  -moz-transform: scaleY(-1);
  -o-transform: scaleY(-1);
  -webkit-transform: scaleY(-1);
  transform: scaleY(-1);
}
</style>
