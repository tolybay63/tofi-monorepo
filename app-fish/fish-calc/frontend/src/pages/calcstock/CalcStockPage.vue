<template>
  <div class="q-pa-md" style="height: calc(100vh - 180px)">

    <q-splitter
      v-model="splitterModel"
      :limits="[30, 100]"
      after-class="overflow-hidden q-pl-sm"
      before-class="overflow-hidden q-pr-sm"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <q-banner class="bg-green-1" dense inline-actions>
          <div style="font-size: 1.2em; font-weight: bold">
            <q-avatar color="black" icon="code" text-color="white"></q-avatar>
            {{ $t("calcStock") }}
          </div>

<!--          <template v-slot:action>
            <q-btn
              class="q-ml-sm"
              color="secondary"
              dense
              icon="expand_more"
              @click="fnExpand()"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ tr("expandAll") }}
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
                {{ tr("collapseAll") }}
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
                {{ tr("create1level") }}
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
                {{ tr("createChild") }}
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
                {{ tr("editRecord") }}
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
                {{ tr("deletingRecord") }}
              </q-tooltip>
            </q-btn>
          </template>-->
        </q-banner>

        <div class="q-pa-md-md">
          {{ tr("selectedCalc") }}:
          <span :class="clsNodeInfo()"> {{ nodeInfo() }} </span>
        </div>

        <div
          class="q-table-container q-table--dense wrap bg-amber-1 scroll sticky-header-table"
          style="height: 100%; width: 100%"
        >
          <table class="q-table q-table--cell-separator wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr>
              <th :style="columns[0]?.headerStyle">{{ columns[0]?.label }}</th>
            </tr>
            </thead>

            <tbody>
            <tr v-for="(item, index) in arrayTreeObj" :key="index">
              <td :data-th="columns[0]?.name" @click="toggle(item, index)">
                  <span
                    :style="setPadding(item)"
                    class="q-tree-link q-tree-label"
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

                    <q-chip :color="fnColor(item)" class="cursor-pointer">
                      <q-menu auto-close context-menu>
                        <q-list>
                          <div v-if="item['iscls']">
                            <q-item clickable>
                              <q-item-section @click="showMenu(item, 'ins')">
                                Создать главный расчет
                              </q-item-section>
                             </q-item>
                          </div>

                          <div v-else>
                            <q-item clickable>
                              <q-item-section @click="showMenu(item, 'ins')">
                                Создать дочерний расчет
                              </q-item-section>
                             </q-item>
                             <q-item clickable>
                              <q-item-section @click="showMenu(item, 'upd')">
                                Редактировать расчет
                              </q-item-section>
                             </q-item>
                             <q-item clickable>
                              <q-item-section @click="showMenu(item, 'del')">
                                Удалить расчет
                              </q-item-section>
                             </q-item>

                          </div>

                        </q-list>
                      </q-menu>
                          {{ item.name }}
                        <q-tooltip>
                          Используйте контекстное меню
                        </q-tooltip>
                    </q-chip>
                  </span>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template v-slot:after>

<props-main-page></props-main-page>

      </template>

    </q-splitter>


  </div>
</template>

<script setup>
import {computed, getCurrentInstance, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {useQuasar} from "quasar";
import {collapsAll, expandAll, findRowForId, hasTarget, notifyError, notifyInfo, pack} from '@/utils/jsutils'

import {api} from "@/boot/axios";
import UpdaterCalcStock from "./UpdaterCalcStock.vue";
import PropsMainPage from "@/pages/calcstock/PropsMainPage.vue";


const $q = useQuasar()
const { proxy } = getCurrentInstance()
const router = useRouter();


const splitterModel = ref(50)
const isExpanded = ref(true);
const selected = ref([]);
const currentNode = ref(null);
const itemId = ref(null);
const columns = ref([]);
const table = ref([]);
const loading = ref(false);

const fnColor = (item) => {
  if (item["iscls"]) {
    if (item["ind"] === 1)
      return "orange-3";
    else
      return "blue-3";
  } else {
    if (item["ind"] === 1)
      return "orange-3";
    else
      return "blue-3";
  }
}

const clsNodeInfo = () => {
  if (currentNode.value) {
    if (currentNode.value["ind"] === 1)
      return "text-bold text-orange";
    else
      return "text-bold text-blue";
  }
}

const showMenu = (item, mode) => {
  console.log("item", item);
  let data = {cls: item["cls"]}
  let isChild = false;
  let parentName = ""
  if (!item["iscls"]) {
    if (mode === "ins") {
      parentName = item.name
      data.parent = item.id
      isChild = true;
      //console.info("ins", item);
    } else if (mode === "upd") {
      data.id = item.id
      data.name = item.name
      data.parent = item.parent
      if (item.parent) {
        isChild = true;
        const recPrt = findRowForId(table.value, parseInt(item.parent, 10))
        if (recPrt) {
          parentName = recPrt.name;
        }
      }
      //console.info("upd", item)
    } else if (mode === "del") {
      data.id = item.id
    } else {
      notifyError("Не известный режим")
    }
  }
  if (mode === "del") {
    fnDel(item)
  } else {
    $q.dialog({
      component: UpdaterCalcStock,
      componentProps: {
        mode: mode,
        isChild: isChild,
        parentName: parentName,
        data: data,
      },
    })
      .onOk(() => {
        fetchData();
        fnExpand();
      });
  }
}

const tr = (item) => {
  return proxy?.$t(item)
}

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

const fnDel = (rec) => {
  $q["dialog"]({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$n("deleteRecord") +
      '<div style="color: plum">(' +
      rec.name +
      ")</div>",
    html: true,
    cancel: true,
    persistent: true,
  })
    .onOk(() => {
      api
        .post("", {
          method: "data/deleteCalc",
          params: [rec.id],
        })
        .then(() => {
          fetchData();
          fnExpand();
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
      method: "data/loadCalc",
      params: ["Typ_Stock"],
    })
    .then(
      (response) => {
        table.value = pack(response.data.result["records"], "id");
      },
      (error) => {
        router["push"]("/");
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data?.error?.message);
        console.error(msg);
      }
    )
    .finally(() => {
      fnExpand();
      loading.value = false;
    });
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("nameCalc"),
    field: "name",
    align: "left",
    classes: "text-bold text-white bg-blue-grey-13",
    headerStyle: "font-size: 1.2em; width:100%",
  }
];

const nodeInfo = () => {
  let res = "";
  if (currentNode.value) {
    res = currentNode.value.name;
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
/*
.img-vert {
  transform: scaleY(-1);
  filter: "FlipV";
  -ms-filter: "FlipV";
}
*/

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
