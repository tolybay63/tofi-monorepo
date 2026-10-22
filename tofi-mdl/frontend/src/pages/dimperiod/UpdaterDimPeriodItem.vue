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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <div v-if="isChild" class="row">
          {{ $t("parent") }}: <b> {{ parentName }} </b>
        </div>

        <!-- periodType -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="pt"
          :options="optPT"
          :label="$t('periodType')"
          option-value="id"
          option-label="text"
          map-options
          :disable="mode === 'upd'"
          @update:model-value="fnSelectPT"
        />

        <!-- periodNameTml -->
        <q-select
          :dense="dense"
          :options-dense="dense"
          v-model="tml"
          :options="optTml"
          :label="$t('periodTml')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectPeriodNameTml"
        />

        <!-- periodIncludeTag -->
        <q-select
          v-if="isChild"
          :dense="dense"
          :options-dense="dense"
          v-model="pit"
          :options="optPIT"
          :label="$t('periodIncludeTag')"
          option-value="id"
          option-label="text"
          map-options
          @update:model-value="fnSelectPeriodIncludeTag"
        />

        <q-item-label
          style="color: cornflowerblue; text-align: center; padding-top: 30px"
        >{{ $t("params") }}</q-item-label
        >

        <!-- dbeg-->
        <q-input
          :dense="dense"
          type="date"
          stack-label
          clearable
          v-model="form.dbeg"
          :label="$t('fldDbeg')"
          @update:model-value="fnSelectDbeg"
        />

        <!-- countPeriod-->
        <q-input
          class="q-mt-md"
          :dense="dense"
          type="number"
          stack-label
          clearable
          v-model="form.countPeriod"
          :rules="[
            (val) =>
              val == null ||
              (val && val > 0) ||
              'Количество периодов должно быть положительной',
          ]"
          :label="$t('countPeriod')"
          @update:model-value="fnUpdPC"
        />

        <!-- dend-->
        <q-input
          class="q-mt-md"
          :dense="dense"
          type="date"
          clearable
          stack-label
          v-model="form.dend"
          :label="$t('fldDend')"
          @update:model-value="fnSelectDend"
        />

        <q-input
          class="q-mt-md"
          :dense="dense"
          type="number"
          stack-label
          v-model="form.lagCurrentDate"
          clearable
          :label="$t('lagCurrentDate')"
          @update:model-value="fnUpdLCP"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          :dense="dense"
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
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
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useDialogPluginComponent } from "quasar";
import { api } from "@/boot/axios";
import { notifyError, notifySuccess } from "@/utils/jsutils";
import allConsts from "@/pages/all-consts";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  isChild: Boolean,
  parentName: String,
  parentPeriodType: [Number, String],
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const visible = ref(false);

const optPT = ref([]);
const pt = ref(props.data.periodType);

const optTml = ref([]);
const tml = ref(props.data.periodNameTml);

const optPIT = ref([]);
const pit = ref(props.data.periodIncludeTag);

const fnUpdPC = () => {
  form.dbeg = null;
};

const fnUpdLCP = () => {
  form.dend = null;
};

const fnSelectDbeg = (value) => {
  visible.value = true;
  api
    .post("", {
      method: "dimperiod/getPeriodBeg",
      params: [value, form.periodType],
    })
    .then(
      (response) => {
        form.dbeg = response.data.result;
        if (form.dbeg.startsWith("0000")) {
          form.countPeriod = 1;
          form.dbeg = null;
        } else {
          form.countPeriod = null;
        }
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data) msg = error.response.data.error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

const fnSelectDend = (value) => {
  visible.value = true;
  api
    .post("", {
      method: "dimperiod/getPeriodEnd",
      params: [value, form.periodType],
    })
    .then(
      (response) => {
        form.dend = response.data.result;
        if (form.dend.startsWith("0000")) {
          form.lagCurrentDate = 0;
          form.dend = null;
        } else {
          form.lagCurrentDate = null;
        }
      },
      (error) => {
        let msg = error.message;
        if (error.response?.data) msg = error.response.data.error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

const fnSelectPT = () => {
  form.dbeg = null;
  form.dend = null;
  form.periodType = typeof pt.value === "object" && pt.value !== null ? pt.value.id : pt.value;
};

const fnSelectPeriodNameTml = () => {
  form.periodNameTml = typeof tml.value === "object" && tml.value !== null ? tml.value.id : tml.value;
};

const fnSelectPeriodIncludeTag = () => {
  form.periodIncludeTag = typeof pit.value === "object" && pit.value !== null ? pit.value.id : pit.value;
};

const validSave = () => {
  if (!props.isChild) {
    return (
      (form.countPeriod && form.countPeriod < 1) ||
      !(
        (form.dbeg == null && form.countPeriod != null) ||
        (form.dbeg != null && form.countPeriod == null)
      ) ||
      !(
        (form.dend == null && form.lagCurrentDate != null) ||
        (form.dend != null && form.lagCurrentDate == null)
      )
    );
  } else {
    return form.countPeriod && form.countPeriod < 1;
  }
};

const onOKClick = () => {
  visible.value = true;
  let err = false;

  const method = props.mode === "ins" ? "insertDPI" : "updateDPI";
  form.periodNameTml = typeof tml.value === "object" && tml.value !== null ? tml.value.id : tml.value;
  form.periodIncludeTag = typeof pit.value === "object" && pit.value !== null ? pit.value.id : pit.value;

  api
    .post("", {
      method: "dimperiod/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        err = true;
        let msg = error.message;
        if (error.response?.data) msg = error.response.data.error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

onMounted(() => {
  visible.value = true;

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PeriodType" }],
    })
    .then((response) => {
      optPT.value = response.data.result.records;
      if (props.isChild) {
        if (props.parentPeriodType === allConsts.FD_PeriodType.year) {
          optPT.value = optPT.value.filter((it) => {
            return [
              allConsts.FD_PeriodType.halfyear,
              allConsts.FD_PeriodType.quarter,
              allConsts.FD_PeriodType.month,
              allConsts.FD_PeriodType.decade,
              allConsts.FD_PeriodType.week,
              allConsts.FD_PeriodType.day,
            ].includes(it.id);
          });
        } else if (props.parentPeriodType === allConsts.FD_PeriodType.halfyear) {
          optPT.value = optPT.value.filter((it) => {
            return [
              allConsts.FD_PeriodType.quarter,
              allConsts.FD_PeriodType.month,
              allConsts.FD_PeriodType.decade,
              allConsts.FD_PeriodType.week,
              allConsts.FD_PeriodType.day,
            ].includes(it.id);
          });
        } else if (props.parentPeriodType === allConsts.FD_PeriodType.quarter) {
          optPT.value = optPT.value.filter((it) => {
            return [
              allConsts.FD_PeriodType.month,
              allConsts.FD_PeriodType.decade,
              allConsts.FD_PeriodType.week,
              allConsts.FD_PeriodType.day,
            ].includes(it.id);
          });
        } else if (props.parentPeriodType === allConsts.FD_PeriodType.month) {
          optPT.value = optPT.value.filter((it) => {
            return [
              allConsts.FD_PeriodType.decade,
              allConsts.FD_PeriodType.week,
              allConsts.FD_PeriodType.day,
            ].includes(it.id);
          });
        } else if (props.parentPeriodType === allConsts.FD_PeriodType.decade) {
          optPT.value = optPT.value.filter((it) => {
            return [
              allConsts.FD_PeriodType.week,
              allConsts.FD_PeriodType.day,
            ].includes(it.id);
          });
        } else if (props.parentPeriodType === allConsts.FD_PeriodType.week) {
          optPT.value = optPT.value.filter((it) => {
            return [allConsts.FD_PeriodType.day].includes(it.id);
          });
        }
      }
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PeriodNameTml" }],
    })
    .then((response) => {
      optTml.value = response.data.result.records;
    });

  api
    .post("", {
      method: "dict/load",
      params: [{ dict: "FD_PeriodIncludeTag" }],
    })
    .then((response) => {
      optPIT.value = response.data.result.records;
      if (form.periodType !== allConsts.FD_PeriodType.week) {
        optPIT.value.splice(-1, 1);
      }
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
