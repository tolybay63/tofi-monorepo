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

      <q-card>
        <q-card-section>
          <!-- Cod -->
          <q-input
            style="margin-bottom: 10px"
            :dense="dense"
            v-model="form.cod"
            :label="$t('code')"
            :placeholder="$t('msgCodeGen')"
          />

          <!-- name -->
          <q-input
            autofocus
            :dense="dense"
            v-model="form.name"
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

          <!-- isOpenness -->
          <q-toggle
            style="margin-left: 5px; margin-top: 5px; margin-bottom: 10px"
            :dense="dense"
            v-model="form.isOpenness"
            :label="$t('isOpenness')"
          />

          <!-- accessLevel -->
          <q-select
            :dense="dense"
            v-model="al"
            :options="optionsLevel"
            :label="$t('accessLevel')"
            option-value="id"
            option-label="text"
            map-options
            @update:model-value="fnSelectAL"
          />

          <!-- database -->
          <q-select
            :dense="dense"
            v-model="db"
            :options="optionsDB"
            :label="$t('modelNameLabel')"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnSelectDB"
          />

          <!-- cmt -->
          <q-input
            :dense="dense"
            v-model="form.cmt"
            type="textarea"
            :label="$t('fldCmt')"
          />
        </q-card-section>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>

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
  typ: [Number, String],
  lg: [String, Object],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const loading = ref(false);
const separator = ref("cell");
const optionsLevel = ref([]);
const al = ref(props.data.accessLevel);

const optionsDB = ref([]);
const db = ref(props.data.dataBase);
const rows = ref([]);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelectAL = () => {
  form.accessLevel = al.value?.id !== undefined ? al.value.id : al.value;
};

const fnSelectDB = () => {
  form.dataBase = db.value?.id !== undefined ? db.value.id : db.value;
};

const validSave = () => {
  return !(
    form.name &&
    form.fullName &&
    form.dataBase !== null
  );
};

const onOKClick = () => {
  let ids = [];
  rows.value.forEach((row) => {
    const { children } = row;
    if (children) {
      children.forEach((it) => {
        if (it.checked === 1) ids.push(it.id.toString());
      });
    }
  });

  const method = props.mode === "ins" ? "insert" : "update";
  let err = false;
  loading.value = true;

  api
    .post("", {
      method: "relcls/" + method,
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
          msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;
        }
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
      optionsLevel.value = response.data.result.records;
    });

  api
    .post("", {
      method: "database/loadDbForSelect",
      params: [],
    })
    .then((response) => {
      optionsDB.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
