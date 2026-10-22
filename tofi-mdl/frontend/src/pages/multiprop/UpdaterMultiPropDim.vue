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
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :dense="dense"
          :rows-per-page-options="[0]"
          :loading="loading"
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

              <q-td key="cod" :props="props">
                {{ props.row.cod }}
              </q-td>

              <q-td key="name" :props="props">
                {{ props.row.name }}
              </q-td>

              <q-td key="isFilled" :props="props">
                <q-btn
                  :dense="dense"
                  flat
                  color="blue"
                  :icon="
                    props.row.isFilled
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click="checkFilled(props.row)"
                />
              </q-td>

              <q-td key="dimNumber" :props="props">
                <q-input
                  :disable="!props.row.isFilled"
                  :dense="dense"
                  flat
                  color="blue"
                  type="number"
                  v-model.number="props.row.dimNumber"
                  @update:model-value="updDimNumber(props.row)"
                />
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
import { ref, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  multiPropId: [Number, String],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const updDimNumber = (r) => {
  if (!r.checked) {
    r.dimNumber = null;
  } else {
    if (r.dimNumber < 1) r.dimNumber = 1;
  }
};

const selectedRow = (row) => {
  row.checked = !row.checked;
};

const checkFilled = (row) => {
  if (!row.checked) return;
  row.isFilled = !row.isFilled;
  if (!row.isFilled) row.dimNumber = null;
};

const validSave = () => {
  return false;
};

const getColumns = () => [
  {
    name: "checked",
    field: "checked",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 60%",
  },
  {
    name: "isFilled",
    label: proxy?.$t("isFilled") || "isFilled",
    field: "isFilled",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
  {
    name: "dimNumber",
    label: "Номер измерения",
    field: "dimNumber",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 10%",
  },
];

const onOKClick = () => {
  loading.value = true;
  let dta = [];

  rows.value.forEach((r) => {
    if (r.checked) {
      dta.push({
        id: r.id,
        dimMultiProp: r["dimId"],
        isFilled: r.isFilled === undefined ? false : r.isFilled,
        dimNumber: r.dimNumber,
      });
    }
  });

  api
    .post("", {
      method: "multiProp/saveMultiPropDim",
      params: [props.multiPropId, dta],
    })
    .then(
      () => {
        onDialogOK({ res: true });
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data?.error?.message)
          msg = error.response.data.error.message;
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
      method: "multiProp/loadMultiPropDimForUpd",
      params: [props.multiPropId],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
