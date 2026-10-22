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
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-bar style="height: 48px">
        <q-btn
          :dense="dense"
          icon="expand_more"
          color="secondary"
          @click="fnExpand"
          style="margin-bottom: 3px"
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
          @click="fnCollapse"
          style="margin-bottom: 3px; margin-left: 5px"
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
            icon="navigate_next"
            :label="$t('next')"
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
        class="q-table-container q-table--dense wrap bg-orange-1 scroll"
        style="height: 90%"
      >
        <table class="q-table q-table--cell-separator q-table--bordered wrap">
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
            <!--name-->
            <td :data-th="cols[0]?.name" @click="toggle(item)">
                <span class="q-tree__node" :style="setPadding(item)">
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
                      item.checked
                        ? 'check_box'
                        : 'check_box_outline_blank'
                    "
                    @click.stop="selectedCheck(item)"
                  />

                  {{ item[cols[0]?.field] }}
                </span>
            </td>

            <!--fullName-->
            <td :data-th="cols[1]?.name">
              {{ item[cols[1]?.field] }}
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
import { api } from "@/boot/axios";
import { checkChilds, collapsAll, expandAll, notifyError, pack, uncheckChilds } from "@/utils/jsutils";
import UpdaterMeterSoftRatesSave from "@/pages/meter/UpdaterMeterSoftRatesSave.vue";

const props = defineProps({
  meter: [Number, String],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const $q = useQuasar();
const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const isExpanded = ref(true);
const currentNode = ref(null);
const itemId = ref(null);
const sz = ref(0);

const expand = (item) => {
  item.expend = ref(true);
  const { children } = item;
  if (children.length > 0) item.leaf = ref(false);
  else item.leaf = ref(true);
};

const collaps = (item) => {
  item.expend = ref(false);
  const { children } = item;
  if (children.length > 0) {
    item.leaf = ref(false);
  } else {
    item.leaf = ref(true);
    item.expend = undefined;
  }
};

const selectedCheck = (item) => {
  if (item.children && item.children.length > 0) {
    if (!item.checked) checkChilds(item);
    else uncheckChilds(item);
  } else {
    item.checked = !item.checked;
  }
};

const loadData = () => {
  loading.value = true;
  api
    .post("", {
      method: "meterrate/loadMeterSoftForUpd",
      params: [props.meter],
    })
    .then((response) => {
      sz.value = response.data.result.records.length;
      rows.value = pack(response.data.result.records, "id");
      expandAll(rows.value);
    })
    .catch((error) => {
      console.error(error.message);
      notifyError("Для данного измерителя не указаны факторы");
    })
    .finally(() => {
      loading.value = false;
    });
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em; width: 40%",
  },
];

const onOKClick = () => {
  let dta = [];
  const tt = (node, chks) => {
    if (node.checked) {
      chks.push(node);
    }
    let children = node.children;
    if (children && children.length > 0) {
      children.forEach((ch) => tt(ch, chks));
    }
  };

  const getCheckeds = (data, chks) => {
    for (let i = 0; i < data.length; i++) {
      tt(data[i], chks);
    }
  };

  getCheckeds(rows.value, dta);

  let d0 = [];
  dta.forEach((d) => {
    let { ...o } = d;
    o.children = null;
    d0.push(o);
  });

  $q.dialog({
    component: UpdaterMeterSoftRatesSave,
    componentProps: {
      meter: props.meter,
      checkeds: d0,
      dense: true,
    },
  }).onOk((data) => {
    if (data.res) {
      onDialogOK({ res: true });
    }
  });
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
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
  if (item.children && item.children.length > 0) {
    if (item.expend) collaps(item);
    else expand(item);
  }
};

const setPadding = (item) => {
  return `padding-left: ${(item.level || 0) * 30}px;`;
};

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  cols.value = getColumns();
  loadData();
});
</script>

<style scoped></style>
