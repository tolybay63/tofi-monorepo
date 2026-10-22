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

        <!-- prop -->
        <TreeSelect
          v-model="form['form.prop']"
          :label="$t('prop')"
          :options="optProp"
          class="q-ma-md"
          node-key="key"
          @select="fnSelect"
          autofocus
        />


<!--
        <q-item-label
          :class="form.prop === 0 ? 'text-red-10' : 'text-grey-7'"
          style="font-size: 0.8em; margin-top: 10px"
          class="row"
        >
          {{ $t("prop") }}
          <q-space />
          <q-icon name="error" v-if="form.prop === 0" color="red-10" size="24px" />
        </q-item-label>

        <treeselect
          :options="optProp"
          v-model="form.prop"
          :normalizer="normalizerProp"
          :placeholder="$t('select')"
          :noResultsText="$t('noResult')"
          :noChildrenText="$t('noChilds')"
          :noOptionsText="$t('noResult')"
          defaultExpandLevel="1"
          autofocus
          @select="fnSelect"
        />

        <q-item-label v-if="form.prop === 0" class="text-red-10" style="font-size: 0.8em">
          {{ $t("chooseProp") }}
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
  parentName: String,
  lg: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive(extend({}, props.data));
const visible = ref(false);

const optProp = ref([]);

const fnSelect = (v) => {
  form.name = props.dimPropName + " (" + v.name + ")";
  form.fullName = props.dimPropName + " (" + v.fullname + ")";
};

/*const normalizerProp = (node) => {
  return {
    id: node.prop,
    label: node.name,
  };
};*/

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const validSave = () => {
  if (!form.prop || !form.name) return true;
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
      method: "prop/loadAllPropForSelect",
      params: [],
    })
    .then((response) => {
      optProp.value = pack(response.data.result.records, "id");
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
