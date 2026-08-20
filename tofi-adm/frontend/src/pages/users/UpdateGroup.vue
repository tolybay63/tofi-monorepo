<template>
  <q-dialog
    ref="dialog"
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

      <q-card-section>
        <q-item-section v-if="isChild">
          {{ $t("parentGroup") }}: {{ parentName }}
        </q-item-section>

        <!-- name -->
        <q-input
          dense
          :model-value="form.name"
          v-model="form.name"
          autofocus
          @blur="onBlurName"
          :label="$t('fldName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>
        <!-- fullName-->
        <q-input
          dense
          :model-value="form.fullName"
          v-model="form.fullName"
          :label="$t('fldFullName')"
          :rules="[(val) => (!!val && !!val.trim()) || $t('req')]"
        >
        </q-input>

        <!-- Использование нашего компонента с древовидной структурой -->
        <TreeSelect
          v-model="parent"
          :options="parents"
          :label="$t('parent', true)"
          @select="fnSelectParent"
        />

        <!-- cmt -->
        <q-input
          dense
          v-model="form['cmt']"
          type="textarea"
          :label="$t('fldCmt')"
        >
        </q-input>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          dense
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validName()"
        />
        <q-btn
          dense
          color="primary"
          icon="cancel"
          :label="$t('cancel')"
          @click="onCancelClick"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import {getCurrentInstance, onMounted, reactive, ref} from "vue";
import {api} from "@/boot/axios";
import {notifyError, notifySuccess, pack} from "../../utils/jsutils";
import TreeSelect from "../../components/TreeSelect.vue";

const props = defineProps({
  data: Object,
  mode: String,
  isChild: Boolean,
  parentName: String,
});

const emit = defineEmits(["ok", "hide"]);
const { proxy } = getCurrentInstance();

const dialog = ref(null);
const form = reactive({ ...props.data });
const parents = ref([]);
const parent = ref(props.data?.parent);
const loading = ref(false);

const fnSelectParent = (v) => {
  form.parent = v.id;
  parent.value = v.id;
};

const onBlurName = () => {
  if (form.name) {
    form.name = form.name.trim();
    form.fullName = form.name.trim();
  }
};

const validName = () => {
  if (!form.name) return true;
  return form.name.trim().length === 0;
};

const show = () => {
  dialog.value?.show();
};

const hide = () => {
  dialog.value?.hide();
};

const onDialogHide = () => {
  emit("hide");
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertGr" : "updateGr";
  api
    .post("", {
      id: form.id,
      method: "usr/" + method,
      params: [{ rec: form }],
    })
    .then(
      (response) => {
        emit("ok", response.data.result["records"][0]);
        notifySuccess(proxy?.$t("success"));
      },
      (error) => {
        notifyError(error.response.data.error.message);
      }
    )
    .finally(() => {
      hide();
    });
};

const onCancelClick = () => {
  hide();
};

onMounted(() => {
  loading.value = true;
  api
    .post("", {
      method: "usr/loadGroupForSelect",
      params: [props.data?.id],
    })
    .then((response) => {
      parents.value = pack(response.data.result["records"], "ord");
    })
    .finally(() => {
      loading.value = false;
    });
});

defineExpose({
  show,
  hide
});
</script>
