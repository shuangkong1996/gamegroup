<template>
  <ToolWorkspace eyebrow="生活工具" title="数字转金额大写" description="把阿拉伯数字金额转换为人民币大写格式。">
    <template #actions>
      <CopyButton :value="output" />
    </template>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">金额输入</h2>
      <input v-model="amount" class="field" inputmode="decimal" placeholder="例如 123456.78" />
      <p v-if="error" class="mt-3 text-sm font-medium text-rose-500">{{ error }}</p>
      <button class="primary-button mt-5 w-full" type="button" @click="convert">转换</button>
    </section>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">人民币大写</h2>
      <textarea v-model="output" class="code-field" spellcheck="false" readonly />
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
const units = ['', '拾', '佰', '仟']
const sections = ['', '万', '亿', '兆']

const amount = ref('123456.78')
const output = ref('')
const error = ref('')

function sectionToChinese(section: number) {
  let result = ''
  let zero = true
  for (let unitIndex = 0; unitIndex < 4; unitIndex += 1) {
    const digit = section % 10
    if (digit === 0) {
      if (!zero) {
        zero = true
        result = digits[0] + result
      }
    } else {
      zero = false
      result = digits[digit] + units[unitIndex] + result
    }
    section = Math.floor(section / 10)
  }
  return result.replace(/零+$/g, '')
}

function integerToChinese(value: number) {
  if (value === 0) {
    return digits[0]
  }
  let result = ''
  let sectionIndex = 0
  let needZero = false
  while (value > 0) {
    const section = value % 10000
    if (section === 0) {
      needZero = true
    } else {
      const sectionText = sectionToChinese(section) + sections[sectionIndex]
      result = (needZero ? digits[0] : '') + sectionText + result
      needZero = section < 1000
    }
    value = Math.floor(value / 10000)
    sectionIndex += 1
  }
  return result.replace(/零+/g, '零').replace(/零$/g, '')
}

function convert() {
  const raw = amount.value.trim().replace(/,/g, '')
  if (!/^\d+(\.\d{1,2})?$/.test(raw)) {
    error.value = '请输入非负金额，最多两位小数'
    output.value = ''
    return
  }
  error.value = ''
  const [integerPart, decimalPart = ''] = raw.split('.')
  const integer = Number(integerPart)
  if (!Number.isSafeInteger(integer)) {
    error.value = '金额过大'
    output.value = ''
    return
  }
  const jiao = Number(decimalPart[0] || 0)
  const fen = Number(decimalPart[1] || 0)
  let decimalText = ''
  if (jiao > 0) {
    decimalText += `${digits[jiao]}角`
  }
  if (fen > 0) {
    decimalText += `${digits[fen]}分`
  }
  output.value = `${integerToChinese(integer)}元${decimalText || '整'}`
}

convert()
</script>
