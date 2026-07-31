<template>
  <q-card class="q-pa-md q-mb-md">
    <q-card-section>
      <div class="text-h6">Настройка визуализации данных куба</div>
    </q-card-section>

    <!-- Панель управления (Интерактивные элементы) -->
    <q-card-section class="row q-col-gutter-md">
      <!-- Выбор измерения для оси X -->
      <div class="col-12 col-md-3">
        <q-select
          v-model="config.xAxisField"
          :options="dimensionOptions"
          label="Измерение по оси X"
          outlined
          dense
          emit-value
          map-options
          @update:model-value="updateChart"
        />
      </div>

      <!-- Выбор измерения для Серий (Легенды) -->
      <div class="col-12 col-md-3">
        <q-select
          v-model="config.seriesField"
          :options="dimensionOptions"
          label="Группировка (Серии)"
          outlined
          dense
          emit-value
          map-options
          @update:model-value="updateChart"
        />
      </div>

      <!-- Тип графика -->
      <div class="col-12 col-md-3">
        <q-select
          v-model="config.chartType"
          :options="[
            льгот = { label: 'Столбчатая (Bar)', value: 'bar' },
            { label: 'Линейная (Line)', value: 'line' }
          ]"
          label="Тип диаграммы"
          outlined
          dense
          emit-value
          map-options
          @update:model-value="updateChart"
        />
      </div>

      <!-- Переключатель накопления (Stack) -->
      <div class="col-12 col-md-3 flex items-center">
        <q-toggle
          v-model="config.isStacked"
          label="С накоплением (Stack)"
          @update:model-value="updateChart"
        />
      </div>
    </q-card-section>

    <!-- Контейнер для графика ECharts -->
    <q-card-section>
      <div ref="chartRef" style="width: 100%; height: 500px;"></div>
    </q-card-section>
  </q-card>
</template>

<script setup>

import { ref, reactive, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';

// DOM-ссылка на блок графика
const chartRef = ref(null);
let chartInstance = null;

// Доступные измерения из вашей системы
const dimensionOptions = [
  { label: 'Вид рыбы', value: 'fishType' },
  { label: 'Возраст рыбы', value: 'age' },
  { label: 'Пол рыбы', value: 'sex' },
  { label: 'Период (Год)', value: 'year' }
];

// Реактивная конфигурация настроек пользователя
const config = reactive({
  xAxisField: 'fishType',
  seriesField: 'age',
  chartType: 'bar',
  isStacked: true
});

// Пример сырых данных из бэкенда (ваш многомерный куб)
const rawData = [
  { year: '2015', fishType: 'Лещ', age: '2 года', sex: 'Самка', value: 6 },
  { year: '2015', fishType: 'Лещ', age: '3 года', sex: 'Самка', value: 18 },
  { year: '2015', fishType: 'Лещ', age: '4 года', sex: 'Самка', value: 72 },
  { year: '2015', fishType: 'Судак', age: '2 года', sex: 'Самка', value: 10 },
  { year: '2015', fishType: 'Судак', age: '3 года', sex: 'Самка', value: 25 },
  // ... другие данные
];

// Функция построения опций для ECharts
function getChartOptions() {
  // 1. Сбор уникальных категорий для оси X и серий
  const xCategories = [...new Set(rawData.map(item => item[config.xAxisField]))];
  const seriesCategories = [...new Set(rawData.map(item => item[config.seriesField]))];

  // 2. Формирование серий данных
  const series = seriesCategories.map(seriesName => {
    const dataForSeries = xCategories.map(xVal => {
      const found = rawData.find(
        item => item[config.xAxisField] === xVal && item[config.seriesField] === seriesName
      );
      return found ? found.value : 0;
    });

    return {
      name: seriesName,
      type: config.chartType,
      stack: config.isStacked ? 'total_stack' : null,
      data: dataForSeries
    };
  });

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      type: 'scroll',
      top: 10
    },
    toolbox: {
      feature: {
        saveAsImage: {},
        dataZoom: {},
        magicType: { type: ['line', 'bar'] }
      }
    },
    xAxis: {
      type: 'category',
      data: xCategories,
      axisLabel: { interval: 0, rotate: 25 }
    },
    yAxis: {
      type: 'value',
      name: 'Количество (шт)'
    },
    series: series
  };
}

// Инициализация и обновление графика
function updateChart() {
  if (chartInstance) {
    chartInstance.setOption(getChartOptions(), true);
  }
}

onMounted(() => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value);
    chartInstance.setOption(getChartOptions());

    // Автоматический ресайз при изменении размера окна
    window.addEventListener('resize', handleResize);
  }
});

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose();
  }
  window.removeEventListener('resize', handleResize);
});

function handleResize() {
  chartInstance?.resize();
}
</script>
