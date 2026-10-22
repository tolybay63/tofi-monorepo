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

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <div class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- factor -->
        <q-btn @click="selectFV" class="q-mt-md full-width">
          Значение фасторов
        </q-btn>

        <!-- name -->
        <q-input
          class="q-mt-md"
          :dense="dense"
          v-model="form.name"
          @blur="onBlurName"
          :label="$t('fldName')"
          :disable="fvs === ''"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        />
        <!-- fullName-->
        <q-input
          :dense="dense"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :disable="fvs === ''"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
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
import { ref, reactive, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "@/utils/jsutils";
import SelectFV from "@/pages/dimprop/SelectFV.vue";
import { extend } from "quasar";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  dimPropName: String,
  parentName: String,
  dimprop: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive(extend({}, props.data));
const visible = ref(false);
const lang = ref(props.lg);
const fvs = ref("");

const selectFV = () => {
  let dimPropItem = 0;
  if (props.mode === "upd") dimPropItem = form.id;

  $q.dialog({
    component: SelectFV,
    componentProps: {
      dimPropItem: dimPropItem,
      lg: lang.value,
      dense: true,
    },
  }).onOk((data) => {
    fvs.value = data.fvs;
    form.fvs = data.fvs;
    form.name = props.dimPropName + " (" + data.nms + ")";
    form.fullName = props.dimPropName + " (" + data.nms + ")";
  });
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const validSave = () => {
  if (!form.name) return true;
  if (form.name.trim().length === 0) return true;
  return false;
};

const onOKClick = () => {
  visible.value = true;
  const method = props.mode === "ins" ? "insertDPI" : "updateDPI";

  api
    .post("", {
      id: form.id,
      method: "dimprop/" + method,
      params: [form],
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
      visible.value = false;
    });
};
</script>

<style scoped></style>
