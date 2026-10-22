<template>
  <q-dialog
    ref="dialogRef"
    autofocus
    persistent
    transition-hide="slide-down"
    transition-show="slide-up"
    @hide="onDialogHide"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- measure -->

        <TreeSelect
          v-model="form.measure"
          :label="$t('measure')"
          :options="optMeasure"
          class="q-ma-md"
          node-key="key"
          @select="fnSelectMeasure"
        />


<!--
        <q-item-label
          class="text-grey-7"
          style="font-size: 0.8em; margin-top: 10px"
        >{{ $t("measure") }}
        </q-item-label>

        <treeselect
          v-model="form.measure"
          :options="optMeasure"
          :default-expand-level="1"
          :noChildrenText="$t('noChilds')"
          :noOptionsText="$t('noResult')"
          :noResultsText="$t('noResult')"
          :normalizer="normalizerMeasure"
          :placeholder="$t('select')"
          @select="fnSelectMeasure"
        />
-->

        <!-- minVal -->
        <q-input
          v-model="form.minVal"
          :label="$t('minVal')"
          type="number"
          dense
        />

        <!-- maxVal -->
        <q-input
          v-model="form.maxVal"
          :label="$t('maxVal')"
          type="number"
          dense
        />

        <!-- digit  -->
        <q-input
          v-model="form.digit"
          :disable="mode === 'upd' && form.parent !== undefined"
          :label="$t('digit')"
          clearable
          type="number"
          dense
          :rules="[
            (val) => !val || val >= 0 || 'Количество знаков после запятой должно быть не отрицательной',
          ]"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :disable="validSave()"
          :label="$t('save')"
          color="primary"
          dense
          icon="save"
          @click="onOKClick"
        />
        <q-btn
          :label="$t('cancel')"
          color="primary"
          dense
          icon="cancel"
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
import { notifyError, notifySuccess, pack } from "@/utils/jsutils";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const meter = ref(props.data.meter);
const optMeasure = ref([]);

const normalizerMeasure = (node) => {
  return {
    id: node.id,
    label: node.name,
  };
};

const fnSelectMeasure = (v) => {
  form.measure = v.id;
};

const validSave = () => {
  return !(form.measure && (!form.digit || form.digit >= 0));
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insDimMultiPropItemMeter" : "updDimMultiPropItemMeter";

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
  api
    .post("", {
      method: "dimMultiProp/loadMeasure",
      params: [],
    })
    .then((response) => {
      optMeasure.value = pack(response.data.result.records, "id");
    });
});
</script>

<style scoped></style>
