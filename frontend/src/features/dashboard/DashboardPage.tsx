import { ReloadOutlined } from '@ant-design/icons'
import { Button, Card, Col, Row, Space, Tag, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useBackendHealth } from './useBackendHealth'

const { Title, Paragraph, Text } = Typography

/** Functional page: visual layout composes the health hook and reusable Ant Design elements. */
export default function DashboardPage() {
  const navigate = useNavigate()
  const { health, checkHealth } = useBackendHealth()

  return (
    <div className="dashboard-layout">
      <Title level={3}>Tổng quan hệ thống báo cáo</Title>
      <Paragraph type="secondary">Phase 1 gồm 7 báo cáo chi phí và 5 báo cáo doanh thu. Trạng thái triển khai được cung cấp bởi backend, không phải số lượng mục trong menu.</Paragraph>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={8}>
          <Card title="Danh mục Phase 1"><Title level={2} style={{ marginBottom: 0 }}>12</Title><Text type="secondary">báo cáo trong phạm vi</Text></Card>
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <Card title="Phân nhóm"><Title level={2} style={{ marginBottom: 0 }}>7 / 5</Title><Text type="secondary">chi phí / doanh thu</Text></Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Kết nối backend">
            <Space wrap>
              <Tag color={health === 'up' ? 'success' : health === 'loading' ? 'processing' : 'error'}>
                {health === 'up' ? 'UP' : health === 'loading' ? 'Đang kiểm tra' : 'Không kết nối'}
              </Tag>
              <Button icon={<ReloadOutlined />} onClick={() => void checkHealth()} loading={health === 'loading'}>Kiểm tra lại</Button>
            </Space>
            <div style={{ marginTop: 12 }}><Text type="secondary">GET /api/health</Text></div>
          </Card>
        </Col>
      </Row>
      <Card title="Mở nhanh báo cáo" style={{ marginTop: 18 }}>
        <Space wrap>
          <Button type="primary" onClick={() => navigate('/reports/cost/i1')}>I.1 — Đối soát KIT kỹ thuật</Button>
          <Button onClick={() => navigate('/reports/revenue/ii1')}>II.1 — Phân bổ doanh thu</Button>
        </Space>
        <Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0 }}>
          I.1 có chế độ đối soát KIT kỹ thuật riêng (khi được bật local). Báo cáo tài chính chính thức chưa nghiệm thu; 11 báo cáo khác chưa triển khai.
        </Paragraph>
      </Card>
    </div>
  )
}
