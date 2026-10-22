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

      <q-card-section class="no-padding">
        <q-table
          style="height: 600px"
          color="primary"
          card-class="bg-amber-1"
          table-class="text-grey-8"
          row-key="id"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :dense="dense"
          :rows-per-page-options="[0]"
        >
          <template v-slot:body="props">
            <q-tr :props="props">
              <td style="width: 5px">
                <q-btn
                  :dense="dense"
                  flat
                  color="blue"
                  :icon="
                    props.row.checked
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click="selectedRow(props.row)"
                />
              </td>

              <q-td key="name" :props="props">
                {{ props.row.name }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  dimPeriodItem: [Number, String],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);
const maxCnt = ref(0);

const selectedRow = (row) => {
  row.checked = !row.checked;
};

const validSave = () => {
  let i = 0;
  rows.value.forEach((r) => {
    if (r.checked) {
      i++;
    }
  });
  return i === maxCnt.value;
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
    headerStyle: "font-size: 1.2em",
    style: "width: 100%",
  },
];

const onOKClick = () => {
  loading.value = true;
  let dta = [];

  rows.value.forEach((r) => {
    if (r.checked) {
      dta.push({ id: r.id, numb: r.numb, name: r.name });
    }
  });

  api
    .post("", {
      method: "dimperiod/updateNotIn",
      params: [props.dimPeriodItem, dta],
    })
    .then(
      () => {
        onDialogOK({ res: true });
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message) {
          msg = error.response.data.error.message;
        }
        notifyError(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
};

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "dimperiod/loadNotInForUpdate",
      params: [props.dimPeriodItem],
    })
    .then((response) => {
      rows.value = response.data.result.records;
      maxCnt.value = rows.value.length;
    });
});
</script>

<style scoped></style>
