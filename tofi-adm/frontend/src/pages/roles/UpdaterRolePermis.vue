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
        class="q-pa-sm-sm q-table-container wrap bg-orange-1 scroll sticky-header-table"
        style="height: 90%"
      >
        <table
          class="q-pa-sm-sm q-table q-table--dense q-table--cell-separator q-table--bordered"
        >
          <thead class="text-bold text-white bg-blue-grey-13">
          <tr style="text-align: left">
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
            <!--text-->
            <td :data-th="cols[0]?.name" @click="toggle(item, index)">
                <span class="q-tree__node" :style="setPadding(item)">
                  <q-icon
                    style="cursor: pointer"
                    :name="iconName(item)"
                    color="secondary"
                  />

                  <q-btn
                    dense
                    flat
                    :color="item.children?.length > 0 ? 'gray' : 'blue'"
                    :icon="
                      item.checked ? 'check_box' : 'check_box_outline_blank'
                    "
                    @click.stop="selectedCheck(item)"
                  />

                  {{ item[cols[0]?.field] }}
                </span>
            </td>

            <!--accessLevel-->
            <td :data-th="cols[1]?.name">
              <q-btn
                dense
                flat
                size="sm"
                color="blue"
                @click.stop="updAL(item)"
                :disable="!item.checked"
                :icon="!item.checked ? '' : 'edit'"
              />
              {{ fnAL(item[cols[1]?.field]) }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div>
        <q-bar>{{ $t("countAll") }}: {{ sz }}</q-bar>
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent, useQuasar } from "quasar";
import { collapsAll, expandAll, notifyError, pack } from "@/utils/jsutils";
import UpdateAccessLevel from "@/pages/roles/UpdateAccessLevel.vue";
import {api} from "@/boot/axios.js";


const props = defineProps({
  role: [Number, String],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const $q = useQuasar();
const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();

let leaf = [];

const cols = ref([]);
const rows = ref([]);
const FDAccessLevel = ref({});
const loading = ref(false);
const isExpanded = ref(true);
const itemId = ref(null);
const sz = ref(0);

const expand = (item) => {
  item.expend = true;
  const children = item.children || [];
  item.leaf = children.length === 0;
};

const collaps = (item) => {
  item.expend = false;
  const children = item.children || [];
  if (children.length > 0) {
    item.leaf = false;
  } else {
    item.leaf = true;
    item.expend = undefined;
  }
};

const checkChilds = (node) => {
  node.checked = true;
  const children = node.children || [];
  children.forEach(checkChilds);
};

const uncheckChilds = (node) => {
  node.checked = false;
  const children = node.children || [];
  children.forEach(uncheckChilds);
};

const checkNode = (node) => {
  if (leaf.includes(node.id)) node.checked = true;
  const children = node.children || [];
  children.forEach(checkNode);
};

const getNode = (data) => {
  for (let i = 0; i < data.length; i++) {
    checkNode(data[i]);
  }
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

const updAL = (row) => {
  $q.dialog({
    component: UpdateAccessLevel,
    componentProps: {
      data: row,
      dense: true,
    },
  }).onOk((data) => {
    for (let key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        row[key] = data[key];
      }
    }
  });
};

const fnAL = (val) => {
  return FDAccessLevel.value ? FDAccessLevel.value[val] : null;
};

const selectedCheck = (item) => {
  if (item.children && item.children.length > 0) {
    if (item.checked === false) checkChilds(item);
    else uncheckChilds(item);
  } else {
    item.checked = !item.checked;
  }

  if (item.checked && item.parent !== undefined) {
    api
      .post("", {
        method: "permis/getLeaf",
        params: [item.id],
      })
      .then((response) => {
        leaf = response.data.result;
      })
      .finally(() => {
        getNode(rows.value);
      });
  }
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
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
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const getColumns = () => [
  {
    name: "text",
    label: proxy?.$t("fldName"),
    field: "text",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 70%",
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

const loadData = () => {
  loading.value = true;
  api
    .post("", {
      method: "role/loadRolePermisForUpd",
      params: [props.role],
    })
    .then((response) => {
      sz.value = response.data.result.records.length;
      rows.value = pack(response.data.result.records, "ord");
      expandAll(rows.value);
    })
    .finally(() => {
      loading.value = false;
    });
};

const onOKClick = () => {
  loading.value = true;
  const dta = [];

  const tt = (node, chks) => {
    if (node.checked) {
      chks.push(node);
    }
    const children = node.children || [];
    if (children.length > 0) {
      children.forEach((ch) => tt(ch, chks));
    }
  };

  for (let i = 0; i < rows.value.length; i++) {
    tt(rows.value[i], dta);
  }

  const d0 = dta.map((d) => {
    const o = { ...d };
    o.children = null;
    return o;
  });

  api
    .post("", {
      method: "role/saveRolePermis",
      params: [{ role: props.role, data: d0 }],
    })
    .then(() => {
      onDialogOK({ res: true });
    })
    .catch((error) => {
      notifyError(error.message);
    })
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  cols.value = getColumns();

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

  loadData();
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
