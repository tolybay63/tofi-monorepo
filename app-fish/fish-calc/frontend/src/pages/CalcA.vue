<template>
  <h6 class="q-mx-lg">Calculation A: Данные из [Meta]</h6>

  <div class="no-padding no-margin">
    <q-table
      :columns="cols"
      :rows="rows"
      :rows-per-page-options="[0]"
      :table-colspan="4"
      :wrap-cells="true"
      card-class="bg-amber-1 text-brown"
      color="primary"
      dense
      row-key="id"
      separator="cell"
      table-header-class="text-bold text-white bg-blue-grey-13"
    >
    </q-table>
  </div>
</template>

<script setup>
import {api} from '../boot/axios.js'
import {onMounted, ref} from 'vue'
import {useI18n} from 'vue-i18n'

const { t } = useI18n()

const rows = ref([])
const cols = ref([])
const getColumns = () => [
  {
    name: 'name',
    label: t('fldName'),
    field: 'name',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width:60%',
  },

  {
    name: 'cod',
    label: t('code'),
    field: 'cod',
    align: 'left',
    classes: 'bg-blue-grey-1',
    headerStyle: 'font-size: 1.2em; width:40%',
  },
]

const fetchData = () => {

  api.post('', {
    method: 'auth/checkTarget',
    params: ['calc'],
  })

  const apiPrefix = import.meta.env.PROD ? 'fast/' : 'http://127.0.0.1:8000/'
  api
    .get(`${apiPrefix}factor_vals_by_cod/Factor_FishType`)
    .then((res) => {
      rows.value = res.data
    })
    .catch((err) => console.error('Ошибка при получении факторов:', err))
}

onMounted(() => {
  cols.value = getColumns()
  fetchData()
})
</script>

<style scoped></style>
