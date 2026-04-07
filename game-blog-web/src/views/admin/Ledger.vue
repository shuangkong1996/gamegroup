<template>
  <div class="ledger-page">
    <section class="page-hero">
      <div>
        <h1 class="hero-title">记账台账</h1>
        <p class="hero-subtitle">管好现金流，才有底气做长期计划</p>
      </div>
      <div class="hero-balance">
        <span>本月结余</span>
        <strong :class="overview.balance >= 0 ? 'income' : 'expense'">{{ formatSignedMoney(overview.balance) }}</strong>
      </div>
    </section>

    <el-card class="toolbar-card glass-card" shadow="never">
      <div class="toolbar-row">
        <el-date-picker
          v-model="selectedMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月份"
          class="field-control"
          @change="handleMonthChange"
        />
        <el-select v-model="selectedAccountId" clearable placeholder="全部账户" class="field-control" @change="handleFilterChange">
          <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="selectedCategoryId" clearable placeholder="全部分类" class="field-control" @change="handleFilterChange">
          <el-option v-for="item in allCategories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <div class="action-group">
          <el-button class="action-btn" type="success" @click="openAccountDialog = true">新增账户</el-button>
          <el-button class="action-btn" @click="openBatchDialog = true">批量记账</el-button>
          <el-button class="action-btn" type="primary" @click="openRecordDrawer = true">记一笔</el-button>
        </div>
      </div>
    </el-card>

    <div class="stats-grid">
      <el-card class="glass-card" shadow="never">
        <div class="stat-title">本月收入</div>
        <div class="stat-value income">+{{ formatMoney(overview.totalIncome) }}</div>
      </el-card>
      <el-card class="glass-card" shadow="never">
        <div class="stat-title">本月支出</div>
        <div class="stat-value expense">-{{ formatMoney(overview.totalExpense) }}</div>
      </el-card>
      <el-card class="glass-card" shadow="never">
        <div class="stat-title">本月结余</div>
        <div class="stat-value" :class="overview.balance >= 0 ? 'income' : 'expense'">{{ formatSignedMoney(overview.balance) }}</div>
      </el-card>
    </div>

    <div class="content-grid">
      <el-card class="records-card glass-card" shadow="never">
        <template #header>
          <div class="card-header">账单记录</div>
        </template>

        <el-table v-if="!isMobile" :data="flatRows" v-loading="loadingRecords" style="width: 100%">
          <el-table-column prop="recordTime" label="时间" width="160" />
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="accountName" label="账户" width="120" />
          <el-table-column prop="remark" label="备注" min-width="180" />
          <el-table-column label="金额" width="120">
            <template #default="scope">
              <span :class="scope.row.type === 2 ? 'income' : 'expense'">
                {{ scope.row.type === 2 ? '+' : '-' }}{{ formatMoney(scope.row.amount) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="scope">
              <el-popconfirm title="确定删除这条记录吗？" @confirm="removeRecord(scope.row.id)">
                <template #reference>
                  <el-button type="danger" link>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="mobile-records" v-loading="loadingRecords">
          <div v-for="group in groupedRecords" :key="group.date" class="day-group">
            <div class="day-header">
              <span>{{ group.date }}</span>
              <span>
                收 {{ formatMoney(group.totalIncome) }} / 支 {{ formatMoney(group.totalExpense) }}
              </span>
            </div>
            <div v-for="record in group.records" :key="record.id" class="record-item">
              <div class="record-main">
                <div class="record-name">{{ record.categoryName }} · {{ record.accountName }}</div>
                <div class="record-remark">{{ record.remark || '-' }}</div>
              </div>
              <div class="record-side">
                <div :class="record.type === 2 ? 'income' : 'expense'">
                  {{ record.type === 2 ? '+' : '-' }}{{ formatMoney(record.amount) }}
                </div>
                <el-button type="danger" link @click="removeRecord(record.id)">删除</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="size"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </el-card>

      <el-card class="charts-card glass-card" v-loading="loadingStats" shadow="never">
        <template #header>
          <div class="card-header">统计</div>
        </template>
        <div class="chart-block">
          <div class="chart-title">分类占比（支出）</div>
          <div ref="expensePieRef" class="chart-canvas"></div>
          <el-empty v-if="!categoryStats.expense.length" description="暂无支出数据" :image-size="80" />
        </div>

        <div class="chart-block">
          <div class="chart-title">每日收支趋势（近7天）</div>
          <div ref="trendLineRef" class="chart-canvas"></div>
          <el-empty v-if="!trendStats.length" description="暂无趋势数据" :image-size="80" />
        </div>
      </el-card>
    </div>

    <el-drawer v-model="openRecordDrawer" :size="isMobile ? '100%' : '520px'" title="记一笔">
      <el-form :model="recordForm" label-width="90px">
        <el-form-item label="类型">
          <el-segmented v-model="recordForm.type" :options="recordTypeOptions" />
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="recordForm.accountId" placeholder="请选择账户">
            <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <div class="category-grid">
            <button
              v-for="item in currentTypeCategories"
              :key="item.id"
              class="category-btn"
              :class="recordForm.categoryId === item.id ? 'active' : ''"
              @click.prevent="recordForm.categoryId = item.id"
            >
              {{ item.name }}
            </button>
          </div>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="recordForm.amount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker v-model="recordForm.recordTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="recordForm.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openRecordDrawer = false">取消</el-button>
        <el-button type="primary" :loading="submittingRecord" @click="submitRecord">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="openAccountDialog" title="新增账户" width="420px">
      <el-form :model="accountForm" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="accountForm.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="accountForm.type" placeholder="如 Cash/Bank" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openAccountDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingAccount" @click="submitAccount">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="openBatchDialog" title="批量记账" width="560px">
      <el-form :model="batchForm" label-width="90px">
        <el-form-item label="类型">
          <el-segmented v-model="batchForm.type" :options="recordTypeOptions" />
        </el-form-item>
        <el-form-item label="账户">
          <el-select v-model="batchForm.accountId" placeholder="请选择账户">
            <el-option v-for="item in accounts" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="batchForm.categoryId" placeholder="请选择分类">
            <el-option v-for="item in batchTypeCategories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="batchForm.amount" :min="0.01" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月份">
          <el-date-picker v-model="batchForm.month" type="month" value-format="YYYY-MM" style="width: 100%" @change="resetBatchDates" />
        </el-form-item>
        <el-form-item label="日期">
          <div class="batch-date-panel">
            <div class="batch-tools">
              <el-button link type="primary" @click="selectAllWorkdays">全选工作日</el-button>
              <el-button link @click="clearBatchDates">清空</el-button>
            </div>
            <div class="batch-dates">
              <button
                v-for="date in batchWorkdays"
                :key="date"
                class="batch-date-btn"
                :class="batchForm.recordDates.includes(date) ? 'active' : ''"
                @click.prevent="toggleBatchDate(date)"
              >
                {{ date.slice(8) }}
              </button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" maxlength="255" placeholder="如：停车费" />
        </el-form-item>
      </el-form>
      <div class="batch-preview">
        将创建 <strong>{{ batchForm.recordDates.length }}</strong> 条记录，共
        <strong>{{ formatMoney(batchTotalAmount) }}</strong> 元
      </div>
      <template #footer>
        <el-button @click="openBatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingBatch" @click="submitBatchRecord">批量保存</el-button>
      </template>
    </el-dialog>

    <div v-if="isMobile" class="mobile-action-bar">
      <button class="fab" @click="openRecordDrawer = true">+ 记账</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { use, init } from 'echarts/core'
import type { EChartsType } from 'echarts/core'

use([CanvasRenderer, LineChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent])

interface LedgerAccount {
  id: number
  name: string
  balance: number
}

interface LedgerCategory {
  id: number
  name: string
  type: number
}

interface LedgerRecord {
  id: number
  amount: number
  type: number
  recordTime: string
  remark?: string
  categoryName: string
  accountName: string
}

interface LedgerDayGroup {
  date: string
  totalIncome: number
  totalExpense: number
  records: LedgerRecord[]
}

interface Overview {
  totalIncome: number
  totalExpense: number
  balance: number
}

interface TrendPoint {
  date: string
  income: number
  expense: number
}

interface CategoryStat {
  categoryId: number
  categoryName: string
  percentage: number
  totalAmount?: number
}

const loadingRecords = ref(false)
const loadingStats = ref(false)
const submittingRecord = ref(false)
const submittingAccount = ref(false)
const submittingBatch = ref(false)
const openRecordDrawer = ref(false)
const openAccountDialog = ref(false)
const openBatchDialog = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const MOBILE_BREAKPOINT = 1200
const selectedMonth = ref(dayjsMonth())
const selectedAccountId = ref<number | undefined>(undefined)
const selectedCategoryId = ref<number | undefined>(undefined)
const isMobile = ref(window.innerWidth < MOBILE_BREAKPOINT)
const expensePieRef = ref<HTMLDivElement | null>(null)
const trendLineRef = ref<HTMLDivElement | null>(null)

const accounts = ref<LedgerAccount[]>([])
const expenseCategories = ref<LedgerCategory[]>([])
const incomeCategories = ref<LedgerCategory[]>([])
const groupedRecords = ref<LedgerDayGroup[]>([])
const trendStats = ref<TrendPoint[]>([])
const overview = reactive<Overview>({ totalIncome: 0, totalExpense: 0, balance: 0 })
const categoryStats = reactive<{ expense: CategoryStat[]; income: CategoryStat[] }>({ expense: [], income: [] })
let expensePieChart: EChartsType | null = null
let trendLineChart: EChartsType | null = null

const recordForm = reactive({
  type: 1,
  accountId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  amount: 0.01,
  recordTime: nowDateTime(),
  remark: ''
})

const accountForm = reactive({
  name: '',
  type: 'General'
})

const batchForm = reactive({
  type: 1,
  accountId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  amount: 0.01,
  month: dayjsMonth(),
  recordDates: [] as string[],
  remark: ''
})

const recordTypeOptions = [
  { label: '支出', value: 1 },
  { label: '收入', value: 2 }
]

const allCategories = computed(() => [...expenseCategories.value, ...incomeCategories.value])

const currentTypeCategories = computed(() => {
  return recordForm.type === 2 ? incomeCategories.value : expenseCategories.value
})

const batchTypeCategories = computed(() => {
  return batchForm.type === 2 ? incomeCategories.value : expenseCategories.value
})

const batchWorkdays = computed(() => {
  const [yearStr, monthStr] = batchForm.month.split('-')
  const year = Number(yearStr)
  const month = Number(monthStr)
  if (!year || !month) {
    return []
  }
  const days: string[] = []
  const end = new Date(year, month, 0).getDate()
  for (let day = 1; day <= end; day++) {
    const date = new Date(year, month - 1, day)
    const week = date.getDay()
    if (week !== 0 && week !== 6) {
      days.push(`${yearStr}-${monthStr}-${`${day}`.padStart(2, '0')}`)
    }
  }
  return days
})

const batchTotalAmount = computed(() => Number(batchForm.amount || 0) * batchForm.recordDates.length)

const flatRows = computed(() => {
  const rows: LedgerRecord[] = []
  groupedRecords.value.forEach(group => {
    group.records.forEach(record => rows.push(record))
  })
  return rows
})

const latestTrend = computed(() => trendStats.value.slice(-7))

const handleResize = () => {
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
  expensePieChart?.resize()
  trendLineChart?.resize()
}

const formatMoney = (value: number | undefined) => (Number(value || 0)).toFixed(2)

const formatSignedMoney = (value: number | undefined) => {
  const num = Number(value || 0)
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}`
}

const loadBaseData = async () => {
  const [accountRes, categoryRes] = await Promise.all<any>([
    request.get('/ledger/accounts'),
    request.get('/ledger/categories')
  ])
  accounts.value = accountRes || []
  expenseCategories.value = categoryRes?.expense || []
  incomeCategories.value = categoryRes?.income || []
  if (!recordForm.accountId && accounts.value.length) {
    recordForm.accountId = accounts.value[0].id
  }
  resetCategoryByType()
}

const loadRecords = async () => {
  loadingRecords.value = true
  try {
    const res: any = await request.get('/ledger/records', {
      params: {
        month: selectedMonth.value,
        accountId: selectedAccountId.value,
        categoryId: selectedCategoryId.value,
        page: page.value,
        size: size.value
      }
    })
    groupedRecords.value = res?.groups || []
    total.value = res?.total || 0
  } finally {
    loadingRecords.value = false
  }
}

const loadStats = async () => {
  loadingStats.value = true
  try {
    const [overviewRes, trendRes, categoryRes] = await Promise.all<any>([
      request.get('/ledger/stats/overview', { params: { month: selectedMonth.value } }),
      request.get('/ledger/stats/trend', { params: { month: selectedMonth.value } }),
      request.get('/ledger/stats/category', { params: { month: selectedMonth.value } })
    ])
    overview.totalIncome = Number(overviewRes?.totalIncome || 0)
    overview.totalExpense = Number(overviewRes?.totalExpense || 0)
    overview.balance = Number(overviewRes?.balance || 0)
    trendStats.value = trendRes || []
    categoryStats.expense = categoryRes?.expense || []
    categoryStats.income = categoryRes?.income || []
    await nextTick()
    renderCharts()
  } finally {
    loadingStats.value = false
  }
}

const initCharts = () => {
  if (expensePieRef.value && !expensePieChart) {
    expensePieChart = init(expensePieRef.value)
  }
  if (trendLineRef.value && !trendLineChart) {
    trendLineChart = init(trendLineRef.value)
  }
}

const renderCharts = () => {
  initCharts()
  if (expensePieChart) {
    expensePieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, left: 'center' },
      series: [
        {
          type: 'pie',
          radius: ['42%', '72%'],
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { formatter: '{b}: {d}%' },
          data: categoryStats.expense.map(item => ({
            name: item.categoryName,
            value: Number(item.totalAmount || 0)
          }))
        }
      ]
    })
  }

  if (trendLineChart) {
    trendLineChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 32, right: 16, top: 24, bottom: 28 },
      xAxis: {
        type: 'category',
        data: latestTrend.value.map(item => item.date.slice(5))
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '收入',
          type: 'line',
          smooth: true,
          data: latestTrend.value.map(item => Number(item.income || 0)),
          areaStyle: { opacity: 0.12 },
          lineStyle: { width: 2, color: '#16a34a' }
        },
        {
          name: '支出',
          type: 'line',
          smooth: true,
          data: latestTrend.value.map(item => Number(item.expense || 0)),
          areaStyle: { opacity: 0.12 },
          lineStyle: { width: 2, color: '#dc2626' }
        }
      ]
    })
  }
}

const refreshAll = async () => {
  await Promise.all([loadRecords(), loadStats(), loadBaseData()])
}

const handleMonthChange = async () => {
  page.value = 1
  await Promise.all([loadRecords(), loadStats()])
}

const handleFilterChange = async () => {
  page.value = 1
  await loadRecords()
}

const handlePageChange = async (nextPage: number) => {
  page.value = nextPage
  await loadRecords()
}

const resetRecordForm = () => {
  recordForm.type = 1
  recordForm.accountId = accounts.value.length ? accounts.value[0].id : undefined
  recordForm.amount = 0.01
  recordForm.recordTime = nowDateTime()
  recordForm.remark = ''
  resetCategoryByType()
}

const resetCategoryByType = () => {
  const list = currentTypeCategories.value
  recordForm.categoryId = list.length ? list[0].id : undefined
}

const resetBatchCategoryByType = () => {
  const list = batchTypeCategories.value
  batchForm.categoryId = list.length ? list[0].id : undefined
}

const resetBatchDates = () => {
  batchForm.recordDates = [...batchWorkdays.value]
}

const toggleBatchDate = (date: string) => {
  const idx = batchForm.recordDates.indexOf(date)
  if (idx >= 0) {
    batchForm.recordDates.splice(idx, 1)
  } else {
    batchForm.recordDates.push(date)
    batchForm.recordDates.sort()
  }
}

const selectAllWorkdays = () => {
  batchForm.recordDates = [...batchWorkdays.value]
}

const clearBatchDates = () => {
  batchForm.recordDates = []
}

const submitRecord = async () => {
  if (!recordForm.accountId || !recordForm.categoryId) {
    ElMessage.warning('请选择账户和分类')
    return
  }
  submittingRecord.value = true
  try {
    await request.post('/ledger/records', { ...recordForm })
    ElMessage.success('记账成功')
    openRecordDrawer.value = false
    resetRecordForm()
    await Promise.all([loadRecords(), loadStats(), loadBaseData()])
  } finally {
    submittingRecord.value = false
  }
}

const removeRecord = async (id: number) => {
  await request.delete(`/ledger/records/${id}`)
  ElMessage.success('删除成功')
  await Promise.all([loadRecords(), loadStats(), loadBaseData()])
}

const submitAccount = async () => {
  if (!accountForm.name.trim()) {
    ElMessage.warning('请输入账户名称')
    return
  }
  submittingAccount.value = true
  try {
    await request.post('/ledger/accounts', {
      name: accountForm.name,
      type: accountForm.type || 'General'
    })
    ElMessage.success('账户创建成功')
    openAccountDialog.value = false
    accountForm.name = ''
    accountForm.type = 'General'
    await loadBaseData()
  } finally {
    submittingAccount.value = false
  }
}

const submitBatchRecord = async () => {
  if (!batchForm.accountId || !batchForm.categoryId) {
    ElMessage.warning('请选择账户和分类')
    return
  }
  if (!batchForm.recordDates.length) {
    ElMessage.warning('请至少选择一个日期')
    return
  }
  submittingBatch.value = true
  try {
    const res: any = await request.post('/ledger/records/batch', {
      accountId: batchForm.accountId,
      categoryId: batchForm.categoryId,
      amount: batchForm.amount,
      type: batchForm.type,
      recordDates: batchForm.recordDates,
      remark: batchForm.remark
    })
    ElMessage.success(`批量创建成功，共 ${res?.createdCount || 0} 条`)
    openBatchDialog.value = false
    await Promise.all([loadRecords(), loadStats(), loadBaseData()])
  } finally {
    submittingBatch.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await refreshAll()
  await nextTick()
  renderCharts()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  expensePieChart?.dispose()
  trendLineChart?.dispose()
  expensePieChart = null
  trendLineChart = null
})

watch(latestTrend, () => {
  renderCharts()
})

watch(
  () => categoryStats.expense,
  () => {
    renderCharts()
  },
  { deep: true }
)

watch(
  () => recordForm.type,
  () => {
    resetCategoryByType()
  }
)

watch(
  () => batchForm.type,
  () => {
    resetBatchCategoryByType()
  }
)

watch(
  () => openBatchDialog.value,
  (visible) => {
    if (!visible) {
      return
    }
    if (!batchForm.accountId && accounts.value.length) {
      batchForm.accountId = accounts.value[0].id
    }
    if (!batchForm.month) {
      batchForm.month = dayjsMonth()
    }
    resetBatchCategoryByType()
    resetBatchDates()
  }
)

function dayjsMonth() {
  const now = new Date()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  return `${now.getFullYear()}-${month}`
}

function nowDateTime() {
  const now = new Date()
  const y = now.getFullYear()
  const m = `${now.getMonth() + 1}`.padStart(2, '0')
  const d = `${now.getDate()}`.padStart(2, '0')
  const h = `${now.getHours()}`.padStart(2, '0')
  const i = `${now.getMinutes()}`.padStart(2, '0')
  const s = `${now.getSeconds()}`.padStart(2, '0')
  return `${y}-${m}-${d}T${h}:${i}:${s}`
}
</script>

<style scoped>
.ledger-page {
  --bg-start: #f4f7ff;
  --bg-end: #eefaf5;
  --card-border: var(--gg-border);
  --text-strong: var(--gg-text);
  --text-muted: var(--gg-text-soft);
  --brand: var(--gg-primary);
  --brand-2: var(--gg-primary-2);
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 8px;
  min-height: calc(100vh - 64px);
  background: radial-gradient(circle at 0% 0%, #dbeafe 0%, transparent 40%),
    radial-gradient(circle at 100% 0%, #d1fae5 0%, transparent 35%),
    linear-gradient(160deg, var(--bg-start), var(--bg-end));
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 20px;
  padding: 20px 24px;
  color: var(--text-strong);
  background: linear-gradient(140deg, rgba(29, 78, 216, 0.92), rgba(14, 165, 233, 0.88));
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.18);
  animation: slide-in 380ms ease-out;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  letter-spacing: 0.4px;
  color: #eff6ff;
}

.hero-subtitle {
  margin: 6px 0 0;
  color: rgba(239, 246, 255, 0.88);
  font-size: 14px;
}

.hero-balance {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  color: #e2e8f0;
}

.hero-balance strong {
  font-size: 26px;
}

.toolbar-row {
  display: grid;
  grid-template-columns: 1.15fr 1fr 1fr auto;
  gap: 12px;
  align-items: center;
}

.field-control {
  width: 100%;
}

.action-group {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.action-btn {
  min-width: 96px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.glass-card {
  border: 1px solid var(--card-border);
  backdrop-filter: blur(6px);
  border-radius: var(--gg-radius-md);
}

.stat-title {
  color: var(--text-muted);
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
}

.income {
  color: #16a34a;
}

.expense {
  color: #dc2626;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 14px;
}

.card-header {
  font-weight: 600;
  color: var(--text-strong);
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.charts-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chart-block {
  margin-bottom: 20px;
}

.chart-title {
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--text-strong);
}

.chart-canvas {
  width: 100%;
  height: 260px;
}

.mobile-records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.day-group {
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  overflow: hidden;
}

.day-header {
  padding: 10px 12px;
  background: linear-gradient(180deg, #f8fbff, #f1f7ff);
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #334155;
}

.record-item {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  border-top: 1px solid #f1f5f9;
}

.record-name {
  font-weight: 600;
}

.record-remark {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.record-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.category-grid {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.batch-date-panel {
  width: 100%;
}

.batch-tools {
  margin-bottom: 8px;
  display: flex;
  gap: 10px;
}

.batch-dates {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 6px;
}

.batch-date-btn {
  border: 1px solid var(--gg-border);
  background: #fff;
  border-radius: 8px;
  height: 34px;
  cursor: pointer;
}

.batch-date-btn.active {
  border-color: var(--brand);
  color: var(--brand);
  background: #eff6ff;
}

.batch-preview {
  margin-top: 8px;
  color: var(--gg-text-soft);
  font-size: 14px;
}

.batch-preview strong {
  color: var(--gg-text);
}

.category-btn {
  border: 1px solid #dbeafe;
  background: linear-gradient(180deg, #fcfdff, #f4f8ff);
  border-radius: 10px;
  padding: 8px;
  cursor: pointer;
  transition: all 180ms ease;
}

.category-btn.active {
  border-color: var(--brand);
  color: var(--brand);
  background: #eff6ff;
  box-shadow: 0 6px 12px rgba(37, 99, 235, 0.16);
}

.fab {
  border: none;
  border-radius: 999px;
  padding: 12px 22px;
  background: linear-gradient(135deg, var(--brand), var(--brand-2));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.3);
}

.fab.secondary {
  background: linear-gradient(135deg, #334155, #475569);
  box-shadow: 0 10px 20px rgba(51, 65, 85, 0.24);
}

.mobile-action-bar {
  position: fixed;
  right: 12px;
  bottom: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  z-index: 9;
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .page-hero {
    padding: 16px 18px;
  }

  .hero-title {
    font-size: 24px;
  }

  .toolbar-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .action-group {
    grid-column: span 2;
    justify-content: flex-start;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .ledger-page {
    padding: 4px;
    gap: 12px;
    padding-bottom: 88px;
  }

  .page-hero {
    border-radius: 14px;
    padding: 14px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .hero-title {
    font-size: 20px;
  }

  .hero-subtitle {
    font-size: 12px;
  }

  .hero-balance {
    width: 100%;
  }

  .toolbar-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .action-group {
    grid-column: auto;
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .stat-value {
    font-size: 24px;
  }

  .content-grid {
    gap: 10px;
  }

  .chart-canvas {
    height: 220px;
  }

  .batch-dates {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .record-item {
    padding: 10px;
  }

  .record-name {
    font-size: 13px;
  }

  .pagination-wrap {
    justify-content: center;
  }
}

@media (max-width: 460px) {
  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mobile-action-bar {
    right: 8px;
  }

  .batch-dates {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
