<template>
  <div class="q-pa-sm-sm" style="height: calc(100vh - 270px)">
    <q-banner class="bg-amber-1" dense inline-actions>
      <div style="font-size: 1.2em; font-weight: bold">
        {{ $t("role_privileges") }}
      </div>

      <template v-slot:action>
        <q-btn color="secondary" dense icon="expand_more" @click="fnExpand()">
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("expandAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          class="q-ml-sm"
          color="secondary"
          dense
          icon="expand_less"
          @click="fnCollapse()"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:role:sel:priv')"
          class="q-ml-sm"
          color="secondary"
          dense
          icon="edit_note"
          @click="fnEdit()"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("update") }}
          </q-tooltip>
        </q-btn>

        <q-inner-loading :showing="loading" color="secondary" />
      </template>
    </q-banner>

    <div
      class="q-table-container q-table--dense wrap bg-amber-1 sticky-header-table"
      style="height: 100%; width: 100%"
    >
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr style="text-align: left">
          <th :style="columns[0]?.headerStyle">{{ columns[0]?.label }}</th>
          <th :style="columns[1]?.headerStyle">{{ columns[1]?.label }}</th>
        </tr>
        </thead>

        <tbody style="background: aliceblue; height: 100%">
        <tr v-for="(item, index) in arrayTreeObj" :key="index">
          <td
            :data-th="columns[0]?.name"
            style="width: 20%"
            @click="toggle(item, index)"
          >
              <span class="q-tree-link q-tree-label" :style="setPadding(item)">
                <q-icon
                  :name="iconName(item)"
                  color="secondary"
                  style="cursor: pointer"
                />
                {{ item.text }}
              </span>
          </td>
          <td :data-th="columns[1]?.name">
            {{ fnAL(item.accessLevel) }}
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { collapsAll, expandAll, hasTarget, pack } from "@/utils/jsutils.js";

import UpdaterRolePermis from "@/pages/roles/UpdaterRolePermis.vue";
import {api} from "@/boot/axios.js";


const $q = useQuasar();
const route = useRoute();
const { proxy } = getCurrentInstance();

const roleId = ref(0);
const FDAccessLevel = ref({});
const isExpanded = ref(true);
const itemId = ref(null);
const columns = ref([]);
const table = ref([]);
const loading = ref(false);

const fnAL = (val) => {
  return FDAccessLevel.value ? FDAccessLevel.value[val] : null;
};

const fnExpand = () => {
  expandAll(table.value);
};

const fnCollapse = () => {
  collapsAll(table.value);
};

const fetchData = (role) => {
  loading.value = true;
  api
    .post("", {
      method: "role/loadRolePermis",
      params: [role],
    })
    .then((response) => {
      table.value = pack(response.data.result.records, "ord");
    })
    .finally(() => {
      fnExpand();
      loading.value = false;
    });
};

const fnEdit = () => {
  $q.dialog({
    component: UpdaterRolePermis,
    componentProps: {
      role: roleId.value,
      dense: true,
    },
  }).onOk(() => {
    fetchData(roleId.value);
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
    item.expend = undefined;
    item.leaf = false;
    itemId.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const getColumns = () => [
  {
    name: "text",
    label: proxy?.$t("fldName"),
    field: "text",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle:
      "font-size: 1.2em; background: bg-blue-grey-13; text-align: left; width:70%",
  },
  {
    name: "accessLevel",
    label: proxy?.$t("accessLevel"),
    field: "accessLevel",
    classes: "bg-blue-grey-1",
    headerStyle:
      "font-size: 1.2em; background: bg-blue-grey-13; text-align: left; width:30%",
  },
];

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(table.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  columns.value = getColumns();
  roleId.value = route.params.role;
  fetchData(roleId.value);

  loading.value = true;
  api
    .post("", {
      method: "dict/loadDict",
      params: ["FD_AccessLevel"],
    })
    .then((response) => {
      FDAccessLevel.value = response.data.result;
    })
    .finally(() => {
      loading.value = false;
    });
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
