<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    style="width: 600px"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" dense>
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" dense>
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <q-input
          v-model="form.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
          dense class="q-pt-sm"
        />
        <!-- name -->
        <q-input
          v-model="form.name"
          dense autofocus @blur="onBlurName"
          :label="$t('fldName')" class="q-pt-sm"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          dense :label="$t('fldFullName')" class="q-pt-sm"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- SourceStock -->
        <q-select
          v-model="ss"
          :options="optSS"
          :label="$t('stocks')"
          option-value="id"
          option-label="name"
          map-options class="q-pt-sm"
          dense options-dense
          @update:model-value="fnSelectSS"
        />

        <!-- SysCodingType -->
        <q-select
          v-model="sc"
          :options="optSC"
          :label="$t('sysCodingType')"
          option-value="id"
          option-label="text"
          map-options class="q-pt-md"
          dense options-dense
          @update:model-value="fnSelectSC"
        />

        <!-- AccessLevel -->
        <q-select
          v-model="al"
          :options="optAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options class="q-pt-md"
          dense options-dense
          @update:model-value="fnSelectAL"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
          dense type="textarea" class="q-pt-md"
          :label="$t('fldCmt')"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>
        <q-btn
          color="primary"
          icon="cancel" dense
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

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
const al = ref(props.data.accessLevel);
const optAL = ref([]);
const ss = ref(props.data.sourceStock);
const optSS = ref([]);
const sc = ref(props.data.sysCodingType);
const optSC = ref([]);
const loading = ref(false);

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

const fnSelectSC = () => {
  form.sysCodingType = typeof sc.value === "object" && sc.value !== null ? sc.value.id : sc.value;
};

const fnSelectSS = () => {
  form.sourceStock = typeof ss.value === "object" && ss.value !== null ? ss.value.id : ss.value;
};

const validSave = () => {
  return form.name === "" || form.fullName === "";
};

const onOKClick = () => {
  loading.value = true;
  let err = false;

  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.sourceStock = typeof ss.value === "object" && ss.value !== null ? ss.value.id : ss.value;
  form.sysCodingType = typeof sc.value === "object" && sc.value !== null ? sc.value.id : sc.value;

  api
    .post("", {
      id: form.id,
      method: "syscoding/" + method,
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
        let msg = error.response?.data?.error?.message || error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
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
      method: "dict/load",
      params: [{ dict: "FD_SysCodingType" }],
    })
    .then((response) => {
      optSC.value = response.data.result.records;
    });

  api
    .post("", {
      method: "stock/loadStockForSelect",
      params: [],
    })
    .then((response) => {
      optSS.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
