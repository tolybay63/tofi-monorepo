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
        <q-input
          v-model="form.cod"
          :label="$t('code')"
          dense
          :placeholder="$t('msgCodeGen')"
        />

        <q-select
          v-model="al"
          :options="options"
          options-dense
          dense
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelect"
        />

        <!-- name -->
        <q-input
          v-model="form.name"
          autofocus
          dense
          @blur="onBlurName"
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

        <q-select
          v-model="dmpt"
          :options="optDMPT"
          :label="$t('dimMultiPropType')"
          option-value="id"
          option-label="text"
          dense
          options-dense
          map-options
          @update:model-value="fnSelectDMPT"
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
          dense
          @click="onOKClick"
          :disable="validName()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          dense
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent, extend } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "@/utils/jsutils";

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

const form = reactive(extend({}, props.data));
const lang = ref(props.lg);
const options = ref([]);
const al = ref(props.data.accessLevel === 0 ? 1 : props.data.accessLevel);
const dmpt = ref(props.data.dimMultiPropType === 0 ? 1 : props.data.dimMultiPropType);
const optDMPT = ref([]);
const optProp = ref([]);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelect = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const fnSelectDMPT = () => {
  form.dimMultiPropType = typeof dmpt.value === "object" && dmpt.value !== null ? dmpt.value.id : dmpt.value;
};

const validName = () => {
  if (
    !form.name ||
    !form.fullName ||
    !form.accessLevel ||
    !form.dimMultiPropType
  ) {
    return true;
  }
  return false;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.dimMultiPropType = typeof dmpt.value === "object" && dmpt.value !== null ? dmpt.value.id : dmpt.value;

  api
    .post("", {
      id: form.id,
      method: "dimMultiProp/" + method,
      params: [{ rec: form }],
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
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      options.value = response.data.result.records;
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_DimMultiPropType" }],
    })
    .then((response) => {
      optDMPT.value = response.data.result.records;
    });

  api
    .post("", {
      method: "dimMultiProp/loadPropForMultiPropItem",
      params: [],
    })
    .then((response) => {
      optProp.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
