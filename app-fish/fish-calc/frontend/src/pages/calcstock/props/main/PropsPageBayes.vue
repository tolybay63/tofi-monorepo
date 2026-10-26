<template>
  <div class="column no-wrap ">
    <q-table
      class="col custom-table"
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
          <q-item v-for="col in slotProps.cols" :key="col.name">
            <q-item-section>
              <q-item-label>{{ col.label }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-item-label>{{ col.value }}</q-item-label>
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
          <q-btn class="q-mx-sm" color="secondary" dense icon="edit" @click="fnEdit()">
            <q-tooltip>{{ $t('editRecord') }}</q-tooltip>
          </q-btn>
        </q-card-actions>
      </template>
    </q-table>
  </div>
</template>

<script setup>
import {api} from "@/boot/axios.js";
import {getCurrentInstance, ref, watch} from "vue";
import UpdaterPropsBayes from "@/pages/calcstock/props/main/UpdaterPropsBayes.vue";
import {useQuasar} from "quasar";

const $q = useQuasar()
const {proxy} = getCurrentInstance()

const props = defineProps({
  own: Number,
  name: String,
})

const loading = ref(false);
const rows = ref([])
const mapCalcStatus = ref(new Map())
const mapReservoir = ref(new Map())
const emit = defineEmits(['update-parent-data'])

const fnEdit = async () => {

  let data = {};

  if (rows.value.length === 0) {
    let newRec = await api.post('', { method: 'data/newRecMainProps', params: [props.own] })
    Object.assign(data, newRec.data.result.records[0]);
  } else {
    Object.assign(data, rows.value[0]);
  }

  $q.dialog({
    component: UpdaterPropsBayes,
    componentProps: {
      data: data,
    },
  }).onOk(() => {
    loadData(props.own)
    // Генерируем событие для родительского компонента
    emit('update-parent-data')
  })
}

const loadData = async (objId) => {
  if (!objId) return;
  loading.value = true;
  await api
    .post("", {
      method: "data/loadMainProps",
      params: [objId]
    })
    .then(
      (response) => {
        rows.value = response.data.result["records"];
        //console.info("SERVER ROWS:", rows.value);
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

const loadFvAsMap = async (objId, codProp) => {
  if (!objId) return;
  loading.value = true;
  await api
    .post("", {
      method: "data/loadFvAsMap",
      params: [codProp]
    })
    .then(
      (response) => {
         mapCalcStatus.value = response.data.result
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

const loadReservoirAsMap = async (objId, codProp) => {
  if (!objId) return;
  loading.value = true
  await api
    .post('', {
      method: 'data/loadReservoirs',
      params: [codProp],
    })
    .then((response) => {
      response.data.result.records.forEach((it) => {
        mapReservoir.value[it['id']] = it['name']
      })
    })
    .finally(() => {
      loading.value = false
    })
}

const cols = [
  {
    name: 'CalcCreatDate',
    label: proxy?.$t('CalcCreatDate'),
    field: 'CalcCreatDate',
  },
  {
    name: 'CalcLastDate',
    label: proxy?.$t('CalcLastDate'),
    field: 'CalcLastDate',
  },
  {
    name: 'objReservoirShore',
    label: proxy?.$t('ReservoirShore'),
    field: 'objReservoirShore',
    format: (v) => mapReservoir.value ? mapReservoir.value[v] : null
  },
  {
    name: 'CalcStartYear',
    label: proxy?.$t('CalcStartYear'),
    field: 'CalcStartYear',
  },
  {
    name: 'CalcEndYear',
    label: proxy?.$t('CalcEndYear'),
    field: 'CalcEndYear',
  },
  {
    name: 'nameUserId',
    label: proxy?.$t('CalcUser'),
    field: 'nameUserId',
  },
  {
    name: 'fvCalcStatus',
    label: proxy?.$t('CalcStatus'),
    field: 'fvCalcStatus',
    format: (v) => mapCalcStatus.value ? mapCalcStatus.value[v] : null
  },
]

watch(
  () => props.own,
   async (newObj) => {
     await loadData(newObj);
     await loadReservoirAsMap(newObj, "Prop_ReservoirShore")
     await loadFvAsMap(newObj, "Prop_CalcStatus")
  },
  {immediate: true}
)

</script>

<style scoped>
.custom-table {
  display: flex;
  flex-direction: column;
  height: 100% !important;
}

/* Шапка таблицы фиксируется */
:deep(.q-table__top) {
  padding-left: 0;
  padding-right: 0;
  flex-shrink: 0;
}

/* Контейнер сетки растягивается на всю оставшуюся высоту и получает скролл */
:deep(.q-table__grid-content) {
  padding: 0;
  flex: 1 1 auto;
  overflow-y: auto !important;
}
</style>
