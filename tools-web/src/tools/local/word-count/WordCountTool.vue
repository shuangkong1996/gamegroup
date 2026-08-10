<template>
  <ToolWorkspace eyebrow="文本工具" title="字数统计" description="统计字符、非空字符、词数、行数和字节数。">
    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">文本</h2>
        <button class="secondary-button" type="button" @click="text = ''">清空</button>
      </div>
      <textarea v-model="text" class="code-field" spellcheck="false" placeholder="输入要统计的文本" />
    </section>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">统计结果</h2>
      <div class="grid gap-4 sm:grid-cols-2">
        <MetricCard label="字符数" :value="stats.characters" />
        <MetricCard label="非空字符" :value="stats.nonWhitespace" />
        <MetricCard label="词数" :value="stats.words" />
        <MetricCard label="行数" :value="stats.lines" />
        <MetricCard label="字节数" :value="stats.bytes" />
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const text = ref('GameGroup 工具箱\n现代极简效率工具')

const stats = computed(() => {
  const value = text.value
  const trimmed = value.trim()
  return {
    characters: Array.from(value).length,
    nonWhitespace: Array.from(value.replace(/\s/g, '')).length,
    words: trimmed ? trimmed.split(/\s+/).length : 0,
    lines: value ? value.split(/\r\n|\r|\n/).length : 0,
    bytes: new TextEncoder().encode(value).length
  }
})
</script>
