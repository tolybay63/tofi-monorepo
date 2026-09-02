<template>
  <div class="q-pa-md" style="height: calc(100vh - 180px)">

    <q-splitter
      v-model="splitterModel"
      :limits="[30, 100]"
      after-class="overflow-hidden q-pl-md"
      before-class="overflow-hidden q-pr-md"
      separator-class="bg-red"
      style="height: calc(100vh - 150px); width: 100%"
    >
      <template v-slot:before>
        <q-banner class="bg-green-1" dense inline-actions>
          <div style="font-size: 1.2em; font-weight: bold">
            <q-avatar color="black" icon="code" text-color="white"></q-avatar>
            {{ $t("calcStock") }} <span :class="infoCalcClass()">{{ infoCalc() }}</span>
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
              v-if="hasTarget('calc')"
              class="q-ml-sm"
              color="secondary"
              dense
              icon="post_add"
              @click="fnEdit(null, true, false, 'ins')"
            >
              <q-tooltip transition-hide="rotate" transition-show="rotate">
                {{ tr("create1level") }}
              </q-tooltip>
            </q-btn>
          </template>
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

                <q-menu auto-close context-menu>
                  <q-list>
                    <div>
                      <q-item clickable @click="showMenu(item, 'ins')">
                        <q-item-section avatar>
                          <q-icon avator name="post_add"/>
                        </q-item-section>
                        <q-item-section>
                          Создать дочерний расчет
                        </q-item-section>
                      </q-item>
                      <q-item clickable @click="showMenu(item, 'upd')">
                        <q-item-section avatar>
                          <q-icon name="edit"/>
                        </q-item-section>
                        <q-item-section>
                          Редактировать расчет
                        </q-item-section>
                      </q-item>
                      <q-item clickable @click="showMenu(item, 'del')">
                        <q-item-section avatar>
                          <q-icon color="red" name="delete"/>
                        </q-item-section>
                        <q-item-section>
                          Удалить расчет
                        </q-item-section>
                      </q-item>
                      <q-separator/>
                      <q-item clickable @click="showMenu(item, 'calc')">
                        <q-item-section avatar>
                          <q-icon color="green" name="pan_tool_alt"/>
                        </q-item-section>
                        <q-item-section>
                          Выбор расчета
                        </q-item-section>
                      </q-item>

                    </div>
                  </q-list>
                </q-menu>

                <q-tooltip>
                  Используйте контекстное меню
                </q-tooltip>

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
                      :disable="!hasTarget('calc')"
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
                      {{ item.name }}
                    </q-chip>
                  </span>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template v-slot:after>

          <props-main-page :codCalc="props.codCls" :data="currentNode"></props-main-page>

      </template>

    </q-splitter>


  </div>
</template>

<script setup>
import {computed, getCurrentInstance, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {extend, useQuasar} from "quasar";
import {collapsAll, expandAll, findRowForId, hasTarget, notifyError, notifyInfo, pack} from '@/utils/jsutils'
import {api} from "@/boot/axios";
import UpdaterCalcStock from "./UpdaterCalcStock.vue";
import PropsMainPage from "@/pages/calcstock/PropsMainPage.vue";

const props = defineProps({
  codCls: String
})

const $q = useQuasar()
const {proxy} = getCurrentInstance()
const router = useRouter();

const cls = ref(0)
const splitterModel = ref(50)
const isExpanded = ref(true);
const selected = ref([]);
const currentNode = ref(null);
const itemId = ref(null);
const columns = ref([]);
const rows = ref([]);
const loading = ref(false);

const fnColor = (item) => {
  if (props.codCls === "Cls_CalcDeterm") {
    return "orange-3";
  } else {
    return "blue-3";
  }
}

const infoCalc = () => {
  return props.codCls === "Cls_CalcDeterm"
    ? " (" + proxy?.$t("calcDeterm") + ")" : " (" + proxy?.$t("calcBayes") + ")"
}

const infoCalcClass = () => {
  return props.codCls === "Cls_CalcDeterm"
    ? "text-caption text-orange" : "text-caption text-blue"
}

const clsNodeInfo = () => {
  if (currentNode.value) {
    if (props.codCls === "Cls_CalcDeterm")
      return "text-bold text-orange";
    else
      return "text-bold text-blue";
  } else
    return "text-bold text-black";

}

const showMenu = (item, mode) => {
  let data = {cls: cls.value}
  let isChild = false;

  if (mode === "ins") {
    data.parent = item.id
    isChild = true;
  } else if (mode === "upd") {
    data = extend(true, item, {cls: cls.value})
    isChild = !!item.parent;
  } else {
    data = extend(true, item, {cls: cls.value})
  }

  if (mode === "calc") {
    fnCalc(data)
  } else if (mode === "del") {
    fnDel(data)
  } else if (["ins", "upd"].includes(mode)) {
    fnEdit(data, false, isChild, mode);
  } else {
    notifyError("Не известный режим")
  }
}

const fnDel = (rec) => {
  $q["dialog"]({
    title: proxy?.$t("confirmation"),
    message:
      proxy?.$t("deleteRecord") +
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

const fnEdit = (rec, isMain, isChild, mode) => {
  if (isMain) {
    rec = {cls: cls.value};
    isChild = false;
  }
  let parentName = ""
  if (isChild) {
    const recPrt = findRowForId(rows.value, parseInt(rec.parent, 10))
    if (recPrt) {
      parentName = recPrt.name;
    }
  }
  //
  $q.dialog({
    component: UpdaterCalcStock,
    componentProps: {
      mode: mode,
      isChild: isChild,
      parentName: parentName,
      data: rec,
    },
  })
    .onOk(() => {
      fetchData();
      fnExpand();
    });
}

const fnCalc = (rec) => {
  if (props.codCls === "Cls_CalcDeterm") {
    router["push"]({
      name: 'CalculationDeterm',
      params: {
        id: rec.id,
        title: rec.name,
      }
    })
  } else if (props.codCls === "Cls_CalcBayes") {
    router["push"]({
      name: 'CalculationBayes',
      params: {
        id: rec.id,
        title: rec.name,
      }
    })
  }

};

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
  currentNode.value = selected.value[0] !== null ? selected.value[0] : null;



};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const fetchData = () => {
  loading.value = true;
  api
    .post("", {
      method: "data/loadCalc",
      params: [cls.value],
    })
    .then(
      (response) => {
        rows.value = pack(response.data.result["records"], "id");
      },
      (error) => {
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
  let res = "Для просмотра свойства расчета выберите узел";
  if (currentNode.value) {
    res = currentNode.value.name;
  }
  return res;
};

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  loading.value = true;
  columns.value = getColumns();
  api
    .post("", {
      method: "data/getCls",
      params: [props.codCls],
    })
    .then(
      (response) => {
        cls.value = response.data.result;
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data?.error?.message);
        console.error(msg);
      }
    )
    .finally(() => {
      loading.value = false;
      fetchData();
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
