<template>
  <q-card class="q-pa-md q-mb-md">
    <q-card-section>
      <div class="q-pa-md row">
        <div class="text-h6">Водоем:
          <span class="text-primary">{{ ownerName }} </span>
        </div>
        <!-- Кнопка быстрого скачивания графика -->
        <q-space/>
        <q-btn
          color="primary"
          dense
          icon="download"
          label="Скачать график"
          outline
          @click="exportChart"
        />
      </div>
    </q-card-section>

    <!-- БЛОК 1: Фиксация параметров (Фильтры) -->
    <q-card-section class="bg-grey-1 q-pa-md rounded-borders q-mb-md">
      <div class="text-subtitle2 text-grey-8 q-mb-sm">
        1. Фильтрация куба (параметры):
      </div>
      <div class="row q-col-gutter-md">
        <!-- Первый фиксированный параметр -->
        <div class="col-12 col-md-6 row q-col-gutter-sm items-center">
          <div class="col-6">
            <q-select
              v-model="fixedParam1.dimension"
              :options="optionsForParam1"
              dense
              emit-value
              label="Параметр 1"
              map-options
              option-label="label"
              option-value="value"
              outlined
              @update:model-value="onFixedParamChange(1)"
            />
          </div>
          <div class="col-6">
            <q-select
              v-model="fixedParam1.value"
              :options="getValuesForDimension(fixedParam1.dimension)"
              clearable
              dense
              emit-value
              label="Значение"
              map-options
              option-label="label"
              option-value="value"
              outlined
              @update:model-value="fetchCubeData"
            />
          </div>
        </div>

        <!-- Второй фиксированный параметр (опциональный, по умолчанию null) -->
        <div class="col-12 col-md-6 row q-col-gutter-sm items-center">
          <div class="col-6">
            <q-select
              v-model="fixedParam2.dimension"
              :options="optionsForParam2"
              clearable
              dense
              emit-value
              label="Параметр 2 (необязательно)"
              map-options
              option-label="label"
              option-value="value"
              outlined
              @update:model-value="onFixedParamChange(2)"
            />
          </div>
          <div class="col-6">
            <q-select
              v-model="fixedParam2.value"
              :disable="!fixedParam2.dimension"
              :options="getValuesForDimension(fixedParam2.dimension)"
              clearable
              dense
              emit-value
              label="Значение"
              map-options
              option-label="label"
              option-value="value"
              outlined
              @update:model-value="fetchCubeData"
            />
          </div>
        </div>
      </div>
    </q-card-section>

    <!-- БЛОК 2: Выбор осей графика -->
    <q-card-section class="row q-col-gutter-md">
      <div class="col-12 col-md-4">
        <q-select
          v-model="chartAxes.xAxisField"
          :options="remainingDimensions"
          dense
          emit-value
          label="Измерение по оси X"
          map-options
          option-label="label"
          option-value="value"
          outlined
          @update:model-value="fetchCubeData"
        />
      </div>

      <div class="col-12 col-md-4">
        <q-select
          v-model="chartAxes.seriesField"
          :options="remainingDimensions"
          dense
          emit-value
          label="Группировка (Серии)"
          map-options
          option-label="label"
          option-value="value"
          outlined
          @update:model-value="fetchCubeData"
        />
      </div>

      <div class="col-12 col-md-4">
        <q-select
          v-model="chartAxes.chartType"
          :options="[
            { label: 'Столбчатая (Bar)', value: 'bar' },
            { label: 'Линейная (Line)', value: 'line' }
          ]"
          dense
          emit-value
          label="Тип диаграммы"
          map-options
          option-label="label"
          option-value="value"
          outlined
          @update:model-value="updateChart"
        />
      </div>

      <div class="col-12 col-md-3 flex items-center">
        <q-toggle
          v-model="chartAxes.isStacked"
          label="С накоплением (Stack)"
          @update:model-value="updateChart"
        />
      </div>

    </q-card-section>

    <!-- Контейнер графика -->
    <q-card-section>
      <div ref="chartRef" style="width: 100%; height: 600px;"></div>
    </q-card-section>
  </q-card>
</template>

<script setup>
import {computed, nextTick, onMounted, onUnmounted, reactive, ref} from 'vue';
import * as echarts from 'echarts';
import {api} from "boot/axios.js";

const chartRef = ref(null);
let chartInstance = null;
const loading = ref(false);
const owner = 1061;
const ownerName = ref(''); // Сделали реактивным для обновления из БД
const meter = 1007;

const allDimensions = [
  {label: 'Период (Год)', value: 'year'},
  {label: 'Вид рыбы', value: 'fishType'},
  {label: 'Возраст рыбы', value: 'age'},
  {label: 'Пол рыбы', value: 'sex'}
];

const databaseDictionary = reactive({
  dims: [],
  year: [],
  fishType: [],
  age: [],
  sex: []
});

const cubeData = ref([]);

// Первый параметр обязателен, второй по умолчанию пустой (null)
const fixedParam1 = reactive({dimension: 'fishType', value: null});
const fixedParam2 = reactive({dimension: null, value: null});

const chartAxes = reactive({xAxisField: 'year', seriesField: 'age', chartType: 'bar', isStacked: false});

// Динамические опции для фильтров с исключением дубликатов
const optionsForParam1 = computed(() => {
  return allDimensions.filter(d => d.value !== fixedParam2.dimension);
});

const optionsForParam2 = computed(() => {
  return allDimensions.filter(d => d.value !== fixedParam1.dimension);
});

// Оставшиеся измерения для осей графика
const remainingDimensions = computed(() => {
  const fixedDims = [fixedParam1.dimension, fixedParam2.dimension].filter(Boolean);
  return allDimensions.filter(d => !fixedDims.includes(d.value));
});

function getValuesForDimension(dimKey) {
  if (!dimKey) return [];
  const rawData = databaseDictionary[dimKey];
  const list = Array.isArray(rawData) ? rawData : (rawData?.records || []);

  return list.map(item => {
    if (typeof item === 'object' && item !== null) {
      return {
        label: item.label || item.name || item.value,
        value: item.value || item.id
      };
    }
    return {
      label: String(item),
      value: item
    };
  });
}

function onFixedParamChange(paramNum) {
  const param = paramNum === 1 ? fixedParam1 : fixedParam2;

  if (!param.dimension) {
    param.value = null;
  } else {
    const vals = getValuesForDimension(param.dimension);
    param.value = vals.length ? vals[0].value : null;
  }

  // Проверка актуальности осей графика
  const free = remainingDimensions.value.map(d => d.value);
  if (!free.includes(chartAxes.xAxisField)) chartAxes.xAxisField = free[0] || 'year';
  if (!free.includes(chartAxes.seriesField)) chartAxes.seriesField = free[1] || free[0] || 'age';

  fetchCubeData();
}

const getCubeMetaData = async () => {
  loading.value = true;
  try {
    const response = await api.post('', {
      method: 'chart/loadCubeMetaData',
      params: [owner, meter],
    });

    ownerName.value = response.data.result.ownerName || 'Водоем';
    databaseDictionary.dims = response.data.result.dims?.records || [];
    databaseDictionary.fishType = response.data.result.fishType?.records || [];
    databaseDictionary.age = response.data.result.age?.records || [];
    databaseDictionary.sex = response.data.result.sex?.records || [];
    databaseDictionary.year = ['2015', '2016', '2017', '2018', '2019', '2020', '2021', '2022', '2023', '2024', '2025', '2026'];
  } catch (error) {
    console.error('Ошибка загрузки метаданных:', error);
  } finally {
    loading.value = false;
  }
};

const loadCubeData = async () => {
  console.log('loadCubeData!');
  loading.value = true;
  try {
    const response = await api.post('', {
      method: 'chart/loadCubeData',
      params: [{
        owner: owner,
        meter: meter,
        param1Key: fixedParam1.dimension,
        param1: fixedParam1.value,
        param2Key: fixedParam2.dimension,
        param2: fixedParam2.value,
        xAxisField: chartAxes.xAxisField,
        seriesField: chartAxes.seriesField,
      }],
    });

    console.log("Data response:", response.data.result);
    cubeData.value = response.data.result.records || [];
  } catch (error) {
    console.error('Ошибка загрузки данных:', error);
  } finally {
    loading.value = false;
  }
};

async function loadDataFromDatabase() {
  try {
    await getCubeMetaData();

    // Дефолт только для первого параметра
    const fishTypes = getValuesForDimension('fishType');
    if (fishTypes.length > 0) {
      fixedParam1.value = fishTypes[0].value;
    }

    await fetchCubeData();
  } catch (error) {
    console.error('Ошибка инициализации из БД:', error);
  }
}

async function fetchCubeData() {
  try {
    await loadCubeData();
    updateChart();
  } catch (error) {
    console.error('Ошибка загрузки данных куба:', error);
  }
}

function getChartOptions() {
  const xField = chartAxes.xAxisField;
  const sField = chartAxes.seriesField;

  const xCategories = [...new Set(cubeData.value.map(item => item[xField]))];
  const seriesCategories = [...new Set(cubeData.value.map(item => item[sField]))];

  const series = seriesCategories.map(seriesName => {
    const dataForSeries = xCategories.map(xVal => {
      const found = cubeData.value.find(
        item => item[xField] === xVal && item[sField] === seriesName
      );
      return found ? found.value : 0;
    });

    const dict = getValuesForDimension(sField);
    const dictItem = dict.find(i => i.value === seriesName || i.label === seriesName);
    const readableName = dictItem ? dictItem.label : seriesName;

    const isArea = chartAxes.chartType === 'area';

    return {
      name: readableName,
      type: isArea ? 'line' : chartAxes.chartType,
      areaStyle: isArea ? {} : undefined, // Превращает линию в залитую область
      data: dataForSeries,
      stack: chartAxes.isStacked ? 'total_stack' : null,
      smooth: true // Плавные линии для красивого отображения трендов
    };
  });

  return {
    // Красивая палитра цветов (можно настроить под бренд-бук)
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
    legend: {type: 'scroll', top: 10},
    // Панель инструментов (зум, сохранение картинки, переключение типов)

/*
    toolbox: {
      feature: {
        saveAsImage: {title: 'Сохранить как PNG'},
        dataZoom: {title: {zoom: 'Зум', back: 'Сброс зума'}},
        magicType: {type: ['line', 'bar'], title: {line: 'Линии', bar: 'Столбцы'}}
      },
      right: 20
    },
*/

    xAxis: {type: 'category', data: xCategories, axisLabel: {interval: 0, rotate: 15}},
    yAxis: {type: 'value', name: 'Количество (шт)'},
    series: series
  };
}


function updateChart() {
  if (chartInstance) {
    chartInstance.setOption(getChartOptions(), true);
  }
}

// Метод программного скачивания графика пользователем
function exportChart() {
  if (!chartInstance) return;
  const url = chartInstance.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  });
  const link = document.createElement('a');
  link.href = url;
  link.download = 'cube-chart-report.png';
  link.click();
}

onMounted(async () => {
  await nextTick();
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value);
    await loadDataFromDatabase();
    window.addEventListener('resize', () => chartInstance?.resize());
  }
});

onUnmounted(() => {
  chartInstance?.dispose();
  window.removeEventListener('resize', () => chartInstance?.resize());
});
</script>
