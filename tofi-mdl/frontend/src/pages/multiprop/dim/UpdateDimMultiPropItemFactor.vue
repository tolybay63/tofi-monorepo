<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <!-- factor -->
        <q-select
          v-model="factor"
          @update:model-value="inputValue"
          use-input
          input-debounce="0"
          :label="$t('factor')"
          :options="optFactor"
          option-label="name"
          dense
          options-dense
          @filter="filterFn"
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

        <!-- format -->
        <q-select
          v-model="vf"
          :options="optVF"
          :label="$t('visualFormat')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectVF"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          dense
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
  lg: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const visible = ref(false);

const vf = ref(props.data.visualFormat);
const optVF = ref([]);

const factor = ref({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
const optFactor = ref([]);
const optFactorOrg = ref([]);

const fnSelectVF = () => {
  form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;
};

const inputValue = (val) => {
  if (val) {
    factor.value = { ...factor.value, id: val.id };
    form.factor = val.id;
  }
};

const filterFn = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optFactor.value = optFactorOrg.value;
    });
    return;
  }
  update(() => {
    if (optFactorOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optFactor.value = optFactorOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const validSave = () => {
  return !form.factor;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insDimMultiPropItemFactor" : "updDimMultiPropItemFactor";

  form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;

  api
    .post("", {
      method: "dimMultiProp/" + method,
      params: [form],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        let msg = error.response?.data?.error?.message || error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  visible.value = true;
  api
    .post("", {
      method: "dimMultiProp/loadFactors",
      params: [],
    })
    .then((response) => {
      optFactorOrg.value = response.data.result.records;
      optFactor.value = [...response.data.result.records];
      optFactor.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_VisualFormat" }],
    })
    .then((response) => {
      optVF.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
