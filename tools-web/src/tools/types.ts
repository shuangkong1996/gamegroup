import type { Component } from 'vue'
import type { LucideIcon } from '@lucide/vue'

export interface ToolCategory {
  key: string
  title: string
  description: string
  icon: LucideIcon
}

export interface ToolMeta {
  key: string
  path: string
  title: string
  description: string
  category: string
  tags: string[]
  accent: string
  icon: LucideIcon
  mode: 'local' | 'server'
  loader: () => Promise<{ default: Component }>
}
