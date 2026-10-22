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

      <q-inner-loading :showing="loading" color="secondary" />

      <q-card>
        <q-card-section>
          <!-- cod -->
          <q-input
            dense
            v-model="form.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
          />

          <!-- nameTable -->
          <q-input
            autofocus
            dense
            v-model="form.nameTable"
            :label="$t('tableName')"
            :rules="[(val) => !validTableName(val) || $t('validTableName')]"
          />

          <!-- accessLevel -->
          <q-select
            dense
            v-model="al"
            :options="optAL"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            @update:model-value="fnAL"
          />

          <!-- name -->
          <q-input
            dense
            v-model="form.name"
            @blur="onBlurName"
            :label="$t('fldName')"
            :rules="[(val) => validName(val) || $t('req')]"
          />
          <!-- fullName-->
          <q-input
            dense
            v-model="form.fullName"
            :label="$t('fldFullName')"
            :rules="[(val) => validName(val) || $t('req')]"
          />
          <!-- cls -->
          <TreeSelect
            :disabled="!!form.relCls"
            v-model="form['form.cls']"
            :label="$t('objCls')"
            :options="optCls"
            class="q-ma-md"
            node-key="key"
          />

<!--
          <q-item-label
            :class="form.cls < 0 ? 'text-red-10' : 'text-grey-7'"
            style="font-size: 0.8em"
            class="row"
          >
            {{ $t("objCls") }}
            <q-space />
            <q-icon name="error" v-if="form.cls < 0" color="red-10" size="24px" />
          </q-item-label>

          <treeselect
            :disabled="!!form.relCls"
            :options="optCls"
            :max-height="600"
            v-model="form.cls"
            :flat="true"
            :default-expand-level="1"
            :show-count="true"
            :normalizer="normalizerCls"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
          />
          <q-item-label v-if="form.cls < 0" class="text-red-10" style="font-size: 0.8em">
            {{ $t("chooseCls") }}
          </q-item-label>
-->

          <!-- relCls -->
          <TreeSelect
            :disabled="!!form.relCls"
            v-model="form['form.relCls']"
            :label="$t('relCls')"
            :options="optRelCls"
            class="q-ma-md"
            node-key="key"
          />

<!--
          <div
            :class="form.relCls === 0 ? 'text-red-10' : 'text-grey-7'"
            style="margin-top: 10px; font-size: 0.8em"
            class="row"
          >
            {{ $t("relCls") }}
            <q-space />
            <q-icon name="error" v-if="form.relCls === 0" color="red-10" size="24px" />
          </div>

          <treeselect
            :disabled="!!form.cls"
            :options="optRelCls"
            v-model="form.relCls"
            :default-expand-level="1"
            :normalizer="normalizerRelCls"
            :placeholder="$t('select')"
            :noChildrenText="$t('noChilds')"
            :noResultsText="$t('noResult')"
            :noOptionsText="$t('noResult')"
          />
          <q-item-label v-if="form.relCls === 0" class="text-red-10" style="font-size: 0.8em">
            {{ $t("chooseRelCls") }}
          </q-item-label>
-->

          <!-- cmt -->
          <q-input
            dense
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
          />
        </q-card-section>
      </q-card>

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
  rec: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: Object,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.rec });
const loading = ref(false);

const optAL = ref([]);
const al = ref(props.rec.accessLevel);

const optCls = ref([]);
const optRelCls = ref([]);

const fnAL = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const validName = (val) => {
  return !!val && !!val.trim();
};

const valid = (val) => {
  let reg = /^[a-zA-Z0-9_]+$/;
  return reg.test(val);
};

const validTableName = (v) => {
  return !valid(v);
};

const validSave = () => {
  return !(
    !validTableName(form.nameTable) &&
    validName(form.name) &&
    validName(form.fullName) &&
    (form.cls > 0 || form.relCls > 0)
  );
};

/*
const normalizerCls = (node) => {
  return {
    id: node.cls,
    label: node.name,
  };
};

const normalizerRelCls = (node) => {
  return {
    id: node.relcls,
    label: node.name,
  };
};
*/

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertFlatTable" : "updateFlatTable";

  loading.value = true;
  let err = false;
  api
    .post("", {
      method: "flatTable/" + method,
      params: [form],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let msg = error.message;
        if (error.response?.data?.error?.message) {
          msg = error.response.data.error.message;
        }
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
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
      method: "dimMultiProp/loadClsForSelect",
      params: [],
    })
    .then(
      (response) => {
        optCls.value = pack(response.data.result.records, "id");
      },
      (error) => {
        notifyError("class" + error.message);
      }
    )
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "dimMultiProp/loadRelClsForSelect",
      params: [],
    })
    .then(
      (response) => {
        optRelCls.value = pack(response.data.result.records, "id");
      },
      (error) => {
        notifyError(error.message);
      }
    )
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
