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
        <!-- relcls -->

        <TreeSelect
          v-model="relCls"
          :label="$t('relCls')"
          :options="optRelCls"
          class="q-ma-md"
          node-key="key"
          @close="fnCloseRelCls"
        />


        <q-item-label
          :class="form.relCls < 0 ? 'text-red-10' : 'text-grey-7'"
          style="font-size: 0.8em; margin-top: 10px"
          class="row q-mt-lg"
        >
          {{ $t("relCls") }}
          <q-space />
          <q-icon name="error" v-if="form.relCls < 0" color="red-10" size="24px" />
        </q-item-label>

<!--
        <treeselect
          class="q-mb-lg"
          :options="optRelCls"
          v-model="relCls"
          :normalizer="normalizer"
          :placeholder="$t('select')"
          :noChildrenText="$t('noChilds')"
          :noResultsText="$t('noResult')"
          :noOptionsText="$t('noResult')"
          @close="fnCloseRelCls"
        />
        <q-item-label
          v-if="form.relCls < 0"
          class="text-red-10"
          style="font-size: 0.8em"
        >
          {{ $t("chooseRelCls") }}
        </q-item-label>
-->

        <!-- format -->
        <q-select
          class="q-my-lg"
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
import { notifyError, notifySuccess, pack } from "@/utils/jsutils";
import TreeSelect from '@/components/TreeSelect.vue'

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

const optRelCls = ref([]);
const relCls = ref(props.data.relCls);

const normalizer = (node) => {
  return {
    id: node.relcls,
    label: node.name,
  };
};

const fnCloseRelCls = (v) => {
  form.relCls = v;
};

const fnSelectVF = () => {
  form.visualFormat = typeof vf.value === "object" && vf.value !== null ? vf.value.id : vf.value;
};

const validSave = () => {
  return form.relCls < 0;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insDimMultiPropItemRelCls" : "updDimMultiPropItemRelCls";

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
      method: "dimMultiProp/loadRelClsForSelect",
      params: [],
    })
    .then((response) => {
      optRelCls.value = pack(response.data.result.records, "id");
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
