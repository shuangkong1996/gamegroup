<template>
  <ToolWorkspace eyebrow="开发工具" title="唯一标识生成" description="批量生成符合浏览器标准的随机唯一标识符。">
    <template #actions>
      <CopyButton :value="uuids.join('\n')" label="复制全部" />
    </template>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">生成设置</h2>
      <label class="block">
        <span class="mb-2 block text-sm font-medium text-muted">数量</span>
        <input v-model.number="count" class="field" type="number" min="1" max="20" />
      </label>
      <button class="primary-button mt-4 w-full" type="button" @click="generate">生成</button>
    </section>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">生成结果</h2>
      <div class="space-y-3">
        <div
          v-for="uuid in uuids"
          :key="uuid"
          class="flex items-center justify-between gap-3 rounded-xl border border-line/70 bg-white/70 px-4 py-3 font-mono text-sm dark:bg-zinc-950/50"
        >
          <span class="min-w-0 truncate">{{ uuid }}</span>
          <CopyButton :value="uuid" />
        </div>
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const count = ref(5)
const uuids = ref<string[]>([])

function generate() {
  const safeCount = Math.min(Math.max(Number(count.value) || 1, 1), 20)
  count.value = safeCount
  uuids.value = Array.from({ length: safeCount }, () => crypto.randomUUID())
}

generate()
</script>
