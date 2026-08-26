<template>
  <q-dialog
    ref="dialog"
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
        <q-item-section v-if="isChild">
          {{ $t("parent") }}: {{ parentName }}
        </q-item-section>

        <!-- name -->
        <q-input
          :model-value="form.name"
          v-model="form.name"
          autofocus
          :label="$t('nameCalc')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validName()"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {getCurrentInstance, onMounted, reactive, ref} from "vue";
import {api} from "@/boot/axios";
import {notifyError, notifySuccess} from "@/utils/jsutils";

const props = defineProps({
  mode: String,
  isChild: Boolean,
  parentName: String,
  data: Object
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();

const dialog = ref(null);
const form = reactive({ ...props.data });

const validName = () => {
  return (form.name === "");
};

const show = () => {
  dialog.value?.show();
};

const hide = () => {
  dialog.value?.hide();
};

const onDialogHide = () => {
  emit("hide");
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertCalc" : "updateCalc";

  api
    .post("", {
      method: "data/" + method,
      params: [form],
    })
    .then(
      () => {
        emit("ok", {res: true});
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = error.response.data?.error?.message;
        console.error(msg);
      }
    )
    .finally(() => {
      hide();
    });
};

const onCancelClick = () => {
  hide();
};

defineExpose({
  show,
  hide
});

onMounted(() => {
console.info("onMounted upd", props.isChild, props.parentName)
})

</script>
