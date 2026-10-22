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
          dense
        />
        <!-- name -->
        <q-input
          v-model="form.name"
          dense autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          v-model="form.fullName"
          dense :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- ScaleType -->
        <q-select
          v-model="st"
          :options="optionsST"
          :label="$t('scaleType')"
          option-value="id"
          option-label="text"
          map-options
          dense options-dense
          @update:model-value="fnSelectST"
        />

        <!-- AccessLevel -->
        <q-select
          v-model="al"
          :options="optionsAL"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          dense options-dense
          @update:model-value="fnSelectAL"
        />

        <!-- cmt -->
        <q-input
          v-model="form.cmt"
          dense type="textarea"
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
const optionsAL = ref([]);
const optionsST = ref([]);
const loading = ref(false);
const al = ref(props.data.accessLevel);
const st = ref(props.data.scaleType);

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

const fnSelectST = () => {
  form.scaleType = typeof st.value === "object" && st.value !== null ? st.value.id : st.value;
};

const validSave = () => {
  return form.name === "" || form.fullName === "";
};

const onOKClick = () => {
  loading.value = true;
  let err = false;

  const method = props.mode === "ins" ? "insert" : "update";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
  form.scaleType = typeof st.value === "object" && st.value !== null ? st.value.id : st.value;

  api
    .post("", {
      id: form.id,
      method: "scale/" + method,
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
        let errMsg = error.response?.data?.error?.message || error.message;
        notifyError(errMsg);
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
      optionsAL.value = response.data.result.records;
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_ScaleType" }],
    })
    .then((response) => {
      optionsST.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
