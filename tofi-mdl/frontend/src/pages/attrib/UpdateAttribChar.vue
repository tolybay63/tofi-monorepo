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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary" :dense="dense">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary" :dense="dense">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-card-section>
        <!-- mask-2 -->
        <q-input
          v-if="avt === 2"
          v-model="form.maskReg"
          :label="$t('maskVal')"
          :dense="dense"
          autofocus
        >
          <template #append>
            <q-btn rounded dense @click="infoMask" icon="help" text-color="primary" />
          </template>
        </q-input>

        <!-- date-3 -->
        <q-select
          v-if="avt === 3"
          v-model="d"
          :options="optionsDate"
          :label="$t('formatDate')"
          @update:model-value="fnSelectValTyp(avt)"
          :dense="dense"
          :options-dense="dense"
          autofocus
        />

        <!-- time-4 -->
        <q-select
          v-if="avt === 4"
          v-model="d"
          :options="optionsTime"
          :label="$t('formatTime')"
          @update:model-value="fnSelectValTyp(avt)"
          :dense="dense"
          :options-dense="dense"
          autofocus
        />

        <!-- datetime-5 -->
        <q-select
          v-if="avt === 5"
          v-model="d"
          :options="optionsDateTime"
          :label="$t('formatDateTime')"
          @update:model-value="fnSelectValTyp(avt)"
          :dense="dense"
          :options-dense="dense"
          autofocus
        />

        <!-- ext-7 -->
        <q-input
          v-if="avt === 7"
          v-model="form.fileExt"
          :label="$t('fileExt')"
          :dense="dense"
          autofocus
        />

        <!-- entityType-8 -->
        <q-select
          v-if="avt === 8"
          autofocus
          v-model="entity"
          :options="optionsEntity"
          :label="$t('entityType')"
          option-value="id"
          option-label="text"
          map-options
          :dense="dense"
          :options-dense="dense"
          @update:model-value="fnSelectValTyp(avt)"
        />

        <!-- periodType-9 -->
        <q-select
          v-if="avt === 9"
          autofocus
          v-model="period"
          :dense="dense"
          :options-dense="dense"
          :options="optionsPeriod"
          :label="$t('periodType')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectValTyp(avt)"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validInput(avt)"
          :dense="dense"
        />
        <q-btn
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          :dense="dense"
          @click="onDialogCancel"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent, useQuasar } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "@/utils/jsutils";

const props = defineProps({
  avt: Number,
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  attrib: [Number, String],
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const $q = useQuasar();
const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });

const optionsEntity = ref([]);
const optionsPeriod = ref([]);

const optionsDate = [
  "YYYY-MM-DD", "YYYY/MM/DD", "DD.MM.YYYY", "DD/MM/YYYY",
  "DD-MM-YYYY", "YY/M/D", "YY-M-D", "D/M/YY", "D-M-YY",
];
const optionsDateTime = [
  "YYYY-MM-DD HH:mm:ss", "YYYY/MM/DD HH:mm:ss", "DD/MM/YYYY HH:mm:ss",
  "DD-MM-YYYY HH:mm:ss", "YY/M/D HH:mm:ss", "YY-M-D HH:mm:ss",
  "D/M/YY HH:mm:ss", "D-M-YY HH:mm:ss",
];
const optionsTime = ["HH:mm:ss", "HH:mm:SSS"];

let dt = null;
let initEntity = null;
let initPeriod = null;

if (props.mode === "ins") {
  if (props.avt === 3) dt = "YYYY-MM-DD";
  else if (props.avt === 4) dt = "HH:mm:ss";
  else if (props.avt === 5) dt = "YYYY-MM-DD HH:mm:ss";
  else if (props.avt === 8) initEntity = 1;
  else if (props.avt === 9) initPeriod = 11;
} else {
  if ([3, 4, 5].includes(props.avt)) dt = props.data.format;
  else if (props.avt === 8) initEntity = props.data.entityType;
  else if (props.avt === 9) initPeriod = props.data.periodType;
}

const d = ref(dt);
const entity = ref(initEntity);
const period = ref(initPeriod);

const infoMask = () => {
  $q.dialog({
    message: proxy?.$t("infoMask"),
    html: true,
  });
};

const fnSelectValTyp = (avt) => {
  if ([3, 4, 5].includes(avt)) {
    form.format = d.value;
  } else if (avt === 8) {
    form.entityType = entity.value?.id || entity.value;
    form.periodType = null;
  } else if (avt === 9) {
    form.periodType = period.value?.id || period.value;
    form.entityType = null;
  }
};

const validInput = (avt) => {
  if (avt === 2) {
    if (!form.maskReg || form.maskReg.trim().length === 0) return true;
  } else if (avt === 7) {
    if (!form.fileExt || form.fileExt.trim().length === 0) return true;
  }
  return false;
};

const onOKClick = () => {
  form.attrib = props.attrib;
  const method = props.mode === "ins" ? "insertChar" : "updateChar";

  if (props.avt === 2) {
    form.format = null;
    form.fileExt = null;
    form.entityType = null;
    form.periodType = null;
  } else if ([3, 4, 5].includes(props.avt)) {
    form.format = d.value;
    form.maskReg = null;
    form.fileExt = null;
    form.entityType = null;
    form.periodType = null;
  } else if (props.avt === 7) {
    form.maskReg = null;
    form.format = null;
    form.periodType = null;
    form.entityType = null;
  } else if (props.avt === 8) {
    form.maskReg = null;
    form.format = null;
    form.fileExt = null;
    form.periodType = null;
    form.entityType = typeof entity.value === "object" && entity.value !== null ? entity.value.id : entity.value;
  } else if (props.avt === 9) {
    form.maskReg = null;
    form.format = null;
    form.fileExt = null;
    form.entityType = null;
    form.periodType = typeof period.value === "object" && period.value !== null ? period.value.id : period.value;
  }

  api
    .post("", {
      method: "attrib/" + method,
      params: [{ rec: form }],
    })
    .then((response) => {
      onDialogOK(response.data.result.records[0]);
      notifySuccess(proxy?.$t("success"));
    })
    .catch((error) => {
      notifyError(error.response?.data?.error?.message || error.message);
    });
};

onMounted(() => {
  api
    .post("", { method: "dict/load", params: [{ dict: "FD_EntityType" }] })
    .then((response) => {
      optionsEntity.value = response.data.result.records;
    });

  api
    .post("", { method: "dict/load", params: [{ dict: "FD_PeriodType" }] })
    .then((response) => {
      optionsPeriod.value = response.data.result.records;
    });
});
</script>
