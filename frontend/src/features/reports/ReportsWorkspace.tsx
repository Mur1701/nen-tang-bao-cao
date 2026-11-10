import { useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert, Button, Card, Checkbox, DatePicker, Empty, Input, InputNumber,
  Popover, Segmented, Select, Space, Table, Tag, Typography,
} from 'antd'
import {
  SettingOutlined, DownloadOutlined, PlayCircleOutlined,
  ReloadOutlined, SearchOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import {
  exportReport, listReports, runReport, loadRevenueTrend,
  type RevenueTrend, type ReportDefinition, type ReportQuery, type ReportResult,
} from './reportApi'
import { findReport } from './reportNavigation'
import { formatGroupHeader, formatReportMonth, formatReportValue, normalizeSearch } from './reportFormat'
import ReportBlueprintView from './ReportBlueprintView'
import RevenueTrendChart from './RevenueTrendChart'

const { Title, Text, Paragraph } = Typography
const currentMonth = () => dayjs().format('YYYY-MM')
const formatCount = (n: number) => n.toLocaleString('vi-VN')

interface WorkspaceProps {
  selectedCode: string
}

export default function ReportsWorkspace({ selectedCode }: WorkspaceProps) {
  const [catalog, setCatalog] = useState<ReportDefinition[]>([])
  const [catalogError, setCatalogError] = useState<string | null>(null)
  const [appliedQuery, setAppliedQuery] = useState<ReportQuery | null>(null)
  const [query, setQuery] = useState<ReportQuery>({ month: currentMonth(), agencyId: null, page: 1, pageSize: 50, previewMode: selectedCode === 'I.1' ? 'LEGACY_REFERENCE' : undefined })
  const [result, setResult] = useState<ReportResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [trend, setTrend] = useState<RevenueTrend | null>(null)
  const [trendVisible, setTrendVisible] = useState(false)
  const [trendLoading, setTrendLoading] = useState(false)
  const [trendError, setTrendError] = useState<string | null>(null)
  const trendRequest = useRef(0)
  const [searchInput, setSearchInput] = useState('')
  const [quickSearch, setQuickSearch] = useState('')
  const [hiddenColumns, setHiddenColumns] = useState<string[]>([])
  const [tableDensity, setTableDensity] = useState<'small' | 'middle'>('middle')
  const requestVersion = useRef(0)

  useEffect(() => {
    requestVersion.current += 1
    setResult(null)
    setAppliedQuery(null)
    trendRequest.current += 1
    setTrend(null)
    setTrendVisible(false)
    setTrendError(null)
    setTrendLoading(false)
    setError(null)
    setLoading(false)
    setSearchInput('')
    setQuickSearch('')
    setHiddenColumns([])
    setQuery(q => ({ ...q, page: 1, agencyId: selectedCode === 'I.1' || selectedCode === 'I.2' ? q.agencyId : null, previewMode: selectedCode === 'I.1' ? 'LEGACY_REFERENCE' : undefined }))
  }, [selectedCode])

  useEffect(() => {
    let active = true
    listReports().then(data => {
      if (active) {
        setCatalog(data)
        setCatalogError(null)
      }
    }).catch(e => {
      if (active) setCatalogError((e as Error).message)
    })
    return () => { active = false }
  }, [])

  const navigation = findReport(selectedCode)
  const definition = catalog.find(r => r.code === selectedCode)
  const isRunnable = definition?.status === 'PREVIEW' || definition?.status === 'IMPLEMENTING' || definition?.status === 'REFERENCE'
  const displayedRows = useMemo(() => {
    const text = normalizeSearch(quickSearch)
    const rows = result?.rows ?? []
    if (!text) return rows
    return rows.filter(row => Object.values(row).some(value =>
      value != null && normalizeSearch(String(value)).includes(text)))
  }, [quickSearch, result])

  const availableColumns = result?.columns ?? []
  const visibleColumns = availableColumns.filter(c => !hiddenColumns.includes(c.key))
  // Build grouped headers from the server result contract. I.1's legacy
  // three-field columns remain unchanged; allocation day groups are metadata-driven.
  const columns: ColumnsType<Record<string, unknown>> = []
  for (const c of visibleColumns) {
    let target: ColumnsType<Record<string, unknown>> = columns
    for (const title of c.groupPath ?? []) {
      const displayedTitle = formatGroupHeader(title)
      const existing = target.find(entry => 'children' in entry && entry.title === displayedTitle)
      if (existing && 'children' in existing && existing.children) {
        target = existing.children
      } else {
        const group = { title: displayedTitle, children: [] as ColumnsType<Record<string, unknown>> }
        target.push(group)
        target = group.children
      }
    }
    target.push({
      key: c.key,
      dataIndex: c.key,
      title: c.title,
      align: c.type === 'integer' || c.type === 'decimal' ? 'right' : 'left',
      ellipsis: { showTitle: true },
      render: (value: unknown) => value == null ? <Text type="secondary">—</Text> : formatReportValue(value, c),
    })
  }

  async function run(patch?: Partial<ReportQuery>) {
    if (!isRunnable) return
    const input = { ...query, ...patch }
    const version = ++requestVersion.current
    trendRequest.current += 1
    setTrendVisible(false)
    setTrend(null)
    setTrendError(null)
    setLoading(true)
    setError(null)
    setResult(null)
    setAppliedQuery(null)
    setSearchInput('')
    setQuickSearch('')
    try {
      const data = await runReport(selectedCode, input)
      if (version === requestVersion.current) {
        setQuery(input)
        setResult(data)
        setAppliedQuery(input)
        setHiddenColumns([])
      }
    } catch (e) {
      if (version === requestVersion.current) setError((e as Error).message)
    } finally {
      if (version === requestVersion.current) setLoading(false)
    }
  }

  async function toggleTrend() {
    if (trendVisible) { setTrendVisible(false); return }
    if (trend) { setTrendVisible(true); return }
    if (!appliedQuery || !selectedCode.startsWith('II.')) return
    const version = ++trendRequest.current
    setTrendLoading(true)
    setTrendError(null)
    try {
      const data = await loadRevenueTrend(selectedCode, appliedQuery)
      if (version === trendRequest.current) { setTrend(data); setTrendVisible(true) }
    } catch (e) {
      if (version === trendRequest.current) setTrendError((e as Error).message)
    } finally {
      if (version === trendRequest.current) setTrendLoading(false)
    }
  }

  async function excel() {
    if (!result || !appliedQuery) return
    setExporting(true)
    setError(null)
    try { await exportReport(selectedCode, appliedQuery) }
    catch (e) { setError((e as Error).message) }
    finally { setExporting(false) }
  }

  const invalidateResult = () => {
    requestVersion.current += 1
    setLoading(false)
    setResult(null)
    setAppliedQuery(null)
    setError(null)
    setSearchInput('')
    setQuickSearch('')
  }

  const reset = () => {
    requestVersion.current += 1
    setLoading(false)
    setQuery({ month: currentMonth(), agencyId: null, page: 1, pageSize: 50, previewMode: selectedCode === 'I.1' ? 'LEGACY_REFERENCE' : undefined })
    setResult(null)
    setAppliedQuery(null)
    setError(null)
    setSearchInput('')
    setQuickSearch('')
    setHiddenColumns([])
  }

  return (
    <div className="report-workspace">
      <div className="report-heading">
        <div>
          <Text type="secondary">{navigation?.group === 'REVENUE' ? 'Báo cáo doanh thu' : 'Báo cáo chi phí'} · {selectedCode}</Text>
          <Title level={3} style={{ margin: '5px 0 6px' }}>{definition?.title ?? navigation?.title ?? selectedCode}</Title>
          <Text type="secondary">{'Chọn kỳ báo cáo để xem dữ liệu và so sánh theo thời gian.'}</Text>
        </div>

      </div>

      <Card title="Điều kiện báo cáo" className="report-filter-card">
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          {catalogError && <Alert showIcon type="warning" message="Không lấy được danh mục từ backend" description={catalogError} />}
          {isRunnable ? (
            <div className="report-filter-grid">
              <div className="report-filter-item">
                <Text strong>Tháng báo cáo</Text>
                <DatePicker
                  picker="month"
                  value={dayjs(query.month + '-01')}
                  format="[Tháng] M [năm] YYYY"
                  allowClear={false}
                  onChange={date => {
                    setQuery(q => ({ ...q, month: date ? date.format('YYYY-MM') : currentMonth(), page: 1 }))
                    invalidateResult()
                  }}
                />
              </div>
              {(selectedCode === 'I.1' || selectedCode === 'I.2') && (
              <div className="report-filter-item">
                <Text strong>ID đại lý <Text type="secondary">(tạm thời)</Text></Text>
                <InputNumber
                  min={1}
                  placeholder="Tất cả đại lý"
                  value={query.agencyId}
                  onChange={value => {
                    setQuery(q => ({ ...q, agencyId: value, page: 1 }))
                    invalidateResult()
                  }}
                  style={{ width: 195 }}
                />
              </div>
              )}
              <div className="report-filter-item">
                <Text strong>Số dòng / trang</Text>
                <Select
                  value={query.pageSize}
                  style={{ width: 160 }}
                  options={[20, 50, 100, 200].map(n => ({ value: n, label: `${n} dòng` }))}
                  onChange={value => {
                    setQuery(q => ({ ...q, page: 1, pageSize: value }))
                    invalidateResult()
                  }}
                />
              </div>
              <div className="report-filter-actions">
                <Button type="primary" icon={<PlayCircleOutlined />} loading={loading} onClick={() => void run({ page: 1 })}>Chạy báo cáo</Button>
                <Button icon={<ReloadOutlined />} disabled={loading} onClick={reset}>Đặt lại</Button>
                <Button icon={<DownloadOutlined />} loading={exporting} disabled={!result} onClick={() => void excel()}>Xuất Excel</Button>
              </div>
            </div>
          ) : definition?.status === 'PLANNED' ? (
            <ReportBlueprintView
              code={selectedCode}
              month={query.month}
              agencyId={query.agencyId}
              onMonthChange={month => {
                setQuery(q => ({ ...q, month, page: 1 }))
                invalidateResult()
              }}
              onAgencyChange={agencyId => {
                setQuery(q => ({ ...q, agencyId, page: 1 }))
                invalidateResult()
              }}
            />
          ) : (
            <Alert type="info" showIcon message="Đang chờ backend cung cấp trạng thái báo cáo" />
          )}
        </Space>
      </Card>

      {error && <Alert style={{ marginBottom: 16 }} type="error" showIcon message="Không thể chạy báo cáo" description={error} />}

      {result && (
        <Card className="report-result-card" title={<Space wrap><span>Kết quả báo cáo</span><Tag color="blue">{result.code}</Tag></Space>}>
          {result.notice && <details className="report-data-notice"><summary>Thông tin nguồn và cách tính</summary><Text type="secondary">{result.notice}</Text></details>}
          <div className="report-result-metrics">
            <Space size="large" wrap>
              <div><Text type="secondary">Tổng số dòng</Text><div><Text strong>{formatCount(result.totalRows)}</Text></div></div>
              <div><Text type="secondary">Trang hiện tại</Text><div><Text strong>{result.page}</Text></div></div>
              <div><Text type="secondary">Kỳ đã chạy</Text><div><Text strong>{formatReportMonth(appliedQuery?.month ?? query.month)}</Text></div></div>
            </Space>

          </div>
          {selectedCode.startsWith('II.') && (
            <div className="revenue-trend-section">
              <Button onClick={() => void toggleTrend()} loading={trendLoading}>
                {trendVisible ? 'Ẩn biểu đồ xu hướng' : 'Xem xu hướng 6 tháng'}
              </Button>
              {trendError && <Alert type="warning" showIcon message="Không tải được biểu đồ" description={trendError} style={{ marginTop: 8 }} />}
              {trendVisible && trend && <RevenueTrendChart trend={trend} />}
            </div>
          )}
          <div className="report-table-toolbar">
            <Input
              prefix={<SearchOutlined />}
              placeholder="Nhập từ khóa, nhấn Enter"
              value={searchInput}
              allowClear
              onChange={e => { setSearchInput(e.target.value); if (!e.target.value) setQuickSearch('') }}
              onPressEnter={() => setQuickSearch(searchInput.trim())}
              style={{ width: 255, maxWidth: '100%' }}
              aria-label="Tìm trong trang hiện tại bằng Enter"
            />
            <Popover trigger="click" placement="bottomRight" content={
              <div style={{ width: 250, maxHeight: 300, overflowY: 'auto' }}>
                <Space style={{ justifyContent: 'space-between', width: '100%', marginBottom: 10 }}>
                  <Text strong>Cột hiển thị</Text>
                  <Button size="small" type="link" onClick={() => setHiddenColumns([])}>Hiện tất cả</Button>
                </Space>
                <Space direction="vertical" size="small">
                  {availableColumns.map(c => (
                    <Checkbox
                      key={c.key}
                      checked={!hiddenColumns.includes(c.key)}
                      disabled={!hiddenColumns.includes(c.key) && visibleColumns.length <= 1}
                      onChange={e => setHiddenColumns(prev => e.target.checked ? prev.filter(key => key !== c.key) : [...prev, c.key])}
                    >{c.title}</Checkbox>
                  ))}
                </Space>
              </div>
            }>
              <Button icon={<SettingOutlined />}>Ẩn/hiện cột</Button>
            </Popover>
            <Segmented
              value={tableDensity}
              onChange={value => setTableDensity(value as 'small' | 'middle')}
              options={[{ label: 'Gọn', value: 'small' }, { label: 'Thoáng', value: 'middle' }]}
            />
          </div>
          <Paragraph type="secondary" style={{ marginBottom: 8, fontSize: 12 }}>
            Nhấn Enter để tìm trong trang đang hiển thị. Excel dùng bộ lọc đã chạy, không bị ảnh hưởng bởi ô tìm kiếm.
            {quickSearch && ` Khớp ${formatCount(displayedRows.length)}/${formatCount(result.rows.length)} dòng trên trang này.`}
          </Paragraph>
          <Table<Record<string, unknown>>
            rowKey={(row, index) => `${String(row.agency_id ?? row.id ?? '')}:${String(row.package_code ?? row.ma_goi ?? '')}:${result.page}:${index ?? 0}`}
            columns={columns}
            dataSource={displayedRows}
            size={tableDensity}
            sticky
            bordered
            scroll={{ x: 'max-content' }}
            loading={loading}
            locale={{ emptyText: <Empty description={quickSearch ? 'Không có dòng nào khớp trên trang này' : 'Không có dữ liệu ở kỳ đã chọn'} /> }}
            pagination={{
              current: result.page,
              pageSize: result.pageSize,
              total: result.totalRows,
              showSizeChanger: false,
              showTotal: total => `Tổng ${formatCount(total)} dòng`,
              onChange: page => { void run({ page }) },
            }}
          />
        </Card>
      )}
    </div>
  )
}
