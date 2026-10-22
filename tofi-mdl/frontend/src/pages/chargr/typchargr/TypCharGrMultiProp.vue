<template>
  <q-banner
    :dense="dense"
    inline-actions
    class="bg-amber-1"
    style="font-size: 1.3em"
  >
    <div style="font-size: 1.2em">{{ $t("itemsMultiPropCharGr") }}</div>
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
        v-if="hasTarget('mdl:mn_ds:typ:sel:char:matrix:edit')"
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
        v-if="hasTarget('mdl:mn_ds:typ:sel:char:matrix:upd')"
        :dense="dense"
        icon="edit"
        color="secondary"
        class="q-ml-sm"
        :disable="disableEdit()"
        @click="editRow(selected[0])"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("editRecord") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-banner>

  <div class="q-table-container q-table--dense wrap bg-orange-1">
    <div class="q-pa-sm-sm bg-orange-1">
      <div class="q-table-middle scroll">
        <table class="q-table q-table--cell-separator q-table--bordered wrap">
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
                    :dense="dense"
                    flat
                    :color="item.children?.length > 0 ? 'gray' : 'blue'"
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
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance, watch } from "vue";
import { useQuasar } from "quasar";
import { collapsAll, expandAll, hasTarget, notifyError, pack } from "@/utils/jsutils";
import { api } from "@/boot/axios";
import UpdateTypCharGrMultiPropUpd from "@/pages/chargr/typchargr/UpdateTypCharGrMultiPropUpd.vue";
import UpdateTypCharGrMultiProp from "@/pages/chargr/typchargr/UpdateTypCharGrMultiProp.vue";

const props = defineProps({
  typCharGr: [String, Number],
  dense: Boolean,
});

const $q = useQuasar();
const { proxy } = getCurrentInstance();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const FD_StorageType = reactive(new Map());
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
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "storageType",
    label: proxy?.$t("fldStorageType"),
    field: "storageType",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 30%",
  },
];

const getVal = (val) => {
  if (val === undefined || val === null) return null;
  return FD_StorageType.get(val);
};

const getIcon = (row) => {
  if (row.multiProp === undefined) return "folder";
  return "view_in_ar";
};

const disableEdit = () => {
  return !!(loading.value || selected.value.length === 0);
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const selectedCheck = (item) => {
  if (!String(item.id).includes("p_")) return;
  if (selected.value.length > 0 && item.id === selected.value[0].id) {
    selected.value = [];
  } else {
    selected.value = [item];
  }
};

const loadData = (typCharGrVal) => {
  if (!typCharGrVal) return;
  loading.value = true;
  api
    .post("", {
      method: "typ/loadTypCharGrMultiProp",
      params: [{ typCharGr: typCharGrVal }],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
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
    component: UpdateTypCharGrMultiProp,
    componentProps: {
      data: rec,
      lg: lang.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      loadData(props.typCharGr);
    }
  });
};

const editData = () => {
  $q.dialog({
    component: UpdateTypCharGrMultiPropUpd,
    componentProps: {
      typCharGr: props.typCharGr,
      lg: lang.value,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      loadData(props.typCharGr);
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

watch(() => props.typCharGr, (newVal) => {
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
});

defineExpose({
  loadData,
});
</script>

<style scoped></style>
