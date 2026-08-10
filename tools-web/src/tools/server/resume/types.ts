export interface ResumeBasics {
  name: string
  title: string
  email: string
  phone: string
  location: string
  website: string
}

export interface ResumeExperienceItem {
  company: string
  role: string
  start: string
  end: string
  bullets: string[]
}

export interface ResumeEducationItem {
  school: string
  degree: string
  start: string
  end: string
  description: string
}

export interface ResumeProjectItem {
  name: string
  role: string
  link: string
  bullets: string[]
}

export type ResumeSectionKey = 'summary' | 'experience' | 'projects' | 'education' | 'skills'

export interface ResumeContent {
  basics: ResumeBasics
  summary: string
  experience: ResumeExperienceItem[]
  education: ResumeEducationItem[]
  skills: string[]
  projects: ResumeProjectItem[]
  sectionOrder: ResumeSectionKey[]
}

export type ResumeTemplateId = 'classic' | 'sidebar'

export const DEFAULT_SECTION_ORDER: ResumeSectionKey[] = [
  'summary',
  'experience',
  'projects',
  'education',
  'skills'
]

export const SECTION_LABELS: Record<ResumeSectionKey, string> = {
  summary: '个人简介',
  experience: '工作经历',
  projects: '项目经历',
  education: '教育经历',
  skills: '技能'
}

export interface ResumeDTO {
  id: number
  title: string
  templateId: ResumeTemplateId | string
  content: ResumeContent
  createTime: string
  updateTime: string
}

export interface CreateResumeRequest {
  title: string
  templateId: string
  content: ResumeContent
}

export interface UpdateResumeRequest {
  title: string
  templateId: string
  content: ResumeContent
}

export function createEmptyResumeContent(): ResumeContent {
  return {
    basics: {
      name: '',
      title: '',
      email: '',
      phone: '',
      location: '',
      website: ''
    },
    summary: '',
    experience: [],
    education: [],
    skills: [],
    projects: [],
    sectionOrder: [...DEFAULT_SECTION_ORDER]
  }
}

export function createSampleResumeContent(): ResumeContent {
  return {
    basics: {
      name: '张三',
      title: '全栈工程师',
      email: 'zhangsan@example.com',
      phone: '138-0000-0000',
      location: '上海',
      website: 'https://github.com/example'
    },
    summary: '专注 Web 全栈开发，熟悉前后端协作与常见工程化实践，喜欢把复杂问题拆成可落地的小步骤。',
    experience: [
      {
        company: '某某科技',
        role: '高级前端工程师',
        start: '2022.03',
        end: '至今',
        bullets: ['负责核心业务中后台与组件库建设', '推动前端工程化，降低发版成本与缺陷率']
      },
      {
        company: '某某网络',
        role: '前端工程师',
        start: '2019.07',
        end: '2022.02',
        bullets: ['参与电商与运营后台开发', '沉淀通用请求层与表单方案']
      }
    ],
    education: [
      {
        school: '某某大学',
        degree: '计算机科学与技术 · 本科',
        start: '2015.09',
        end: '2019.06',
        description: ''
      }
    ],
    skills: ['TypeScript', 'Vue', 'Java', 'Spring Boot', 'MySQL', 'Redis'],
    projects: [
      {
        name: '个人工具站',
        role: '独立开发',
        link: '',
        bullets: ['实现常用开发工具与简历生成能力', '前后端分离，支持登录态数据同步']
      }
    ],
    sectionOrder: [...DEFAULT_SECTION_ORDER]
  }
}

export function normalizeSectionOrder(order?: string[] | null): ResumeSectionKey[] {
  const result: ResumeSectionKey[] = []
  const allowed = new Set<ResumeSectionKey>(DEFAULT_SECTION_ORDER)
  for (const item of order || []) {
    if (allowed.has(item as ResumeSectionKey) && !result.includes(item as ResumeSectionKey)) {
      result.push(item as ResumeSectionKey)
    }
  }
  for (const key of DEFAULT_SECTION_ORDER) {
    if (!result.includes(key)) result.push(key)
  }
  return result
}
