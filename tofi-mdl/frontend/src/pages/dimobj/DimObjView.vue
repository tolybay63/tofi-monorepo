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
        <div>{{ $t("view") }}</div>
      </q-bar>

      <q-bar class="bg-orange-1" style="height: 48px">
        <q-btn
          dense
          icon="expand_more"
          color="secondary"
          @click="fnExpand"
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
          @click="fnCollapse"
          style="margin-bottom: 5px; margin-left: 5px"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("collapseAll") }}
          </q-tooltip>
        </q-btn>

        <q-input
          v-if="cmdDate"
          v-model="dt"
          stack-label
          type="date"
          dense
          style="width: 100px; margin-left: 20px"
          :label="$t('date')"
          @update:model-value="fnDt"
        />

        <q-space />
        <q-card-actions align="right">
          <q-btn
            dense
            color="secondary"
            icon="cancel"
            :label="$t('close')"
            @click="onDialogCancel"
          />
        </q-card-actions>
      </q-bar>
      <q-inner-loading :showing="loading" color="secondary" />

      <div
        class="q-table-container q-table--dense wrap bg-orange-1 scroll"
        style="height: 89%"
      >
        <div class="q-pa-sm-sm bg-orange-1">
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
              <!--cod-->
              <td :data-th="cols[0]?.name" @click="toggle(item)">
                  <span class="q-tree__node" :style="setPadding(item)">
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    />

                    <q-icon :name="getIcon()" />
                    {{ item[cols[0]?.field] }}
                  </span>
              </td>

              <!--name-->
              <td :data-th="cols[1]?.name">
                {{ item[cols[1]?.field] }}
              </td>

              <!--dbeg-->
              <td :data-th="cols[2]?.name">
                {{ item[cols[2]?.field] }}
              </td>

              <!--dend-->
              <td :data-th="cols[3]?.name">
                {{ item[cols[3]?.field] }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div>
        <q-bar>{{ $t("countAll") }}: {{ sz }}</q-bar>
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api, tofi_dbeg, tofi_dend } from "@/boot/axios";
import { collapsAll, expandAll, pack } from "@/utils/jsutils";
import { date } from "quasar";

const props = defineProps({
  dimobj: [Number, String],
  lg: String,
  cmdDate: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const lang = ref(props.lg);
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const isExpanded = ref(true);
const currentNode = ref(null);
const itemId = ref(null);
const sz = ref(0);

const today = () => {
  let d = new Date();
  let currDate = d.getDate();
  let currMonth = d.getMonth() + 1;
  let currYear = d.getFullYear();
  return (
    currYear +
    "-" +
    (currMonth < 10 ? "0" + currMonth : currMonth) +
    "-" +
    (currDate < 10 ? "0" + currDate : currDate)
  );
};

const dt = ref(today());

const expand = (item) => {
  item.expend = true;
  const { children } = item;
  if (children && children.length > 0) item.leaf = false;
  else item.leaf = true;
};

const collaps = (item) => {
  item.expend = false;
  const { children } = item;
  if (children && children.length > 0) {
    item.leaf = false;
  } else {
    item.leaf = true;
    item.expend = undefined;
  }
};

const loadData = (params) => {
  loading.value = true;
  api
    .post("", {
      method: "dimobj/loadTreeForView",
      params: [params],
    })
    .then((response) => {
      sz.value = response.data.result.records.length;
      rows.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnDt = (val) => {
  loadData({ dimobj: props.dimobj, currDate: val });
};

const getIcon = () => {
  return "date_range";
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code"),
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 70%",
  },
  {
    name: "dbeg",
    label: proxy?.$t("fldDbeg"),
    field: "dbeg",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
    format: (val) => (val <= tofi_dbeg ? "..." : date.formatDate(val, "DD.MM.YYYY")),
  },
  {
    name: "dend",
    label: proxy?.$t("fldDend"),
    field: "dend",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em;",
    style: "width: 10%",
    format: (val) => (val >= tofi_dend ? "..." : date.formatDate(val, "DD.MM.YYYY")),
  },
];

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
  if (item.expend) return "remove_circle_outline";
  if (item.children && item.children.length > 0) return "control_point";
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
  let currDtStr = today().toString();
  loadData({ dimobj: props.dimobj, currDate: currDtStr });
});
</script>

<style scoped></style>
