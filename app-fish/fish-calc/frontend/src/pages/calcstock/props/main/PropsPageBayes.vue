<template>
  <div class="full-width" style="height: calc(100vh - 410px);">
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
              <q-item-label >{{ col.value }}</q-item-label>
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
const mapCalcFishSpec = ref(new Map())
const mapCalcStatus = ref(new Map())
const mapReservoir = ref(new Map())

const fnEdit = async () => {
  //console.log("rows: ", rows.value)
  //console.log("rows2: ", rows.value[0])

  let data = {};

  if (rows.value.length === 0) {
    let newRec = await api.post('', { method: 'data/newRecMainProps', params: [props.own] })
    Object.assign(data, newRec.data.result.records[0]);
  } else {
    Object.assign(data, rows.value[0]);
  }

  console.info("DATA", data)


  $q.dialog({
    component: UpdaterPropsBayes,
    componentProps: {
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

const loadFvAsMap = async (objId, codProp) => {
  if (!objId) return;
  loading.value = true;
  api
    .post("", {
      method: "data/loadFvAsMap",
      params: [codProp]
    })
    .then(
      (response) => {
        if (codProp==="Prop_CalcFishSpec") {
          mapCalcFishSpec.value = response.data.result
          console.info("mapCalcFishSpec", mapCalcFishSpec.value)
        } else if (codProp==="Prop_CalcStatus") {
          mapCalcStatus.value = response.data.result
          console.info("mapCalcStatus", mapCalcStatus.value)
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

const loadReservoirAsMap = async (objId, codProp) => {
  if (!objId) return;
  loading.value = true
  api
    .post('', {
      method: 'data/loadReservoirs',
      params: [codProp],
    })
    .then((response) => {
      console.info("Reservoir Recs", response.data.result.records)
      response.data.result.records.forEach((it) => {
        mapReservoir.value[it['id']] = it['name']
      })

      console.info("Reservoir", mapReservoir.value)
    })
    .finally(() => {
      loading.value = false
    })


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
    label: 'Водоем',
    field: 'objReservoirShore',
    format: (v) => (mapReservoir.value ? mapReservoir.value[v] : null),
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
    format: (v) => (mapCalcFishSpec.value ? mapCalcFishSpec.value[v] : null),
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
    format: (v) => (mapCalcStatus.value ? mapCalcStatus.value[v] : null),
  },
]

watch(
  () => props.own,
  (newObj) => {
    loadReservoirAsMap(newObj, "Prop_ReservoirShore")
    loadFvAsMap(newObj, "Prop_CalcFishSpec")
    loadFvAsMap(newObj, "Prop_CalcStatus")
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
