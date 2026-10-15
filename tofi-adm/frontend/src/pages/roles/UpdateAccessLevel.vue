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
      <q-bar class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="loading" color="secondary" />

      <q-card-section>
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
          clearable
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
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
import { ref, reactive, onMounted } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();

const form = reactive(JSON.parse(JSON.stringify(props.data || {})));
const options = ref([]);
const al = ref(props.data?.accessLevel ?? null);
const loading = ref(false);

const fnSelect = () => {
  form.accessLevel = al.value ? al.value.id : null;
};

const onOKClick = () => {
  onDialogOK(form);
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "dict/loadDictAsStore",
      params: ["FD_AccessLevel"],
    })
    .then((response) => {
      options.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
