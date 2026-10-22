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


      <q-card-section>

        <TreeSelect
          v-model="cls"
          :label="$t('cls')"
          :options="optCls"
          class="q-ma-md"
          node-key="key"
          @close="fnCloseCls"
        />

<!--
                <q-item-label class="text-grey-7" style="font-size: 0.8em">{{
                    $t("cls")
                  }}</q-item-label>

                <treeselect
                  :options="optCls"
                  maxHeight="800"
                  v-model="cls"
                  :normalizer="normalizer"
                  :placeholder="$t('select')"
                  :noChildrenText="$t('noChilds')"
                  :noResultsText="$t('noResult')"
                  :noOptionsText="$t('noResult')"
                  @close="fnCloseCls"
                />
        -->


        <TreeSelect
          v-model="obj"
          :label="$t('obj')"
          :options="optObj"
          class="q-ma-md"
          node-key="key"
          @close="fnCloseObj"
        />


<!--
        <q-item-label
          class="text-grey-7"
          style="font-size: 0.8em; margin-top: 10px"
        >{{ $t("obj") }}</q-item-label>

        <treeselect
          :options="optObj"
          maxHeight="800"
          v-model="obj"
          :placeholder="$t('select')"
          :noChildrenText="$t('noChilds')"
          :noResultsText="$t('noResult')"
          :noOptionsText="$t('noResult')"
          @close="fnCloseObj"
        />
-->

      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          color="primary"
          icon="save"
          :label="$t('save')"
          @click="onOKClick"
          :disable="validOk()"
        />
        <q-btn
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
import { notifyError, notifySuccess, pack } from "@/utils/jsutils";
import TreeSelect from '@/components/TreeSelect.vue'

const props = defineProps({
  mode: String,
  data: {
    type: Object,
    default: () => ({}),
  },
  typ: [Number, String],
});

defineEmits([...useDialogPluginComponent.emits]);

const { proxy } = getCurrentInstance();
const { dialogRef, onDialogHide, onDialogOK, onDialogCancel } = useDialogPluginComponent();

const form = reactive({ ...props.data });
const optCls = ref([]);
const cls = ref(props.data.clsOrObjCls);

const optObj = ref([]);
const obj = ref(props.data.obj);

const fetchData = (selectedCls) => {
  api
    .post("", {
      method: "obj/loadObjTreeSelect",
      params: [selectedCls],
    })
    .then(
      (response) => {
        optObj.value = pack(response.data.result.records, "ord");
      },
      (error) => {
        let msg;
        if (error.response?.data?.error?.message) msg = error.response.data.error.message;
        else msg = error.message;
        notifyError(msg);
      }
    );
};

const fnCloseCls = (v) => {
  form.clsOrObjCls = v;
  fetchData(v);
};

const fnCloseObj = (v) => {
  form.obj = v;
};

/*const normalizer = (node) => {
  return {
    id: node.ent,
    label: node.name,
  };
};*/

const validOk = () => {
  return !form.clsOrObjCls;
};

const onOKClick = () => {
  const method = props.mode === "ins" ? "insertNotExtended" : "updateNotExtended";
  let err = false;
  api
    .post("", {
      method: "typ/" + method,
      params: [form],
    })
    .then(
      (response) => {
        err = false;
        onDialogOK(response.data.result.records[0]);
        notifySuccess(proxy?.$t("success") || "success");
      },
      (error) => {
        err = true;
        let msg = error.response?.data?.error?.message
          ? error.response.data.error.message
          : error.message;
        notifyError(msg);
      }
    )
    .finally(() => {
      // dialogRef handles closing via onDialogOK/onDialogCancel
      if (!err) {
        onDialogHide()
      }
    });
};

onMounted(() => {
  api
    .post("", {
      method: "typ/loadClsTree",
      params: [{ typ: props.typ }],
    })
    .then((response) => {
      optCls.value = pack(response.data.result.records, "ord");
    });

  if (props.mode === "upd") {
    fetchData(form.clsOrObjCls);
  }
});
</script>

<style scoped></style>
