<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 800px"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <q-select
          v-model="factor"
          map-options
          use-input
          input-debounce="0"
          :label="$t('factor')"
          :options="optionsFactor"
          option-value="id"
          option-label="name"
          :dense="dense"
          :options-dense="dense"
          clearable
          @clear="fnClearFactor"
          @update:model-value="inputValueFactor"
          @filter="filterFnFactor"
          :disable="factorDisable"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-select
          v-model="typ"
          use-input
          map-options
          input-debounce="0"
          :label="$t('typ')"
          :options="optionsTyp"
          option-value="id"
          option-label="name"
          :dense="dense"
          :options-dense="dense"
          @update:model-value="inputValueTyp"
          @filter="filterFnTyp"
          clearable
          @clear="fnClearTyp"
          :disable="typDisable"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-select
          v-model="relTyp"
          map-options
          use-input
          input-debounce="0"
          :label="$t('reltyp')"
          :options="optionsRelTyp"
          option-value="id"
          option-label="name"
          :dense="dense"
          :options-dense="dense"
          :disable="relTypDisable"
          @update:model-value="inputValueRelTyp"
          @filter="filterFnRelTyp"
          clearable
          @clear="fnClearRelTyp"
        >
          <template v-slot:no-option>
            <q-item>
              <q-item-section class="text-grey">
                {{ $t("noResults") }}
              </q-item-section>
            </q-item>
          </template>
        </q-select>

        <q-toggle
          style="margin-left: 10px; margin-top: 10px"
          :dense="dense"
          v-model="form.isReq"
          :label="$t('isReq')"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          :dense="dense"
          color="primary"
          icon="cancel"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  multiProp: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const visible = ref(false);

const factor = ref(props.data.factor);
const optionsFactor = ref([]);
const optionsFactorOrg = ref([]);

const typ = ref(props.data.typ);
const optionsTyp = ref([]);
const optionsTypOrg = ref([]);

const relTyp = ref(props.data.relTyp);
const optionsRelTyp = ref([]);
const optionsRelTypOrg = ref([]);

const factorDisable = ref(false);
const typDisable = ref(false);
const relTypDisable = ref(false);

const fnClearFactor = () => {
  form.factor = null;
  typDisable.value = false;
  relTypDisable.value = false;
};

const fnClearTyp = () => {
  form.typ = null;
  factorDisable.value = false;
  relTypDisable.value = false;
};

const fnClearRelTyp = () => {
  form.relTyp = null;
  factorDisable.value = false;
  typDisable.value = false;
};

const inputValueFactor = (val) => {
  if (val) {
    factor.value = { ...factor.value, id: val.id };
    form.factor = val.id;

    typDisable.value = true;
    relTypDisable.value = true;
  }
};

const inputValueTyp = (val) => {
  if (val) {
    typ.value = { ...typ.value, id: val.id };
    form.typ = val.id;

    factorDisable.value = true;
    relTypDisable.value = true;
  }
};

const inputValueRelTyp = (val) => {
  if (val) {
    relTyp.value = { ...relTyp.value, id: val.id };
    form.relTyp = val.id;

    typDisable.value = true;
    factorDisable.value = true;
  }
};

const filterFnFactor = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsFactor.value = optionsFactorOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsFactorOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsFactor.value = optionsFactorOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const filterFnTyp = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsTyp.value = optionsTypOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsTypOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsTyp.value = optionsTypOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const filterFnRelTyp = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsRelTyp.value = optionsRelTypOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsRelTypOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsRelTyp.value = optionsRelTypOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const validSave = () => {
  return !(
    form.factor !== null ||
    form.typ !== null ||
    form.relTyp !== null
  );
};

const onOKClick = () => {
  form.multiProp = props.multiProp;
  const method = props.mode === "ins" ? "insertMultiPropCond" : "updateMultiPropCond";

  let err = false;
  api
    .post("", {
      method: "multiProp/" + method,
      params: [form],
    })
    .then(
      () => {
        err = false;
        onDialogOK({ res: true });
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let msg = error.response?.data?.error?.message || error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      if (!err) {
        // dialogRef handles closing via onDialogOK/onDialogCancel
      }
    });
};

const loadFactors = () => {
  visible.value = true;
  api
    .post("", {
      method: "multiProp/loadFactorsForSelect",
      params: [props.multiProp, props.mode],
    })
    .then((response) => {
      optionsFactorOrg.value = response.data.result.records;
      optionsFactor.value = response.data.result.records;
    })
    .finally(() => {
      if (props.mode === "upd") {
        factorDisable.value = true;
        typDisable.value = true;
        relTypDisable.value = true;
      }
      visible.value = false;
    });
};

const loadTyps = () => {
  visible.value = true;
  api
    .post("", {
      method: "multiProp/loadTypsForSelect",
      params: [props.multiProp, props.mode],
    })
    .then((response) => {
      optionsTypOrg.value = response.data.result.records;
      optionsTyp.value = response.data.result.records;
    })
    .finally(() => {
      if (props.mode === "upd") {
        factorDisable.value = true;
        typDisable.value = true;
        relTypDisable.value = true;
      }
      visible.value = false;
    });
};

const loadRelTyps = () => {
  visible.value = true;
  api
    .post("", {
      method: "multiProp/loadRelTypsForSelect",
      params: [props.multiProp, props.mode],
    })
    .then((response) => {
      optionsRelTypOrg.value = response.data.result.records;
      optionsRelTyp.value = response.data.result.records;
    })
    .finally(() => {
      if (props.mode === "upd") {
        factorDisable.value = true;
        typDisable.value = true;
        relTypDisable.value = true;
      }
      visible.value = false;
    });
};

onMounted(() => {
  if (props.mode === "ins") {
    loadFactors();
    loadTyps();
    loadRelTyps();
  } else if (props.mode === "upd") {
    if (form.factor != null) {
      loadFactors();
    } else if (form.typ != null) {
      loadTyps();
    } else if (form.relTyp != null) {
      loadRelTyps();
    } else {
      notifyError("Error :-(");
    }
  }
});
</script>

<style scoped></style>
