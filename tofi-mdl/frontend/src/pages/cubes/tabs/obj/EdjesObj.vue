<template>
  <div class="q-pa-sm">
    <q-table
      style="height: calc(100vh - 320px); width: 100%"
      color="primary"
      card-class="bg-amber-1"
      row-key="id"
      dense
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :loading="loading"
      :rows-per-page-options="[0]"
    >
      <template #body-cell="props">
        <q-td :props="props">
          <div v-if="isCol(props)">
            <q-select
              v-model="props.row[props.col.name]"
              :options="getOptRC(props.row.id, props.col.name.split('_')[0])"
              option-value="id"
              option-label="name"
              map-options
              dense
              options-dense
              class="q-mb-md"
              :bg-color="fnColor(props.row.id, props.col.name.split('_')[0])"
              @update:model-value="fnSelectRC($event, props.row, props.col)"
            />
          </div>
          <div v-else>
            {{ props.value }}
          </div>
        </q-td>
      </template>

      <template v-slot:top>
        <div v-if="showAction">
          <q-btn
            dense
            icon="save"
            color="secondary"
            class="q-ml-lg"
            :loading="loading"
            @click="fnBind"
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
            @click="fnUnBind"
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
      </template>
    </q-table>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance } from "vue";
import { api } from "@/boot/axios.js";
import { notifyError, notifySuccess } from "@/utils/jsutils.js";

const props = defineProps({
  cols: Array,
  rows: Array,
  cubeSFace: [Number, String],
  do1: [Number, String],
  do2: [Number, String],
  optRC: [Array, Object],
  dimCount: Number,
  doi3: [Number, String],
  doi4: [Number, String],
  cl3: [Number, String],
  cl4: [Number, String],
  face: String,
});

const emit = defineEmits(["updateDelete", "updateBind"]);

const { proxy } = getCurrentInstance();

const loading = ref(false);
const rc = reactive(new Map());
const showAction = ref(true);
const showSelect = ref(true);

const fnColor = (cl1, cl2) => {
  if (props.dimCount === 2) {
    if (props.optRC[cl1 + "_" + cl2]?.length === 0) return "red-2";
  } else if (props.dimCount === 3) {
    if (props.face === "face2") {
      if (props.optRC[cl1 + "_" + cl2]?.length === 0) return "red-2";
    } else {
      if (props.optRC[cl1 + "_" + cl2 + "_" + props.cl3]?.length === 0) return "red-2";
    }
  } else if (props.dimCount === 4) {
    if (props.face === "face2") {
      if (props.optRC[cl1 + "_" + cl2]?.length === 0) return "red-2";
    } else if (props.face === "face3") {
      if (props.optRC[cl1 + "_" + cl2 + "_" + props.cl3]?.length === 0) return "red-2";
    } else if (props.face === "face4") {
      if (props.optRC[cl1 + "_" + cl2 + "_" + props.cl3 + "_" + props.cl4]?.length === 0)
        return "red-2";
    }
  } else {
    return "red-2";
  }
};

const fnSelectRC = (v, r, c) => {
  saveCubesFace(v.id, v.reltyp, r.doi, c.name.split("_")[1], r.id, c.name.split("_")[0]);
};

const saveCubesFace = (rcVal, rt, doi1, doi2, cl1, cl2) => {
  let params = [props.cubeSFace, rcVal, rt, doi1, doi2, cl1, cl2, 0, 0, 0, 0];
  if (props.dimCount === 3) {
    params = [
      props.cubeSFace,
      rcVal,
      rt,
      doi1,
      doi2,
      cl1,
      cl2,
      props.doi3 ? props.doi3 : 0,
      props.cl3 ? props.cl3 : 0,
      0,
      0,
    ];
  }
  if (props.dimCount === 4) {
    params = [
      props.cubeSFace,
      rcVal,
      rt,
      doi1,
      doi2,
      cl1,
      cl2,
      props.doi3 ? props.doi3 : 0,
      props.cl3 ? props.cl3 : 0,
      props.doi4 ? props.doi4 : 0,
      props.cl4 ? props.cl4 : 0,
    ];
  }

  api
    .post("", {
      method: "cubes/saveCubesFace",
      params: params,
    })
    .then(() => {
      notifySuccess(proxy?.$t("success"));
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

const getOptRC = (c1, c2) => {
  if (props.dimCount === 2) {
    return props.optRC[c1 + "_" + c2.split("_")[0]];
  } else if (props.dimCount === 3) {
    if (props.face === "face2") {
      return props.optRC[c1 + "_" + c2.split("_")[0]];
    } else {
      return props.optRC[c1 + "_" + c2.split("_")[0] + "_" + props.cl3];
    }
  } else if (props.dimCount === 4) {
    if (props.face === "face2") {
      return props.optRC[c1 + "_" + c2.split("_")[0]];
    } else if (props.face === "face3") {
      return props.optRC[c1 + "_" + c2.split("_")[0] + "_" + props.cl3];
    } else if (props.face === "face4") {
      return props.optRC[c1 + "_" + c2.split("_")[0] + "_" + props.cl3 + "_" + props.cl4];
    }
  }
};

const isCol = (propsData) => {
  return propsData.col.label !== "";
};

const fnBind = () => {
  emit("updateBind", {});
};

const fnUnBind = () => {
  loading.value = true;
  api
    .post("", {
      method: "cubes/unbindingDO",
      params: [
        props.cubeSFace,
        props.do1,
        props.do2,
        props.cl3 ? props.cl3 : 0,
        props.cl4 ? props.cl4 : 0,
      ],
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
</script>

<style scoped></style>
