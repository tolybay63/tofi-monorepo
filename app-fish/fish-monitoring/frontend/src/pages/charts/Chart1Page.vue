<template>
  <q-page class="q-pa-md row q-col-gutter-md">
    <!-- Боковая панель настроек -->
    <div class="col-12 col-md-3">
      <q-card flat bordered class="full-height">
        <q-card-section>
          <div class="text-h6 q-mb-md">⚙️ Настройки 4D</div>

          <!-- Переключатель типа окраски (4-го измерения) -->
          <q-select
            v-model="colorPalette"
            :options="['Rainbow', 'Viridis', 'Sunset']"
            label="Цветовая схема (4D)"
            filled
            class="q-mb-md"
            @update:model-value="updateChart"
          />

          <!-- Интерактивный фильтр по максимальному размеру точек -->
          <div class="text-caption q-mb-sm">Макс. размер точек (Сложность):</div>
          <q-slider
            v-model="maxPointSize"
            :min="5"
            :max="30"
            :step="1"
            label
            color="primary"
            class="q-mb-lg"
          />

          <!-- Переключатель вращения -->
          <q-toggle
            v-model="autoRotate"
            label="Автоматическое вращение"
            color="secondary"
          />
        </q-card-section>
      </q-card>
    </div>

    <!-- Область графика -->
    <div class="col-12 col-md-9">
      <q-card flat bordered style="height: 600px;">
        <v-chart class="chart" :option="chartOption" ref="chartRef" autofocus />
      </q-card>
    </div>
  </q-page>
</template>

<script setup>
import {computed, provide, ref} from 'vue'
import {use} from 'echarts/core'
import {CanvasRenderer} from 'echarts/renderers'
import {TooltipComponent, VisualMapComponent} from 'echarts/components'
import VChart, {THEME_KEY} from 'vue-echarts'

// ИМПОРТИРУЕМ ИЗ ECHARTS-GL С ПРАВИЛЬНЫМИ НАЗВАНИЯМИ КЛАССОВ:
import {Grid3DComponent} from 'echarts-gl/components'
import {Scatter3DChart} from 'echarts-gl/charts' // <-- Здесь изменен суффикс с Series на Chart

// Регистрируем компоненты
use([
  CanvasRenderer,
  VisualMapComponent,
  TooltipComponent,
  Grid3DComponent,
  Scatter3DChart  // <-- Передаем правильный класс
])
// Опционально: задаем темную или светлую тему (Quasar integration)
provide(THEME_KEY, 'light')

const chartRef = ref(null)
const colorPalette = ref('Rainbow')
const maxPointSize = ref(20)
const autoRotate = ref(true)

// Генерация демонстрационных 4D данных
// Структура массива: [X, Y, Z, 4D-Value (Цвет/Размер)]
const generate4DData = () => {
  const data = []
  for (let i = 0; i < 200; i++) {
    const x = (Math.random() * 100).toFixed(1)
    const y = (Math.random() * 100).toFixed(1)
    const z = (Math.random() * 100).toFixed(1)
    const val4D = (Math.random() * 1000).toFixed(0) // Четвертое измерение
    data.push([x, y, z, val4D])
  }
  return data
}

const rawData = ref(generate4DData())

// Цветовые схемы для 4-го измерения
const colorGradients = {
  Rainbow: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffbf', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026'],
  Viridis: ['#440154', '#414487', '#2a788e', '#22a884', '#7ad151', '#fde725'],
  Sunset: ['#ffe600', '#ff5e00', '#ff0000', '#990000']
}

// Вычисляемые настройки графика (реактивно обновляются при изменении контролов Quasar)
const chartOption = computed(() => {
  return {
    tooltip: {
      formatter: (params) => {
        return `
          <b>Точка данных</b><br/>
          X (Параметр 1): ${params.value[0]}<br/>
          Y (Параметр 2): ${params.value[1]}<br/>
          Z (Параметр 3): ${params.value[2]}<br/>
          4D (Вес/Цвет): ${params.value[3]}
        `
      }
    },
    xAxis3D: { name: 'X Axis', type: 'value' },
    yAxis3D: { name: 'Y Axis', type: 'value' },
    zAxis3D: { name: 'Z Axis', type: 'value' },
    grid3D: {
      viewControl: {
        autoRotate: autoRotate.value, // Интерактивное управление вращением
        autoRotateSpeed: 10
      },
      boxWidth: 100,
      boxHeight: 100,
      boxDepth: 100
    },
    // VisualMap связывает 4-й элемент массива данных (индекс 3) с цветом и размером точки
    visualMap: [
      {
        type: 'continuous',
        dimension: 3, // Индекс 4-го измерения в массиве данных
        min: 0,
        max: 1000,
        inRange: {
          color: colorGradients[colorPalette.value],
          symbolSize: [5, maxPointSize.value] // Размер точки тоже зависит от 4D значения
        },
        text: ['High 4D', 'Low 4D'],
        calculable: true,
        bottom: 20
      }
    ],
    series: [
      {
        type: 'scatter3D',
        data: rawData.value,
        opacity: 0.8,
        emphasis: {
          itemStyle: {
            color: '#fff' // Подсветка белым при наведении
          }
        }
      }
    ]
  }
})
</script>

<style scoped>
.chart {
  width: 100%;
  height: 100%;
}
</style>
