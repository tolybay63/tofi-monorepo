<template>
  <div class="full-width">
    <q-table
      class="full-width"
      :columns="cols"
      :rows="rows"
      :loading="loading"
      :wrap-cells="true"
      color="primary"
      grid
      hide-header
      row-key="own"
      :rows-per-page-options="[0]"
    >

      <template v-slot:item="slotProps">
        <q-list class="full-width q-pa-sm bg-amber-1">
          <q-item
            v-for="col in slotProps.cols"
            :key="col.name"
          >
            <q-item-section>
              <q-item-label>{{ col.label }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-item-label caption>{{ col.value }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </template>

      <template v-slot:top>
        <q-card-actions class="bg-blue-grey-12 full-width row">
          <div>
            Наименование расчета:
            <span class="q-pa-sm text-white text-bold">{{ name }}</span>
          </div>

          <q-space />

          <q-btn
            class="q-mx-sm"
            color="secondary"
            dense
            icon="edit"
            @click="fnEdit()"
          >
            <q-tooltip>
              {{ $t('editRecord') }}
            </q-tooltip>
          </q-btn>
        </q-card-actions>
      </template>

    </q-table>
  </div>
</template>

<script setup>
import {api} from "@/boot/axios.js";
import {getCurrentInstance, ref, watch} from "vue";
import UpdaterProps from "@/pages/calcstock/props/main/UpdaterProps.vue";
import {useQuasar} from "quasar";
const $q = useQuasar()

const {proxy} = getCurrentInstance()



const props = defineProps({
  own: Number,
  name: String,
})

const loading = ref(false);
const rows = ref([])

const fnEdit = async () => {
  //console.log("rows: ", rows.value)
  //console.log("rows2: ", rows.value[0])
  let cntFld=0
  for (let fld in rows.value[0]) {
      if (fld.includes("id"))
        cntFld++
  }
  //console.log("cntFld", cntFld)
  let mode = "upd"
  let data = rows.value[0]
  if (cntFld < 8) {
    let newRec = await api.post('', { method: 'data/newRecMainProps', params: [props.own] })
    Object.assign(data, newRec.data.result.records[0]);
    mode = "ins"
  }

  $q.dialog({
    component: UpdaterProps,
    componentProps: {
      mode: mode,
      data: data,
    },
  }).onOk(() => {
    loadData(props.own)
  })

}

const loadData = async (objId) => {
  if (!objId) return;
  loading.value = true;
  api
    .post("", {
      method: "data/loadMainProps",
      params: [objId]
    })
    .then(
      (response) => {
        rows.value = response.data.result["records"];
        console.info("SERVER ROWS:", rows.value);
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

const cols = [
  {
    name: 'CalcCreatDate',
    label: 'Дата создания расчета',
    field: 'CalcCreatDate',
  },
  {
    name: 'CalcLastDate',
    label: 'Дата последнего проведения расчета',
    field: 'CalcLastDate',
  },
  {
    name: 'objReservoirShore',
    label: 'Ссылка на водоем',
    field: 'objReservoirShore',
  },
  {
    name: 'CalcStartYear',
    label: 'Начальный год',
    field: 'CalcStartYear',
  },
  {
    name: 'CalcEndYear',
    label: 'Конечный год',
    field: 'CalcEndYear',
  },
  {
    name: 'fvCalcFishSpec',
    label: 'Вид рыбы',
    field: 'fvCalcFishSpec',
  },
  {
    name: 'objCalcUser',
    label: 'Пользователь проводивший расчет',
    field: 'objCalcUser',
  },
  {
    name: 'fvCalcStatus',
    label: 'Статус расчета',
    field: 'fvCalcStatus',
  },
]

watch(
  () => props.own,
  (newObj) => {
    loadData(newObj);
  },
  {immediate: true}
)
</script>

<style scoped>
:deep(.q-table__top) {
  padding-left: 0;
  padding-right: 0;
}

:deep(.q-table__grid-content) {
  padding: 0;
}
</style>
