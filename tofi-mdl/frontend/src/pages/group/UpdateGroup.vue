<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-item-section v-if="isChild">
          {{ $t("parentGroup") }}: {{ parentName }}
        </q-item-section>
        <!-- cod -->
        <q-input
          :dense="dense"
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />
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
          :dense="dense"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- accessLevel -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectAL"
          style="margin-bottom: 5px"
        />

        <!-- parent -->
        <TreeSelect
          v-model="form['parent']"
          :label="$t('parent')"
          :options="parents"
          class="q-my-md" :dense="dense"
          node-key="key"
          @close="fnCloseParent"
        />


<!--
        <q-item-label
          class="text-grey-7"
          style="font-size: 0.8em; margin-top: 10px"
        >
          {{ $t("parent") }}
        </q-item-label>
        <treeselect
          :normalizer="normalizer"
          :options="parents"
          v-model="parent"
          :placeholder="$t('select')"
          :noChildrenText="$t('noChilds')"
          :noResultsText="$t('noResult')"
          :noOptionsText="$t('noResult')"
          @close="fnCloseParent"
        />
-->

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
          :disable="validName()"
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
  lg: String,
  isChild: Boolean,
  parentName: String,
  tableName: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const optAL = ref([]);
const al = ref(props.data.accessLevel);

const parents = ref([]);
const parent = ref(props.data.parent);

const fnCloseParent = (v) => {
  form.parent = v;
  parent.value = v;
};

/*
const normalizer = (node) => {
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

const fnSelectAL = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const validName = () => {
  if (!form.name) return true;
  if (form.name.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;

  api
    .post("", {
      id: form.id,
      method: "group/" + method,
      params: [{ rec: form, tableName: props.tableName }],
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
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      optAL.value = response.data.result.records;
    });

  api
    .post("", {
      method: "group/loadGroupForSelect",
      params: [{ id: props.data.id, tableName: props.tableName }],
    })
    .then((response) => {
      parents.value = pack(response.data.result.records, "ord");
      if (props.mode === "upd") {
        parents.value.push({ id: 0, name: "Выбор...", children: [] });
      }
    });
});
</script>

<style scoped></style>
