<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
  >
    <q-card class="q-dialog-plugin" style="width: 600px">
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
          table-header-style="size: 3em"
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
                    props.row.checked === 1
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

              <q-td key="isDefault" :props="props">
                <q-btn
                  :dense="dense"
                  flat
                  color="blue"
                  :icon="
                    props.row.isDefault === 1
                      ? 'check_box'
                      : 'check_box_outline_blank'
                  "
                  @click="checkDefault(props.row)"
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
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifyInfo } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const cols = ref([]);
const rows = ref([]);
const loading = ref(false);

const selectedRow = (row) => {
  row.checked = !row.checked;
  if (!row.checked) row.isDefault = null;
};

const checkDefault = (row) => {
  if (!row.checked) return;
  if (row.isDefault)
    row.isDefault = null;
  else {
    rows.value.forEach((r) => {
      r.isDefault = null;
    });
    row.isDefault = 1;
  }
};

const validSave = () => {
  let i = 0;
  let isDef = false;
  rows.value.forEach((r) => {
    if (r.checked) {
      i++;
      if (r.isDefault) isDef = true;
    }
  });
  return i > 0 && !isDef;
};

const getColumns = () => [
  {
    name: "checked",
    field: "checked",
  },
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 20%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 70%",
  },
  {
    name: "isDefault",
    label: proxy?.$t("isDefault") || "isDefault",
    field: "isDefault",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 10%",
  },
];

const onOKClick = () => {
  loading.value = true;
  let dta = [];

  rows.value.forEach((r) => {
    if (r.checked) {
      dta.push({
        id: r.id,
        factorVal: r.factorVal,
        isDefault: r.isDefault === undefined ? null : r.isDefault,
      });
    }
  });

  api
    .post("", {
      method: form.act + "/saveStatus",
      params: [form.fk, dta],
    })
    .then(
      (response) => {
        let res = response.data.result;
        if (res !== "") {
          let msg = res + " участвует в данных";
          if (res.includes(",")) msg = res + " участвуют в данных";
          notifyInfo(msg);
        }
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
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: form.act + "/loadStatusForUpd",
      params: [form.fk, form.factor],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
