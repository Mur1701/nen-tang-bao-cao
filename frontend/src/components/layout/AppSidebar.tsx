import { DashboardOutlined, DollarOutlined, FileTextOutlined, WalletOutlined } from '@ant-design/icons'
import { Layout, Menu } from 'antd'
import type { MenuProps } from 'antd'
import { REPORT_NAVIGATION } from '../../features/reports/reportNavigation'

const { Sider } = Layout

const navigationItems: MenuProps['items'] = [
  { key: '/', icon: <DashboardOutlined />, label: 'Tổng quan' },
  {
    key: 'cost-group', icon: <WalletOutlined />, label: 'Báo cáo chi phí (7)',
    children: REPORT_NAVIGATION.filter(r => r.group === 'COST').map(r => ({
      key: r.path, icon: <FileTextOutlined />, label: `${r.code} — ${r.title}`, title: `${r.code} — ${r.title}`,
    })),
  },
  {
    key: 'revenue-group', icon: <DollarOutlined />, label: 'Báo cáo doanh thu (5)',
    children: REPORT_NAVIGATION.filter(r => r.group === 'REVENUE').map(r => ({
      key: r.path, icon: <FileTextOutlined />, label: `${r.code} — ${r.title}`, title: `${r.code} — ${r.title}`,
    })),
  },
]

interface Props {
  collapsed: boolean
  selectedPath: string
  onBreakpoint: (isBelow: boolean) => void
  onNavigate: (path: string) => void
}

/** Pure functional sidebar. The router owns the selected path, not duplicated component state. */
export default function AppSidebar({ collapsed, selectedPath, onBreakpoint, onNavigate }: Props) {
  return (
    <Sider
      width={286}
      collapsedWidth={72}
      collapsible
      collapsed={collapsed}
      trigger={null}
      breakpoint="lg"
      onBreakpoint={onBreakpoint}
      theme="dark"
      className="app-sidebar"
    >
      <div className="app-logo" style={collapsed ? { padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' } : undefined}>
        {collapsed ? (
          <img
            src="/pottel-mascot.png"
            alt="Pottel mascot"
            width={52}
            height={52}
            style={{ objectFit: 'contain', display: 'block' }}
          />
        ) : (
          <>
            POTTEL
            <div className="app-logo-subtitle">Reporting System</div>
          </>
        )}
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[selectedPath]}
        defaultOpenKeys={['cost-group', 'revenue-group']}
        items={navigationItems}
        onClick={({ key }) => onNavigate(key)}
        className="app-navigation"
      />
    </Sider>
  )
}
