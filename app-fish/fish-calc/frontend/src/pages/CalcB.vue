<template>
  <h6 class="q-mx-lg">Calculation B: Данные из [Monitoring]</h6>

  <div class="no-padding no-margin">
    <q-table
      color="primary"
      dense
      card-class="bg-amber-1 text-brown"
      row-key="id"
      :columns="cols"
      :rows="rows"
      :wrap-cells="true"
      :table-colspan="4"
      table-header-class="text-bold text-white bg-blue-grey-13"
      separator="cell"
      :rows-per-page-options="[0]"
    >
    </q-table>
  </div>
</template>

<script setup>
import { api } from '../boot/axios.js'
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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
  const apiPrefix = import.meta.env.PROD ? 'fast/' : 'http://127.0.0.1:8000/'
  api
    .get(`${apiPrefix}load_obj/1008`)
    .then((res) => {
      rows.value = res.data
    })
    .catch((err) => console.error('Ошибка при получении факторов:', err))
}

onMounted(() => {
  cols.value = getColumns()
  fetchData()
})

/*
export default {
  name: "CalculationB",

  data: function() {
    return {
      rows: [],
      cols: []
    }
  },


  methods: {
    getColumns() {
      return [
        {
          name: "name",
          label: this.$t("fldName"),
          field: "name",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:60%",
        },

        {
          name: "cod",
          label: this.$t("code"),
          field: "cod",
          align: "left",
          classes: "bg-blue-grey-1",
          headerStyle: "font-size: 1.2em; width:40%",
        },
      ]
    },
  },

  mounted() {

  },

  created() {
    this.cols = this.getColumns();

    const apiPrefix = import.meta.env.PROD ? 'fast/' : 'http://127.0.0.1:8000/';
    api
      .get(`${apiPrefix}load_obj/1008`)
      .then(res => {
        this.rows = res.data;
      })
      .catch(err => console.error("Ошибка при загрузке объекта:", err));
  }
}
*/
</script>

<style scoped></style>
