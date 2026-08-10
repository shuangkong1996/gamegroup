import {
  Binary,
  Braces,
  Calculator,
  Clock3,
  Code2,
  FileText,
  Hash,
  Image,
  Landmark,
  Link,
  ListFilter,
  ListTree,
  LockKeyhole,
  RadioTower,
  Repeat,
  ShieldCheck,
  Type,
  IdCard
} from '@lucide/vue'
import type { ToolCategory, ToolMeta } from './types'

export const categories: ToolCategory[] = [
  {
    key: 'dev',
    title: '开发运维',
    description: '编码、格式化、调试与常用开发辅助',
    icon: Code2
  },
  {
    key: 'text',
    title: '文本处理',
    description: '统计、清洗、转换与内容整理',
    icon: Type
  },
  {
    key: 'life',
    title: '生活工具',
    description: '贴近日常决策的计算工具',
    icon: Landmark
  }
]

export const tools: ToolMeta[] = [
  {
    key: 'json',
    path: '/tools/json',
    title: 'JSON 格式化',
    description: '格式化、压缩与校验 JSON',
    category: 'dev',
    tags: ['JSON', '格式化', '压缩', '校验'],
    accent: 'indigo',
    icon: Braces,
    mode: 'local',
    loader: () => import('./local/json/JsonTool.vue')
  },
  {
    key: 'base64',
    path: '/tools/base64',
    title: 'Base64 编解码',
    description: '支持中文的 Base64 转换',
    category: 'dev',
    tags: ['Base64', '编码', '解码'],
    accent: 'emerald',
    icon: Binary,
    mode: 'local',
    loader: () => import('./local/base64/Base64Tool.vue')
  },
  {
    key: 'timestamp',
    path: '/tools/timestamp',
    title: '时间戳转换',
    description: '秒、毫秒与本地时间互转',
    category: 'dev',
    tags: ['时间戳', '日期', '时间'],
    accent: 'amber',
    icon: Clock3,
    mode: 'local',
    loader: () => import('./local/timestamp/TimestampTool.vue')
  },
  {
    key: 'uuid',
    path: '/tools/uuid',
    title: '唯一标识生成',
    description: '批量生成随机唯一标识符',
    category: 'dev',
    tags: ['唯一标识', 'UUID', '随机'],
    accent: 'sky',
    icon: Hash,
    mode: 'local',
    loader: () => import('./local/uuid/UuidTool.vue')
  },
  {
    key: 'url-codec',
    path: '/tools/url-codec',
    title: '网址编码解码',
    description: '网址安全编码与解码转换',
    category: 'dev',
    tags: ['网址', 'URL', '编码', '解码'],
    accent: 'indigo',
    icon: Link,
    mode: 'local',
    loader: () => import('./local/url-codec/UrlCodecTool.vue')
  },
  {
    key: 'url-params',
    path: '/tools/url-params',
    title: '网址参数解析',
    description: '查询参数解析、编辑与构造',
    category: 'dev',
    tags: ['网址', '参数', '查询'],
    accent: 'emerald',
    icon: ListTree,
    mode: 'local',
    loader: () => import('./local/url-params/UrlParamsTool.vue')
  },
  {
    key: 'password',
    path: '/tools/password',
    title: '随机密码生成',
    description: '按规则生成安全随机密码',
    category: 'dev',
    tags: ['密码', '随机', '安全'],
    accent: 'violet',
    icon: LockKeyhole,
    mode: 'local',
    loader: () => import('./local/password/PasswordTool.vue')
  },
  {
    key: 'hash',
    path: '/tools/hash',
    title: '哈希摘要',
    description: '计算 SHA 系列摘要值',
    category: 'dev',
    tags: ['哈希', '摘要', 'SHA'],
    accent: 'sky',
    icon: Hash,
    mode: 'local',
    loader: () => import('./local/hash/HashTool.vue')
  },
  {
    key: 'jwt',
    path: '/tools/jwt',
    title: 'JWT 解析生成',
    description: '解析、校验与本地生成 JWT 令牌',
    category: 'dev',
    tags: ['JWT', 'Token', '签名', 'HS256'],
    accent: 'amber',
    icon: ShieldCheck,
    mode: 'local',
    loader: () => import('./local/jwt/JwtTool.vue')
  },
  {
    key: 'websocket',
    path: '/tools/websocket',
    title: '长连接调试',
    description: '连接、收发并查看实时消息',
    category: 'dev',
    tags: ['长连接', 'WebSocket', '调试', '消息'],
    accent: 'indigo',
    icon: RadioTower,
    mode: 'local',
    loader: () => import('./local/websocket/WebSocketTool.vue')
  },
  {
    key: 'word-count',
    path: '/tools/word-count',
    title: '字数统计',
    description: '字符、词数、行数与字节统计',
    category: 'text',
    tags: ['字数', '文本', '统计'],
    accent: 'violet',
    icon: FileText,
    mode: 'local',
    loader: () => import('./local/word-count/WordCountTool.vue')
  },
  {
    key: 'text-dedupe',
    path: '/tools/text-dedupe',
    title: '文本去重',
    description: '按行去重并保留顺序',
    category: 'text',
    tags: ['文本', '去重', '行处理'],
    accent: 'emerald',
    icon: ListFilter,
    mode: 'local',
    loader: () => import('./local/text-dedupe/TextDedupeTool.vue')
  },
  {
    key: 'naming-case',
    path: '/tools/naming-case',
    title: '命名风格转换',
    description: '小驼峰、下划线、短横线等互转',
    category: 'text',
    tags: ['命名', '驼峰', '下划线', '短横线'],
    accent: 'amber',
    icon: Repeat,
    mode: 'local',
    loader: () => import('./local/naming-case/NamingCaseTool.vue')
  },
  {
    key: 'number-to-rmb',
    path: '/tools/number-to-rmb',
    title: '数字转金额大写',
    description: '人民币金额大写转换',
    category: 'life',
    tags: ['金额', '人民币', '大写'],
    accent: 'emerald',
    icon: Calculator,
    mode: 'local',
    loader: () => import('./local/number-to-rmb/NumberToRmbTool.vue')
  },
  {
    key: 'image-base64',
    path: '/tools/image-base64',
    title: '图片转 Base64',
    description: '本地图片与数据地址互转',
    category: 'dev',
    tags: ['图片', 'Base64', '数据地址'],
    accent: 'violet',
    icon: Image,
    mode: 'local',
    loader: () => import('./local/image-base64/ImageBase64Tool.vue')
  },
  {
    key: 'mortgage',
    path: '/tools/mortgage',
    title: '房贷计算器',
    description: '商贷、公积金与还款计划估算',
    category: 'life',
    tags: ['房贷', '商贷', '公积金'],
    accent: 'indigo',
    icon: Calculator,
    mode: 'server',
    loader: () => import('./server/mortgage/MortgageTool.vue')
  },
  {
    key: 'resume',
    path: '/tools/resume',
    title: '简历生成',
    description: '填写信息、选择模板并导出 PDF',
    category: 'life',
    tags: ['简历', 'PDF', '求职'],
    accent: 'emerald',
    icon: IdCard,
    mode: 'server',
    loader: () => import('./server/resume/ResumeTool.vue')
  }
]

export function getCategory(key: string) {
  return categories.find((category) => category.key === key)
}

export function getTool(key: string) {
  return tools.find((tool) => tool.key === key)
}

export function getToolsByCategory(categoryKey: string) {
  return tools.filter((tool) => tool.category === categoryKey)
}

export function searchTools(keyword: string, source = tools) {
  const normalized = keyword.trim().toLowerCase()
  if (!normalized) {
    return source
  }
  return source.filter((tool) => {
    const haystack = [tool.title, tool.description, ...tool.tags].join(' ').toLowerCase()
    return haystack.includes(normalized)
  })
}
