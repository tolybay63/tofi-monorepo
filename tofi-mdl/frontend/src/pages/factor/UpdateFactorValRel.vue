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
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("addFactor") }}</div>
      </q-bar>
      <q-card-section class="q-pa-md-md">
        <p class="no-padding no-margin">
          <span style="color: #1976d2">{{ $t("factor1") }}:</span>
          {{ name_factor1 }}
        </p>
        <p class="no-padding no-margin" v-if="mode === 'upd'">
          <span style="color: #1976d2">{{ $t("factor2") }}:</span>
          {{ name_factor2 }}
        </p>
        <p class="no-padding no-margin" v-else>
          <q-select
            v-model="factor2"
            @update:model-value="inputValue"
            use-input
            input-debounce="0"
            :label="$t('factor2')"
            :options="options"
            :option-label="dictText()"
            :dense="dense"
            :options-dense="dense"
            @filter="filterFn"
            style="min-width: 350px; max-width: 450px"
            autofocus
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>
        </p>
      </q-card-section>

      <q-card-section>
        <q-table
          style="height: calc(100vh - 300px); width: 100%"
          :dense="dense"
          color="primary"
          card-class="bg-amber-1 text-brown"
          table-class="text-grey-8"
          row-key="id"
          :columns="rels.cols"
          :rows="rels.rows"
          table-header-class="text-bold text-white bg-blue-grey-13"
          table-header-style="size: 3em"
          :separator="separator2"
          :loading="loading"
          :rows-per-page-options="[0]"
        >
          <template #body-cell="props">
            <q-td :props="props">
              <div v-if="isFV(props.value)">
                <q-btn
                  :dense="dense"
                  round
                  :class="
                    props.value !== '0'
                      ? 'bg-white text-red'
                      : 'bg-white text-green'
                  "
                  size="xs"
                  :icon="props.value !== '0' ? 'close' : 'done'"
                  flat
                  label=""
                  @click="fnCell(props)"
                >
                  <q-badge
                    floating
                    rounded
                    color="purple"
                    text-color="white"
                    @click.stop="fnCmt(props)"
                    v-if="props.value !== '0'"
                  >
                    info
                    <q-tooltip
                      class="bg-amber-2 text-black shadow-4"
                      :offset="[10, 10]"
                    >
                      {{ $t("fldCmt") }}
                    </q-tooltip>
                  </q-badge>
                </q-btn>
              </div>
              <div v-else>
                {{ props.value }}
              </div>
            </q-td>
          </template>

          <template v-slot:top>
            <q-btn
              round
              :dense="dense"
              icon="done"
              text-color="green"
              color="white"
              :disable="
                (mode === 'ins' && (loading || factor2.id === 0)) ||
                (mode === 'upd' && loading)
              "
              @click="setOnOff(0)"
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("allCompatible") }}
              </q-tooltip>
            </q-btn>

            <q-btn
              round
              :dense="dense"
              icon="close"
              color="white"
              text-color="red"
              class="q-ml-sm"
              @click="setOnOff(1)"
              :disable="
                (mode === 'ins' && (loading || factor2.id === 0)) ||
                (mode === 'upd' && loading)
              "
            >
              <q-tooltip transition-show="rotate" transition-hide="rotate">
                {{ $t("allIncompatible") }}
              </q-tooltip>
            </q-btn>
            <q-space />
            <q-toggle
              style="margin-left: 10px"
              v-model="dense"
              :label="$t('isDense')"
            />
          </template>

          <template #loading>
            <q-inner-loading showing color="secondary" />
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          :loading="loading"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>
        <q-btn
          color="primary"
          icon="cancel"
          :dense="dense"
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  params: Object,
  lg: Object,
  action: String,
  data: Object,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

let modeVal = props.params.mode;
let rowsVal = JSON.parse(JSON.stringify(props.data.rows));
let colsVal = JSON.parse(JSON.stringify(props.data.cols));
let cmtVal = JSON.parse(JSON.stringify(props.data.cmt));

if (modeVal === "ins") {
  colsVal = [];
  rowsVal = [];
}

const loading = ref(false);
const separator2 = ref("cell");
const name_factor1 = ref("");
const name_factor2 = ref("");
const factor2 = ref({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
const rec_factor2 = ref({});
const options = ref([]);
const optionsOrg = ref([]);
const mode = ref(modeVal);
const lang = ref(props.lg);
const act = ref(props.action);

const rels = reactive({
  rows: rowsVal,
  cols: colsVal,
  cmt: cmtVal,
});
const dense = ref(true);

const setOnOff = (val) => {
  rels.rows.forEach((row) => {
    rels.cols.forEach((col) => {
      if (col.name !== "name") {
        row[col.name] = val === 0 ? "0" : col.name.substring(2);
      }
    });
  });
};

const fnCmt = (p) => {
  let key = p.row.id + "_" + p.row[p.col.name];

  proxy?.$q
    .dialog({
      title: proxy?.$t("fldCmt") || "fldCmt",
      message: "",
      prompt: {
        model: rels.cmt[key],
        type: "textarea",
      },
      cancel: true,
      persistent: true,
      focus: "cancel",
    })
    .onOk((data) => {
      rels.cmt[key] = data;
    });
};

const inputValue = (val) => {
  if (val && val.id > 0) {
    factor2.value.id = val.id;
    loadData(props.params.factor1, val.id);
    loadFactor("factor2", val.id);
  } else {
    rels.rows = [];
    rels.cols = [];
  }
};

const dictText = () => {
  return "name";
};

const filterFn = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      options.value = optionsOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    options.value = optionsOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const loadFactor = (factorTarget, id) => {
  api
    .post("", {
      method: "factor/loadRec",
      params: [{ id: id }],
    })
    .then(
      (response) => {
        if (factorTarget === "factor1")
          name_factor1.value = response.data.result.records[0].name;
        if (factorTarget === "factor2") {
          name_factor2.value = response.data.result.records[0].name;
        }
      },
      (error) => {
        notifyError(error.response.data.error.message);
      }
    );
};

const loadData = (factor1, factor2Val) => {
  loading.value = true;
  api
    .post("", {
      method: "factorrel/factorValRel",
      params: [{ factor1: factor1, factor2: factor2Val }],
    })
    .then((response) => {
      rels.rows = response.data.result.rows.records;
      rels.cols = response.data.result.cols;
      if (rels.cols[0]) {
        rels.cols[0].label =
          (proxy?.$t("factor1") || "factor1") + "/" + (proxy?.$t("factor2") || "factor2");
      }
    })
    .catch((error) => {
      console.log(error);
      notifyError(error.message);
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnCell = (p) => {
  if (p.row[p.col.name] === "0") {
    p.row[p.col.name] = p.col.name.substring(2);
  } else {
    p.row[p.col.name] = "0";
  }
};

const isFV = (val) => {
  return /^[0-9]+$/.test(val);
};

const onOKClick = () => {
  let data = {};
  let factor1 = props.params.factor1;
  let factor2Val = props.params.factor2;
  if (props.params.mode === "ins") {
    factor2Val = factor2.value.id;
  }
  rels.rows.forEach((row) => {
    rels.cols.forEach((col) => {
      if (col.name !== "name") {
        if (row[col.name] !== "0") {
          let key = row.id + "_" + row[col.name];
          data[key] = rels.cmt[key] || "";
        }
      }
    });
  });

  loading.value = true;
  api
    .post("", {
      method: "factorrel/saveFactorValRel",
      params: [{ factor1: factor1, factor2: factor2Val }, data],
    })
    .then(
      (response) => {
        rec_factor2.value = response.data.result.records[0];
        onDialogOK(rec_factor2.value);
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = error.response.data.error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  loadFactor("factor1", props.params.factor1);
  if (mode.value === "upd") {
    loadFactor("factor2", props.params.factor2);
  } else {
    api
      .post("", {
        id: 1,
        method: "factorrel/factor2",
        params: [
          {
            factor: props.params.factor1,
          },
        ],
      })
      .then((response) => {
        optionsOrg.value = response.data.result.records;
        options.value = [...response.data.result.records];
        options.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
      })
      .catch((error) => {
        console.log(error);
        notifyError(error.message);
      });
  }
});
</script>

<style scoped></style>
