import { LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { Breadcrumb, Button, Layout, Select, Space, Tag, Typography } from 'antd'
import { REPORT_NAVIGATION, type ReportNavigationItem } from '../../features/reports/reportNavigation'

const { Header } = Layout
const { Text } = Typography

interface Props {
  current?: ReportNavigationItem
  collapsed: boolean
  onToggle: () => void
  onNavigate: (path: string) => void
  adminUsername: string
  onLogout: () => void
}

/** Common header for dashboard and all 12 reports; never owns report data. */
export default function AppHeader({ current, collapsed, onToggle, onNavigate, adminUsername, onLogout }: Props) {
  const selectedGroup = current?.group === 'REVENUE' ? 'Doanh thu' : 'Chi phí'

  return (
    <Header className="app-header">
      <Space size="middle" className="app-header-main">
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          aria-label={collapsed ? 'Mở menu' : 'Thu gọn menu'}
          onClick={onToggle}
        />
        <div className="app-header-label">
          <Text strong>Pottel Reporting System</Text>
          <Breadcrumb items={current ? [
            { title: 'Báo cáo' },
            { title: selectedGroup },
            { title: `${current.code} — ${current.title}` },
          ] : [{ title: 'Tổng quan' }]} />
        </div>
      </Space>
      <Space className="app-header-actions">
        <Select
          showSearch
          allowClear
          value={null}
          optionFilterProp="label"
          placeholder="Tìm báo cáo..."
          aria-label="Tìm và mở báo cáo"
          style={{ width: 270 }}
          options={REPORT_NAVIGATION.map(r => ({ label: `${r.code} — ${r.title}`, value: r.path }))}
          onChange={path => { if (path) onNavigate(path) }}
        />
        <Tag>Admin: {adminUsername}</Tag>
        <Button icon={<LogoutOutlined />} onClick={onLogout}>Đăng xuất</Button>
      </Space>
    </Header>
  )
}
