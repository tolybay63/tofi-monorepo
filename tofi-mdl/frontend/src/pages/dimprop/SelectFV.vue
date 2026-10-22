<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin full-height" style="width: 600px">
      <q-bar class="text-white bg-primary">
        {{ $t("select") }}
      </q-bar>

      <q-card>
        <q-banner dense inline-actions class="bg-orange-1 no-scroll">
          <template v-slot:action>
            <q-btn
              dense
              icon="expand_more"
              color="secondary"
              @click="fnExpand"
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
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("collapseAll") }}
              </q-tooltip>
            </q-btn>
          </template>
        </q-banner>

        <div style="height: calc(100vh - 200px); width: 100%">
          <div
            class="q-table-container q-table--dense wrap scroll"
            style="height: 100%"
          >
            <table
              class="q-table q-table--cell-separator q-table--bordered wrap"
            >
              <thead class="text-bold text-white bg-blue-grey-13">
              <tr style="text-align: left">
                <th style="font-size: 1.3em; width: 80%">
                  {{ cols[0]?.label }}
                </th>
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
                        :color="item.parent === undefined ? 'gray' : 'blue'"
                        :icon="
                          item.checked ? 'check_box' : 'check_box_outline_blank'
                        "
                        @click.stop="selectedRow(item)"
                      />

                      <q-icon
                        :name="getIcon(item)"
                        :color="getIconColor(item)"
                      />
                      {{ item.name }}
                    </span>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validFV()"
        />
        <q-btn
          :dense="dense"
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { collapsAll, expandAll, pack } from "@/utils/jsutils";

const props = defineProps({
  dimPropItem: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const separator = ref("cell");
const isExpanded = ref(true);
const itemId = ref(null);

const getIconColor = (row) => {
  if (row.isOwn === 1) return "green";
  return "gray";
};

const getIcon = (row) => {
  if (row.parent) return "brightness_1";
  return "square";
};

const selectedRow = (item) => {
  if (!item.parent) return;
  let parent = rows.value.filter((row) => {
    return row.id === item.parent;
  });
  if (parent.length === 0) return;

  if (item.checked === 1) {
    item.checked = 0;
    parent[0].checked = 0;
  } else {
    let { children } = parent[0];
    if (children) {
      children.map((row) => {
        row.checked = 0;
      });
    }
    parent[0].checked = 1;
    item.checked = 1;
  }
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    style: "font-size: 1.3em; width: 80%",
  },
];

const validFV = () => {
  let i = 0;
  let flt = [];
  while (i < rows.value.length) {
    let { children } = rows.value[i];
    if (children) {
      flt = children.filter((ch) => ch.checked === 1);
      if (flt.length > 0) {
        return false;
      }
    }
    i = i + 1;
  }
  return true;
};

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
  if (item.expend) {
    return "remove_circle_outline";
  }
  if (item.children && item.children.length > 0) {
    return "control_point";
  }
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

const fetchData = () => {
  api
    .post("", {
      method: "dimprop/loadForFvSelect",
      params: [props.dimPropItem],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
    });
};

const onOKClick = () => {
  let ids = [];
  let names = [];
  rows.value.forEach((row) => {
    const { children } = row;
    if (children) {
      children.forEach((it) => {
        if (it.checked === 1) {
          ids.push(it.id.toString());
          names.push(it.name.toString());
        }
      });
    }
  });
  onDialogOK({ fvs: ids.join(","), nms: names.join("; ") });
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  cols.value = getColumns();
  fetchData();
});
</script>

<style scoped></style>
