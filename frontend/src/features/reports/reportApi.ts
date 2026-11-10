import { apiUrl } from '../../config/api'
import { csrfHeader } from '../auth/authApi'
export type ReportGroup = 'COST' | 'REVENUE'
export type PeriodFilter = 'MONTH' | 'TBD'
export type ReportStatus = 'PLANNED' | 'PREVIEW' | 'IMPLEMENTING' | 'REFERENCE'

export interface ReportDefinition {
  code: string
  title: string
  group: ReportGroup
  periodFilter: PeriodFilter
  status: ReportStatus
  description: string
}

export interface ReportSchemaColumn {
  key: string
  title: string
  type: 'text' | 'integer' | 'decimal' | 'datetime'
  group: string | null
}

export interface ReportSchemaDynamicGroup {
  key: string
  title: string
  period: 'CURRENT_MONTH' | 'NEXT_MONTH'
  measures: string[]
}

export interface ReportSchema {
  code: string
  source: string
  filters: ('month' | 'agencyId')[]
  columns: ReportSchemaColumn[]
  dynamicGroups: ReportSchemaDynamicGroup[]
  status: string
  executable: boolean
  blocker: string
}

export interface ReportQuery {
  month: string
  agencyId: number | null
  page: number
  pageSize: number
  previewMode?: 'AGENCY_KITS' | 'PACKAGE_KITS' | 'LEGACY_REFERENCE'
}

export interface ReportColumn {
  key: string
  title: string
  type: string
  groupPath?: string[]
}

export interface ReportResult {
  code: string
  title: string
  preview: boolean
  notice: string
  columns: ReportColumn[]
  rows: Record<string, unknown>[]
  page: number
  pageSize: number
  totalRows: number
}

async function request(path: string, options?: RequestInit): Promise<Response> {
  const response = await fetch(apiUrl(path), { credentials: 'same-origin', cache: 'no-store', ...options })
  if (!response.ok) {
    if (response.status === 401) throw new Error('Phiên admin đã hết hạn; đăng nhập lại để tiếp tục.')
    if (response.status === 403) {
      let reason = ''
      try {
        const payload = await response.json() as { error?: string }
        reason = payload.error ?? ''
      } catch { /* Hide response body from unrecognized endpoints. */ }
      if (reason === 'CSRF_TOKEN_MISSING')
        throw new Error('CSRF_TOKEN_MISSING: Yêu cầu POST không có mã CSRF hợp lệ.')
      if (reason === 'CSRF_TOKEN_INVALID')
        throw new Error('CSRF_TOKEN_INVALID: Mã CSRF không khớp với phiên hiện tại.')
      if (reason === 'ACCESS_DENIED')
        throw new Error('ACCESS_DENIED: Phiên đăng nhập không có quyền truy cập báo cáo.')
      throw new Error('HTTP 403: Quyền truy cập hoặc CSRF không hợp lệ.')
    }
    let detail = `HTTP ${response.status}`
    try {
      const payload = await response.json() as { detail?: string; message?: string; error?: string }
      detail = payload.detail || payload.message || payload.error || detail
    } catch { /* non-JSON error */ }
    throw new Error(detail)
  }
  return response
}

async function postOptions(query: ReportQuery): Promise<RequestInit> {
  return {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(await csrfHeader()),
    },
    body: JSON.stringify(query),
  }
}

export async function listReports(): Promise<ReportDefinition[]> {
  return (await request('/api/reports')).json() as Promise<ReportDefinition[]>
}

export async function getReportSchema(code: string): Promise<ReportSchema> {
  return (await request(`/api/reports/${encodeURIComponent(code)}/schema`))
    .json() as Promise<ReportSchema>
}

export async function listReportSchemas(): Promise<ReportSchema[]> {
  return (await request('/api/reports/schemas')).json() as Promise<ReportSchema[]>
}

export async function runReport(code: string, query: ReportQuery): Promise<ReportResult> {
  return (await request(`/api/reports/${encodeURIComponent(code)}/run`, await postOptions(query)))
    .json() as Promise<ReportResult>
}

export async function exportReport(code: string, query: ReportQuery): Promise<void> {
  const response = await request(`/api/reports/${encodeURIComponent(code)}/excel`, await postOptions(query))
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  try {
    const a = document.createElement('a')
    a.href = url
    a.download = `pottel-${code.replace('.', '-')}${query.previewMode === 'AGENCY_KITS' || query.previewMode === 'PACKAGE_KITS' ? '-KIT-PREVIEW' : '-REFERENCE'}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 2000)
  }
}
