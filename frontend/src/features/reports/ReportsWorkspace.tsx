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
  exportReport, listReports, runReport,
  type ReportDefinition, type ReportQuery, type ReportResult,
} from './reportApi'
import { REPORT_NAVIGATION, findReport } from './reportNavigation'
import ReportBlueprintView from './ReportBlueprintView'

const { Title, Text, Paragraph } = Typography
const currentMonth = () => dayjs().format('YYYY-MM')
const formatCount = (n: number) => n.toLocaleString('vi-VN')
const showPackagePreview = import.meta.env.VITE_ENABLE_I1_PACKAGE_PREVIEW === 'true'

interface WorkspaceProps {
  selectedCode: string
  onSelectReport: (code: string) => void
}

export default function ReportsWorkspace({ selectedCode, onSelectReport }: WorkspaceProps) {
  const [catalog, setCatalog] = useState<ReportDefinition[]>([])
  const [catalogError, setCatalogError] = useState<string | null>(null)
  const [query, setQuery] = useState<ReportQuery>({ month: currentMonth(), agencyId: null, page: 1, pageSize: 50, previewMode: 'AGENCY_KITS' })
  const [result, setResult] = useState<ReportResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [quickSearch, setQuickSearch] = useState('')
  const [hiddenColumns, setHiddenColumns] = useState<string[]>([])
  const [tableDensity, setTableDensity] = useState<'small' | 'middle'>('middle')
  const requestVersion = useRef(0)

  useEffect(() => {
    requestVersion.current += 1
    setResult(null)
    setError(null)
    setLoading(false)
    setQuickSearch('')
    setHiddenColumns([])
    setQuery(q => ({ ...q, page: 1 }))
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
  const isRunnable = definition?.status === 'PREVIEW' || definition?.status === 'IMPLEMENTING'
  const groups = useMemo(() => ([
    {
      label: 'Chi phí',
      options: REPORT_NAVIGATION.filter(r => r.group === 'COST').map(r => ({
        value: r.code, label: `${r.code} — ${catalog.find(d => d.code === r.code)?.title ?? r.title}`,
      })),
    },
    {
      label: 'Doanh thu',
      options: REPORT_NAVIGATION.filter(r => r.group === 'REVENUE').map(r => ({
        value: r.code, label: `${r.code} — ${catalog.find(d => d.code === r.code)?.title ?? r.title}`,
      })),
    },
  ]), [catalog])

  const displayedRows = useMemo(() => {
    const text = quickSearch.trim().toLocaleLowerCase('vi-VN')
    const rows = result?.rows ?? []
    if (!text) return rows
    return rows.filter(row => Object.values(row).some(value =>
      value != null && String(value).toLocaleLowerCase('vi-VN').includes(text)))
  }, [quickSearch, result])

  const availableColumns = result?.columns ?? []
  const visibleColumns = availableColumns.filter(c => !hiddenColumns.includes(c.key))
  // Build grouped headers from the server result contract. I.1's legacy
  // three-field columns remain unchanged; allocation day groups are metadata-driven.
  const columns: ColumnsType<Record<string, unknown>> = []
  for (const c of visibleColumns) {
    let target: ColumnsType<Record<string, unknown>> = columns
    for (const title of c.groupPath ?? []) {
      const existing = target.find(entry => 'children' in entry && entry.title === title)
      if (existing && 'children' in existing) {
        target = existing.children
      } else {
        const group = { title, children: [] as ColumnsType<Record<string, unknown>> }
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
      render: (value: unknown) => value == null ? <Text type="secondary">—</Text> :
        c.type === 'integer' && typeof value === 'number' ? formatCount(value) : String(value),
    })
  }

  async function run(patch?: Partial<ReportQuery>) {
    if (!isRunnable) return
    const input = { ...query, ...patch }
    const version = ++requestVersion.current
    setLoading(true)
    setError(null)
    setResult(null)
    setQuickSearch('')
    try {
      const data = await runReport(selectedCode, input)
      if (version === requestVersion.current) {
        setQuery(input)
        setResult(data)
        setHiddenColumns([])
      }
    } catch (e) {
      if (version === requestVersion.current) setError((e as Error).message)
    } finally {
      if (version === requestVersion.current) setLoading(false)
    }
  }

  async function excel() {
    if (!result) return
    setExporting(true)
    setError(null)
    try { await exportReport(selectedCode, query) }
    catch (e) { setError((e as Error).message) }
    finally { setExporting(false) }
  }

  const invalidateResult = () => {
    requestVersion.current += 1
    setLoading(false)
    setResult(null)
    setError(null)
    setQuickSearch('')
  }

  const reset = () => {
    requestVersion.current += 1
    setLoading(false)
    setQuery({ month: currentMonth(), agencyId: null, page: 1, pageSize: 50, previewMode: 'AGENCY_KITS' })
    setResult(null)
    setError(null)
    setQuickSearch('')
    setHiddenColumns([])
  }

  return (
    <div className="report-workspace">
      <div className="report-heading">
        <div>
          <Text type="secondary">{navigation?.group === 'REVENUE' ? 'Báo cáo doanh thu' : 'Báo cáo chi phí'} · {selectedCode}</Text>
          <Title level={3} style={{ margin: '5px 0 6px' }}>{definition?.title ?? navigation?.title ?? selectedCode}</Title>
          <Text type="secondary">{definition?.description ?? 'Báo cáo thuộc danh mục Phase 1'}</Text>
        </div>
        <Tag color={definition?.status === 'IMPLEMENTING' ? 'orange' : definition?.status === 'PREVIEW' ? 'blue' : definition?.status === 'PLANNED' ? 'default' : 'orange'}>
          {definition?.status === 'IMPLEMENTING' ? 'Đang tích hợp Stored Function' : definition?.status === 'PREVIEW' ? 'Preview kỹ thuật' : definition?.status === 'PLANNED' ? 'Chưa triển khai' : 'Đang kiểm tra trạng thái'}
        </Tag>
      </div>

      <Card title="Điều kiện báo cáo" className="report-filter-card">
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <div className="report-filter-item">
            <Text strong>Chọn báo cáo</Text>
            <Select
              showSearch
              optionFilterProp="label"
              aria-label="Chọn báo cáo"
              value={selectedCode}
              options={groups}
              onChange={code => {
                requestVersion.current += 1
                onSelectReport(code)
              }}
              style={{ width: '100%', maxWidth: 780 }}
            />
          </div>
          {catalogError && <Alert showIcon type="warning" message="Không lấy được danh mục từ backend" description={catalogError} />}
          {isRunnable ? (
            <div className="report-filter-grid">
              <div className="report-filter-item">
                <Text strong>Tháng báo cáo</Text>
                <DatePicker
                  picker="month"
                  value={dayjs(query.month + '-01')}
                  format="MM/YYYY"
                  allowClear={false}
                  onChange={date => {
                    setQuery(q => ({ ...q, month: date ? date.format('YYYY-MM') : currentMonth(), page: 1 }))
                    invalidateResult()
                  }}
                />
              </div>
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
              <div className="report-filter-item">
                <Text strong>Chế độ I.1</Text>
                <Segmented
                  value={query.previewMode ?? 'OFFICIAL'}
                  options={[
                    { label: 'Đối soát KIT (kỹ thuật)', value: 'AGENCY_KITS' },
                    ...(showPackagePreview ? [{ label: 'KIT theo đại lý × gói', value: 'PACKAGE_KITS' }] : []),
                    { label: 'Báo cáo tài chính (chưa nghiệm thu)', value: 'OFFICIAL' },
                  ]}
                  onChange={value => {
                    setQuery(q => ({ ...q, previewMode: value === 'OFFICIAL' ? undefined : value as 'AGENCY_KITS' | 'PACKAGE_KITS', page: 1 }))
                    invalidateResult()
                  }}
                />
                <Text type="secondary">KIT preview không phải số thuê bao đủ điều kiện hoặc tiền thưởng. Chế độ tài chính vẫn khóa đến khi kiểm chứng nguồn.</Text>
              </div>
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
          {result.notice && <Alert style={{ marginBottom: 16 }} type="warning" showIcon message={result.notice} />}
          <div className="report-result-metrics">
            <Space size="large" wrap>
              <div><Text type="secondary">Tổng số dòng</Text><div><Text strong>{formatCount(result.totalRows)}</Text></div></div>
              <div><Text type="secondary">Trang hiện tại</Text><div><Text strong>{result.page}</Text></div></div>
              <div><Text type="secondary">Kỳ đã chạy</Text><div><Text strong>{query.month}</Text></div></div>
            </Space>
            <Tag>Không tính tổng tài chính ở frontend</Tag>
          </div>
          <div className="report-table-toolbar">
            <Input
              prefix={<SearchOutlined />}
              placeholder="Tìm trong trang đã tải..."
              value={quickSearch}
              allowClear
              onChange={e => setQuickSearch(e.target.value)}
              style={{ width: 255, maxWidth: '100%' }}
              aria-label="Tìm nhanh trên trang hiện tại"
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
            Tìm nhanh và ẩn cột chỉ ảnh hưởng bảng đang xem; Excel vẫn xuất theo bộ lọc báo cáo, không theo ô tìm nhanh.
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
