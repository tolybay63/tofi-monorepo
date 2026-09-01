<template>
  <q-card>

    <q-card-actions class="bg-blue-grey-12">
      <div>
        Наименование расчета:
        <span class="q-pa-sm text-white text-bold"> {{ props.name }}</span>
      </div>
      <q-space/>
      <q-btn
        :disable="isEdit" class="q-mx-sm"
        color="secondary"
        dense
        icon="edit"
        @click="fnEdit()"
      >
        <q-tooltip>
          {{ $t('editRecord') }}
        </q-tooltip>
      </q-btn>

      <q-btn
        :disable="!isEdit" class="q-mx-sm"
        color="secondary"
        dense
        icon="save"
        @click="fnSave()"
      >
        <q-tooltip>
          {{ $t('save') }}
        </q-tooltip>
      </q-btn>

    </q-card-actions>

    <q-card-section>
      <q-editor
        class="my-custom-editor"
        v-model="form['CalcDescription']"
        @update:model-value="fnUpd()"
        :fonts="{
          arial: 'Arial',
          arial_black: 'Arial Black',
          comic_sans: 'Comic Sans MS',
          courier_new: 'Courier New',
          impact: 'Impact',
          lucida_grande: 'Lucida Grande',
          times_new_roman: 'Times New Roman',
          verdana: 'Verdana'
        }"
        :readonly="!isEdit"
        :toolbar="[
          [
            {
              label: $q.lang.editor.align,
              icon: $q.iconSet.editor.align,
              fixedLabel: true,
              list: 'only-icons',
              options: ['left', 'center', 'right', 'justify']
            },
            {
              label: $q.lang.editor.align,
              icon: $q.iconSet.editor.align,
              fixedLabel: true,
              options: ['left', 'center', 'right', 'justify']
            }
          ],
          ['bold', 'italic', 'strike', 'underline', 'subscript', 'superscript'],
          ['token', 'hr', 'link', 'custom_btn'],
          ['print', 'fullscreen'],
          [
            {
              label: $q.lang.editor.formatting,
              icon: $q.iconSet.editor.formatting,
              list: 'no-icons',
              options: ['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'code']
            },
            {
              label: $q.lang.editor.fontSize,
              icon: $q.iconSet.editor.fontSize,
              fixedLabel: true,
              fixedIcon: true,
              list: 'no-icons',
              options: [
                'size-1',
                'size-2',
                'size-3',
                'size-4',
                'size-5',
                'size-6',
                'size-7'
              ]
            },
            {
              label: $q.lang.editor.defaultFont,
              icon: $q.iconSet.editor.font,
              fixedIcon: true,
              list: 'no-icons',
              options: [
                'default_font',
                'arial',
                'arial_black',
                'comic_sans',
                'courier_new',
                'impact',
                'lucida_grande',
                'times_new_roman',
                'verdana'
              ]
            },
            'removeFormat'
          ],
          ['quote', 'unordered', 'ordered', 'outdent', 'indent'],
          ['undo', 'redo'],
          ['viewsource']
        ]"
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
  height: calc(100vh - 325px);
  display: flex;
  flex-direction: column;
}

:deep(.q-editor__content) {
  overflow-y: auto;
  flex: 1;
}
</style>
