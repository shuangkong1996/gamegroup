<template>
  <button class="secondary-button relative" type="button" @click="copy">
    <Check v-if="copied" class="h-4 w-4 text-emerald-500" />
    <Copy v-else class="h-4 w-4" />
    {{ copied ? '已复制' : label }}
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Check, Copy } from '@lucide/vue'

const props = withDefaults(
  defineProps<{
    value: string
    label?: string
  }>(),
  {
    label: '复制'
  }
)

const copied = ref(false)
let timer = 0

async function copy() {
  if (!props.value) {
    return
  }
  await navigator.clipboard.writeText(props.value)
  copied.value = true
  window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    copied.value = false
  }, 1400)
}
</script>
