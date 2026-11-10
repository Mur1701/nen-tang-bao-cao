import { useState } from 'react'
import { LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { Breadcrumb, Button, Input, Layout, Space, Tag, Typography } from 'antd'
import { REPORT_NAVIGATION, type ReportNavigationItem } from '../../features/reports/reportNavigation'
import { normalizeSearch } from '../../features/reports/reportFormat'

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

/** Sidebar is primary navigation; report lookup runs only when Enter/search is pressed. */
export default function AppHeader({ current, collapsed, onToggle, onNavigate, adminUsername, onLogout }: Props) {
  const [keyword, setKeyword] = useState('')
  const [matches, setMatches] = useState<ReportNavigationItem[] | null>(null)
  const selectedGroup = current?.group === 'REVENUE' ? 'Doanh thu' : 'Chi phí'
  function openReport(path: string) {
    setMatches(null)
    setKeyword('')
    onNavigate(path)
  }
  function searchReports(value: string) {
    const term = normalizeSearch(value)
    if (!term) { setMatches(null); return }
    const exact = REPORT_NAVIGATION.find(r => normalizeSearch(r.code) === term)
    if (exact) { openReport(exact.path); return }
    const found = REPORT_NAVIGATION.filter(r =>
      normalizeSearch(r.code + ' ' + r.title).includes(term))
    if (found.length === 1) { openReport(found[0].path); return }
    setMatches(found)
  }

  return (
    <Header className="app-header">
      <Space size="middle" className="app-header-main">
        <Button type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          aria-label={collapsed ? 'Mở menu' : 'Thu gọn menu'}
          onClick={onToggle}
        />
        <div className="app-header-label">
          <Text strong>Pottel Reporting System</Text>
          <Breadcrumb items={current ? [
            { title: 'Báo cáo' }, { title: selectedGroup },
            { title: current.code + ' — ' + current.title },
          ] : [{ title: 'Tổng quan' }]} />
        </div>
      </Space>
      <Space className="app-header-actions">
        <div className="app-report-search">
          <Input.Search
            aria-label="Tìm báo cáo bằng Enter"
            placeholder="Tìm mã hoặc tên báo cáo"
            value={keyword}
            allowClear
            enterButton="Tìm"
            onChange={e => { setKeyword(e.target.value); setMatches(null) }}
            onSearch={searchReports}
          />
          {matches !== null && (
            <div className="app-report-search-results" role="status">
              {matches.length ? matches.map(r => (
                <Button block type="text" key={r.code}
                  onClick={() => openReport(r.path)}>
                  {r.code} — {r.title}
                </Button>
              )) : <Text type="secondary">Không tìm thấy báo cáo phù hợp.</Text>}
            </div>
          )}
        </div>
        <Tag>Quản trị: {adminUsername}</Tag>
        <Button icon={<LogoutOutlined />} onClick={onLogout}>Đăng xuất</Button>
      </Space>
    </Header>
  )
}
