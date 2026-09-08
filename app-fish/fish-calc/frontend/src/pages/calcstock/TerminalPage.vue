<template>
  <q-dialog v-model="isOpen" persistent>
    <q-card class="bg-grey-10 text-white q-pa-none" style="width: 750px; max-width: 90vw;">
      <q-card-section class="row items-center bg-grey-9 q-py-sm">
        <div class="text-subtitle2 text-white font-mono flex items-center q-gutter-x-sm">
          <q-spinner-dots v-if="isLoading" color="primary" size="1.5em" />
          <q-icon v-else name="check_circle" color="positive" size="sm" />
          <span>{{ isLoading ? `Выполнение расчета (ID: ${calculationId})...` : 'Расчет завершен!' }}</span>
        </div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup :disable="isLoading" color="white" />
      </q-card-section>

      <q-card-section class="q-pa-md">
        <div
          ref="terminalContainer"
          class="terminal-box bg-black q-pa-md rounded-borders"
          style="height: 350px; overflow-y: auto; font-family: 'Courier New', Courier, monospace; font-size: 13px; white-space: pre-wrap;"
        >
          <div v-for="(log, index) in logs" :key="index" class="q-mb-xs">
            <span :class="log.isError ? 'text-red-4' : (log.isSuccess ? 'text-green-4' : 'text-white')">
              {{ log.text }}
            </span>
          </div>
        </div>
      </q-card-section>

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
import {api} from "@/boot/axios.js";

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  calculationId: { type: [Number, String], required: true }
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
  logs.value.push({ text, isError, isSuccess })
  scrollToBottom()
}

const checkTarget = async () => {
  await api.post('', {
    method: 'auth/checkTarget',
    params: ['calc'],
  })
}

const startStreamExecution = async () => {
  try {
    await checkTarget()
    logs.value = []
    isLoading.value = true
    const apiPrefix = import.meta.env.PROD ? 'fast/' : 'http://127.0.0.1:8000/'
    const url = `${apiPrefix}calc_bayes/${props.calculationId}/run`
    const response = await fetch(url)

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { value, done } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      // Разделяем чанк по строкам, если пришло сразу несколько
      chunk.split('\n').forEach(line => {
        if (line.trim()) {
          const isErr = line.includes('ОШИБКА')
          const isSucc = line.includes('успешно') || line.includes('завершен')
          addLog(line, isErr, isSucc)
        }
      })
    }

    isLoading.value = false

  } catch (error) {
    addLog(`ОШИБКА СОЕДИНЕНИЯ: ${error.message}`, true, false)
    isLoading.value = false
  }
}

onMounted(() => {
  startStreamExecution()
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
