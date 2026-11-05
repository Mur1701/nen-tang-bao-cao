import { useState } from 'react'
import { Layout } from 'antd'
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from 'react-router-dom'
import AppSidebar from './components/layout/AppSidebar'
import AppHeader from './components/layout/AppHeader'
import DashboardPage from './features/dashboard/DashboardPage'
import ReportsWorkspace from './features/reports/ReportsWorkspace'
import { REPORT_NAVIGATION, reportPath } from './features/reports/reportNavigation'

const { Content } = Layout

function HomeRoute() {
  const location = useLocation()
  if (new URLSearchParams(location.search).get('view') === 'reports') {
    return <Navigate to="/reports/cost/i1" replace />
  }
  return <DashboardPage />
}

function ReportRoute({ group }: { group: 'cost' | 'revenue' }) {
  const { reportSlug } = useParams()
  const navigate = useNavigate()
  const slug = reportSlug?.toLowerCase() ?? ''
  const valid = group === 'cost' ? /^i[1-7]$/.test(slug) : /^ii[1-5]$/.test(slug)
  if (!valid) {
    return <Navigate to={group === 'cost' ? '/reports/cost/i1' : '/reports/revenue/ii1'} replace />
  }
  const selectedCode = group === 'cost' ? `I.${slug.slice(1)}` : `II.${slug.slice(2)}`
  return <ReportsWorkspace selectedCode={selectedCode} onSelectReport={code => navigate(reportPath(code))} />
}

/** App shell and routing are shared by all 12 Phase 1 reports. */
function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const current = REPORT_NAVIGATION.find(r => r.path === location.pathname)
  const [collapsed, setCollapsed] = useState(false)

  return (
    <Layout className="app-shell" style={{ minHeight: '100vh' }}>
      <AppSidebar
        collapsed={collapsed}
        selectedPath={location.pathname}
        onBreakpoint={setCollapsed}
        onNavigate={navigate}
      />
      <Layout style={{ minWidth: 0 }}>
        <AppHeader
          current={current}
          collapsed={collapsed}
          onToggle={() => setCollapsed(value => !value)}
          onNavigate={navigate}
        />
        <Content className="app-content">
          <Routes>
            <Route path="/" element={<HomeRoute />} />
            <Route path="/reports/cost" element={<Navigate to="/reports/cost/i1" replace />} />
            <Route path="/reports/revenue" element={<Navigate to="/reports/revenue/ii1" replace />} />
            <Route path="/reports/cost/:reportSlug" element={<ReportRoute group="cost" />} />
            <Route path="/reports/revenue/:reportSlug" element={<ReportRoute group="revenue" />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  )
}

export default function App() {
  return <BrowserRouter><AppLayout /></BrowserRouter>
}
