<template>
  <q-field
    :model-value="modelValue"
    :label="label"
    dense
    stack-label
    clearable
    @clear="onClear"
    class="cursor-pointer"
  >
    <template v-slot:control>
      <div
        class="self-center full-width no-outline q-field__native row items-center q-gutter-xs"
        style="padding-top: 4px; min-height: 32px;"
        tabindex="0"
      >
        <!-- Множественный выбор: чипсы -->
        <template v-if="multiple && Array.isArray(modelValue) && modelValue.length > 0">
          <q-chip
            v-for="val in modelValue"
            :key="val"
            dense
            removable
            @remove="removeTag(val)"
            color="grey-3"
            text-color="dark"
            class="q-ma-none"
          >
            {{ findNodeName(options, val) }}
          </q-chip>
        </template>
        <!-- Одиночный выбор: текст -->
        <template v-else-if="!multiple">
          <span style="padding-top: 4px;">{{ displayLabel }}</span>
        </template>
        <template v-else>
          <span class="text-grey-6" style="padding-top: 4px;">Выберите...</span>
        </template>
      </div>
    </template>

    <template v-slot:append>
      <q-icon name="arrow_drop_down" />
    </template>

    <q-popup-proxy v-model="menuOpen" fit anchor="bottom left" self="top left">
      <q-card style="min-width: 300px; max-height: 400px" class="scroll">
        <q-card-section class="q-pa-xs">
          <!-- Одиночный выбор -->
          <q-tree
            v-if="!multiple"
            :nodes="options"
            :node-key="nodeKey"
            label-key="name"
            children-key="children"
            v-model:selected="selectedSingle"
            @update:selected="handleSingleSelect"
            default-expand-all
          />
          <!-- Множественный выбор -->
          <q-tree
            v-else
            :nodes="options"
            :node-key="nodeKey"
            label-key="name"
            children-key="children"
            tick-strategy="strict"
            v-model:ticked="selectedMultiple"
            @update:ticked="handleMultipleSelect"
            default-expand-all
          />
        </q-card-section>
      </q-card>
    </q-popup-proxy>
  </q-field>
</template>

<script setup>
import {computed, ref, watch} from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Array],
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
  multiple: {
    type: Boolean,
    default: false,
  },
  // Новое свойство: какое поле использовать в качестве ключа/значения (по умолчанию 'id')
  nodeKey: {
    type: String,
    default: 'id',
  },
})

const emit = defineEmits(['update:modelValue', 'select'])

const menuOpen = ref(false)

const selectedSingle = ref(props.multiple ? null : props.modelValue)
const selectedMultiple = ref(props.multiple ? (Array.isArray(props.modelValue) ? props.modelValue : []) : [])

watch(
  () => props.modelValue,
  (newVal) => {
    if (props.multiple) {
      selectedMultiple.value = Array.isArray(newVal) ? newVal : []
    } else {
      selectedSingle.value = newVal
    }
  },
  { deep: true }
)

// Ищем название узла по выбранному значению (поддерживает и id, и key)
const findNodeName = (nodes, targetVal) => {
  if (!Array.isArray(nodes)) return ''
  for (const node of nodes) {
    if (node[props.nodeKey] === targetVal) return node.name
    if (node.children && node.children.length > 0) {
      const found = findNodeName(node.children, targetVal)
      if (found) return found
    }
  }
  return ''
}

// Ищем объект узла по значению
const findNodeObject = (nodes, targetVal) => {
  if (!Array.isArray(nodes)) return null
  for (const node of nodes) {
    if (node[props.nodeKey] === targetVal) return node
    if (node.children) {
      const found = findNodeObject(node.children, targetVal)
      if (found) return found
    }
  }
  return null
}

const displayLabel = computed(() => {
  if (props.multiple) return ''
  return findNodeName(props.options, selectedSingle.value) || ''
})

const handleSingleSelect = (targetVal) => {
  if (targetVal !== null) {
    selectedSingle.value = targetVal
    emit('update:modelValue', targetVal)
    const foundNode = findNodeObject(props.options, targetVal)
    emit('select', foundNode)
    menuOpen.value = false
  }
}

const handleMultipleSelect = (targetVals) => {
  selectedMultiple.value = targetVals
  emit('update:modelValue', targetVals)
  const foundNodes = targetVals.map((val) => findNodeObject(props.options, val)).filter(Boolean)
  emit('select', foundNodes)
}

const removeTag = (valToRemove) => {
  selectedMultiple.value = selectedMultiple.value.filter((val) => val !== valToRemove)
  emit('update:modelValue', selectedMultiple.value)
  const foundNodes = selectedMultiple.value.map((val) => findNodeObject(props.options, val)).filter(Boolean)
  emit('select', foundNodes)
}

const onClear = () => {
  if (props.multiple) {
    selectedMultiple.value = []
    emit('update:modelValue', [])
    emit('select', [])
  } else {
    selectedSingle.value = null
    emit('update:modelValue', null)
    emit('select', null)
  }
}
</script>
