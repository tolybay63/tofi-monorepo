<template>
  <q-bar class="q-pa-md bg-amber-1" style="height: 50px">
    <q-btn
      dense
      icon="expand_more"
      color="secondary"
      @click="fnExpand()"
      style="margin-bottom: 5px"
      size="16px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("expandAll") }}
      </q-tooltip>
    </q-btn>
    <q-btn
      dense
      icon="expand_less"
      color="secondary"
      class="q-ml-sm q-mr-lg"
      @click="fnCollapse()"
      style="margin-bottom: 5px"
      size="16px"
    >
      <q-tooltip transition-show="rotate" transition-hide="rotate">
        {{ $t("collapseAll") }}
      </q-tooltip>
    </q-btn>

    <div v-if="showAction">
      <q-btn
        dense
        icon="save"
        color="secondary"
        class="q-ml-lg"
        :loading="loading"
        :disable="btnDisabled()"
        @click="fnBind()"
        style="margin-bottom: 5px"
        :label="$t('bind')"
        no-caps
        size="16px"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("bind") }}
        </q-tooltip>

        <template #loading>
          <q-spinner-hourglass color="white" />
        </template>
      </q-btn>

      <q-btn
        dense
        icon="delete"
        color="secondary"
        class="q-ml-lg"
        :loading="loading"
        :disable="!btnDisabled()"
        @click="fnUnBind()"
        style="margin-bottom: 5px"
        :label="$t('delbind')"
        no-caps
        size="16px"
      >
        <q-tooltip transition-show="rotate" transition-hide="rotate">
          {{ $t("delbind") }}
        </q-tooltip>
        <template #loading>
          <q-spinner-hourglass color="white" />
        </template>
      </q-btn>
    </div>
  </q-bar>

  <div class="q-pa-sm-sm bg-grey-2 scroll" style="height: calc(100vh - 350px); width: 100%">
    <table class="q-table q-table--cell-separator q-table--bordered wrap">
      <thead class="text-bold text-white bg-blue-grey-13">
      <tr>
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
        <td :data-th="cols0[0]?.name" @click="toggle(item)">
          <div class="row">
            <div class="q-tree__node q-ma-xs-xs" :style="setPadding(item)">
              <q-icon
                :name="iconName(item)"
                color="secondary"
                size="16px"
                style="cursor: pointer"
              />
            </div>
            <div class="q-mb-xs" style="align-content: end">
              {{ item[cols0[0]?.field] }}
            </div>
          </div>
        </td>
        <!--other cols without 0-->
        <td v-for="(col, i) in cols_" :key="i" :data-th="col.name">
          <div v-if="col.name === 'val'">

            <TreeSelect
              :disabled="!showSelect"
              v-model="item['val']"
              :label="$t('factorVal')"
              :options="optProp"
              class="q-ma-md"
              node-key="key"
              @select="fnSelectProp(item)"
              @close="fnClose"
            />

<!--
            <treeselect
              :disabled="!showSelect"
              :options="optProp"
              v-model="item['val']"
              :normalizer="normalizerProp"
              :placeholder="$t('select')"
              :noResultsText="$t('noResult')"
              :noChildrenText="$t('noChilds')"
              :noOptionsText="$t('noResult')"
              :default-expand-level="1"
              @select="fnSelectProp(item)"
              @close="fnClose"
            />
            <q-item-label v-if="item['val'] < 0" class="text-red-10" style="font-size: 0.8em">
              <q-icon name="error" color="red-10" size="24px" />
              {{ $t("chooseProp") }}
            </q-item-label>
-->

          </div>
          <div v-else>
            {{ item[col.field] }}
          </div>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, getCurrentInstance } from "vue";
import { api } from "@/boot/axios.js";

import {
  collaps,
  collapsAll,
  expand,
  expandAll,
  notifyError,
  notifySuccess,
  pack,
} from "@/utils/jsutils.js";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  cols: {
    type: Array,
    default: () => [],
  },
  rows: {
    type: Array,
    default: () => [],
  },
  propField: String,
  cubeS: [Number, String],
  showAction: Boolean,
  showSelect: Boolean,
  mapping: Array,
});

const emit = defineEmits(["updateDelete", "updateBind"]);

const { proxy } = getCurrentInstance();

const loading = ref(false);
const isExpanded = ref(false);
const itemId = ref(null);
const optProp = ref([]);
const selDPI = ref(0);
const visible = ref(false);

const fnClose = (v) => {
  let val = v === undefined ? 0 : v;
  loading.value = true;
  api
    .post("", {
      method: "cubes/selectValue",
      params: [props.cubeS, selDPI.value, val, props.mapping, props.propField],
    })
    .then(() => {
      notifySuccess("success");
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnSelectProp = (item) => {
  selDPI.value = item["id"];
};

const normalizerProp = (node) => {
  return {
    id: node.prop,
    label: node.name,
  };
};

const fnBind = () => {
  emit("updateBind", {});
};

const fnUnBind = () => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/unbinding",
      params: [props.cubeS, props.mapping],
    })
    .then(() => {
      emit("updateDelete", { res: true });
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnExpand = () => {
  expandAll(props.rows);
};

const fnCollapse = () => {
  collapsAll(props.rows);
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

const cols0 = computed(() => props.cols.slice(0, 1));
const cols_ = computed(() => props.cols.slice(1));

const arrayTreeObj = computed(() => {
  const newObj = [];
  recursive(props.rows, newObj, 0, itemId.value, isExpanded.value);
  return newObj;
});

const btnDisabled = () => {
  let isBined = false;
  arrayTreeObj.value.forEach((item) => {
    if (item["val"]) isBined = true;
  });
  return isBined;
};

onMounted(() => {
  visible.value = true;
  api
    .post("", {
      method: "prop/loadPropForSelect",
      params: [props.propField],
    })
    .then((response) => {
      optProp.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
