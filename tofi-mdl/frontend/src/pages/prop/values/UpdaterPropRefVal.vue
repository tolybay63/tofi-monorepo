<template>
  <q-dialog
    ref="dialogRef"
    @hide="onDialogHide"
    persistent
    autofocus
    transition-show="slide-up"
    transition-hide="slide-down"
    full-height
    full-width
  >
    <q-card class="q-dialog-plugin no-scroll">
      <q-bar class="text-white bg-primary">
        <div>{{ $t("update") }}</div>
      </q-bar>

      <q-bar style="height: 45px">
        <q-btn
          dense
          icon="expand_circle_down"
          color="secondary"
          class="q-ml-sm q-mr-sm"
          @click="onAll"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("onAll") }}
          </q-tooltip>
        </q-btn>

        <q-btn
          dense
          icon="unpublished"
          color="secondary"
          class="q-ml-sm"
          @click="offAll"
        >
          <q-tooltip transition-show="rotate" transition-hide="rotate">
            {{ $t("offAll") }}
          </q-tooltip>
        </q-btn>

        <q-space />

        <q-btn
          class="q-mr-sm"
          :loading="loading"
          :dense="dense"
          color="secondary"
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
          class="q-ml-sm"
          :dense="dense"
          color="secondary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onDialogCancel"
        />
      </q-bar>

      <div
        class="q-table-container q-table--dense wrap bg-orange-1 scroll"
        style="height: 90%"
      >
        <div class="q-pa-sm-sm bg-orange-1">
          <q-card-section>
            <q-table
              virtual-scroll
              color="primary"
              card-class="bg-amber-1"
              table-class="text-grey-8"
              row-key="id"
              :columns="cols"
              :rows="rows"
              :wrap-cells="true"
              :table-colspan="4"
              table-header-class="text-bold text-white bg-blue-grey-13"
              separator="ref"
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

                  <q-td key="cod" :props="props">
                    {{ props.row.cod }}
                  </q-td>

                  <q-td key="name" :props="props">
                    {{ props.row.name }}
                  </q-td>

                  <q-td key="fullName" :props="props">
                    {{ props.row.fullName }}
                  </q-td>
                </q-tr>
              </template>
            </q-table>
          </q-card-section>
          <q-banner class="bg-orange-1" />
          <q-banner class="bg-orange-1" />
        </div>
      </div>

      <q-bar />
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  prop: [Number, String],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const propId = ref(props.prop);
const cols = ref([]);
const rows = ref([]);
const separator = ref("cell");
const loading = ref(false);

const selectedRow = (row) => {
  row.checked = !row.checked;
};

const onAll = () => {
  rows.value.forEach((r) => {
    r.checked = true;
  });
};

const offAll = () => {
  rows.value.forEach((r) => {
    r.checked = false;
  });
};

const validSave = () => {
  return false;
};

const getColumns = () => [
  {
    name: "checked",
    field: "checked",
    style: "width: 3%",
  },
  {
    name: "cod",
    label: proxy?.$t("code") || "code",
    field: "cod",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em;",
    style: "width: 10%",
  },
  {
    name: "name",
    label: proxy?.$t("fldName") || "fldName",
    field: "name",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 37%",
  },
  {
    name: "fullName",
    label: proxy?.$t("fldFullName") || "fldFullName",
    field: "fullName",
    align: "left",
    classes: "bg-blue-grey-1",
    headerStyle: "font-size: 1.2em",
    style: "width: 50%",
  },
];

const onOKClick = () => {
  loading.value = true;
  let dta = [];

  rows.value.forEach((r) => {
    if (r.checked) {
      dta.push({ id: r.id });
    }
  });

  api
    .post("", {
      method: "prop/savePropRefVal",
      params: [propId.value, dta],
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
      // dialogRef handles closing via onDialogOK/onDialogCancel
    });
};

onMounted(() => {
  cols.value = getColumns();

  api
    .post("", {
      method: "prop/loadPropValForUpd",
      params: [propId.value],
    })
    .then((response) => {
      rows.value = response.data.result.records;
    });
});
</script>

<style scoped></style>
