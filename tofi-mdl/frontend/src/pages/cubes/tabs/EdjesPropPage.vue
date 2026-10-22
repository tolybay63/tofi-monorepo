<template>
  <div class="q-pa-sm-sm">
    <q-splitter
      v-model="splitterModel"
      :model-value="splitterModel"
      :limits="[20, 100]"
      before-class="overflow-hidden q-mr-sm"
      after-class="overflow-hidden q-ml-sm"
      separator-class="bg-red"
      class="bg-amber-1"
    >
      <template v-slot:before>
        <div style="height: calc(100vh - 310px); width: 100%">
          <div v-for="(dimProp, index) in dimProps" class="q-ma-sm" :key="dimProp.id || index">

            <TreeSelect
              :disabled="fixedDim"
              v-model="dp[index]"
              :label="$t('dimProp.name')"
              :options="optDP[index]"
              class="q-ma-md"
              node-key="key"
              @select="fnSelectDP"
            />

<!--
            <q-item-label class="text-grey-7">
              {{ dimProp.name }}
            </q-item-label>
            <treeselect
              :options="optDP[index]"
              v-model="dp[index]"
              :normalizer="normalizerDP"
              :placeholder="$t('select')"
              :noResultsText="$t('noResult')"
              :noChildrenText="$t('noChilds')"
              :noOptionsText="$t('noResult')"
              :default-expand-level="1"
              @select="fnSelectDP"
              :disabled="fixedDim"
            />
-->

          </div>

          <q-toggle
            v-model="fixedDim"
            color="green"
            label="Зафиксировать"
            :disable="validAll()"
            @update:model-value="updValue"
          />

          <div class="q-ma-sm">
            {{ fnMapping() }}
          </div>
        </div>
      </template>

      <q-inner-loading :showing="loading" color="secondary" />

      <template v-slot:after>
        <div v-if="!showGrid">
          <h3> Грани прокуба свойств</h3>
        </div>
        <div v-else>
          <edjes-prop
            :cols="cols"
            :rows="rows"
            :propField="propField"
            :cubeS="cubeS"
            @updateDelete="onUpdateDelete"
            @updateBind="onUpdateBind"
            :showAction="showAction"
            :showSelect="showSelect"
            :mapping="fnMapping()"
          />
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { api } from "@/boot/axios.js";

import { notifyError, pack } from "@/utils/jsutils.js";
import EdjesProp from "@/pages/cubes/tabs/prop/EdjesProp.vue";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  cubeS: [Number, String],
  dOrg: String,
});

const { proxy } = getCurrentInstance();

const splitterModel = ref(30);
const loading = ref(false);
const dimProps = ref([]);
const dp = reactive([]);
const optDP = reactive([]);
const cols = ref([]);
const rows = ref([]);
const propField = ref("prop");
const showAction = ref(true);
const showSelect = ref(true);
const fixedDim = ref(false);
const showGrid = ref(false);

const onUpdateDelete = () => {
  fixedDim.value = false;
  rows.value = [];
  setTimeout(() => {
    fixedDim.value = true;
    updValue(fixedDim.value);
  }, 100);
};

const onUpdateBind = () => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/binding",
      params: [props.cubeS, propField.value, fnMapping()],
    })
    .then((response) => {
      rows.value = [];
      let rr = response.data.result.store;
      rows.value = pack(rr.records, "id");
    })
    .then(() => {
      updValue(false);
      setTimeout(() => {
        fixedDim.value = true;
        updValue(fixedDim.value);
      }, 100);
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

const updValue = (v) => {
  if (v) {
    getGrid(fnMapping());
  } else {
    showGrid.value = false;
    cols.value = [];
    rows.value = [];
  }
};

const getGrid = (par) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/getGrid",
      params: [props.cubeS, par],
    })
    .then((response) => {
      cols.value = response.data.result.cols;
      propField.value = response.data.result.propField;
      showAction.value = response.data.result.showAction;
      showSelect.value = response.data.result.showSelect;
      rows.value = pack(response.data.result.rows.records, "id");
      showGrid.value = true;
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

const fnMapping = () => {
  let map = new Map();
  dimProps.value.forEach((e, index) => {
    map.set(e.id, dp[index]);
  });
  return Array.from(map.entries());
};

const fnSelectDP = () => {};

const normalizerDP = (node) => {
  return {
    id: node.id,
    label: node.name,
  };
};

const validAll = () => {
  dp.forEach((r, index) => {
    if (r === undefined || !r) {
      dp[index] = 0;
    }
  });

  let d1 = dp.filter((r) => r === 1);
  let d2 = dp.filter((r) => r > 1);

  if (dp.length === 1) {
    return !(d1.length === 1);
  } else {
    return !(d1.length === 1 && d2.length > 0);
  }
};

const loadDimPropItem = (dimProp, index) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimPropItem",
      params: [dimProp],
    })
    .then((response) => {
      let dta = pack(response.data.result.records, "id");
      optDP[index] = [{ id: 1, parent: null, name: "Все" }, ...dta];
    })
    .then(() => {
      dp[index] = dp[index] ? dp[index] : 0;
    })
    .finally(() => {
      loading.value = false;
    });
};

const loadDimPropItemFV = (dimProp, index) => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimPropItemFV",
      params: [dimProp],
    })
    .then((response) => {
      optDP[index] = pack(response.data.result.records, "id");
    })
    .then(() => {
      dp[index] = dp[index] ? dp[index] : 0;
    })
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/loadDimProp",
      params: [props.cubeS],
    })
    .then((response) => {
      dimProps.value = response.data.result.records;
    })
    .then(() => {
      dimProps.value.forEach((dim, index) => {
        if (dim["dimproptype"] === 2) {
          loadDimPropItemFV(dim.id, index);
        } else {
          loadDimPropItem(dim.id, index);
        }
      });
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
