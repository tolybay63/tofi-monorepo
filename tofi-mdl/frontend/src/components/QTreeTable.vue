<template>
  <span class="q-pa-sm-sm">
    <q-btn
      dense
      icon="expand_more"
      color="secondary"
      @click="fnExpand()"
      style="margin-bottom: 5px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("expandAll") }}
      </q-tooltip>
    </q-btn>
    <q-btn
      dense
      icon="expand_less"
      color="secondary"
      class="q-ml-sm"
      @click="fnCollapse()"
      style="margin-bottom: 5px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("collapseAll") }}
      </q-tooltip>
    </q-btn>
    <span v-if="checked_visible">
      <span style="color: #1976d2; margin-left: 5px">
        {{ $t("selectedNode") }}:
      </span>
      {{ nodeInfo() }}
    </span>
  </span>

  <div class="q-pa-sm-sm bg-orange-1 sticky-header-table">
    <table class="q-table q-table--cell-separator q-table--bordered wrap">
      <thead class="text-bold text-white bg-blue-grey-13">
      <tr>
        <th
          v-for="(col, index) in cols"
          :key="index"
          :class="col.headerClass"
          :style="col.headerStyle"
        >
          {{ col.label }}
        </th>
      </tr>
      </thead>

      <tbody style="background: aliceblue">
      <tr v-for="(item, index) in arrayTreeObj" :key="index">
        <td :data-th="cols0[0]?.name" @click="toggle(item)">
            <span class="q-tree__node" :style="setPadding(item)">
              <q-icon
                style="cursor: pointer"
                :name="iconName(item)"
                color="secondary"
              />

              <span v-if="checked_visible">
                <q-btn
                  dense
                  flat
                  color="blue"
                  :icon="
                    selected.length === 1 && item.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click.stop="selectedRow(item)"
                >
                </q-btn>
              </span>

              {{
                cols0[0]?.field === "periodType"
                  ? fnPeriodType(item[cols0[0]?.field])
                  : item[cols0[0]?.field]
              }}
            </span>
        </td>
        <!--other cols without 0-->
        <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">
            <span v-if="col.checked && col.checked === 'true'">
              <q-btn
                dense
                flat
                color="blue"
                :icon="
                  item[col.field] ? 'check_box' : 'check_box_outline_blank'
                "
              >
              </q-btn>
            </span>
          <span v-else>
              <span v-if="col.field === 'propType'">
                <q-icon
                  size="24px"
                  :color="getColor(item)"
                  :name="getIcon(item[col.field])"
                ></q-icon>
              </span>

              <span v-if="col.field === 'entityType'">
                <q-icon
                  size="16px"
                  :color="getColorET()"
                  :name="getIconET(item[col.field])"
                ></q-icon>
              </span>
              {{
              col.field === "accessLevel"
                ? fnAL(item[col.field])
                : col.field === "propType"
                  ? fnPT(item)
                  : col.field === "entityType"
                    ? fnET(item)
                    : col.field === "dimPropType"
                      ? fnDPT(item)
                      : col.field === "dbeg"
                        ? fnDbeg(item[col.field])
                        : col.field === "dend"
                          ? fnDend(item[col.field])
                          : item[col.field]
            }}
            </span>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from "vue";
import { collapsAll, expandAll } from "@/utils/jsutils.js";
import { tofi_dbeg, tofi_dend } from "@/boot/axios.js";
import { date } from "quasar";
import allConsts from "@/pages/all-consts.js";

const props = defineProps([
  "rows",
  "cols",
  "icon_leaf",
  "checked_visible",
  "meterStruct",
  "FD_PropType",
  "FD_AccessLevel",
  "FD_PeriodType",
  "FD_DimPropType",
  "emptydate",
]);

const emit = defineEmits(["updateSelect"]);

const { proxy } = getCurrentInstance();

const isExpanded = ref(false);
const currentNode = ref(null);
const itemId = ref(null);
const selected = ref([]);

const expand = (item) => {
  item.expend = true;
  if (item.children && item.children.length > 0) {
    item.leaf = false;
  } else {
    item.leaf = true;
  }
};

const collaps = (item) => {
  item.expend = false;
  if (item.children && item.children.length > 0) {
    item.leaf = false;
  } else {
    item.leaf = true;
    item.expend = undefined;
  }
};

const clrAny = () => {
  selected.value = [];
  currentNode.value = null;
};

const getColor = (item) => {
  if (item.propType === 2 || item.propType === 3) {
    if (props.meterStruct === allConsts.FD_MeterStruct.hard) return "orange";
    else return "green";
  } else {
    if (item.propType === allConsts.FD_PropType.complex) return "red";
    else return "orange";
  }
};

const getIcon = (val) => {
  if (val === allConsts.FD_PropType.factor) return "account_tree";
  else if (val === allConsts.FD_PropType.meter) return "scale";
  else if (val === allConsts.FD_PropType.rate) return "speed";
  else if (val === allConsts.FD_PropType.attr) return "format_shapes";
  else if (val === allConsts.FD_PropType.typ) return "view_quilt";
  else if (val === allConsts.FD_PropType.reltyp) return "view_column";
  else if (val === allConsts.FD_PropType.measure) return "square_foot";
  else if (val === allConsts.FD_PropType.complex) return "category";
};

const getColorET = () => {
  return "orange";
};

const getIconET = (val) => {
  if (val === "factorval") return "account_tree";
  else if (val === "meter") return "scale";
  else if (val === "attrib") return "format_shapes";
  else if (val === "obj") return "view_quilt";
  else if (val === "relobj") return "view_column";
  else if (val === "measure") return "square_foot";
};

const fnAL = (val) => {
  return props.FD_AccessLevel ? props.FD_AccessLevel.get(val) : null;
};

const fnPeriodType = (val) => {
  return props.FD_PeriodType ? props.FD_PeriodType.get(val) : null;
};

const fnDPT = (item) => {
  return props.FD_DimPropType
    ? props.FD_DimPropType.get(item.dimPropType)
    : null;
};

const fnPT = (item) => {
  let at = "";
  if (item.propType === allConsts.FD_PropType.attr) {
    if (item.attribValType === allConsts.FD_AttribValType.str)
      at = " (" + proxy?.$t("str") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.mask)
      at = " (" + proxy?.$t("mask") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.dt)
      at = " (" + proxy?.$t("dt") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.dttm)
      at = " (" + proxy?.$t("dttm") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.tm)
      at = " (" + proxy?.$t("tm") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.integ)
      at = " (" + proxy?.$t("integ") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.num)
      at = " (" + proxy?.$t("num") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.file)
      at = " (" + proxy?.$t("file") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.multistr)
      at = " (" + proxy?.$t("multistr") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.period)
      at = " (" + proxy?.$t("period") + ")";
    else if (item.attribValType === allConsts.FD_AttribValType.entity)
      at = " (" + proxy?.$t("entity") + ")";
  }
  return props.FD_PropType ? props.FD_PropType.get(item.propType) + at : null;
};

const fnET = (item) => {
  return item.entityType === undefined ? "" : proxy?.$t(item.entityType);
};

const fnDbeg = (val) => {
  let fmt = "...";
  if (props.emptydate !== undefined) fmt = props.emptydate;
  return val <= tofi_dbeg ? fmt : date.formatDate(val, "DD.MM.YYYY");
};

const fnDend = (val) => {
  let fmt = props.emptydate === undefined ? "..." : props.emptydate;
  return val >= tofi_dend ? fmt : date.formatDate(val, "DD.MM.YYYY");
};

const fnExpand = () => {
  expandAll(props.rows);
};

const fnCollapse = () => {
  collapsAll(props.rows);
};

const restoreSelect = (item) => {
  selected.value = [];
  selected.value.push(item);
};

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [];
    selected.value.push(item);
  }
  currentNode.value = selected.value[0] !== undefined ? selected.value[0] : null;
  emit("updateSelect", { selected: selected.value[0] });
};

const recursive = (obj, newObj, level, currentItemId, isExpend) => {
  if (!obj) return;
  obj.forEach((o) => {
    if (o.children && o.children.length !== 0) {
      o.level = level;
      o.leaf = false;
      newObj.push(o);
      if (o.id === currentItemId) {
        o.expend = isExpend;
      }
      if (o.expend === true) {
        recursive(o.children, newObj, o.level + 1, currentItemId, isExpend);
      }
    } else {
      o.level = level;
      o.leaf = true;
      newObj.push(o);
    }
  });
};

const iconName = (item) => {
  if (item.expend === true) {
    return "remove_circle_outline";
  }
  if (item.children && item.children.length > 0) {
    return "control_point";
  }
  return props.icon_leaf;
};

const toggle = (item) => {
  if (item.children && item.children.length > 0) {
    if (item.expend) collaps(item);
    else expand(item);
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const nodeInfo = () => {
  let res = "";
  if (currentNode.value) {
    res = !currentNode.value.cod
      ? !currentNode.value.name
        ? props.FD_PeriodType.get(currentNode.value.periodType)
        : currentNode.value.name
      : currentNode.value.cod;
  }
  return res;
};

const cols0 = computed(() => {
  return props.cols ? props.cols.slice(0, 1) : [];
});

const cols_ = computed(() => {
  return props.cols ? props.cols.slice(1) : [];
});

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(props.rows, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

// Открываем методы для вызова из родительских компонентов через ref
defineExpose({
  clrAny,
  restoreSelect,
  fnExpand,
  fnCollapse
});
</script>

<style scoped>
.sticky-header-table {
  max-height: 95%;
  overflow: auto;
}

.sticky-header-table table {
  border-collapse: separate;
  border-spacing: 0;
}

.sticky-header-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background-color: #607d8b;
}

.sticky-header-table .q-table--bordered {
  border-top: none;
}
</style>
