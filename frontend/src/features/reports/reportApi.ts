import { apiUrl } from '../../config/api'
export type ReportGroup = 'COST' | 'REVENUE'
export type PeriodFilter = 'MONTH' | 'TBD'
export type ReportStatus = 'PLANNED' | 'PREVIEW'

export interface ReportDefinition {
  code: string
  title: string
  group: ReportGroup
  periodFilter: PeriodFilter
  status: ReportStatus
  description: string
}

export interface ReportQuery {
  month: string
  agencyId: number | null
  page: number
  pageSize: number
}

export interface ReportColumn {
  key: string
  title: string
  type: string
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
  const response = await fetch(apiUrl(path), options)
  if (!response.ok) {
    let detail = `HTTP ${response.status}`
    try {
      const payload = await response.json() as { detail?: string; message?: string; error?: string }
      detail = payload.detail || payload.message || payload.error || detail
    } catch { /* non-JSON error */ }
    throw new Error(detail)
  }
  return response
}

function postOptions(query: ReportQuery): RequestInit {
  return {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(query),
  }
}

export async function listReports(): Promise<ReportDefinition[]> {
  return (await request('/api/reports')).json() as Promise<ReportDefinition[]>
}

export async function runReport(code: string, query: ReportQuery): Promise<ReportResult> {
  return (await request(`/api/reports/${encodeURIComponent(code)}/run`, postOptions(query)))
    .json() as Promise<ReportResult>
}

export async function exportReport(code: string, query: ReportQuery): Promise<void> {
  const response = await request(`/api/reports/${encodeURIComponent(code)}/excel`, postOptions(query))
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  try {
    const a = document.createElement('a')
    a.href = url
    a.download = `pottel-${code.replace('.', '-')}-preview.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 2000)
  }
}
