/** Phase 1 report identifiers and navigation labels (NOT implementation status). */
export interface ReportNavigationItem {
  code: string
  title: string
  group: 'COST' | 'REVENUE'
  path: string
}

const costs = [
  ['I.1', 'Khuyến khích theo sản lượng'],
  ['I.2', 'Chi tiết thuê bao khuyến khích'],
  ['I.3', 'Phân bổ chi phí thuê bao'],
  ['I.4', 'Chưa phân bổ chi phí trong kỳ'],
  ['I.5', 'Đã phân bổ chi phí trong kỳ'],
  ['I.6', 'Phân bổ chi phí sang kỳ sau'],
  ['I.7', 'Tổng hợp phân bổ chi phí bán gói'],
] as const
const revenues = [
  ['II.1', 'Phân bổ doanh thu thuê bao'],
  ['II.2', 'Chưa phân bổ doanh thu trong kỳ'],
  ['II.3', 'Đã phân bổ doanh thu trong kỳ'],
  ['II.4', 'Chờ phân bổ doanh thu sang kỳ sau'],
  ['II.5', 'Tổng hợp phân bổ doanh thu bán gói'],
] as const

export const REPORT_NAVIGATION: ReportNavigationItem[] = [
  ...costs.map(([code, title]) => ({ code, title, group: 'COST' as const, path: `/reports/cost/${code.toLowerCase().replace('.', '')}` })),
  ...revenues.map(([code, title]) => ({ code, title, group: 'REVENUE' as const, path: `/reports/revenue/${code.toLowerCase().replace('.', '')}` })),
]

export function findReport(code: string): ReportNavigationItem | undefined {
  return REPORT_NAVIGATION.find(item => item.code === code)
}

export function reportPath(code: string): string {
  return findReport(code)?.path ?? '/reports/cost/i1'
}
