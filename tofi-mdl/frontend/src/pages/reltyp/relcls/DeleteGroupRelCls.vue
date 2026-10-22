<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    full-width
    full-height
  >
    <q-card class="q-dialog-plugin">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("deleteGroupRecords") }}</div>
      </q-bar>

      <q-bar class="bg-orange-1" style="height: 48px">
        <q-space />
        <q-card-actions align="right">
          <q-btn
            :loading="loading"
            dense
            color="secondary"
            icon="delete"
            :label="$t('delete')"
            :disable="validSave()"
            @click="onOKClick"
          >
            <template #loading>
              <q-spinner-hourglass color="white" />
            </template>
          </q-btn>

          <q-btn
            dense
            color="secondary"
            icon="cancel"
            :label="$t('cancel')"
            @click="onDialogCancel"
          />
        </q-card-actions>
      </q-bar>

      <div>
        <q-table
          style="height: calc(100vh - 200px); width: 100%"
          color="primary"
          card-class="bg-amber-1"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          dense
          selection="multiple"
          v-model:selected="selected"
          :rows-per-page-options="[20, 15, 0]"
        />
      </div>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  relTyp: [Number, String],
  lg: [String, Object],
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const lang = ref(props.lg);
const loading = ref(false);
const cols = ref([]);
const rows = ref([]);
const selected = ref([]);

const validSave = () => {
  return selected.value.length === 0;
};

const onOKClick = () => {
  loading.value = true;
  let err = false;

  api
    .post("", {
      method: "relcls/deleteGroupRelCls",
      params: [props.relTyp, selected.value],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result);
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

const getColumns = () => [
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width:35%;",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em;width:50%;",
  },
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width:15%;",
  },
];

const loadData = () => {
  loading.value = true;

  api
    .post("", {
      method: "relcls/load",
      params: [props.relTyp],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message)
        msg = proxy?.$t(error.response.data.error.message) || error.response.data.error.message;

      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  cols.value = getColumns();
  loadData();
});
</script>

<style scoped></style>
