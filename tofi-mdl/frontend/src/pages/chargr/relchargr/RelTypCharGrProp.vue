<template>
  <q-banner :dense="dense" inline-actions class="bg-amber-1">
    <div style="font-size: 1.2em">{{ $t("itemsPropCharGr") }}</div>
    <template v-slot:action>
      <q-btn
        :dense="dense"
        icon="expand_more"
        color="secondary"
        @click="fnExpand()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("expandAll") }}
        </q-tooltip>
      </q-btn>
      <q-btn
        :dense="dense"
        icon="expand_less"
        color="secondary"
        class="q-ml-sm"
        @click="fnCollapse()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("collapseAll") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:prop:edit')"
        :dense="dense"
        icon="edit_note"
        color="secondary"
        class="q-ml-sm"
        @click="editData()"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>

      <q-btn
        v-if="hasTarget('mdl:mn_ds:reltyp:sel:char:prop:upd')"
        :dense="dense"
        icon="edit"
        color="secondary"
        class="q-ml-sm"
        :disable="loading || selected.length === 0"
        @click="editRow(selected[0])"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-banner>

  <div style="height: calc(100vh - 245px); width: 100%" class="no-scroll">
    <div class="q-table-container q-table--dense scroll" style="height: 100%">
      <table class="q-table q-table--cell-separator q-table--bordered wrap q-table-middle">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr style="text-align: left">
          <th :style="cols[0]?.headerStyle">{{ cols[0]?.label }}</th>
          <th :style="cols[1]?.headerStyle">{{ cols[1]?.label }}</th>
          <th :style="cols[2]?.headerStyle">{{ cols[2]?.label }}</th>
          <th :style="cols[3]?.headerStyle">{{ cols[3]?.label }}</th>
          <th :style="cols[4]?.headerStyle">{{ cols[4]?.label }}</th>
          <th :style="cols[5]?.headerStyle">{{ cols[5]?.label }}</th>
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
                  :dense="dense"
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
                {{ item.cod }}
              </span>
          </td>
          <td :data-th="cols[1]?.name">{{ item.name }}</td>
          <td :data-th="cols[2]?.name">{{ getVal(item.storageType) }}</td>
          <td :data-th="cols[3]?.name">{{ getTable(item.flatTable) }}</td>
          <td :data-th="cols[4]?.name">{{ item.p_measure }}</td>
          <td :data-th="cols[5]?.name">{{ item.pv_measure }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance, watch } from "vue";
import { useQuasar } from "quasar";
import { collapsAll, expandAll, hasTarget, notifyError, pack } from "@/utils/jsutils";
import { api } from "@/boot/axios";
import allConsts from "@/pages/all-consts";
import UpdaterRelTypCharGrPropUpd from "@/pages/chargr/relchargr/UpdaterRelTypCharGrPropUpd.vue";
import UpdaterRelTypCharGrProp from "@/pages/chargr/relchargr/UpdaterRelTypCharGrProp.vue";

const props = defineProps({
  relTypCharGr: [String, Number],
  dense: Boolean,
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const FD_StorageType = reactive(new Map());
const FD_NameTable = reactive(new Map());
const isExpanded = ref(true);
const itemId = ref(null);
const selected = ref([]);

const lang = ref(localStorage.getItem("curLang") === "en-US" ? "en" : localStorage.getItem("curLang"));

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 18%",
  },
  {
    name: "storageType",
    label: proxy?.$t("fldStorageType"),
    field: "storageType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 18%",
  },
  {
    name: "flatTable",
    label: proxy?.$t("tableName"),
    field: "flatTable",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 6%",
  },
  {
    name: "p_measure",
    label: proxy?.$t("propMeasure"),
    field: "p_measure",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 18%",
  },
  {
    name: "pv_measure",
    label: proxy?.$t("measure"),
    field: "pv_measure",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
];

const getVal = (val) => {
  if (val === undefined || val === null) return null;
  return FD_StorageType.get(val);
};

const getTable = (val) => {
  if (val === undefined || val === null) return null;
  return FD_NameTable.get(val);
};

const getIcon = (row) => {
  if (row.propType === undefined) return "folder";

  if (allConsts.FD_PropType.factor === row.propType) return "account_tree";
  if (allConsts.FD_PropType.attr === row.propType) return "format_shapes";
  if (allConsts.FD_PropType.meter === row.propType) return "scale";
  if (allConsts.FD_PropType.rate === row.propType) return "speed";
  if (allConsts.FD_PropType.typ === row.propType) return "view_quilt";
  if (allConsts.FD_PropType.reltyp === row.propType) return "view_column";
  if (allConsts.FD_PropType.measure === row.propType) return "square_foot";
  if (allConsts.FD_PropType.complex === row.propType) return "category";

  return "";
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const selectedCheck = (item) => {
  if (item.propType === undefined) return;

  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [item];
  }
};

const loadData = (relTypCharGrVal) => {
  if (!relTypCharGrVal) return;

  loading.value = true;
  api
    .post("", {
      method: "reltyp/loadRelTypCharGrProp",
      params: [{ relTypCharGr: relTypCharGrVal }],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "id");
      fnExpand();
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

const editRow = (rec) => {
  $q.dialog({
    component: UpdaterRelTypCharGrProp,
    componentProps: {
      dta: rec,
      relTypCharGr: props.relTypCharGr,
      lg: lang.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      loadData(props.relTypCharGr);
    }
  });
};

const editData = () => {
  $q.dialog({
    component: UpdaterRelTypCharGrPropUpd,
    componentProps: {
      relTypCharGr: props.relTypCharGr,
      lg: lang.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      loadData(props.relTypCharGr);
    }
  });
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
      if (o.expend) {
        recursive(o.children, newObj, o.level + 1, currentItemId, isExpend);
      }
    } else {
      o.level = level;
      o.leaf = true;
      newObj.push(o);
    }
  });
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

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
    item.expend = undefined;
    item.leaf = false;
    itemId.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

watch(() => props.relTypCharGr, (newVal) => {
  if (newVal) loadData(newVal);
}, { immediate: true });

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_StorageType" }],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_StorageType.set(it.id, it.text);
      });
    });

  api
    .post("", {
      method: "flatTable/loadTables",
      params: [{}],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        FD_NameTable.set(it.id, it.nameTable);
      });
    });
});

defineExpose({
  loadData
});
</script>

<style scoped></style>
