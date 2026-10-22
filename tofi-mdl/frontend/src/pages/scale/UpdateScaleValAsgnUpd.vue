<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    full-width
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
      <q-bar class="text-white bg-primary" dense>
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-card-section>
        <q-table
          style="height: 100%; width: 100%"
          dense color="primary"
          card-class="bg-amber-1"
          row-key="val"
          :columns="cols"
          :rows="rows"
          :wrap-cells="true"
          :table-colspan="4"
          table-header-class="text-bold text-white bg-blue-grey-13"
          separator="cell"
          :loading="loading"
          selection="single"
        >
          <template v-slot:body="props">
            <q-tr :props="props">
              <td style="width: 5px">
                <q-btn
                  dense flat color="blue"
                  :icon="props.row.checked ? 'check_box' : 'check_box_outline_blank'"
                  @click="selectedRow(props.row)"
                />
              </td>

              <q-td key="val" :props="props">
                {{ props.row.val }}
              </q-td>

              <q-td key="name" :props="props">
                {{ props.row.name }}
              </q-td>

              <q-td key="fullName" :props="props">
                {{ props.row.fullName }}
              </q-td>

              <q-td key="scaleValColor" :props="props">
                <q-avatar size="28px" :style="fnStyle(props.row)" />
                {{ props.row.scaleValColor }}
              </q-td>

              <q-td key="cmd" :props="props">
                <q-btn
                  :disable="!props.row.checked"
                  round
                  size="sm"
                  icon="edit"
                  color="blue"
                  flat
                  dense
                  @click="fnEdit(props.row)"
                  class="no-padding no-margin"
                >
                  <q-tooltip
                    transition-show="rotate"
                    transition-hide="rotate"
                  >
                    {{ $t("update") }}
                  </q-tooltip>
                </q-btn>
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save" dense
          :label="$t('save')"
          @click="onOKClick"
        >
          <template #loading>
            <q-spinner-hourglass color="white" />
          </template>
        </q-btn>
        <q-btn
          color="primary"
          icon="cancel" dense
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent, useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "@/utils/jsutils";
import UpdateScaleValAsgn from "@/pages/scale/UpdateScaleValAsgn.vue";

const props = defineProps({
  scale: [Number, String],
  scaleAsgn: [Number, String],
  nmAsgn: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const $q = useQuasar();
const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const fnStyle = (row) => {
  return "background:" + row["scaleValColor"];
};

const selectedRow = (row) => {
  row.checked = !row.checked;
};

const fnEdit = (row) => {
  $q.dialog({
    component: UpdateScaleValAsgn,
    componentProps: {
      data: row,
    },
  }).onOk((r) => {
    for (let key in r) {
      if (Object.prototype.hasOwnProperty.call(r, key)) {
        row[key] = r[key];
      }
    }
  });
};

const getColumns = () => [
  {
    name: "val",
    label: proxy?.$t("val") || "val",
    field: "val",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 20%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 35%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 45%",
  },
  {
    name: "scaleValColor",
    label: proxy?.$t("scaleColor") || "scaleColor",
    field: "scaleValColor",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
  {
    name: "cmd",
    label: "",
    field: "cmd",
    align: "center",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em; width: 5%",
  },
];

const onOKClick = () => {
  loading.value = true;
  let err = false;
  let data = [];
  rows.value.forEach((r) => {
    if (r.checked) {
      data.push(r);
    }
  });

  api
    .post("", {
      method: "scale/updateScaleValAsgn",
      params: [props.scale, props.scaleAsgn, data],
    })
    .then(
      () => {
        err = false;
        onDialogOK({ res: true });
        notifySuccess(proxy?.$t("success") || "success");
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

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "scale/loadScaleValAsgnUpd",
      params: [props.scale, props.scaleAsgn, props.nmAsgn],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
