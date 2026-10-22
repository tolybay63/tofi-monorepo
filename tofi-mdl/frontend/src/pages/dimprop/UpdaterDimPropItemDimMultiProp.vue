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
        <div class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- dimMultiPropItem -->

        <TreeSelect
          v-model="form['dimMultiPropItem']"
          :label="$t('dimMultiPropItem')"
          :options="optDimMultiPropItem"
          class="q-ma-md"
          node-key="key"
          @select="fnSelectDMPI"
          autofocus
        />

<!--
        <q-item-label
          :class="form.dimMultiPropItem === 0 ? 'text-red-10' : 'text-grey-7'"
          style="font-size: 0.8em; margin-top: 10px"
          class="row"
        >
          {{ $t("dimMultiPropItem") }}
          <q-space />
          <q-icon name="error" v-if="form.dimMultiPropItem === 0" color="red-10" size="24px" />
        </q-item-label>

        <treeselect
          :options="optDimMultiPropItem"
          v-model="form.dimMultiPropItem"
          :normalizer="normalizerMP"
          :placeholder="$t('select')"
          :noResultsText="$t('noResult')"
          :noChildrenText="$t('noChilds')"
          :noOptionsText="$t('noResult')"
          defaultExpandLevel="2"
          autofocus
          @select="fnSelectDMPI"
        />

        <q-item-label
          v-if="form.dimMultiPropItem === 0"
          class="text-red-10"
          style="font-size: 0.8em"
        >
          {{ $t("chooseDimMultiProp") }}
        </q-item-label>
-->

        <!-- name -->
        <q-input
          class="q-mt-md"
          dense
          v-model="form.name"
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          dense
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- cmt -->
        <q-input
          dense
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
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
import { extend } from "quasar";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  dimProp: [Number, String],
  dimPropName: String,
  dimPropType: [Number, String],
  dimMultiProp: [Number, String],
  dimMultiPropType: [Number, String],
  parentName: String,
  lg: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive(extend({}, props.data));
const visible = ref(false);
const optDimMultiPropItem = ref([]);

const fnSelectDMPI = (v) => {
  if (v) {
    form.name = props.dimPropName + " (" + v.name + ")";
    form.fullName = props.dimPropName + " (" + v.fullname + ")";
  } else {
    form.name = null;
    form.fullName = null;
  }
};

/*
const normalizerMP = (node) => {
  return {
    id: node.id,
    label: node.name,
  };
};
*/

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const validSave = () => {
  if (!form.dimMultiPropItem || !form.name) return true;
  return false;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertDPI" : "updateDPI";

  api
    .post("", {
      id: form.id,
      method: "dimprop/" + method,
      params: [form],
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
  visible.value = true;
  api
    .post("", {
      method: "dimprop/loadDimMultiPropItemForSelect",
      params: [props.dimMultiProp],
    })
    .then((response) => {
      optDimMultiPropItem.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
