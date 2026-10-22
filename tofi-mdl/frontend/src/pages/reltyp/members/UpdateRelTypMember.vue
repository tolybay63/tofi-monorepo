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

        <!-- card-->
        <q-input
          :dense="dense"
          type="number"
          v-model="form.card"
          :label="$t('fldCard')"
          :rules="[(val) => val >= 0 || $t('msgCard')]"
        />

        <!-- memberTyp -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="mt"
          :options="optMT"
          :label="$t('memberType')"
          option-value="id"
          option-label="text"
          map-options
          :disable="mode === 'upd'"
          @update:model-value="fnSelectMT"
        />

        <!-- typ -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="typ"
          :options="optTyp"
          :label="$t('typ')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectTyp"
          :disable="typ_disabled"
        />

        <!-- relTypMemb -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="relTypMemb"
          :options="optRT"
          :label="$t('reltyp')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelectRT"
          :disable="!typ_disabled"
        />

        <!-- role -->
        <q-select
          :dense="dense"
          v-model="relrole"
          :options="relroles"
          :label="$t('role2')"
          option-value="id"
          option-label="name"
          map-options
          @update:model-value="fnSelRole"
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
import allConsts from "@/pages/all-consts.js";
import { notifyError, notifySuccess } from "@/utils/jsutils";

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
const optMT = ref([]);
const mt = ref(props.data.memberType);
const relroles = ref([]);
const relrole = ref(props.data.role);
const optTyp = ref([]);
const typ = ref(props.data.typ);
const optRT = ref([]);
const relTypMemb = ref(props.data.relTypMemb);

const typ_disabled = ref(false);

const fnSelectMT = () => {
  form.memberType = mt.value?.id !== undefined ? mt.value.id : mt.value;
  if (form.memberType === allConsts.FD_MemberType.typ) {
    relTypMemb.value = null;
    form.relTypMemb = null;
    typ_disabled.value = false;
  } else {
    typ.value = null;
    form.typ = null;
    typ_disabled.value = true;
  }
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelRole = () => {
  form.role = relrole.value?.id !== undefined ? relrole.value.id : relrole.value;
};

const fnSelectTyp = () => {
  form.typ = typ.value?.id !== undefined ? typ.value.id : typ.value;
  form.relTypMemb = null;
};

const fnSelectRT = () => {
  form.typ = null;
  form.relTypMemb = relTypMemb.value?.id !== undefined ? relTypMemb.value.id : relTypMemb.value;
};

const validSave = () => {
  return (
    !form.name ||
    !form.fullName ||
    form.card < 0 ||
    (form.memberType === 1 && form.typ === 0) ||
    (form.memberType === 2 && form.relTypMemb === 0)
  );
};

const onOKClick = () => {
  let err = false;
  const method =
    props.mode === "ins" ? "insertRelTypMember" : "updateRelTypMember";

  api
    .post("", {
      id: form.id,
      method: "reltypmember/" + method,
      params: [{ rec: form }],
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
      optMT.value = response.data.result.records;
      optMT.value.splice(2, 2);
    });

  api
    .post("", {
      method: "role/loadRoles",
      params: [{}],
    })
    .then((response) => {
      relroles.value = response.data.result.records;
      relroles.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });

  api
    .post("", {
      method: "typ/loadTypForSelect",
      params: [],
    })
    .then((response) => {
      optTyp.value = response.data.result.records;
      optTyp.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });

  api
    .post("", {
      method: "reltyp/loadRelTypForSelect",
      params: [],
    })
    .then((response) => {
      optRT.value = response.data.result.records;
      optRT.value.unshift({ id: 0, name: proxy?.$t("notChosen") || "notChosen" });
    });

  if (props.mode === "ins") {
    typ_disabled.value = false;
  } else {
    typ_disabled.value = form.memberType !== allConsts.FD_MemberType.typ;
  }
});
</script>

<style scoped></style>
