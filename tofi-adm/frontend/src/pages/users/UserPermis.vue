<template>
  <div class="q-pa-sm-sm"  style="height: calc(100vh - 270px)">
    <q-banner class="bg-amber-1" dense inline-actions>
      <div style="font-size: 1.2em; font-weight: bold">
        {{ $t("roles2") }}
      </div>

      <template v-slot:action>
        <q-btn
          class="q-ml-sm"
          color="secondary"
          dense
          icon="expand_more"
          @click="fnExpand()"
        >
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
          v-if="hasTarget('adm:usr:gr:usr:sel:priv')"
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

        <q-inner-loading :showing="loading" color="secondary"/>
      </template>
    </q-banner>

    <div
      class="q-table-container q-table--dense wrap bg-amber-1 sticky-header-table"
      style="height: 100%; width: 100%"
    >
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr class style="text-align: left">
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
                  <span
                    class="q-tree-link q-tree-label"
                    :style="setPadding(item)"
                  >
                    <q-icon
                      :name="iconName(item)"
                      color="secondary"
                      style="cursor: pointer"
                    ></q-icon>

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
import {computed, getCurrentInstance, onMounted, ref} from "vue";
import {useRoute} from "vue-router";
import {useQuasar} from "quasar";
import {collapsAll, expandAll, hasTarget, pack} from "../../utils/jsutils";
import {api} from "@/boot/axios";
import UpdaterUserPermis from "@/pages/users/UpdaterUserPermis.vue";

const { proxy } = getCurrentInstance();
const $q = useQuasar();
const route = useRoute();

const user_id = ref(0);
const userName = ref("");
const FD_AccessLevel = ref({});
const isExpanded = ref(true);
const itemId = ref(null);
const columns = ref([]);
const table = ref([]);
const loading = ref(false);

const fnAL = (val) => {
  return FD_AccessLevel.value ? FD_AccessLevel.value[val] : null;
};

const fetchData = (user) => {
  loading.value = true;
  api
    .post("", {
      method: "usr/loadUserPermis",
      params: [user],
    })
    .then((response) => {
      table.value = pack(response.data.result.records, "ord");
    })
    .finally(() => {
      fnExpand();
      loading.value = false;
    });
};

const loadUser = (user) => {
  loading.value = true;
  api
    .post("", {
      method: "usr/loadUser",
      params: [user],
    })
    .then((response) => {
      userName.value = response.data.result.records[0].fullName;
    })
    .finally(() => {
      fnExpand();
      loading.value = false;
    });
};

const fnEdit = () => {
  $q.dialog({
    component: UpdaterUserPermis,
    componentProps: {
      user: user_id.value,
      userName: userName.value,
      dense: true,
    },
  }).onOk(() => {
    fetchData(user_id.value);
  });
};

const recursive = (obj, newObj, level, currentItemId, isExpend) => {
  if (!obj) return;
  obj.forEach(function (o) {
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
    item.children.forEach(function (o) {
      o.expend = undefined;
    });
    item["expend"] = undefined;
    item["leaf"] = false;
    itemId.value = null;
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const fnExpand = () => {
  expandAll(table.value);
};

const fnCollapse = () => {
  collapsAll(table.value);
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
  let newObj = [];
  recursive(table.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  user_id.value = route.params.user;
  loadUser(user_id.value);
  fetchData(user_id.value);

  columns.value = getColumns();
  loading.value = true;
  api
    .post("", {
      method: "dict/loadDict",
      params: ["FD_AccessLevel"],
    })
    .then((response) => {
      FD_AccessLevel.value = response.data.result;
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped>
.sticky-header-table {
  max-height: 100%;
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
