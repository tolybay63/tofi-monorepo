<template>
  <q-banner dense inline-actions class="bg-orange-1" style="font-size: 1.3em">
    <template v-slot:action>
      <q-btn dense icon="expand_more" color="secondary" @click="fnExpand">
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

      <q-btn
        v-if="hasTarget('mdl:mn_ds:prop:val:edit')"
        dense
        icon="edit_note"
        color="secondary"
        class="q-ml-sm"
        @click="editData"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("update") }}
        </q-tooltip>
      </q-btn>
    </template>
  </q-banner>

  <div class="q-table-container q-table--dense wrap bg-orange-1">
    <div class="q-pa-sm-sm bg-orange-1">
      <div class="q-table-middle scroll">
        <table class="q-table q-table--cell-separator q-table--bordered wrap">
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr class style="text-align: left">
            <th :style="cols[0]?.headerStyle">{{ cols[0]?.label }}</th>
            <th :style="cols[1]?.headerStyle">{{ cols[1]?.label }}</th>
            <th :style="cols[2]?.headerStyle">{{ cols[2]?.label }}</th>
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
                  <q-icon :name="getIcon(item)" />
                  {{ item.cod }}
                </span>
            </td>
            <td :data-th="cols[1]?.name">{{ item.name }}</td>
            <td :data-th="cols[2]?.name">{{ item.fullName }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { collapsAll, expandAll, hasTarget, notifyError, pack } from "@/utils/jsutils";
import UpdaterPropEntityValTree from "@/pages/prop/values/UpdaterPropEntityValTree.vue";

const props = defineProps({
  entityType: [Number, String],
});

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const rows = ref([]);
const cols = ref([]);
const loading = ref(false);
const separator = ref("cell");
const isExpanded = ref(true);
const selectedRowID = ref({});
const currentNode = ref(null);
const itemId = ref(null);
const propId = ref(null);
const dense = ref(true);
const lang = ref("");

const getIcon = (row) => {
  if (row.parent) return "";
  else return "folder";
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const editData = () => {
  $q.dialog({
    component: UpdaterPropEntityValTree,
    componentProps: {
      prop: propId.value,
      entityType: props.entityType,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      fetchData(propId.value);
    }
  });
};

const fetchData = (prop) => {
  loading.value = true;

  api
    .post("", {
      method: "prop/loadPropValEntity",
      params: [prop, props.entityType],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = error.response.data.error.message;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
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
    headerStyle: "font-size: 1.2em; width: 35%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 45%",
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

  if (
    !item.leaf &&
    item.expend === undefined &&
    item.children !== undefined
  ) {
    if (item.children.length !== 0) {
      recursive(item.children, [], item.level + 1, item.id, true);
    }
  }
  if (item.expend && item.children !== undefined) {
    item.children.forEach((o) => {
      o.expend = undefined;
    });

    item["expend"] = ref(undefined);
    item["leaf"] = ref(false);
    itemId.value = null;
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
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  propId.value = parseInt(route.params.prop, 10);
  fetchData(propId.value);
});
</script>

<style scoped></style>
