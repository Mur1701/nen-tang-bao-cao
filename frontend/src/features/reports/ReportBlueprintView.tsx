import { Alert, Card, DatePicker, Empty, InputNumber, Space, Table, Tag, Typography } from 'antd'
import dayjs from 'dayjs'
import { REPORT_BLUEPRINTS } from './reportBlueprints'

const { Text, Paragraph } = Typography

interface Props {
  code: string
  month: string
  agencyId: number | null
  onMonthChange: (month: string) => void
  onAgencyChange: (agencyId: number | null) => void
}

/** Presentation-only draft. It must never call /run, fabricate rows or display an empty query as a financial result. */
export default function ReportBlueprintView({ code, month, agencyId, onMonthChange, onAgencyChange }: Props) {
  const blueprint = REPORT_BLUEPRINTS[code]
  if (!blueprint) {
    return <Alert type="warning" showIcon message="Chưa có đặc tả hiển thị cho báo cáo này" />
  }

  const columns = blueprint.columns.map((label, index) => ({
    key: `draft-${index}`,
    title: label,
    dataIndex: `draft-${index}`,
    width: Math.max(145, Math.min(330, label.length * 8 + 35)),
  }))

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert
        showIcon
        type="info"
        message="Biểu mẫu dự kiến — chưa có dữ liệu nghiệp vụ"
        description="Tên cột và bộ lọc được lấy từ SRS Phase 1 bản DRAFT. Báo cáo này chưa có truy vấn PostgreSQL/API/Excel đã kiểm chứng; không hiển thị số liệu giả."
      />
      <Space wrap size="middle" align="end">
        <div className="report-filter-item">
          <Text strong>Tháng báo cáo</Text>
          <DatePicker
            picker="month"
            value={dayjs(month + '-01')}
            format="MM/YYYY"
            allowClear={false}
            onChange={date => onMonthChange(date ? date.format('YYYY-MM') : dayjs().format('YYYY-MM'))}
          />
        </div>
        {blueprint.filters.includes('agencyId') && (
          <div className="report-filter-item">
            <Text strong>ID đại lý <Text type="secondary">(tạm thời)</Text></Text>
            <InputNumber
              min={1}
              placeholder="Tất cả đại lý"
              value={agencyId}
              onChange={onAgencyChange}
              style={{ width: 195 }}
            />
          </div>
        )}
        <Tag color="default">Chưa nối Stored Function</Tag>
      </Space>
      <Card title="Cấu trúc bảng dự kiến (DRAFT)" size="small">
        <Paragraph type="secondary" style={{ marginBottom: 8 }}>
          {blueprint.purpose}
        </Paragraph>
        <Table
          columns={columns}
          dataSource={[]}
          bordered
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="Chưa truy vấn dữ liệu — đây chỉ là thiết kế cột, không phải kết quả rỗng" /> }}
        />
        <Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0, fontSize: 12 }}>
          Nguồn: {blueprint.source}. Các nhóm cột theo ngày và VAT sẽ cần header nhiều tầng,
          schema API và dữ liệu đối soát trước khi nghiệm thu.
        </Paragraph>
      </Card>
    </Space>
  )
}
