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

      <q-card>
        <q-card-section>
          <!-- cls -->
          <q-select
            v-model="cls"
            use-input
            map-options
            input-debounce="0"
            :label="$t('cls')"
            option-value="id"
            option-label="name"
            dense
            options-dense
            :options="optCls"
            clearable
            @update:model-value="inputValueCls"
            @filter="filterFnCls"
            @clear="fnClearCls"
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <!-- obj -->
          <q-select
            v-model="obj"
            use-input
            map-options
            input-debounce="0"
            :label="$t('obj')"
            option-value="id"
            option-label="name"
            dense
            options-dense
            :options="optObj"
            clearable
            @update:model-value="inputValueObj"
            @filter="filterFnObj"
            @clear="fnClearObj"
            :disable="!form.cls"
          >
            <template v-slot:no-option>
              <q-item>
                <q-item-section class="text-grey">
                  {{ $t("noResults") }}
                </q-item-section>
              </q-item>
            </template>
          </q-select>

          <!-- isDefault -->
          <q-toggle
            style="margin-left: 5px; margin-top: 15px"
            dense
            v-model="form.isDefault"
            :label="$t('isDefault')"
          />
        </q-card-section>
      </q-card>

      <q-card-actions align="right">
        <q-btn
          :loading="loading"
          dense
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
import { notifyError } from "@/utils/jsutils";

const props = defineProps({
  data: {
    type: Object,
    default: () => ({}),
  },
  act: String,
  prop: [Number, String],
  typ: [Number, String],
  mode: String,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const lang = ref(props.lg);
const cls = ref(props.data.cls);
const optCls = ref([]);
const optClsOrg = ref([]);

const obj = ref(props.data.obj);
const optObj = ref([]);
const optObjOrg = ref([]);
const loading = ref(false);

const validSave = () => {
  return false;
};

const loadObj = (clsVal) => {
  loading.value = true;
  api
    .post("", {
      method: props.act + "/loadProviderObjForSelect",
      params: [clsVal],
    })
    .then((response) => {
      optObj.value = response.data.result.records;
      optObjOrg.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });
};

const fnClearCls = () => {
  form.cls = null;
};

const inputValueCls = (val) => {
  if (val) {
    form.cls = val.id;
    loadObj(val.id);
  }
};

const filterFnCls = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optCls.value = optClsOrg.value;
    });
    return;
  }
  update(() => {
    if (optClsOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optCls.value = optClsOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const fnClearObj = () => {
  form.obj = null;
};

const inputValueObj = (val) => {
  if (val) {
    form.obj = val.id;
  }
};

const filterFnObj = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optObj.value = optObjOrg.value;
    });
    return;
  }
  update(() => {
    if (optObjOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optObj.value = optObjOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const onOKClick = () => {
  loading.value = true;
  api
    .post("", {
      method: props.act + "/saveProvider",
      params: [form, props.mode],
    })
    .then(
      (response) => {
        onDialogOK(response.data.result.records[0]);
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
  loading.value = true;
  api
    .post("", {
      method: props.act + "/loadProviderClsForSelect",
      params: [props.prop, props.typ, props.mode],
    })
    .then((response) => {
      optCls.value = response.data.result.records;
      optClsOrg.value = response.data.result.records;
    })
    .finally(() => {
      loading.value = false;
    });

  if (props.mode === "upd") {
    loadObj(cls.value);
  }
});
</script>

<style scoped></style>
