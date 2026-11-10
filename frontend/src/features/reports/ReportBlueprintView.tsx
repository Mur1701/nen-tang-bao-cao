import { useEffect, useMemo, useState } from 'react'
import { Alert, Card, DatePicker, Empty, InputNumber, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { REPORT_BLUEPRINTS } from './reportBlueprints'
import { getReportSchema, type ReportSchema } from './reportApi'

const { Text, Paragraph } = Typography

interface Props {
  code: string
  month: string
  agencyId: number | null
  onMonthChange: (month: string) => void
  onAgencyChange: (agencyId: number | null) => void
}

/**
 * A read-only SCHEMA preview. No financial execution, no fake rows and no
 * speculative calendar dates. Static local labels are an offline fallback.
 */
export default function ReportBlueprintView({ code, month, agencyId, onMonthChange, onAgencyChange }: Props) {
  const blueprint = REPORT_BLUEPRINTS[code]
  const [schema, setSchema] = useState<ReportSchema | null>(null)
  const [schemaError, setSchemaError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    setSchema(null)
    setSchemaError(null)
    getReportSchema(code).then(data => {
      if (active) setSchema(data)
    }).catch(error => {
      if (active) setSchemaError((error as Error).message)
    })
    return () => { active = false }
  }, [code])

  const tableColumns = useMemo((): ColumnsType<Record<string, unknown>> => {
    if (!schema) {
      return (blueprint?.columns ?? []).map((title, index) => ({
        key: `draft-${index}`,
        title,
        dataIndex: `draft-${index}`,
        width: 190,
      }))
    }
    const result: ColumnsType<Record<string, unknown>> = []
    let currentGroup: { title: string; children: ColumnsType<Record<string, unknown>> } | null = null
    for (const column of schema.columns) {
      const leaf = {
        key: column.key,
        dataIndex: column.key,
        title: column.title,
        width: 190,
        align: column.type === 'decimal' || column.type === 'integer' ? 'right' as const : 'left' as const,
      }
      if (column.group) {
        if (!currentGroup || currentGroup.title !== column.group) {
          currentGroup = { title: column.group, children: [] }
          result.push(currentGroup)
        }
        currentGroup.children.push(leaf)
      } else {
        currentGroup = null
        result.push(leaf)
      }
    }
    return result
  }, [blueprint, schema])

  if (!blueprint && !schema) {
    return <Alert type="warning" showIcon message="Chưa có đặc tả hiển thị cho báo cáo này" />
  }

  const filters = schema?.filters ?? blueprint?.filters ?? ['month']
  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Alert
        showIcon
        type="info"
        message="Hợp đồng biểu mẫu Phase 1 — chưa có dữ liệu nghiệp vụ"
        description="Các cột và nhóm ngày dựa trên SRS DRAFT. Chưa có Stored Function tài chính được kiểm chứng cho báo cáo này. Bảng trống là bản thiết kế, không phải kết quả chạy báo cáo."
      />
      {schemaError && <Alert type="warning" showIcon message="Không tải được schema từ backend; đang dùng đặc tả local" description={schemaError} />}
      {schema && (
        <Alert type="warning" showIcon message="Điều kiện còn thiếu để triển khai"
          description={schema.blocker} />
      )}
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
        {filters.includes('agencyId') && (
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
        <Tag>Chưa nối Stored Function đã kiểm chứng</Tag>
      </Space>
      <Card title="Hợp đồng cột báo cáo (DRAFT)" size="small">
        <Paragraph type="secondary">{blueprint?.purpose}</Paragraph>
        <Table<Record<string, unknown>>
          columns={tableColumns}
          dataSource={[]}
          bordered
          pagination={false}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: <Empty description="Bản thiết kế cột — không có số liệu giả" /> }}
        />
        {!!schema?.dynamicGroups.length && (
          <div style={{ marginTop: 16 }}>
            <Text strong>Nhóm cột động theo ngày (chờ nguồn lịch phân bổ):</Text>
            {schema.dynamicGroups.map(group => (
              <Paragraph key={group.key} style={{ marginTop: 8, marginBottom: 6 }}>
                <Text strong>{group.title}</Text>
                {' — '}{group.measures.join(' / ')}
                <Text type="secondary"> ({group.period === 'NEXT_MONTH' ? 'kỳ sau' : 'trong kỳ'}; chưa tạo ngày khi chưa có lịch phân bổ)</Text>
              </Paragraph>
            ))}
          </div>
        )}
        <Paragraph type="secondary" style={{ marginTop: 12, marginBottom: 0, fontSize: 12 }}>
          Nguồn: {schema?.source ?? blueprint?.source}. Ngày phân bổ, công thức tiền và giá trị thật chỉ được điền khi quy tắc và dữ liệu đã được kiểm chứng.
        </Paragraph>
      </Card>
    </Space>
  )
}
