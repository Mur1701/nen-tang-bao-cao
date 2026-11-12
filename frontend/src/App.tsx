import { useEffect, useState } from 'react'
import { Layout, Spin, Typography } from 'antd'
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
import { REPORT_NAVIGATION } from './features/reports/reportNavigation'
import LoginPage from './features/auth/LoginPage'
import { currentAdmin, logoutAdmin, type AdminIdentity } from './features/auth/authApi'

const { Content } = Layout

function HomeRoute({ adminUsername }: { adminUsername: string }) {
  const location = useLocation()
  if (new URLSearchParams(location.search).get('view') === 'reports') {
    return <Navigate to="/reports/cost/i1" replace />
  }
  return <DashboardPage adminUsername={adminUsername} />
}

function ReportRoute({ group }: { group: 'cost' | 'revenue' }) {
  const { reportSlug } = useParams()
  const slug = reportSlug?.toLowerCase() ?? ''
  const valid = group === 'cost' ? /^i[1-7]$/.test(slug) : /^ii[1-5]$/.test(slug)
  if (!valid) {
    return <Navigate to={group === 'cost' ? '/reports/cost/i1' : '/reports/revenue/ii1'} replace />
  }
  const selectedCode = group === 'cost' ? `I.${slug.slice(1)}` : `II.${slug.slice(2)}`
  return <ReportsWorkspace selectedCode={selectedCode} />
}

/** One shared React SPA for all 12 Phase 1 reports; auth wraps the same layout. */
function AppLayout({ admin, onLogout }: { admin: AdminIdentity; onLogout: () => Promise<void> }) {
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
          adminUsername={admin.username}
          onLogout={() => void onLogout()}
        />
        <Content className="app-content">
          <Routes>
            <Route path="/" element={<HomeRoute adminUsername={admin.username} />} />
            <Route path="/login" element={<Navigate to="/" replace />} />
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

function AuthenticatedApp() {
  const [admin, setAdmin] = useState<AdminIdentity | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    void currentAdmin().then(user => { if (active) setAdmin(user) })
      .catch(() => {
        if (active) { setError('Không kết nối được máy chủ xác thực.'); setAdmin(null) }
      })
    return () => { active = false }
  }, [])

  async function afterLogin() {
    const user = await currentAdmin()
    if (!user) throw new Error('Không xác minh được quyền admin sau đăng nhập.')
    setError(null)
    setAdmin(user)
  }
  async function signOut() {
    try {
      await logoutAdmin()
      setAdmin(null)
    } catch {
      setError('Không thể đăng xuất. Hãy đóng trình duyệt và liên hệ quản trị.')
    }
  }
  if (admin === undefined) {
    return <div className="pottel-login-page"><Spin size="large" tip="Đang kiểm tra phiên admin..."><div style={{ width: 240, height: 100 }} /></Spin></div>
  }
  if (!admin) return <>
    {error && <Typography.Text type="warning">{error}</Typography.Text>}
    <Routes>
      <Route path="/login" element={<LoginPage onLoggedIn={afterLogin} />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  </>
  return <AppLayout admin={admin} onLogout={signOut} />
}

export default function App() {
  return <BrowserRouter><AuthenticatedApp /></BrowserRouter>
}
