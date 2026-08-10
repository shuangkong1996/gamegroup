<template>
  <ToolWorkspace eyebrow="生活工具" title="房贷计算器" description="接入 GameGroup 后端，估算月供、总还款和总利息。">
    <section class="panel p-5">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">贷款参数</h2>
        <button class="secondary-button" type="button" @click="loadDefaults">默认利率</button>
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">贷款年限</span>
          <input v-model.number="form.years" class="field" type="number" min="1" max="40" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">还款方式</span>
          <select v-model="form.repaymentMethod" class="field">
            <option value="EQUAL_INSTALLMENT">等额本息</option>
            <option value="EQUAL_PRINCIPAL">等额本金</option>
          </select>
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">商贷本金（元）</span>
          <input v-model.number="form.commercialPrincipal" class="field" type="number" min="0" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">公积金本金（元）</span>
          <input v-model.number="form.providentFundPrincipal" class="field" type="number" min="0" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">商贷年利率（%）</span>
          <input v-model.number="form.commercialAnnualRatePercent" class="field" type="number" min="0" step="0.01" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">公积金年利率（%）</span>
          <input v-model.number="form.providentFundAnnualRatePercent" class="field" type="number" min="0" step="0.01" />
        </label>
      </div>

      <p v-if="defaults?.referencePeriod" class="mt-4 text-xs leading-5 text-muted">
        利率参考期：{{ defaults.referencePeriod }}
      </p>
      <p v-if="error" class="mt-4 text-sm font-medium text-rose-500">{{ error }}</p>
      <button class="primary-button mt-5 w-full" type="button" :disabled="loading" @click="calculate">
        {{ loading ? '计算中…' : '开始计算' }}
      </button>
    </section>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">计算结果</h2>
      <div v-if="result" class="space-y-6">
        <div class="grid gap-4 sm:grid-cols-2">
          <MetricCard label="首月月供" :value="money(result.combined.firstMonthPayment)" />
          <MetricCard label="末月月供" :value="money(result.combined.lastMonthPayment)" />
          <MetricCard label="总还款" :value="money(result.combined.totalPayment)" />
          <MetricCard label="总利息" :value="money(result.combined.totalInterest)" />
        </div>

        <div class="overflow-hidden rounded-2xl border border-line/70">
          <table class="w-full text-left text-sm">
            <thead class="bg-slate-100/80 text-xs font-semibold text-muted dark:bg-zinc-800/70">
              <tr>
                <th class="px-4 py-3">期数</th>
                <th class="px-4 py-3">月供</th>
                <th class="px-4 py-3">本金</th>
                <th class="px-4 py-3">利息</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-line/70">
              <tr
                v-for="row in previewRows"
                :key="row.monthIndex"
                class="transition-colors duration-200 hover:bg-slate-50/80 dark:hover:bg-zinc-800/40"
              >
                <td class="px-4 py-3">{{ row.monthIndex }}</td>
                <td class="px-4 py-3">{{ money(row.monthlyPayment) }}</td>
                <td class="px-4 py-3">{{ money(row.principalPortion) }}</td>
                <td class="px-4 py-3">{{ money(row.interestPortion) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="text-xs text-muted">仅预览前 12 期还款明细</p>
      </div>
      <div v-else class="empty-state">填写参数后点击开始计算</div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'
import {
  calculateMortgage,
  getMortgageDefaults,
  type MortgageCalculateResponse,
  type MortgageDefaultsResponse,
  type MortgageRepaymentMethod
} from '@/shared/api/mortgage'

const loading = ref(false)
const error = ref('')
const defaults = ref<MortgageDefaultsResponse>()
const result = ref<MortgageCalculateResponse>()

const form = reactive({
  years: 30,
  repaymentMethod: 'EQUAL_INSTALLMENT' as MortgageRepaymentMethod,
  commercialPrincipal: 1000000,
  providentFundPrincipal: 0,
  commercialAnnualRatePercent: 3.05,
  providentFundAnnualRatePercent: 2.6
})

const previewRows = computed(() => result.value?.combinedSchedule.slice(0, 12) ?? [])

function money(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2
  }).format(value || 0)
}

async function loadDefaults() {
  try {
    defaults.value = await getMortgageDefaults()
    form.commercialAnnualRatePercent = Number(defaults.value.commercialAnnualRatePercent)
    form.providentFundAnnualRatePercent = Number(defaults.value.providentFundFirstHomeFiveYearPlusPercent)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '默认利率加载失败'
  }
}

async function calculate() {
  loading.value = true
  error.value = ''
  try {
    result.value = await calculateMortgage({
      years: form.years,
      repaymentMethod: form.repaymentMethod,
      commercialPrincipal: Number(form.commercialPrincipal || 0),
      providentFundPrincipal: Number(form.providentFundPrincipal || 0),
      commercialAnnualRatePercent: Number(form.commercialAnnualRatePercent || 0),
      providentFundAnnualRatePercent: Number(form.providentFundAnnualRatePercent || 0),
      providentFundTier: 'FIRST_HOME'
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '计算失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDefaults()
})
</script>
