<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 800px">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-card-section>
        <q-table
          color="primary"
          card-class="bg-amber-1"
          table-class="text-grey-8"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :loading="loading"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          table-header-style="size: 3em"
          separator="cell"
          dense
          :rows-per-page-options="[0]"
        >
          <template v-slot:body="cellProps">
            <q-tr :props="cellProps">
              <td style="width: 5px">
                <q-btn
                  :dense="dense"
                  flat
                  color="blue"
                  :icon="
                    cellProps.row.checked
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click="selectedRow(cellProps.row)"
                />
              </td>

              <q-td key="name" :props="cellProps">
                {{ cellProps.row.name }}
              </q-td>

              <q-td key="fullName" :props="cellProps">
                {{ cellProps.row.fullName }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
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
import { ref, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  user: [Number, String],
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } =
  useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const dense = ref(true);

const selectedRow = (row) => {
  row.checked = !row.checked;
};

const getColumns = () => [
  {
    name: "checked",
    field: "checked",
  },
  {
    name: "name",
    label: proxy?.$t("fldName"),
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em",
    style: "width: 40%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName"),
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.3em",
    style: "width: 60%",
  },
];

const onOKClick = () => {
  loading.value = true;
  const dta = [];

  rows.value.forEach((r) => {
    if (r.checked) {
      dta.push({ id: r.id });
    }
  });

  api
    .post("", {
      method: "usr/saveUserRole",
      params: [props.user, dta],
    })
    .then(() => {
      onDialogOK({ res: true });
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message) {
        msg = error.response.data.error.message;
      }
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  cols.value = getColumns();
  loading.value = true;

  api
    .post("", {
      method: "usr/loadUserRolesForUpd",
      params: [props.user],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    })
    .catch((error) => {
      let msg = error.message;
      if (error.response?.data?.error?.message) {
        msg = error.response.data.error.message;
      }
      notifyError(msg);
    })
    .finally(() => {
      loading.value = false;
    });
});
</script>

<style scoped></style>
