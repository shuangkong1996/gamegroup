<template>
  <ToolWorkspace eyebrow="开发工具" title="网址编码解码" description="对网址片段或完整文本执行安全编码与解码。">
    <template #actions>
      <CopyButton :value="output" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold text-slate-900 dark:text-zinc-100">输入</h2>
        <button class="secondary-button" type="button" @click="clear">清空</button>
      </div>
      <textarea v-model="input" class="code-field" spellcheck="false" placeholder="输入网址或文本" />
      <p v-if="error" class="mt-3 text-sm font-medium text-rose-500">{{ error }}</p>
    </section>

    <section class="panel p-5">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输出</h2>
        <div class="flex gap-2">
          <button class="secondary-button" type="button" @click="decode">解码</button>
          <button class="primary-button" type="button" @click="encode">编码</button>
        </div>
      </div>
      <textarea v-model="output" class="code-field" spellcheck="false" readonly />
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const input = ref('https://example.com/search?q=工具箱&sort=最新')
const output = ref('')
const error = ref('')

function encode() {
  error.value = ''
  output.value = encodeURIComponent(input.value)
}

function decode() {
  try {
    error.value = ''
    output.value = decodeURIComponent(input.value)
  } catch {
    error.value = '输入不是有效的网址编码文本'
  }
}

function clear() {
  input.value = ''
  output.value = ''
  error.value = ''
}

encode()
</script>
