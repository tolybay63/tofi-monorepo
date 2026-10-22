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
        <q-item-section v-if="isChild">
          {{ $t("parent") }}: {{ parentName }}
        </q-item-section>

        <q-input
          class="q-my-md"
          dense
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <q-select
          dense
          options-dense
          v-model="et"
          :options="optET"
          :label="$t('multiValEntityType')"
          option-value="id"
          option-label="text"
          map-options
          class="q-mb-md"
          @update:model-value="fnSelectET"
        />

        <div v-if="dimMultiPropType === 2">
          <!-- prop -->

          <TreeSelect
            v-model="form['form.prop']"
            :label="$t('prop')"
            :options="optProp"
            class="q-ma-md"
            node-key="key"
            @close="fnCloseProp"
            @select="fnSelectProp"
          />


<!--
          <q-item-label
            :class="form.prop === 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em; margin-top: 10px"
            class="row q-mt-lg"
          >
            {{ $t("prop") }}
            <q-space />
            <q-icon name="error" v-if="form.prop === 0" color="red-10" size="24px" />
          </q-item-label>

          <treeselect
            class="q-mb-lg"
            :options="optProp"
            v-model="form.prop"
            :normalizer="normalizer"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @close="fnCloseProp"
            @select="fnSelectProp"
          />
          <q-item-label
            v-if="form.prop === 0"
            class="text-red-10"
            style="font-size: 0.8em"
          >
            {{ $t("chooseProp") }}
          </q-item-label>

-->
        </div>
        <!-- name -->
        <q-input
          v-model="form.name"
          class="q-mb-md"
          @blur="onBlurName"
          dense
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          class="q-mb-md"
          :label="$t('fldFullName')"
          dense
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <div v-for="tit in titles" :key="tit.id">
          <q-input
            v-model="form['col_' + tit.id]"
            :label="tit['title']"
            class="q-mb-md"
            dense
          />
        </div>

        <q-input
          v-model="form['multiPropItemTag']"
          class="q-mb-md"
          :label="$t('propTag')"
          dense
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
          :label="$t('save')"
          @click="onOKClick"
          dense
          :disable="validName()"
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
import { useDialogPluginComponent, extend } from "quasar";
import { api } from "@/boot/axios.js";
import { notifyError, notifySuccess, pack } from "@/utils/jsutils.js";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: String,
  isChild: Boolean,
  parentName: String,
  dimMultiPropName: String,
  dimMultiPropType: Number,
  titles: {
    type: Array,
    default: () => [],
  },
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

let dta = {};
extend(true, dta, props.data);

const form = reactive(dta);
const visible = ref(false);
const et = ref(props.data.multiEntityType === undefined ? 1 : props.data.multiEntityType);
const optET = ref([]);
const entityName = ref("");
const prop = ref(props.data.prop);
const optProp = ref([]);

/*const normalizer = (node) => {
  return {
    id: node.prop,
    label: node.name,
  };
};*/

const fnCloseProp = (v) => {
  form.prop = v;
};

const fnSelectProp = (v) => {
  form.name = v.name;
  form.fullName = v.name;
};

const fnSelectET = () => {
  form.multiEntitytype = typeof et.value === "object" && et.value !== null ? et.value.id : et.value;
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const validName = () => {
  if (props.dimMultiPropType === 1) {
    if (!form.name || !form.fullName) return true;
    else if (form.name.trim().length === 0) return true;
  } else {
    if (!form.name || !form.fullName || !form.prop) return true;
    else if (form.name.trim().length === 0) return true;
  }
  return false;
};

const onOKClick = () => {
  form.name = form.name + entityName.value;
  form.fullName = form.fullName + entityName.value;

  const method = props.mode === "ins" ? "insertDimItem" : "updateDimItem";
  form.multiEntityType = typeof et.value === "object" && et.value !== null ? et.value.id : et.value;

  api
    .post("", {
      id: form.id,
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
      method: "dict/load",
      params: [{ dict: "FD_MultiValEntityType" }],
    })
    .then((response) => {
      optET.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "dimMultiProp/loadPropForMultiPropItem",
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
