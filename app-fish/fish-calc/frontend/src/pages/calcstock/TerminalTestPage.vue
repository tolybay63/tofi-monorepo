<template>
  <q-dialog v-model="isOpen" persistent>
    <q-card class="bg-grey-10 text-white q-pa-none" style="width: 750px; max-width: 90vw;">
      <!-- Шапка с признаком жизни и кнопкой закрытия -->
      <q-card-section class="row items-center bg-grey-9 q-py-sm">
        <div class="text-subtitle2 text-white font-mono flex items-center q-gutter-x-sm">
          <q-spinner-dots v-if="isLoading" color="primary" size="1.5em" />
          <q-icon v-else name="check_circle" color="positive" size="sm" />
          <span>{{ isLoading ? `Выполнение шагов расчета (ID: ${calculationId})...` : 'Все шаги успешно выполнены!' }}</span>
        </div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup :disable="isLoading" color="white" />
      </q-card-section>

      <!-- Тело терминала со скроллингом -->
      <q-card-section class="q-pa-md">
        <div
          ref="terminalContainer"
          class="terminal-box bg-black q-pa-md rounded-borders"
          style="height: 350px; overflow-y: auto; font-family: 'Courier New', Courier, monospace; font-size: 13px; white-space: pre-wrap;"
        >
          <div v-for="(log, index) in logs" :key="index" class="q-mb-xs">
            <span class="text-grey-5">[{{ log.time }}]</span>
            <span :class="log.isError ? 'text-red-4' : (log.isSuccess ? 'text-green-4' : 'text-white')">
              {{ log.text }}
            </span>
          </div>
        </div>
      </q-card-section>

      <!-- Подвал с кнопкой закрытия -->
      <q-card-actions align="right" class="bg-grey-9 q-pa-sm">
        <q-btn
          label="Закрыть и загрузить результаты"
          color="primary"
          :disable="isLoading"
          v-close-popup
          @click="onFinishModal"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { api } from '@/boot/axios'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  calculationId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'completed'])

const isOpen = ref(true)
const isLoading = ref(true)
const logs = ref([])
const terminalContainer = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  if (terminalContainer.value) {
    terminalContainer.value.scrollTop = terminalContainer.value.scrollHeight
  }
}

const addLog = (text, isError = false, isSuccess = false) => {
  const time = new Date().toLocaleTimeString()
  logs.value.push({ time, text, isError, isSuccess })
  scrollToBottom()
}

const startTestSequence = async () => {
  try {
    logs.value = []
    isLoading.value = true

    addLog(`Старт проверки для расчета ID: ${props.calculationId}...`)

    // Используем прямой полный путь до FastAPI, чтобы исключить проблемы с прокси/конфигом axios
    const url = `http://127.0.0.1:8000/props/${props.calculationId}`
    addLog(`Запрос: GET ${url}`)

    const response = await api.get(url)

    addLog(`Успешно получено! Ответ:`, false, true)
    addLog(JSON.stringify(response.data, null, 2), false, true)

    addLog('Все проверки успешно завершены!', false, true)
    isLoading.value = false

  } catch (error) {
    console.error('Terminal execution error:', error)
    const errorMsg = error.response?.data?.detail || error.message || String(error)
    addLog(`ОШИБКА: ${errorMsg}`, true, false)
    isLoading.value = false
  }
}

// onMounted гарантирует выполнение сразу после отрисовки модального окна в DOM
onMounted(() => {
  startTestSequence()
})

const onFinishModal = () => {
  emit('completed', props.calculationId)
}
</script>

<style scoped>
.font-mono {
  font-family: 'Courier New', Courier, monospace;
}
.terminal-box::-webkit-scrollbar {
  width: 8px;
}
.terminal-box::-webkit-scrollbar-track {
  background: #121212;
}
.terminal-box::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 4px;
}
.terminal-box::-webkit-scrollbar-thumb:hover {
  background: #666;
}
</style>
