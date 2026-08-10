import axios from 'axios'
import http from './http'
import type { CreateResumeRequest, ResumeDTO, UpdateResumeRequest } from '@/tools/server/resume/types'

export function listResumes() {
  return http.get<unknown, ResumeDTO[]>('/resume')
}

export function getResume(id: number) {
  return http.get<unknown, ResumeDTO>(`/resume/${id}`)
}

export function createResume(payload: CreateResumeRequest) {
  return http.post<unknown, ResumeDTO>('/resume', payload)
}

export function updateResume(id: number, payload: UpdateResumeRequest) {
  return http.put<unknown, ResumeDTO>(`/resume/${id}`, payload)
}

export function deleteResume(id: number) {
  return http.delete<unknown, void>(`/resume/${id}`)
}

async function readErrorMessage(data: unknown) {
  if (!(data instanceof Blob)) {
    if (data && typeof data === 'object' && 'message' in data) {
      return String((data as { message?: string }).message || 'PDF 生成失败')
    }
    return 'PDF 生成失败'
  }
  const text = await data.text()
  try {
    const body = JSON.parse(text) as { message?: string }
    return body.message || 'PDF 生成失败'
  } catch {
    return text || 'PDF 生成失败'
  }
}

export async function exportResumePdf(payload: CreateResumeRequest) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  const token = localStorage.getItem('token')
  try {
    const response = await axios.post(`${baseURL}/resume/pdf`, payload, {
      responseType: 'blob',
      timeout: 60000,
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    })

    const contentType = String(response.headers['content-type'] || '')
    if (contentType.includes('application/json')) {
      throw new Error(await readErrorMessage(response.data))
    }

    const blob = response.data as Blob
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `${payload.title?.trim() || 'resume'}.pdf`
    anchor.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    if (axios.isAxiosError(err)) {
      throw new Error(await readErrorMessage(err.response?.data))
    }
    throw err
  }
}
