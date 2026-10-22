<!-- DimObjItemPropVal -->
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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card-section>
        <div v-if="pt === 22 || pt === 33 || pt === 46 || pt === 47">
          <q-input
            autofocus
            dense
            v-model="form.numberVal"
            :label="$t('val')"
            type="number"
          />
        </div>

        <div v-if="pt === 41 || pt === 42">
          <q-input
            autofocus
            dense
            v-model="form.strVal"
            :label="$t('val')"
          />
        </div>

        <div v-if="pt === 43 || pt === 44 || pt === 45">
          <q-input
            autofocus
            dense
            v-model="form.dateTimeVal"
            :label="$t('val')"
            type="date"
            stack-label
            class="q-mt-md"
          />
        </div>

        <div v-if="pt === 48">
          <q-input
            autofocus
            dense
            v-model="form.multiStrVal"
            :label="$t('val')"
            type="textarea"
            class="q-mt-lg"
          />
        </div>

        <div v-if="pt === 11 || pt === 55 || pt === 66 || pt === 77">
          <q-select
            v-model="propVal"
            dense
            options-dense
            :options="optPropVal"
            :label="$t('val')"
            class="q-mt-lg"
            option-value="id"
            option-label="name"
            map-options
            @update:model-value="fnPropVal"
          />
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validSave()"
        />
        <q-btn
          dense
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

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  dimObjItemProp: [Number, String],
  prop: [Number, String],
  pt: Number,
  kf: Number,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });

const propVal = ref(props.data.propVal);
const optPropVal = ref([]);
const visible = ref(false);

const fnPropVal = (val) => {
  form.propVal = typeof val === "object" && val !== null ? val.id : val;
};

const validSave = () => {
  if (props.pt === 22 || props.pt === 33 || props.pt === 46 || props.pt === 47) {
    return !form.numberVal;
  }
  if (props.pt === 41 || props.pt === 42) {
    return !form.strVal;
  }
  if (props.pt === 43 || props.pt === 44 || props.pt === 45) {
    return !form.dateTimeVal;
  }
  if (props.pt === 48) {
    return !form.multiStrVal;
  }
  if (props.pt === 11 || props.pt === 55 || props.pt === 66 || props.pt === 77) {
    return !form.propVal;
  }
  return false;
};

const onOKClick = () => {
  visible.value = true;
  const method = props.mode === "ins" ? "insertDOIpropValue" : "updateDOIpropValue";

  form.dimObjItemProp = props.dimObjItemProp;

  if (props.pt === 22 || props.pt === 23) {
    form.numberVal = form.numberVal * props.kf;
  }

  api
    .post("", {
      id: form.id,
      method: "dimobjitem/" + method,
      params: [form],
    })
    .then(
      () => {
        onDialogOK(form);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response?.data?.error?.message || error.message);
      }
    )
    .finally(() => {
      visible.value = false;
    });
};

onMounted(() => {
  if (props.pt === 11 || props.pt === 55 || props.pt === 66 || props.pt === 77) {
    visible.value = true;
    api
      .post("", {
        method: "dimobjitem/loadOptPropVal",
        params: [props.prop, props.pt, props.dimObjItemProp, props.mode],
      })
      .then((response) => {
        optPropVal.value = response.data.result.records;
      })
      .catch((error) => {
        let msg = error.message;
        if (error.response) msg = proxy?.$t(error.response.data.error.message) || msg;
        notifyError(msg);
      })
      .finally(() => {
        visible.value = false;
      });
  }
});
</script>

<style scoped></style>
