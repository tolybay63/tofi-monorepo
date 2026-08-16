<template>
  <q-input v-model="displayLabel" :label="label" dense readonly clearable @clear="onClear">
    <template v-slot:append>
      <q-icon name="arrow_drop_down" class="cursor-pointer" />
    </template>
    <q-popup-proxy v-model="menuOpen" fit anchor="bottom left" self="top left">
      <q-card style="min-width: 300px; max-height: 400px" class="scroll">
        <q-card-section class="q-pa-xs">
          <q-tree
            :nodes="options"
            node-key="id"
            label-key="name"
            children-key="children"
            v-model:selected="selectedId"
            @update:selected="handleSelect"
          />
        </q-card-section>
      </q-card>
    </q-popup-proxy>
  </q-input>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: null,
  },
  options: {
    type: Array,
    default: () => [],
  },
  label: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'select'])

const menuOpen = ref(false)
const selectedId = ref(props.modelValue)

// Синхронизация при изменении значения снаружи
watch(
  () => props.modelValue,
  (newVal) => {
    selectedId.value = newVal
  },
)

// Рекурсивный поиск названия выбранного узла для отображения в поле ввода
const findNodeName = (nodes, id) => {
  if (!Array.isArray(nodes)) return ''
  for (const node of nodes) {
    if (node.id === id) return node.name
    if (node.children && node.children.length > 0) {
      const found = findNodeName(node.children, id)
      if (found) return found
    }
  }
  return ''
}

const displayLabel = computed(() => {
  return findNodeName(props.options, selectedId.value) || ''
})

const handleSelect = (targetId) => {
  if (targetId !== null) {
    selectedId.value = targetId
    emit('update:modelValue', targetId)

    // Находим полный объект узла, если родителю нужны дополнительные поля (например, pv)
    const foundNode = findNodeObject(props.options, targetId)
    emit('select', foundNode)

    menuOpen.value = false // Закрываем меню при выборе
  }
}

const findNodeObject = (nodes, id) => {
  if (!Array.isArray(nodes)) return null
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNodeObject(node.children, id)
      if (found) return found
    }
  }
  return null
}

const onClear = () => {
  selectedId.value = null
  emit('update:modelValue', null)
  emit('select', null)
}
</script>
