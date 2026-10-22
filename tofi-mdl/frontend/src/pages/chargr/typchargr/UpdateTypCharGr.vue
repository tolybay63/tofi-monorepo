<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- name -->
        <q-input
          :dense="dense"
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          :dense="dense"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- typ -->
        <q-select
          :disable="mode === 'upd'"
          :dense="dense"
          :options-dense="dense"
          v-model="typ"
          :options="optTyp"
          :label="$t('typ')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectTyp"
        />

        <!-- Cluster FV-->

        <TreeSelect
          v-model="form.factorVal"
          :label="$t('factorVal')"
          :disabled="mode === 'upd'"
          :options="optionsCFV"
          class="q-ma-md"
          node-key="key"
          @close="fnSelectCFV"
        />

<!--
        <q-item-label
          :class="form.factorVal === 0 ? 'text-red-10' : 'text-grey-7'"
          style="font-size: 0.8em; margin-top: 10px"
          class="row"
        >
          {{ $t("factorVal") }}
          <q-space />
          <q-icon
            name="error"
            v-if="form.factorVal === 0"
            color="red-10"
            size="24px"
          />
        </q-item-label>

        <div style="width: 100%; min-height: 50px;">
          <treeselect
            :disabled="mode === 'upd'"
            :options="optionsCFV"
            v-model="form.factorVal"
            :default-expand-level="1"
            :max-height="600"
            :normalizer="normalizer"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
            @select="fnSelectCFV"
          />
        </div>


        <q-item-label
          v-if="form.factorVal === 0"
          class="text-red-10"
          style="font-size: 0.8em"
        >
          {{ $t('chooseFV') }}
        </q-item-label>
-->

        <!-- cod -->
        <q-input
          :dense="dense"
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <!--accessLevel-->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="al"
          :options="optionsAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectAL"
        />

        <!-- cmt -->
        <q-input
          :dense="dense"
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
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
import { notifyError, notifySuccess, pack } from "@/utils/jsutils";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const al = ref(props.data.accessLevel);
const typ = ref(props.data.typ);

const optionsAL = ref([]);
const optionsCFV = ref([]);
const optTyp = ref([]);

const loadclustFactorVal = (typVal, modeVal) => {
  api
    .post("", {
      method: "typ/loadTypClustFactorVal",
      params: [typVal, modeVal],
    })
    .then((response) => {
      optionsCFV.value = pack(response.data.result.records, "ord");
    });
};

const fnSelectTyp = (val) => {
  form.typ = typ.value?.id || typ.value;
  optionsCFV.value = [];
  form.factorVal = null;
  loadclustFactorVal(form.typ, props.mode);
};

const fnSelectCFV = (v) => {
  form.factorVal = v.key;
};

const normalizer = (node) => {
  return {
    id: node.key,
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
  form.accessLevel = al.value?.id || al.value;
};

const validSave = () => {
  return !(
    form.name &&
    form.fullName &&
    form.typ &&
    form.factorVal &&
    form.factorVal !== 0
  );
};

const onOKClick = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.typ = typeof typ.value === "object" && typ.value !== null ? typ.value.id : typ.value;

  const method = props.mode === "ins" ? "insertTypCharGr" : "updateTypCharGr";

  api
    .post("", {
      method: "typ/" + method,
      params: [form],
    })
    .then((response) => {
      onDialogOK(response.data.result.records[0]);
      notifySuccess(proxy?.$t("success"));
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    });
};

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      optionsAL.value = response.data.result.records;
    });

  api
    .post("", {
      method: "typ/loadTypForSelect",
      params: [],
    })
    .then((response) => {
      optTyp.value = response.data.result.records;
      optTyp.value.unshift({ id: 0, name: proxy?.$t("notChosen") });
    });

  if (props.mode === "upd") {
    loadclustFactorVal(form.typ, props.mode);
  }
});
</script>
