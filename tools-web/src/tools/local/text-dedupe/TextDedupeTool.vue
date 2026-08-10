<template>
  <ToolWorkspace eyebrow="文本工具" title="文本去重" description="按行去重，默认保留第一次出现的顺序。">
    <template #actions>
      <CopyButton :value="output" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输入</h2>
        <button class="secondary-button" type="button" @click="input = ''">清空</button>
      </div>
      <textarea v-model="input" class="code-field" spellcheck="false" placeholder="每行一条文本" />
      <div class="mt-4 flex flex-wrap gap-4">
        <label class="check-label"><input v-model="trimLine" type="checkbox" />忽略首尾空格</label>
        <label class="check-label"><input v-model="ignoreCase" type="checkbox" />忽略大小写</label>
        <label class="check-label"><input v-model="removeEmpty" type="checkbox" />移除空行</label>
      </div>
      <button class="primary-button mt-5 w-full" type="button" @click="dedupe">去重</button>
    </section>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输出</h2>
        <span class="text-sm text-muted">{{ stats }}</span>
      </div>
      <textarea v-model="output" class="code-field" spellcheck="false" readonly />
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const input = ref('苹果\n香蕉\n苹果\n香蕉\n橙子')
const output = ref('')
const stats = ref('')
const trimLine = ref(true)
const ignoreCase = ref(false)
const removeEmpty = ref(true)

function normalized(line: string) {
  const value = trimLine.value ? line.trim() : line
  return ignoreCase.value ? value.toLowerCase() : value
}

function displayLine(line: string) {
  return trimLine.value ? line.trim() : line
}

function dedupe() {
  const seen = new Set<string>()
  const lines = input.value.split(/\r\n|\r|\n/)
  const result: string[] = []
  lines.forEach((line) => {
    const key = normalized(line)
    if (removeEmpty.value && key === '') {
      return
    }
    if (!seen.has(key)) {
      seen.add(key)
      result.push(displayLine(line))
    }
  })
  output.value = result.join('\n')
  stats.value = `${lines.length} 行 → ${result.length} 行`
}

dedupe()
</script>
