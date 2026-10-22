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
      <q-bar v-if="mode === 'ins'" class="text-white bg-primary">
        <div>{{ $t("newRecord") }}</div>
      </q-bar>
      <q-bar v-if="mode === 'upd'" class="text-white bg-primary">
        <div>{{ $t("editRecord") }}</div>
      </q-bar>

      <q-inner-loading :showing="visible" color="secondary" />

      <q-card>
        <q-tabs v-model="tab" class="text-teal">
          <q-tab name="main" :label="$t('mainProp')" />
          <q-tab name="dop" :label="$t('dopProp')" />
        </q-tabs>

        <q-separator />

        <q-tab-panels
          v-model="tab"
          animated
          swipeable
          vertical
          transition-prev="jump-up"
          transition-next="jump-up"
        >
          <q-tab-panel name="main">
            <q-item-label
              style="color: orange; text-underline: orange; margin-top: 10px"
            >
              Свойства:
            </q-item-label>
            <!-- name -->
            <q-input
              autofocus
              :dense="dense"
              v-model="form.name"
              @blur="onBlurName"
              :label="$t('fldName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />
            <!-- fullName-->
            <q-input
              :dense="dense"
              v-model="form.fullName"
              :label="$t('fldFullName')"
              :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
            />

            <!-- Cod -->
            <q-input
              :dense="dense"
              v-model="form.cod"
              :label="$t('code')"
              :placeholder="$t('msgCodeGen')"
            />

            <!-- AccessLevel -->
            <q-select
              :dense="dense"
              v-model="al"
              :options="optionsLevel"
              :label="$t('accessLevel')"
              option-value="id"
              option-label="text"
              map-options
              @update:model-value="fnSelectDict"
            />

            <q-item-label
              style="color: orange; text-underline: orange; margin-top: 10px"
            >
              {{ $t("statusFactor") }}:
            </q-item-label>

            <q-select
              v-model="status"
              map-options
              use-input
              input-debounce="0"
              :label="$t('factor')"
              :options="optionsStatus"
              option-value="id"
              option-label="name"
              :dense="dense"
              :options-dense="dense"
              clearable
              @clear="fnClearStatus"
              @update:model-value="inputValueStatus"
              @filter="filterFnStatus"
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t("noResults") }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>

            <q-item-label
              style="color: orange; margin-top: 10px; text-underline: orange"
            >
              {{ $t("providerTyp") }}:
            </q-item-label>

            <q-select
              v-model="provider"
              use-input
              map-options
              input-debounce="0"
              :label="$t('typ')"
              :options="optionsProvider"
              option-value="id"
              option-label="name"
              :dense="dense"
              :options-dense="dense"
              @update:model-value="inputValueProvider"
              @filter="filterFnProvider"
              clearable
              @clear="fnClearProvider"
            >
              <template v-slot:no-option>
                <q-item>
                  <q-item-section class="text-grey">
                    {{ $t("noResults") }}
                  </q-item-section>
                </q-item>
              </template>
            </q-select>
          </q-tab-panel>

          <q-tab-panel name="dop">
            <div>
              <q-toggle
                style="margin-left: 10px; margin-top: 10px"
                :dense="dense"
                v-model="form['isDependValueOnPeriod']"
                :label="$t('dependValueOnPeriod')"
              />
            </div>

            <q-toggle
              style="margin-left: 10px; margin-top: 10px"
              :dense="dense"
              v-model="form.isUniq"
              :label="$t('isUniq')"
            />

            <q-input
              class="q-mt-lg"
              v-model.number="form.fillMore"
              type="number"
              :dense="dense"
              :label="$t('fillMore')"
              :rules="[(val) => val > 0 || $t('minFillMore')]"
            />

            <!-- cmt -->
            <q-input
              :dense="dense"
              v-model="form.cmt"
              type="textarea"
              :label="$t('fldCmt')"
            />

            <q-input
              :dense="dense"
              v-model="form.propTag"
              :label="$t('propTag')"
            />
          </q-tab-panel>
        </q-tab-panels>
      </q-card>

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

const props = defineProps({
  rec: {
    type: Object,
    default: () => ({}),
  },
  mode: String,
  lg: String,
  dense: Boolean,
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.rec });
const visible = ref(false);

const optionsLevel = ref([]);
const al = ref(props.rec.accessLevel);

const status = ref(props.rec.statusFactor);
const optionsStatus = ref([]);
const optionsStatusOrg = ref([]);

const provider = ref(props.rec.providerTyp);
const optionsProvider = ref([]);
const optionsProviderOrg = ref([]);

const tab = ref("main");

const fnClearStatus = () => {
  form.statusFactor = null;
};

const fnClearProvider = () => {
  form.providerTyp = null;
};

const inputValueStatus = (val) => {
  if (val) {
    status.value = { ...status.value, id: val.id };
    form.statusFactor = val.id;
  }
};

const filterFnStatus = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsStatus.value = optionsStatusOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsStatusOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsStatus.value = optionsStatusOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const inputValueProvider = (val) => {
  if (val) {
    provider.value = { ...provider.value, id: val.id };
    form.providerTyp = val.id;
  }
};

const filterFnProvider = (val, update) => {
  if (val === null || val === "") {
    update(() => {
      optionsProvider.value = optionsProviderOrg.value;
    });
    return;
  }
  update(() => {
    if (optionsProviderOrg.value.length < 2) return;
    const needle = val.toLowerCase();
    let name = "name";
    optionsProvider.value = optionsProviderOrg.value.filter((v) => {
      return v[name].toLowerCase().indexOf(needle) > -1;
    });
  });
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    if (!form.fullName || form.fullName.trim() === "") {
      form.fullName = form.name;
    }
  }
};

const fnSelectDict = () => {
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;
};

const validSave = () => {
  return !(
    form.name !== null &&
    form.name !== undefined &&
    form.name.length > 2 &&
    form.fullName !== null &&
    form.fullName !== undefined &&
    form.fullName.length > 2 &&
    form.fillMore > 0
  );
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertMultiProp" : "updateMultiProp";
  form.accessLevel = typeof al.value === "object" && al.value !== null ? al.value.id : al.value;

  let err = false;
  api
    .post("", {
      method: "multiProp/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let msg = error.response?.data?.error?.message || error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      if (!err) {
        // dialogRef handles closing via onDialogOK/onDialogCancel
      }
    });
};

const getDict = (dictName) => {
  api
    .post("", {
      method: "dict/load",
      params: [{ dict: dictName }],
    })
    .then((response) => {
      if (dictName === "FD_AccessLevel") {
        optionsLevel.value = response.data.result.records;
      }
    });
};

onMounted(() => {
  getDict("FD_AccessLevel");

  visible.value = true;
  api
    .post("", {
      method: "factor/loadForSelect",
      params: [],
    })
    .then((response) => {
      optionsStatusOrg.value = response.data.result.records;
      optionsStatus.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });

  visible.value = true;
  api
    .post("", {
      method: "typ/loadTypForSelect",
      params: [],
    })
    .then((response) => {
      optionsProviderOrg.value = response.data.result.records;
      optionsProvider.value = response.data.result.records;
    })
    .finally(() => {
      visible.value = false;
    });
});
</script>

<style scoped></style>
