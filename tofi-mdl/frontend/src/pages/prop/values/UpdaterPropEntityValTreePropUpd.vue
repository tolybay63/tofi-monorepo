<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    full-width
    full-height
  >
    <q-card class="q-dialog-plugin no-scroll">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-bar class="bg-orange-1" style="height: 48px">
        <q-btn
          dense
          icon="expand_more"
          color="secondary"
          @click="fnExpand"
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
          @click="fnCollapse"
          style="margin-bottom: 5px; margin-left: 5px"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>
        <q-space />
        <q-card-actions align="right">
          <q-btn
            :loading="loading"
            :dense="dense"
            color="secondary"
            icon="save"
            :label="$t('save')"
            @click="onOKClick"
          >
            <template #loading>
              <q-spinner-hourglass color="white" />
            </template>
          </q-btn>

          <q-btn
            :dense="dense"
            color="secondary"
            icon="cancel"
            :label="$t('cancel')"
            @click="onDialogCancel"
          />
        </q-card-actions>
      </q-bar>

      <div
        class="q-table-container q-table--dense wrap bg-orange-1 scroll"
        style="height: 85%"
      >
        <div class="q-pa-sm-sm bg-orange-1">
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr class style="text-align: left">
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
              <!--name-->
              <td :data-th="cols[0].name" @click="toggle(item)">
                  <span class="q-tree__node" :style="setPadding(item)">
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    />

                    <q-btn
                      dense
                      flat
                      :color="item.children.length > 0 ? 'gray' : 'blue'"
                      :icon="
                        item.checked
                          ? 'check_box'
                          : 'check_box_outline_blank'
                      "
                      @click.stop="selectedCheck(item)"
                    />

                    <q-icon :name="getIcon(item)" />
                    {{ item[cols[0].field] }}
                  </span>
              </td>

              <!--name-->
              <td :data-th="cols[1].name">
                {{ item[cols[1].field] }}
              </td>
              <!--name-->
              <td :data-th="cols[2].name">
                {{ item[cols[2].field] }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <q-bar>{{ $t("countAll") }}: {{ sz }}</q-bar>
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import {
  checkChilds,
  collapsAll,
  expandAll,
  notifyError,
  pack,
  uncheckChilds,
} from "@/utils/jsutils";
import allConsts from "@/pages/all-consts";

const props = defineProps({
  prop: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const lang = ref(props.lg);
const cols = ref([]);
const rows = ref([]);
const separator = ref("cell");
const loading = ref(false);

const isExpanded = ref(true);
const currentNode = ref(null);
const itemId = ref(null);
const sz = ref(0);

const expand = (item) => {
  item.expend = ref(true);
  const { children } = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
};

const collaps = (item) => {
  item.expend = ref(false);
  const { children } = item;
  if (children.length > 0) {
    item.leaf = ref(false);
  } else {
    item.leaf = ref(true);
    item.expend = undefined;
  }
};

const selectedCheck = (item) => {
  if (item.children.length > 0) {
    if (item.checked === false) checkChilds(item);
    else uncheckChilds(item);
  } else {
    item.checked = !item.checked;
  }
};

const loadData = () => {
  loading.value = true;
  api
    .post("", {
      method: "prop/loadPropValEntityTreePropForUpd",
      params: [props.prop],
    })
    .then((response) => {
      sz.value = response.data.result.records.length;
      rows.value = pack(response.data.result.records, "id");
      expandAll(rows.value);
    })
    .finally(() => {
      loading.value = false;
    });
};

const getIcon = (row) => {
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
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em; width: 20%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 35%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 45%",
  },
];

const onOKClick = () => {
  let dta = [];

  const tt = (node, chks) => {
    if (node.checked && node.propType !== undefined) {
      chks.push(node);
    }
    let children = node.children;
    if (children.length > 0) {
      children.forEach((ch) => tt(ch, chks));
    }
  };

  const getCheckeds = (data, chks) => {
    for (let i = 0; i < data.length; i++) {
      tt(data[i], chks);
    }
  };

  getCheckeds(rows.value, dta);

  let d0 = [];
  dta.forEach((d) => {
    let { ...o } = d;
    o.children = null;
    d0.push(o);
  });

  api
    .post("", {
      method: "prop/savePropEntityVal",
      params: [props.prop, d0],
    })
    .then(
      () => {
        onDialogOK({ res: true });
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = error.response.data.error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

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
  if (item.expend) {
    return "remove_circle_outline";
  }
  if (item.children && item.children.length > 0) {
    return "control_point";
  }
  return "";
};

const toggle = (item) => {
  if (item.children && item.children.length > 0) {
    if (item.expend) collaps(item);
    else expand(item);
  }
};

const setPadding = (item) => {
  return `padding-left: ${item.level * 30}px;`;
};

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  cols.value = getColumns();
  loadData();
});
</script>

<style scoped></style>
