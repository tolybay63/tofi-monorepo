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
        <div>{{ $t("createGroupRecords") }}</div>
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
        <q-space />

        <q-select
          autofocus
          v-model="dbType"
          :options="optionsDB"
          :label="$t('db')"
          option-value="id"
          option-label="name"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectDB"
        />

        <q-space />
        <q-card-actions align="right">
          <q-btn
            :loading="loading"
            dense
            color="secondary"
            icon="save"
            :label="$t('save')"
            :disable="validSave()"
            @click="onOKClick"
          >
            <template #loading>
              <q-spinner-hourglass color="white" />
            </template>
          </q-btn>

          <q-btn
            dense
            color="secondary"
            icon="cancel"
            :label="$t('cancel')"
            @click="onDialogCancel"
          />
        </q-card-actions>
      </q-bar>

      <div class="q-table--dense wrap bg-orange-1 scroll" style="height: 90%">
        <q-card-section>
          <table class="q-table q-table--cell-separator q-table--bordered wrap">
            <thead class="text-bold text-white bg-blue-grey-13">
            <tr class style="text-align: left">
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
              <td :data-th="cols0[0].name" @click="toggle(item, index)">
                  <span class="q-tree__node" :style="setPadding(item)">
                    <q-icon
                      style="cursor: pointer"
                      :name="iconName(item)"
                      color="secondary"
                    />
                    <q-btn
                      dense
                      flat
                      :color="!item.parent ? 'gray' : 'blue'"
                      :icon="
                        item.checked ? 'check_box' : 'check_box_outline_blank'
                      "
                      @click.stop="updateCheck(item)"
                    />

                    <q-icon
                      v-if="item.isOwn !== -1"
                      :name="getIconName(item)"
                      :color="getIconColor(item)"
                    />

                    {{ item[cols0[0].field] }}
                  </span>
              </td>

              <td v-for="(col, i) in cols_" :data-th="col.name" :key="i">
                {{ item[col.field] }}
              </td>
            </tr>
            </tbody>
          </table>
        </q-card-section>
      </div>
      <div>
        <q-bar style="height: 16px" />
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance, reactive } from 'vue'
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import {
  checkChilds,
  collapsAll,
  expandAll,
  notifyError,
  notifySuccess,
  pack,
  uncheckChilds,
} from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

let checkedMembers = [];
const getCheckets = (node) => {
  if (node.parent && node.checked && !checkedMembers.includes(node.ent))
    checkedMembers.push({ memType: node.memberType, card: node.card, ent: node.ent });
  const children = node.children || [];
  children.forEach(getCheckets);
};

const form = reactive({ ...props.data });
const loading = ref(false);
const cols = ref([]);
const rows = ref([]);
const isExpanded = ref(false);
const itemId = ref(null);
const sizeRTM = ref(0);

const dbType = ref(null);
const optionsDB = ref([]);

const fnSelectDB = (val) => {
  console.info("dbType, val", dbType.value, val);
};

const updateCheck = (item) => {
  if (item.level === 0) {
    if (item.children && item.children.length > 0) {
      if (!item.checked) checkChilds(item);
      else uncheckChilds(item);
    }
  } else {
    item.checked = !item.checked;
  }
};

const getIconName = (row) => {
  if (row.isOwn === 1) return "brightness_1";
  else if (row.isOwn === -2) return "brightness_7";
};

const getIconColor = (row) => {
  if (row.isOwn === 1) return "green";
  else if (row.isOwn === 0) return "dark-gray";
  else return "orange";
};

const fnExpand = () => {
  expandAll(rows.value);
};

const fnCollapse = () => {
  collapsAll(rows.value);
};

const recursive = (obj, newObj, level, targetItemId, isExpend) => {
  obj.forEach((o) => {
    if (o.children && o.children.length !== 0) {
      o.level = level;
      o.leaf = false;
      newObj.push(o);
      if (o.id === targetItemId) {
        o.expend = isExpend;
      }
      if (o.expend === true) {
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
  if (item.expend === true) {
    return "remove_circle_outline";
  }
  if (item.children && item.children.length > 0) {
    return "control_point";
  }
  return "";
};

const toggle = (item) => {
  if (item.children && item.children.length > 0) {
    if (item.expend) {
      item.expend = false;
      item.leaf = true;
    } else {
      item.expend = true;
      item.leaf = false;
    }
  }
};

const setPadding = (item) => {
  return `padding-left: ${item.level * 30}px;`;
};

const validSave = () => {
  if (sizeRTM.value < 2) return true;
  let rez = [];
  for (let i = 0; i < sizeRTM.value; i++) {
    checkedMembers = [];
    getCheckets(rows.value[i]);
    rez.push(checkedMembers);
  }
  let b = false;
  rez.forEach((it) => {
    if (it.length === 0) b = true;
  });
  return b && dbType.value?.id !== null;
};

const onOKClick = () => {
  loading.value = true;
  let err = false;
  let rez = [];
  for (let i = 0; i < sizeRTM.value; i++) {
    checkedMembers = [];
    getCheckets(rows.value[i]);
    rez.push(checkedMembers);
  }

  api
    .post("", {
      method: "relcls/createGroupRelCls",
      params: [form.relTyp, rez, dbType.value.id],
    })
    .then(
      () => {
        err = false;
        onDialogOK({ res: true });
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let errMsg = error.response?.data?.error?.message || error.message;
        notifyError(errMsg);
      }
    )
    .finally(() => {
      loading.value = false;
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    headerStyle: "font-size: 1.2em; width:35%;",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    headerStyle: "font-size: 1.2em;width:50%;",
  },
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    headerStyle: "font-size: 1.2em; width:15%;",
  },
];

const cols0 = computed(() => cols.value.slice(0, 1));
const cols_ = computed(() => cols.value.slice(1));

const arrayTreeObj = computed(() => {
  let newObj = [];
  recursive(rows.value, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

onMounted(() => {
  cols.value = getColumns();

  loading.value = true;
  api
    .post("", {
      method: "relcls/loadAllMembers",
      params: [form],
    })
    .then((response) => {
      rows.value = pack(response.data.result.records, "ord");
      sizeRTM.value = rows.value.length;
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "database/loadDbForSelect",
      params: [],
    })
    .then((response) => {
      optionsDB.value = response.data.result.records;
      dbType.value = optionsDB.value[0];
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
