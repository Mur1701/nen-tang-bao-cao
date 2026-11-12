import { useEffect, useMemo, useState } from 'react'
import {
  ArrowRightOutlined,
  CalendarOutlined,
  CheckCircleFilled,
  CloudServerOutlined,
  DatabaseOutlined,
  DollarOutlined,
  FileTextOutlined,
  ReloadOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import { Button } from 'antd'
import { useNavigate } from 'react-router-dom'
import { listReports, type ReportDefinition, type ReportStatus } from '../reports/reportApi'
import { REPORT_NAVIGATION, type ReportNavigationItem } from '../reports/reportNavigation'
import { useBackendHealth } from './useBackendHealth'
import './DashboardPage.css'

type CatalogState = 'loading' | 'ready' | 'error'

const STATUS_LABELS: Record<ReportStatus, string> = {
  REFERENCE: 'Tham chiếu',
  PREVIEW: 'Xem trước',
  IMPLEMENTING: 'Đang triển khai',
  PLANNED: 'Kế hoạch',
}

function statusLabel(status: ReportStatus | undefined) {
  return status ? STATUS_LABELS[status] : 'Chưa xác định'
}

function DashboardReport({
  report,
  definition,
  loading,
  onOpen,
}: {
  report: ReportNavigationItem
  definition?: ReportDefinition
  loading: boolean
  onOpen: (path: string) => void
}) {
  const status = definition?.status
  return (
    <button
      type="button"
      className="pottel-dashboard-report"
      onClick={() => onOpen(report.path)}
      aria-label={`Mở báo cáo ${report.code}: ${report.title}`}
    >
      <span className="pottel-dashboard-report-code">{report.code}</span>
      <span className="pottel-dashboard-report-detail">
        <span className="pottel-dashboard-report-title">{report.title}</span>
        <span className="pottel-dashboard-report-subtitle">
          {loading ? 'Đang tải trạng thái…' : statusLabel(status)}
        </span>
      </span>
      <ArrowRightOutlined className="pottel-dashboard-report-arrow" aria-hidden="true" />
    </button>
  )
}

/**
 * Homepage modeled on the old BSS welcome + grouped report directory.
 * All numbers here are Phase 1 scope counts; never substitute legacy financial KPIs.
 * The backend catalog supplies implementation status, and /api/health supplies API health.
 */
export default function DashboardPage({ adminUsername }: { adminUsername: string }) {
  const navigate = useNavigate()
  const { health, checkHealth } = useBackendHealth()
  const [catalog, setCatalog] = useState<ReportDefinition[]>([])
  const [catalogState, setCatalogState] = useState<CatalogState>('loading')
  const [catalogReload, setCatalogReload] = useState(0)

  useEffect(() => {
    let active = true
    listReports().then(items => {
      if (!active) return
      setCatalog(items)
      setCatalogState('ready')
    }).catch(() => {
      if (active) setCatalogState('error')
    })
    return () => { active = false }
  }, [catalogReload])

  const byCode = useMemo(() => new Map(catalog.map(item => [item.code, item])), [catalog])
  const costReports = REPORT_NAVIGATION.filter(item => item.group === 'COST')
  const revenueReports = REPORT_NAVIGATION.filter(item => item.group === 'REVENUE')
  const today = new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh',
  }).format(new Date())
  const healthLabel = health === 'up' ? 'Đang hoạt động' : health === 'loading' ? 'Đang kiểm tra' : 'Không phản hồi'

  return (
    <main className="pottel-dashboard" aria-label="Tổng quan hệ thống báo cáo">
      <section className="pottel-dashboard-hero" aria-labelledby="pottel-dashboard-welcome">
        <div className="pottel-dashboard-hero-grid" aria-hidden="true" />
        <div className="pottel-dashboard-hero-copy">
          <div className="pottel-dashboard-eyebrow"><span className="pottel-dashboard-gold-dot" /> POTTEL · HỆ THỐNG BÁO CÁO</div>
          <p className="pottel-dashboard-date"><CalendarOutlined aria-hidden="true" /> {today}</p>
          <h1 id="pottel-dashboard-welcome">Chào <span>{adminUsername || 'quản trị viên'}</span>,<br />chào mừng quay lại!</h1>
          <p className="pottel-dashboard-hero-description">
            Không gian làm việc tập trung cho báo cáo chi phí và doanh thu Phase 1.
            Chọn báo cáo, thiết lập kỳ và đối chiếu kết quả trực tiếp trên hệ thống.
          </p>
          <div className="pottel-dashboard-hero-actions">
            <Button type="primary" size="large" onClick={() => navigate('/reports/cost/i1')}>
              Mở báo cáo I.1 <ArrowRightOutlined />
            </Button>
            <Button size="large" className="pottel-dashboard-hero-secondary" onClick={() => navigate('/reports/revenue/ii1')}>
              Nhóm doanh thu
            </Button>
          </div>
        </div>
        <div className="pottel-dashboard-hero-art" aria-hidden="true">
          <div className="pottel-dashboard-sim pottel-dashboard-sim-one"><span /></div>
          <div className="pottel-dashboard-sim pottel-dashboard-sim-two"><span /></div>
          <div className="pottel-dashboard-hero-halo" />
          <img src="/pottel-mascot.png" alt="" className="pottel-dashboard-mascot" />
        </div>
      </section>

      <section aria-labelledby="pottel-dashboard-overview" className="pottel-dashboard-section">
        <div className="pottel-dashboard-section-title">
          <div>
            <div className="pottel-dashboard-section-kicker">TỔNG QUAN</div>
            <h2 id="pottel-dashboard-overview">Phạm vi báo cáo</h2>
          </div>
          <span className="pottel-dashboard-section-hint">Danh mục Phase 1, không phải số liệu doanh thu thực tế</span>
        </div>
        <div className="pottel-dashboard-metrics">
          <div className="pottel-dashboard-metric">
            <span className="pottel-dashboard-metric-icon is-blue"><FileTextOutlined /></span>
            <span className="pottel-dashboard-metric-label">Tổng số báo cáo</span>
            <div className="pottel-dashboard-metric-value">12 <span>báo cáo</span></div>
            <span className="pottel-dashboard-metric-footnote">Trong phạm vi Phase 1</span>
          </div>
          <div className="pottel-dashboard-metric">
            <span className="pottel-dashboard-metric-icon is-gold"><WalletOutlined /></span>
            <span className="pottel-dashboard-metric-label">Nhóm chi phí</span>
            <div className="pottel-dashboard-metric-value">07 <span>báo cáo</span></div>
            <span className="pottel-dashboard-metric-footnote">Từ I.1 đến I.7</span>
          </div>
          <div className="pottel-dashboard-metric">
            <span className="pottel-dashboard-metric-icon is-indigo"><DollarOutlined /></span>
            <span className="pottel-dashboard-metric-label">Nhóm doanh thu</span>
            <div className="pottel-dashboard-metric-value">05 <span>báo cáo</span></div>
            <span className="pottel-dashboard-metric-footnote">Từ II.1 đến II.5</span>
          </div>
          <div className="pottel-dashboard-metric pottel-dashboard-metric-health">
            <span className="pottel-dashboard-metric-icon is-green"><CloudServerOutlined /></span>
            <span className="pottel-dashboard-metric-label">Kết nối API</span>
            <div className={`pottel-dashboard-health-value is-${health}`}>
              <span className="pottel-dashboard-health-indicator" /> {healthLabel}
            </div>
            <Button type="link" size="small" icon={<ReloadOutlined />} loading={health === 'loading'} onClick={() => void checkHealth()}>
              Kiểm tra lại
            </Button>
          </div>
        </div>
      </section>

      <section aria-labelledby="pottel-dashboard-catalog" className="pottel-dashboard-section">
        <div className="pottel-dashboard-section-title">
          <div>
            <div className="pottel-dashboard-section-kicker">ĐIỀU HƯỚNG NHANH</div>
            <h2 id="pottel-dashboard-catalog">Danh mục báo cáo VNPOST</h2>
          </div>
          <span className="pottel-dashboard-section-hint">Chọn một báo cáo để xem bộ lọc và dữ liệu</span>
        </div>
        {catalogState === 'error' && (
          <div className="pottel-dashboard-catalog-warning" role="status">
            Chưa tải được trạng thái báo cáo từ API. Danh mục điều hướng vẫn sử dụng được.
            <Button size="small" type="link" onClick={() => { setCatalogState('loading'); setCatalogReload(n => n + 1) }}>Thử lại</Button>
          </div>
        )}
        <div className="pottel-dashboard-catalog">
          <div className="pottel-dashboard-catalog-card">
            <div className="pottel-dashboard-catalog-head">
              <span className="pottel-dashboard-catalog-icon is-cost"><WalletOutlined /></span>
              <div><span className="pottel-dashboard-catalog-overline">NHÓM I · 07 BÁO CÁO</span><h3>Báo cáo chi phí</h3></div>
            </div>
            <div className="pottel-dashboard-catalog-list">
              {costReports.map(report => <DashboardReport key={report.code} report={report} definition={byCode.get(report.code)} loading={catalogState === 'loading'} onOpen={navigate} />)}
            </div>
          </div>
          <div className="pottel-dashboard-catalog-card">
            <div className="pottel-dashboard-catalog-head">
              <span className="pottel-dashboard-catalog-icon is-revenue"><DollarOutlined /></span>
              <div><span className="pottel-dashboard-catalog-overline">NHÓM II · 05 BÁO CÁO</span><h3>Báo cáo doanh thu</h3></div>
            </div>
            <div className="pottel-dashboard-catalog-list">
              {revenueReports.map(report => <DashboardReport key={report.code} report={report} definition={byCode.get(report.code)} loading={catalogState === 'loading'} onOpen={navigate} />)}
            </div>
          </div>
        </div>
      </section>

      <section className="pottel-dashboard-bottom" aria-label="Thông tin kết nối và dữ liệu">
        <div className="pottel-dashboard-bottom-icon"><DatabaseOutlined /></div>
        <div className="pottel-dashboard-bottom-copy">
          <h2>Dữ liệu &amp; đối soát</h2>
          <p>Chỉ số vận hành, mốc CDR và lũy kế như trên BSS cũ cần API tổng hợp riêng. Dashboard này không tự suy ra doanh thu hoặc sản lượng từ danh mục báo cáo.</p>
        </div>
        <div className="pottel-dashboard-bottom-status">
          <CheckCircleFilled aria-hidden="true" /> Báo cáo chi tiết và Excel ở từng màn hình
        </div>
      </section>
      <p className="pottel-dashboard-footer">POTTEL REPORTING SYSTEM <span>·</span> Giai đoạn 1 <span>·</span> Dữ liệu nghiệp vụ lấy từ các báo cáo được kiểm chứng</p>
    </main>
  )
}