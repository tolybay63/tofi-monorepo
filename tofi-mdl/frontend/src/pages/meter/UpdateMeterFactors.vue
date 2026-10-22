<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-select
          v-model="factor"
          @update:model-value="inputValue"
          use-input
          input-debounce="0"
          :label="$t('factor')"
          :options="options"
          option-label="name"
          :dense="dense"
          :options-dense="dense"
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

        <!-- ordDim -->
        <q-input
          v-model.number="form.ordDim"
          :label="$t('measureNumber')"
          type="number"
          input-class="text-right"
          :rules="[(val) => (val > 0 && val < maxDim + 1) || $t('invalidMeasureNumber')]"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="onofOk()"
        />
        <q-btn
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
  meterId: [Number, String],
  data: {
    type: Object,
    default: () => ({}),
  },
  dense: Boolean,
  maxDim: Number,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const factor = ref({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
const options = ref([]);
const optionsOrg = ref([]);

const onofOk = () => {
  return (
    factor.value.id === 0 ||
    !(form.ordDim > 0 && form.ordDim <= props.maxDim)
  );
};

const inputValue = (val) => {
  if (val) {
    factor.value.id = val.id;
    form.factor = val.id;
  }
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

const onOKClick = () => {
  form.meter = props.meterId;
  api
    .post("", {
      method: "meterfactor/insert",
      params: [{ rec: form }],
    })
    .then(
      () => {
        onDialogOK({ res: true });
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  api
    .post("", {
      method: "meterfactor/factors",
      params: [{ meter: props.meterId }],
    })
    .then((response) => {
      optionsOrg.value = response.data.result.records;
      options.value = [...response.data.result.records];
      options.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });
});
</script>

<style scoped></style>
