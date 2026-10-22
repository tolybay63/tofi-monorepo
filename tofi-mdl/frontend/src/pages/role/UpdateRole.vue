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
        <q-input
          :dense="dense"
          v-model="myData.cod"
          :label="$t('code')"
          :placeholder="$t('msgCodeGen')"
        />

        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="al"
          :options="options"
          :label="$t('accessLevel')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelect"
        />

        <!-- name -->
        <q-input
          :dense="dense"
          v-model="myData.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          :dense="dense"
          v-model="myData.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- roleTag-->
        <q-input
          :dense="dense"
          v-model="myData.roleTag"
          :label="$t('fldRoleTag')"
        />

        <!-- cmt -->
        <q-input
          :dense="dense"
          v-model="myData.cmt"
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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  form: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: [String, Object],
  action: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const myData = reactive({ ...props.form });
const lang = ref(props.lg);
const act = ref(props.action);
const options = ref([]);
const al = ref(props.form.accessLevel === 0 ? 1 : props.form.accessLevel);

const onBlurName = () => {
  if (myData.name) {
    myData.name = myData.name.trim();
    if (!myData.fullName || myData.fullName.trim() === "") {
      myData.fullName = myData.name;
    }
  }
};

const fnSelect = () => {
  myData.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const validName = () => {
  if (!myData.name) return true;
  if (myData.name.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  let err = false;
  const method = props.mode === "ins" ? "insert" : "update";
  myData.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;

  api
    .post("", {
      id: myData.id,
      method: act.value + "/" + method,
      params: [{ rec: myData }],
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
});
</script>

<style scoped></style>
