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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>
      <q-card-section>
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

        <!--reltyp-->
        <q-select
          dense
          options-dense
          v-model="form.relTyp"
          :options="optRelTyp"
          :label="$t('reltyp')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectRelTyp"
        />

        <!--relcls-->
        <q-select
          dense
          options-dense
          v-model="form.relCls"
          :options="optRelCls"
          :label="$t('relcls')"
          option-value="id"
          option-label="name"
          map-options
          clearable
          @clear="fnClearRelCls"
          @update:model-value="fnSelectRelCls"
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
          @update:model-value="fnSelect"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const optRelTyp = ref([]);
const optRelCls = ref([{ id: null, name: "Все классы" }]);
const al = ref(props.data.accessLevel);
const optionsAL = ref([]);
const loading = ref(false);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const loadRelCls = (relTyp) => {
  loading.value = true;
  api
    .post("", {
      method: "reltyp/loadRelClsForSelect",
      params: [relTyp],
    })
    .then((response) => {
      optRelCls.value = response.data.result.records;
      optRelCls.value.push({ id: null, name: "Все классы" });
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnSelectRelTyp = (val) => {
  form.relTyp = val ? val.id : null;
  if (val) {
    loadRelCls(val.id);
  } else {
    optRelCls.value = [{ id: null, name: "Все классы" }];
    form.relCls = null;
  }
};

const fnClearRelCls = () => {
  form.relCls = null;
};

const fnSelectRelCls = (val) => {
  form.relCls = val ? val.id : null;
};

const fnSelect = (val) => {
  form.accessLevel = val ? val.id : null;
};

const validSave = () => {
  return !(
    form.name !== null &&
    form.name !== undefined &&
    form.name.trim() !== "" &&
    form.fullName !== null &&
    form.fullName !== undefined &&
    form.fullName.trim() !== ""
  );
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertRelTypCharGr" : "updateRelTypCharGr";

  // При сохранении приводим al/relTyp/relCls к значениям id, если в модели остались объекты
  form.accessLevel = typeof al.value === 'object' && al.value !== null ? al.value.id : al.value;
  if (typeof form.relTyp === 'object' && form.relTyp !== null) form.relTyp = form.relTyp.id;
  if (typeof form.relCls === 'object' && form.relCls !== null) form.relCls = form.relCls.id;

  api
    .post("", {
      method: "reltyp/" + method,
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
  loading.value = true;
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_AccessLevel" }],
    })
    .then((response) => {
      optionsAL.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });

  loading.value = true;
  api
    .post("", {
      method: "reltyp/loadRelTypForSelect",
      params: [],
    })
    .then((response) => {
      optRelTyp.value = response.data.result.records;
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    })
    .finally(() => {
      loading.value = false;
    });

  if (props.mode === "upd" && props.data.relTyp) {
    loadRelCls(props.data.relTyp);
  }
});
</script>
