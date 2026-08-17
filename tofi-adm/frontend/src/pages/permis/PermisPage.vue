<template>
  <div class="q-pa-md" style="height: calc(100vh - 180px)">
    <q-banner class="bg-amber-1" dense inline-actions>
      <div style="font-size: 1.2em; font-weight: bold">
        <q-avatar color="black" icon="code" text-color="white"></q-avatar>
        {{ $t("tml_permis") }}
      </div>

      <template v-slot:action>
        <q-btn color="secondary" dense icon="save" @click="fnSave()">
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("save") }}
          </q-tooltip>
        </q-btn>

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
          v-if="hasTarget('adm:tml:ins')"
          class="q-ml-sm"
          color="secondary"
          dense
          icon="post_add"
          @click="fnIns('ins', false)"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("create1level") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:ins')"
          :disable="currentNode == null"
          class="q-ml-sm img-vert"
          color="secondary"
          dense
          icon="post_add"
          @click="fnIns('ins', true)"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("createChild") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:upd')"
          :disable="currentNode == null"
          class="q-ml-sm"
          color="secondary"
          dense
          icon="edit"
          @click="fnIns('upd', null)"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("editRecord") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          v-if="hasTarget('adm:tml:del')"
          :disable="currentNode == null"
          class="q-ml-sm"
          color="red"
          dense
          icon="delete"
          @click="fnDel(currentNode)"
        >
          <q-tooltip transition-hide="rotate" transition-show="rotate">
            {{ $t("deletingRecord") }}
          </q-tooltip>
        </q-btn>
      </template>
    </q-banner>

    <div class="q-pa-md-md">
      <span style="color: #1976d2"> {{ $t("selectedNode") }}: </span>
      {{ nodeInfo() }}
    </div>

    <div
      class="q-table-container q-table--dense wrap bg-amber-1 scroll sticky-header-table"
      style="height: 100%; width: 100%"
    >
      <table class="q-table q-table--cell-separator q-table--bordered wrap">
        <thead class="text-bold text-white bg-blue-grey-13">
        <tr>
          <th :style="columns[0]?.headerStyle">{{ columns[0]?.label }}</th>
          <th :style="columns[1]?.headerStyle">{{ columns[1]?.label }}</th>
        </tr>
        </thead>

        <tbody style="background: aliceblue;">
        <tr v-for="(item, index) in arrayTreeObj" :key="index">
          <td :data-th="columns[0]?.name" @click="toggle(item, index)">
              <span
                class="q-tree-link q-tree-label"
                :style="setPadding(item)"
              >
                <q-icon
                  :name="iconName(item)"
                  color="secondary"
                  style="cursor: pointer"
                ></q-icon>

                <q-btn
                  :disable="!hasTarget('adm:tml:upd')"
                  :icon="
                    selected.length === 1 && item.id === selected[0].id
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  color="blue"
                  dense
                  flat
                  @click.stop="selectedRow(item)"
                >
                </q-btn>

                {{ item.text }}
              </span>
          </td>
          <!--id-->
          <td :data-th="columns[1]?.id">{{ item.id }}</td>
        </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { collapsAll, expandAll, getParentNode, hasTarget, notifyError, notifyInfo, pack } from "../../utils/jsutils";
import UpdaterPermis from "@/pages/permis/UpdaterPermis.vue";
import { api } from "@/boot/axios";
import { exportFile } from "quasar";

const { proxy } = getCurrentInstance();
const $q = useQuasar();
const route = useRoute();
const router = useRouter();

const isExpanded = ref(true);
const selected = ref([]);
const currentNode = ref(null);
const itemId = ref(null);
const columns = ref([]);
const table = ref([]);
const loading = ref(false);

const fnSave = () => {
  const data = JSON.stringify(table.value);
  const status = exportFile("important.txt", data);
  if (status === true) {
    console.log("Ok: " + status);
  } else {
    console.log("Error: " + status);
  }
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

const selectedRow = (item) => {
  if (selected.value.length > 0 && item.id === selected.value[0].id)
    selected.value = [];
  else {
    selected.value = [];
    selected.value.push(item);
  }
  currentNode.value = selected.value[0] !== undefined ? selected.value[0] : null;
};

const fnExpand = () => {
  expandAll(table.value);
};

const fnCollapse = () => {
  collapsAll(table.value);
};

const fnIns = (mode, isChild) => {
  let data = {
    id: "",
    text: "",
  };

  let parent = null;
  let parentName = null;
  if (isChild) {
    parent = currentNode.value.id;
    parentName = currentNode.value.text;
  }
  if (mode === "ins") {
    data.parent = parent;
  } else if (mode === "upd") {
    data = {
      id: currentNode.value.id,
      parent: currentNode.value.parent,
      text: currentNode.value.text,
    };
    if (currentNode.value.parent > 0) {
      let parentNode = [];
      getParentNode(table.value, currentNode.value.parent, parentNode);
      parentName = parentNode[0].text;
      isChild = true;
    }
  }

  $q.dialog({
    component: UpdaterPermis,
    componentProps: {
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      data: data,
    },
  })
    .onOk((respData) => {
      fetchData();
      selected.value.push(respData);
      selectedRow(respData);
      fnExpand();
    });
};

const fnDel = (rec) => {
  $q.dialog({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
      '<div style="color: plum">(' +
      rec.text +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
  })
    .onOk(() => {
      api
        .post("", {
          method: "permis/delete",
          params: [rec],
        })
        .then(() => {
          fetchData();
          currentNode.value = null;
        });
    })
    .onCancel(() => {
      notifyInfo(proxy?.$t("canceled"));
    });
};

const fetchData = () => {
  loading.value = true;
  api
    .post("", {
      method: "permis/load",
      params: [],
    })
    .then(
      (response) => {
        table.value = pack(response.data.result.records, "ord");
      },
      (error) => {
        router.push("/");
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data.error.message);
        notifyError(msg);
      }
    )
    .finally(() => {
      fnCollapse();
      loading.value = false;
    });
};

const getColumns = () => [
  {
    name: "text",
    label: proxy?.$t("fldName"),
    field: "text",
    align: "left",
    classes: "text-bold text-white bg-blue-grey-13",
    headerStyle: "font-size: 1.2em; width:70%",
  },
  {
    name: "id",
    label: proxy?.$t("target"),
    field: "id",
    classes: "text-bold text-white bg-blue-grey-13",
    headerStyle: "font-size: 1.2em; width:30%",
  },
];

const nodeInfo = () => {
  let res = "";
  if (currentNode.value) {
    res = currentNode.value.text;
  }
  return res;
};

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(table.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  columns.value = getColumns();
  fetchData();
});
</script>

<style scoped>
.img-vert {
  transform: scaleY(-1);
  filter: "FlipV";
  -ms-filter: "FlipV";
}
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
