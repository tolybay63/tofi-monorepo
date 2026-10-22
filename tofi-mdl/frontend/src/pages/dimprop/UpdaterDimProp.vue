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

      <q-card-section>
        <!-- cod -->
        <q-input
          v-model="form.cod"
          :label="$t('code')"
          dense
          :placeholder="$t('msgCodeGen')"
        />

        <!-- dimPropType -->
        <q-select
          :disable="mode === 'upd'"
          v-model="dpt"
          :options="optDPT"
          :label="$t('dimPropType')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectDimPropType"
        />

        <!-- dimMultiProp -->
        <div v-if="form.dimPropType === consDimPropTypeDMP">

          <TreeSelect
            v-model="form.dimMultiProp"
            :label="$t('dimMultiProp')"
            :options="optDimMP"
            class="q-ma-md"
            node-key="key"
            @select="fnSelectDimMP"
          />

<!--
          <q-item-label
            :class="form.dimMultiProp === 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px"
            class="row"
          >
            {{ $t("dimMultiProp") }}
            <q-space />
            <q-icon name="error" v-if="form.dimMultiProp === 0" color="red-10" size="24px" />
          </q-item-label>

          <treeselect
            :options="optDimMP"
            v-model="form.dimMultiProp"
            :normalizer="normalizerDimMP"
            :placeholder="$t('select')"
            :noResultsText="$t('noResult')"
            :noChildrenText="$t('noChilds')"
            :noOptionsText="$t('noResult')"
            defaultExpandLevel="2"
            autofocus
            @select="fnSelectDimMP"
          />

          <q-item-label v-if="form.dimMultiProp === 0" class="text-red-10" style="font-size: 0.8em">
            {{ $t("chooseDimMultiProp") }}
          </q-item-label>
-->
        </div>

        <!-- name -->
        <q-input
          v-model="form.name"
          @blur="onBlurName"
          dense
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          :label="$t('fldFullName')"
          dense
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- accessLevel -->
        <q-select
          v-model="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          dense
          options-dense
          @update:model-value="fnSelectAL"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
          type="textarea"
          dense
          :label="$t('fldCmt')"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          dense
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
import allConsts from "@/pages/all-consts.js";
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

const loading = ref(false);
const form = reactive({ ...props.data });
const lang = ref(props.lg);

const optAL = ref([]);
const al = ref(props.data.accessLevel);

const optDPT = ref([]);
const dpt = ref(props.data.dimPropType);

const optDimMP = ref([]);
const consDimPropTypeDMP = allConsts.FD_DimPropType.dimMultiProp;

const fnSelectDimPropType = (v) => {
  form.dimPropType = typeof v === "object" && v !== null ? v.id : v;
};

const fnSelectDimMP = (v) => {
  form.dimMultiProp = typeof v === "object" && v !== null ? v.id : v;
};

const normalizerDimMP = (node) => {
  return {
    id: node.dimMultiProp,
    label: node.name,
  };
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelectAL = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const validSave = () => {
  if (!form.name) return true;
  if (form.name.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.dimPropType = typeof dpt.value === "object" && dpt.value !== null ? dpt.value.id : dpt.value;

  api
    .post("", {
      id: form.id,
      method: "dimprop/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response?.data?.error?.message || error.message);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      optAL.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimPropType" }],
    })
    .then((response) => {
      optDPT.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "dimprop/loadAllDimMultiPropForSelect",
      params: [],
    })
    .then((response) => {
      optDimMP.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
