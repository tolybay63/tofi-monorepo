<template>
  <div style="height: calc(100vh - 400px); width: 100%">
    <q-splitter
      v-model="splitterModel"
      :limits="[40, 60]"
      horizontal
    >
      <template v-slot:before>
        <q-table
          color="primary"
          card-class="bg-amber-1 text-brown"
          row-key="cod"
          :dense="value"
          :columns="cols"
          :rows="factorrels"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :filter="filter"
          :loading="loading"
          selection="single"
          @selection="fnSelection"
          v-model:selected="selected"
          :rows-per-page-options="[0]"
        >
          <template #bottom-row>
            <q-td colspan="100%" v-if="selected.length > 0">
              <span class="text-blue"> {{ $t("selectedRow") }}: </span>
              <span class="text-bold"> {{ infoSelected(selected[0]) }} </span>
            </q-td>
            <q-td
              colspan="100%"
              v-else-if="factorrels.length > 0"
              class="text-bold"
            >
              {{ $t("infoFactorValRel") }}
            </q-td>
          </template>

          <template v-slot:top>
            <div style="font-size: 1.2em; font-weight: bold;">
              {{ $t("relFactors") }}
            </div>

            <q-space />
            <q-btn
              v-if="hasTarget('mdl:mn_ds:fac:sel:rel:ins')"
              icon="post_add"
              color="secondary"
              :dense="value"
              :disable="loading"
              @click="editVals('ins')"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("newRecord") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              :dense="value"
              icon="edit"
              color="secondary"
              class="q-ml-sm"
              @click="editVals('upd')"
              :disable="loading || selected.length === 0"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("editRecord") }}
              </q-tooltip>
            </q-btn>

            <q-toggle
              style="margin-left: 10px"
              v-model="value"
              :label="$t('isDense')"
            />

            <q-space />
            <q-input
              :dense="value"
              debounce="300"
              color="primary"
              v-model="filter"
              :label="$t('txt_filter')"
            >
              <template v-slot:append>
                <q-icon name="search" />
              </template>
            </q-input>
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary" />
          </template>
        </q-table>
      </template>

      <template v-slot:after>
        <div v-if="factor2() > 0">
          <div style="font-size: 1.2em; font-weight: bold;" class="bg-green-1">
            {{ $t("compatibility") }}
          </div>

          <div>
            <q-table
              style="height: 100%; width: 100%"
              color="primary"
              card-class="bg-amber-1 text-brown"
              table-class="text-grey-8"
              row-key="id"
              :dense="value"
              :columns="propFactors.cols"
              :rows="propFactors.rows"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="cell"
              :loading="loading2"
              :rows-per-page-options="[0]"
            >
              <template #body-cell="props">
                <q-td :props="props">
                  <div v-if="isFV(props.value)">
                    <q-btn
                      round
                      :class="
                        props.value !== '0'
                          ? 'bg-white text-red'
                          : 'bg-white text-green'
                      "
                      size="xs"
                      :icon="props.value !== '0' ? 'close' : 'done'"
                      flat
                      :dense="value"
                      label=""
                    >
                      <q-tooltip
                        class="bg-amber-2 text-black shadow-4"
                        :offset="[10, 10]"
                        max-width="300px"
                        v-if="props.value !== '0'"
                      >
                        {{ fnCmt(props) }}
                      </q-tooltip>
                    </q-btn>
                  </div>
                  <div v-else>
                    {{ props.value }}
                  </div>
                </q-td>
              </template>

              <template #loading>
                <q-inner-loading showing color="secondary" />
              </template>

              <template #bottom-row>
                <div style="margin-left: 10px">
                  ☑ : <strong>{{ propFactors.cnt_s }}</strong> ☒ :
                  <strong>{{ propFactors.cnt_ns }}</strong>
                </div>
              </template>
            </q-table>
          </div>
        </div>
      </template>
    </q-splitter>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute } from "vue-router";
import { useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { hasTarget, notifyError } from "@/utils/jsutils";
import UpdateFactorValRel from "@/pages/factor/UpdateFactorValRel.vue";

const route = useRoute();
const $q = useQuasar();
const { proxy } = getCurrentInstance();

const requestParam = {
  factor: 0,
  filter: "",
  descending: false,
  sortBy: null,
};

const splitterModel = ref(40);
const cols = ref([]);
const factorrels = ref([]);
const filter = ref("");
const loading = ref(false);
const selected = ref([]);
const factor1_id = ref(null);
const factor = ref({});

const loading2 = ref(false);
const propFactors = reactive({
  rows: [],
  cols: [],
  cmt: {},
  cnt_s: 0,
  cnt_ns: 0,
});
const value = ref(true);
const lang = ref("");

const fnCmt = (props) => {
  let key = props.row.id + "_" + props.row[props.col.name];
  if (propFactors.cmt[key]) {
    return propFactors.cmt[key];
  } else {
    return proxy?.$t("notComment") || "notComment";
  }
};

const factor2 = () => {
  if (!selected.value[0]) return 0;
  return selected.value[0].id;
};

const isFV = (val) => {
  return /^[0-9]+$/.test(val);
};

const fnSelection = (par) => {
  const factor1 = factor1_id.value;
  const factor2Val = par.rows[0].id;

  loading2.value = true;
  api
    .post("", {
      method: "factorrel/factorValRel",
      params: [{ factor1: factor1, factor2: factor2Val }],
    })
    .then((response) => {
      propFactors.rows = response.data.result.rows.records;
      let s = 0,
        ns = 0;
      propFactors.rows.forEach((el) => {
        for (let key in el) {
          if (key.startsWith("fv")) {
            if (el[key] === "0") s = s + 1;
            else ns = ns + 1;
          }
        }
      });
      propFactors.cnt_s = s;
      propFactors.cnt_ns = ns;

      propFactors.cols = response.data.result.cols;
      if (propFactors.cols[0]) {
        propFactors.cols[0].label =
          (proxy?.$t("factor1") || "factor1") + "/" + (proxy?.$t("factor2") || "factor2");
      }
      propFactors.cmt = response.data.result.cmt;
    })
    .catch((error) => {
      console.log(error);
      notifyError(error.message);
    })
    .finally(() => {
      loading2.value = false;
    });
};

const editVals = (mode) => {
  let factor1 = factor1_id.value;
  let factor2Val = 0;
  if (mode === "upd") factor2Val = selected.value[0].id;

  let params = {
    factor1: factor1,
    factor2: factor2Val,
    mode: mode,
  };
  const lg = { name: lang.value };

  $q.dialog({
    component: UpdateFactorValRel,
    componentProps: {
      params: params,
      lg: lg,
      action: "factorrel",
      data: propFactors,
    },
  }).onOk((rec) => {
    if (mode === "ins") {
      if (rec !== undefined) {
        factorrels.value.push(rec);
        selected.value = [];
        selected.value.push(rec);
        let par = { rows: [{ id: selected.value[0].id }] };
        fnSelection(par);
      }
    } else {
      let par = { rows: [{ id: selected.value[0].id }] };

      if (rec && Object.keys(rec).length > 0) {
        let tmp = selected.value[0];
        selected.value = [];
        selected.value.push(tmp);
        fnSelection(par);
      } else {
        let index = factorrels.value.findIndex(
          (row) => row.id === selected.value[0].id
        );
        factorrels.value.splice(index, 1);
        selected.value = [];
      }
    }
  });
};

const fetchData = (requestProps) => {
  loading.value = true;
  const orderBy = requestProps.sortBy;
  const reqFilter = requestProps.filter;
  const reqFactor = requestProps.factor;

  api
    .post("", {
      method: "factorrel/load",
      params: [
        {
          factor: reqFactor,
          orderBy: orderBy,
          filter: reqFilter,
        },
      ],
    })
    .then((response) => {
      factorrels.value = response.data.result.records;
      selected.value = [];
    })
    .catch((error) => {
      console.log(error);
      notifyError(error.message);
    })
    .finally(() => {
      loading.value = false;
    });
};

const getColumns = () => [
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 30%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    sortable: true,
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 30%",
  },
  {
    name: "cmt",
    label: proxy?.$t("fldCmt") || "fldCmt",
    field: "cmt",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 40%",
  },
];

const infoSelected = (row) => {
  return " " + row.cod + " - " + row.name;
};

onMounted(() => {
  lang.value = localStorage.getItem("curLang");
  lang.value = lang.value === "en-US" ? "en" : lang.value;
  cols.value = getColumns();

  factor1_id.value = parseInt(route.params.factor, 10) || 0;
  requestParam.factor = factor1_id.value;
  fetchData(requestParam);
});
</script>

<style></style>
