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
        <!-- name -->
        <q-input
          dense
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- fullName-->
        <q-input
          dense
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />

        <!-- card-->
        <q-input
          dense
          type="number"
          v-model="form.card"
          :label="$t('fldCard')"
          :rules="[(val) => val >= 0 || $t('msgCard')]"
        />

        <!-- cmt -->
        <q-input
          dense
          v-model="form.cmt"
          type="textarea"
          :label="$t('fldCmt')"
        />
      </q-card-section>

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
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: [String, Object],
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const validSave = () => {
  return !form.name || !form.fullName || form.card < 0;
};

const onOKClick = () => {
  let err = false;

  api
    .post("", {
      id: form.id,
      method: "relcls/updateRelClsMember",
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
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_MemberType" }],
    })
    .then((response) => {
      let optMT = response.data.result.records;
      optMT.splice(2, 2);
    });

  api
    .post("", {
      method: "role/loadRoles",
      params: [{}],
    })
    .then((response) => {
      let relroles = response.data.result.records;
      relroles.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });

  api
    .post("", {
      method: "typ/loadTypForSelect",
      params: [],
    })
    .then((response) => {
      let optTyp = response.data.result.records;
      optTyp.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });

  api
    .post("", {
      method: "reltyp/loadRelTypForSelect",
      params: [],
    })
    .then((response) => {
      let optRT = response.data.result.records;
      optRT.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });
});
</script>

<style scoped></style>
