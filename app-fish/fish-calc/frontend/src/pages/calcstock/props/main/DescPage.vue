<template>
  <q-card class="column no-wrap fit">
    <q-card-actions class="bg-blue-grey-12 col-auto">
      <div>
        Наименование расчета:
        <span class="q-pa-sm text-white text-bold"> {{ props.name }}</span>
      </div>
      <q-space/>
      <q-btn :disable="isEdit" class="q-mx-sm" color="secondary" dense icon="edit" @click="fnEdit()">
        <q-tooltip>{{ $t('editRecord') }}</q-tooltip>
      </q-btn>
      <q-btn :disable="!isEdit" class="q-mx-sm" color="secondary" dense icon="save" @click="fnSave()">
        <q-tooltip>{{ $t('save') }}</q-tooltip>
      </q-btn>
    </q-card-actions>

    <q-card-section class="col q-pa-none column no-wrap">
      <q-editor
        class="my-custom-editor col"
        v-model="form['CalcDescription']"
        @update:model-value="fnUpd()"
        :readonly="!isEdit"
        autofocus
      />
    </q-card-section>
  </q-card>
</template>
<script setup>
import {getCurrentInstance, reactive, ref, watch} from "vue";
import {api} from "@/boot/axios.js";
import {notifySuccess} from "@/utils/jsutils.js";
import {Notify} from "quasar";
const {proxy} = getCurrentInstance()

const props = defineProps({
  own: Number,
  name: String,
})

const loading = ref(false)
const isEdit = ref(false)
const form = reactive({own: null, idCalcDescription: null, CalcDescription: ""})

const fnUpd = () => {

}

const fnEdit = () => {
  isEdit.value = !isEdit.value
  Notify.create({
    type: "warning",
    position: "top",
    timeout: 5000,
    message: "Не забудьте сохранить после изменения...",
  })

}

const fnSave = async () => {
  isEdit.value = !isEdit.value;
  api
    .post("", {
      method: "data/saveDesc",
      params: [form]
    })
    .then(
      () => {
        notifySuccess("Ok")
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data?.error?.message);
        console.error(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
}

const loadData = async (objId) => {
  if (!objId) return;
  loading.value = true;
  api
    .post("", {
      method: "data/loadDesc",
      params: [objId]
    })
    .then(
      (response) => {
        const records = response.data.result["records"];
        if (records && records.length > 0) {
          Object.assign(form, records[0]);
        } else {
          // Очищаем форму, если для узла еще нет описания в базе
          form.idCalcDescription = null;
          form.CalcDescription = "";
          form.own = objId;
        }
      },
      (error) => {
        let msg = error.message;
        if (error.response)
          msg = proxy?.$t(error.response.data?.error?.message);
        console.error(msg);
      }
    )
    .finally(() => {
      loading.value = false;
    });
}

// Автоматически отслеживаем смену выбранного узла
watch(
  () => props.own,
  (newObj) => {
    loadData(newObj);
  },
  { immediate: true }
)
</script>

<style scoped>
.my-custom-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.q-editor__content) {
  overflow-y: auto;
  flex: 1;
}
</style>
