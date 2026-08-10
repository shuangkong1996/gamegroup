<template>
  <div class="space-y-3">
    <div
      v-for="(item, index) in items"
      :key="itemKey(item, index)"
      class="rounded-xl border border-line/70 p-3 transition-colors"
      :class="dragIndex === index ? 'border-brand-500/50 bg-brand-500/5' : ''"
      draggable="true"
      @dragstart="onDragStart(index, $event)"
      @dragover.prevent="onDragOver(index)"
      @drop.prevent="onDrop(index)"
      @dragend="onDragEnd"
    >
      <div class="mb-3 flex items-center justify-between gap-2">
        <button
          class="inline-flex cursor-grab items-center gap-1 text-xs text-muted active:cursor-grabbing"
          type="button"
          title="拖拽排序"
        >
          <GripVertical class="h-3.5 w-3.5" />
          拖拽排序
        </button>
        <div class="flex items-center gap-2">
          <button class="text-xs text-muted disabled:opacity-40" type="button" :disabled="index === 0" @click="move(index, -1)">
            上移
          </button>
          <button
            class="text-xs text-muted disabled:opacity-40"
            type="button"
            :disabled="index === items.length - 1"
            @click="move(index, 1)"
          >
            下移
          </button>
          <button class="text-xs text-rose-500" type="button" @click="remove(index)">删除</button>
        </div>
      </div>
      <slot :item="item" :index="index" />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T">
import { ref } from 'vue'
import { GripVertical } from '@lucide/vue'

const props = defineProps<{
  items: T[]
  itemKey?: (item: T, index: number) => string | number
}>()

const emit = defineEmits<{
  reorder: [next: T[]]
}>()

const dragIndex = ref<number | null>(null)

function itemKey(item: T, index: number) {
  return props.itemKey?.(item, index) ?? index
}

function onDragStart(index: number, event: DragEvent) {
  dragIndex.value = index
  event.dataTransfer?.setData('text/plain', String(index))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function onDragOver(index: number) {
  if (dragIndex.value === null || dragIndex.value === index) return
}

function onDrop(index: number) {
  if (dragIndex.value === null || dragIndex.value === index) return
  const next = [...props.items]
  const [moved] = next.splice(dragIndex.value, 1)
  next.splice(index, 0, moved)
  emit('reorder', next)
  dragIndex.value = null
}

function onDragEnd() {
  dragIndex.value = null
}

function move(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= props.items.length) return
  const next = [...props.items]
  const [moved] = next.splice(index, 1)
  next.splice(target, 0, moved)
  emit('reorder', next)
}

function remove(index: number) {
  const next = [...props.items]
  next.splice(index, 1)
  emit('reorder', next)
}
</script>
