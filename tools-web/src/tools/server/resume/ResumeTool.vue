<template>
  <ToolWorkspace
    eyebrow="生活工具"
    title="简历生成"
    description="填写信息、拖拽排序、切换模板，支持浏览器打印与服务端 PDF 导出。"
  >
    <template #actions>
      <button class="secondary-button" type="button" :disabled="saving" @click="saveResume">
        {{ saving ? '保存中…' : currentId ? '保存' : '新建保存' }}
      </button>
      <button class="secondary-button" type="button" :disabled="exportingPdf" @click="downloadServerPdf">
        {{ exportingPdf ? '生成中…' : '服务端 PDF' }}
      </button>
      <button class="secondary-button" type="button" @click="printResume">浏览器打印</button>
      <button class="secondary-button" type="button" @click="logout">退出登录</button>
    </template>

    <section class="panel space-y-5 p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">编辑</h2>
          <div class="flex flex-wrap gap-2">
            <select
              v-if="resumes.length"
              v-model="selectedId"
              class="field !w-auto min-w-[10rem] py-2"
              @change="onSelectResume"
            >
              <option :value="0">新建简历</option>
              <option v-for="item in resumes" :key="item.id" :value="item.id">
                {{ item.title }}
              </option>
            </select>
            <button class="secondary-button !py-2" type="button" @click="fillSample">填入示例</button>
            <button
              v-if="currentId"
              class="secondary-button !py-2 text-rose-600"
              type="button"
              :disabled="deleting"
              @click="removeResume"
            >
              删除
            </button>
          </div>
        </div>

        <p v-if="statusMessage" class="text-sm text-emerald-600 dark:text-emerald-400">{{ statusMessage }}</p>
        <p v-if="error" class="text-sm font-medium text-rose-500">{{ error }}</p>

        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">简历标题</span>
          <input v-model="title" class="field" placeholder="例如：张三-前端简历" @change="persistDraft" />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">模板</span>
          <select v-model="templateId" class="field" @change="persistDraft">
            <option value="classic">经典单栏</option>
            <option value="sidebar">侧栏双栏</option>
          </select>
        </label>

        <div class="space-y-2">
          <div class="flex items-center justify-between gap-2">
            <h3 class="text-sm font-semibold text-slate-800 dark:text-zinc-200">区块顺序</h3>
            <span class="text-xs text-muted">拖拽调整预览中的章节顺序</span>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="(section, index) in content.sectionOrder"
              :key="section"
              class="inline-flex items-center gap-1 rounded-lg border border-line/80 bg-slate-50 px-2.5 py-1.5 text-xs font-medium text-slate-700 dark:bg-zinc-800 dark:text-zinc-200"
              type="button"
              draggable="true"
              @dragstart="onSectionDragStart(index)"
              @dragover.prevent
              @drop.prevent="onSectionDrop(index)"
            >
              <GripVertical class="h-3.5 w-3.5 text-muted" />
              {{ SECTION_LABELS[section] }}
            </button>
          </div>
        </div>

        <div class="space-y-3">
          <h3 class="text-sm font-semibold text-slate-800 dark:text-zinc-200">基本信息</h3>
          <div class="grid gap-3 sm:grid-cols-2">
            <label class="block">
              <span class="mb-1 block text-xs text-muted">姓名</span>
              <input v-model="content.basics.name" class="field" @change="persistDraft" />
            </label>
            <label class="block">
              <span class="mb-1 block text-xs text-muted">职位</span>
              <input v-model="content.basics.title" class="field" @change="persistDraft" />
            </label>
            <label class="block">
              <span class="mb-1 block text-xs text-muted">邮箱</span>
              <input v-model="content.basics.email" class="field" @change="persistDraft" />
            </label>
            <label class="block">
              <span class="mb-1 block text-xs text-muted">手机</span>
              <input v-model="content.basics.phone" class="field" @change="persistDraft" />
            </label>
            <label class="block">
              <span class="mb-1 block text-xs text-muted">城市</span>
              <input v-model="content.basics.location" class="field" @change="persistDraft" />
            </label>
            <label class="block">
              <span class="mb-1 block text-xs text-muted">主页 / GitHub</span>
              <input v-model="content.basics.website" class="field" @change="persistDraft" />
            </label>
          </div>
        </div>

        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">个人简介</span>
          <textarea v-model="content.summary" class="field min-h-[96px] resize-y" @change="persistDraft" />
        </label>

        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-800 dark:text-zinc-200">工作经历</h3>
            <button class="secondary-button !py-1.5 !text-xs" type="button" @click="addExperience">添加</button>
          </div>
          <SortableBlocks :items="content.experience" @reorder="onExperienceReorder">
            <template #default="{ item }">
              <div class="grid gap-3 sm:grid-cols-2">
                <input v-model="item.company" class="field" placeholder="公司" @change="persistDraft" />
                <input v-model="item.role" class="field" placeholder="职位" @change="persistDraft" />
                <input v-model="item.start" class="field" placeholder="开始时间" @change="persistDraft" />
                <input v-model="item.end" class="field" placeholder="结束时间" @change="persistDraft" />
              </div>
              <textarea
                class="field mt-3 min-h-[88px] resize-y"
                :value="item.bullets.join('\n')"
                placeholder="每行一条职责/成果"
                @input="onBulletsInput(item, $event)"
              />
            </template>
          </SortableBlocks>
        </div>

        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-800 dark:text-zinc-200">项目经历</h3>
            <button class="secondary-button !py-1.5 !text-xs" type="button" @click="addProject">添加</button>
          </div>
          <SortableBlocks :items="content.projects" @reorder="onProjectReorder">
            <template #default="{ item }">
              <div class="grid gap-3 sm:grid-cols-2">
                <input v-model="item.name" class="field" placeholder="项目名称" @change="persistDraft" />
                <input v-model="item.role" class="field" placeholder="角色" @change="persistDraft" />
                <input v-model="item.link" class="field sm:col-span-2" placeholder="链接（可选）" @change="persistDraft" />
              </div>
              <textarea
                class="field mt-3 min-h-[88px] resize-y"
                :value="item.bullets.join('\n')"
                placeholder="每行一条描述"
                @input="onBulletsInput(item, $event)"
              />
            </template>
          </SortableBlocks>
        </div>

        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-800 dark:text-zinc-200">教育经历</h3>
            <button class="secondary-button !py-1.5 !text-xs" type="button" @click="addEducation">添加</button>
          </div>
          <SortableBlocks :items="content.education" @reorder="onEducationReorder">
            <template #default="{ item }">
              <div class="grid gap-3 sm:grid-cols-2">
                <input v-model="item.school" class="field" placeholder="学校" @change="persistDraft" />
                <input v-model="item.degree" class="field" placeholder="学历 / 专业" @change="persistDraft" />
                <input v-model="item.start" class="field" placeholder="开始时间" @change="persistDraft" />
                <input v-model="item.end" class="field" placeholder="结束时间" @change="persistDraft" />
              </div>
              <textarea
                v-model="item.description"
                class="field mt-3 min-h-[72px] resize-y"
                placeholder="补充说明（可选）"
                @change="persistDraft"
              />
            </template>
          </SortableBlocks>
        </div>

        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">技能（逗号或换行分隔）</span>
          <textarea
            :value="skillsText"
            class="field min-h-[72px] resize-y"
            placeholder="TypeScript, Vue, Java"
            @input="onSkillsInput"
          />
        </label>
      </section>

      <section class="panel space-y-4 p-5">
        <div class="flex items-center justify-between gap-3">
          <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">预览</h2>
          <div class="flex flex-wrap gap-2">
            <button class="secondary-button !py-2" type="button" :disabled="exportingPdf" @click="downloadServerPdf">
              {{ exportingPdf ? '生成中…' : '服务端 PDF' }}
            </button>
            <button class="secondary-button !py-2" type="button" @click="printResume">打印</button>
          </div>
        </div>
        <div class="resume-preview-frame overflow-auto rounded-xl border border-line/70 bg-slate-100 p-4 dark:bg-zinc-950">
          <div
            id="resume-print-root"
            class="resume-sheet mx-auto bg-white shadow-sm"
            :class="templateId === 'sidebar' ? 'resume-sheet--flush' : ''"
          >
            <ClassicTemplate v-if="templateId === 'classic'" :content="content" />
            <SidebarTemplate v-else :content="content" />
          </div>
        </div>
        <p class="text-xs text-muted">
          服务端 PDF 由 Java（openhtmltopdf）生成，部署到 Linux 无需安装浏览器；也可用浏览器打印作兜底。
        </p>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { GripVertical } from '@lucide/vue'
import { useRouter } from 'vue-router'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'
import { clearSession } from '@/shared/auth/session'
import {
  createResume,
  deleteResume,
  exportResumePdf,
  listResumes,
  updateResume
} from '@/shared/api/resume'
import SortableBlocks from './SortableBlocks.vue'
import ClassicTemplate from './templates/ClassicTemplate.vue'
import SidebarTemplate from './templates/SidebarTemplate.vue'
import {
  createEmptyResumeContent,
  createSampleResumeContent,
  normalizeSectionOrder,
  SECTION_LABELS,
  type ResumeContent,
  type ResumeDTO,
  type ResumeEducationItem,
  type ResumeExperienceItem,
  type ResumeProjectItem,
  type ResumeSectionKey,
  type ResumeTemplateId
} from './types'

const DRAFT_KEY = 'tools-web-resume-draft'

const router = useRouter()

const resumes = ref<ResumeDTO[]>([])
const selectedId = ref(0)
const currentId = ref<number | null>(null)
const title = ref('我的简历')
const templateId = ref<ResumeTemplateId>('classic')
const content = reactive<ResumeContent>(createEmptyResumeContent())

const saving = ref(false)
const deleting = ref(false)
const exportingPdf = ref(false)
const error = ref('')
const statusMessage = ref('')
const skillsText = ref('')
const sectionDragIndex = ref<number | null>(null)

onMounted(async () => {
  restoreDraft()
  await refreshList()
})

function logout() {
  clearSession()
  router.replace({ name: 'resume-login', query: { redirect: '/tools/resume' } })
}

function handleSessionExpired() {
  clearSession()
  router.replace({ name: 'resume-login', query: { redirect: '/tools/resume' } })
}

function isSessionExpired(error: unknown) {
  const message = error instanceof Error ? error.message : String(error)
  return message.includes('未登录') || message.includes('token已失效') || message.includes('401')
}

function setRequestError(requestError: unknown, fallback: string) {
  if (isSessionExpired(requestError)) {
    handleSessionExpired()
    return
  }
  error.value = requestError instanceof Error ? requestError.message : fallback
}

async function refreshList() {
  error.value = ''
  try {
    resumes.value = await listResumes()
    if (!currentId.value && resumes.value.length) {
      await loadResume(resumes.value[0])
    }
  } catch (err) {
    setRequestError(err, '加载简历列表失败')
  }
}

async function onSelectResume() {
  if (!selectedId.value) {
    resetEditor()
    return
  }
  const found = resumes.value.find((item) => item.id === selectedId.value)
  if (found) {
    await loadResume(found)
  }
}

async function loadResume(item: ResumeDTO) {
  currentId.value = item.id
  selectedId.value = item.id
  title.value = item.title
  templateId.value = item.templateId === 'sidebar' ? 'sidebar' : 'classic'
  Object.assign(content, normalizeContent(item.content))
  skillsText.value = content.skills.join(', ')
  persistDraft()
}

function resetEditor() {
  currentId.value = null
  selectedId.value = 0
  title.value = '我的简历'
  templateId.value = 'classic'
  Object.assign(content, createEmptyResumeContent())
  skillsText.value = ''
  persistDraft()
}

function fillSample() {
  Object.assign(content, createSampleResumeContent())
  skillsText.value = content.skills.join(', ')
  if (!title.value || title.value === '我的简历') {
    title.value = `${content.basics.name || '我的'}-简历`
  }
  persistDraft()
}

function addExperience() {
  content.experience.push({ company: '', role: '', start: '', end: '', bullets: [''] })
  persistDraft()
}

function addProject() {
  content.projects.push({ name: '', role: '', link: '', bullets: [''] })
  persistDraft()
}

function addEducation() {
  content.education.push({ school: '', degree: '', start: '', end: '', description: '' })
  persistDraft()
}

function onExperienceReorder(next: ResumeExperienceItem[]) {
  content.experience.splice(0, content.experience.length, ...next)
  persistDraft()
}

function onProjectReorder(next: ResumeProjectItem[]) {
  content.projects.splice(0, content.projects.length, ...next)
  persistDraft()
}

function onEducationReorder(next: ResumeEducationItem[]) {
  content.education.splice(0, content.education.length, ...next)
  persistDraft()
}

function onSectionDragStart(index: number) {
  sectionDragIndex.value = index
}

function onSectionDrop(index: number) {
  if (sectionDragIndex.value === null || sectionDragIndex.value === index) return
  const next = [...content.sectionOrder]
  const [moved] = next.splice(sectionDragIndex.value, 1)
  next.splice(index, 0, moved)
  content.sectionOrder.splice(0, content.sectionOrder.length, ...next)
  sectionDragIndex.value = null
  persistDraft()
}

function onBulletsInput(item: ResumeExperienceItem | ResumeProjectItem, event: Event) {
  const value = (event.target as HTMLTextAreaElement).value
  item.bullets = value.split('\n')
  persistDraft()
}

function onSkillsInput(event: Event) {
  const value = (event.target as HTMLTextAreaElement).value
  skillsText.value = value
  content.skills = value
    .split(/[,，\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
  persistDraft()
}

function buildPayload() {
  return {
    title: title.value.trim() || '我的简历',
    templateId: templateId.value,
    content: JSON.parse(JSON.stringify(content)) as ResumeContent
  }
}

async function saveResume() {
  saving.value = true
  error.value = ''
  statusMessage.value = ''
  try {
    const saved = currentId.value
      ? await updateResume(currentId.value, buildPayload())
      : await createResume(buildPayload())
    currentId.value = saved.id
    selectedId.value = saved.id
    statusMessage.value = '已保存'
    persistDraft()
    await refreshList()
  } catch (err) {
    setRequestError(err, '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeResume() {
  if (!currentId.value) return
  if (!window.confirm('确认删除这份简历？')) return
  deleting.value = true
  error.value = ''
  try {
    await deleteResume(currentId.value)
    resetEditor()
    statusMessage.value = '已删除'
    await refreshList()
  } catch (err) {
    setRequestError(err, '删除失败')
  } finally {
    deleting.value = false
  }
}

async function downloadServerPdf() {
  exportingPdf.value = true
  error.value = ''
  statusMessage.value = ''
  try {
    await exportResumePdf(buildPayload())
    statusMessage.value = 'PDF 已下载'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'PDF 生成失败'
  } finally {
    exportingPdf.value = false
  }
}

function printResume() {
  persistDraft()
  window.print()
}

function persistDraft() {
  const draft = {
    title: title.value,
    templateId: templateId.value,
    content,
    currentId: currentId.value
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

function restoreDraft() {
  const raw = localStorage.getItem(DRAFT_KEY)
  if (!raw) return
  try {
    const draft = JSON.parse(raw) as {
      title?: string
      templateId?: ResumeTemplateId
      content?: ResumeContent
      currentId?: number | null
    }
    if (draft.title) title.value = draft.title
    if (draft.templateId === 'sidebar' || draft.templateId === 'classic') {
      templateId.value = draft.templateId
    }
    if (draft.content) Object.assign(content, normalizeContent(draft.content))
    if (draft.currentId) currentId.value = draft.currentId
    skillsText.value = content.skills.join(', ')
  } catch {
    // ignore broken draft
  }
}

function normalizeContent(raw: Partial<ResumeContent> | null | undefined): ResumeContent {
  const empty = createEmptyResumeContent()
  if (!raw) return empty
  return {
    basics: { ...empty.basics, ...(raw.basics || {}) },
    summary: raw.summary || '',
    experience: Array.isArray(raw.experience) ? raw.experience.map(normalizeExperience) : [],
    education: Array.isArray(raw.education) ? raw.education.map(normalizeEducation) : [],
    skills: Array.isArray(raw.skills) ? raw.skills.filter((item): item is string => typeof item === 'string') : [],
    projects: Array.isArray(raw.projects) ? raw.projects.map(normalizeProject) : [],
    sectionOrder: normalizeSectionOrder(raw.sectionOrder as ResumeSectionKey[] | undefined)
  }
}

function normalizeExperience(item: Partial<ResumeExperienceItem>): ResumeExperienceItem {
  return {
    company: item.company || '',
    role: item.role || '',
    start: item.start || '',
    end: item.end || '',
    bullets: Array.isArray(item.bullets) ? item.bullets : ['']
  }
}

function normalizeEducation(item: Partial<ResumeEducationItem>): ResumeEducationItem {
  return {
    school: item.school || '',
    degree: item.degree || '',
    start: item.start || '',
    end: item.end || '',
    description: item.description || ''
  }
}

function normalizeProject(item: Partial<ResumeProjectItem>): ResumeProjectItem {
  return {
    name: item.name || '',
    role: item.role || '',
    link: item.link || '',
    bullets: Array.isArray(item.bullets) ? item.bullets : ['']
  }
}
</script>

<style>
.resume-sheet {
  width: 210mm;
  min-height: 297mm;
  padding: 14mm 16mm;
  box-sizing: border-box;
}

.resume-sheet--flush {
  padding: 0;
}

@media print {
  body * {
    visibility: hidden !important;
  }

  #resume-print-root,
  #resume-print-root * {
    visibility: visible !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  #resume-print-root {
    position: absolute !important;
    left: 0 !important;
    top: 0 !important;
    width: 210mm !important;
    min-height: auto !important;
    margin: 0 !important;
    box-shadow: none !important;
    background: white !important;
  }

  #resume-print-root:not(.resume-sheet--flush) {
    padding: 12mm 14mm !important;
  }

  #resume-print-root.resume-sheet--flush {
    padding: 0 !important;
  }

  @page {
    size: A4;
    margin: 0;
  }
}
</style>
