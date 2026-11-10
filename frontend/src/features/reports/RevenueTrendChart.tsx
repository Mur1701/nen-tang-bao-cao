import { Alert, Card, Empty, Typography } from 'antd'
import type { RevenueTrend, RevenueTrendPoint } from './reportApi'
import { formatReportMonth, formatViNumber } from './reportFormat'

const { Text } = Typography
const WIDTH = 720, HEIGHT = 285
const LEFT = 90, RIGHT = 28, TOP = 28, BOTTOM = 66
type Series = 'primary' | 'secondary'
const COLORS = ['#2563eb', '#08916d']

/** Visual-only use of JS Number; financial totals remain exact strings from SQL. */
export default function RevenueTrendChart({ trend }: { trend: RevenueTrend }) {
  const points = trend.points
  const series: { key: Series; name: string; color: string }[] = [
    { key: 'primary', name: trend.primaryLabel, color: COLORS[0] },
    ...(trend.secondaryLabel ? [{ key: 'secondary' as const, name: trend.secondaryLabel, color: COLORS[1] }] : []),
  ]
  const values = points.flatMap(p => series.map(s => p[s.key]))
    .filter((n): n is string => n != null)
    .map(Number).filter(Number.isFinite)
  if (!values.length) {
    return <Empty description="Chưa có số liệu doanh thu trong các kỳ được chọn" />
  }

  const low = Math.min(0, ...values)
  const high = Math.max(0, ...values)
  const range = high === low ? 1 : high - low
  const plotH = HEIGHT - TOP - BOTTOM, plotW = WIDTH - LEFT - RIGHT
  const x = (i: number) => LEFT + i * plotW / Math.max(points.length - 1, 1)
  const y = (v: number) => TOP + (high - v) / range * plotH
  const shortMoney = (value: number) =>
    new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 }).format(value)

  function paths(key: Series): string[] {
    const result: string[] = []
    let segment = ''
    for (let i = 0; i < points.length; i++) {
      const val = points[i][key], numeric = val == null ? NaN : Number(val)
      if (!Number.isFinite(numeric)) {
        if (segment) result.push(segment)
        segment = ''
        continue
      }
      segment += (segment ? ' L ' : 'M ') + x(i).toFixed(1) + ' ' + y(numeric).toFixed(1)
    }
    if (segment) result.push(segment)
    return result
  }

  return (
    <Card size="small" title="Xu hướng doanh thu theo tháng"
      extra={<Text type="secondary">Đơn vị: đồng</Text>}>
      <Alert type="info" showIcon style={{ marginBottom: 12 }}
        message="Biểu đồ tổng hợp toàn bộ đại lý theo từng tháng, không phụ thuộc phân trang của bảng." />
      <svg viewBox={'0 0 ' + WIDTH + ' ' + HEIGHT} role="img"
        aria-label="Biểu đồ xu hướng doanh thu qua sáu tháng"
        style={{ width: '100%', height: 'auto', maxHeight: 350, display: 'block' }}>
        {[0, 1, 2, 3, 4].map(i => {
          const value = low + (high - low) * i / 4
          return <g key={i}>
            <line x1={LEFT} x2={WIDTH - RIGHT} y1={y(value)} y2={y(value)}
              stroke="currentColor" strokeOpacity={0.09} />
            <text x={LEFT - 12} y={y(value) + 4} fontSize={12} textAnchor="end"
              fill="currentColor">{shortMoney(value)}</text>
          </g>
        })}
        {points.map((p, i) => <text key={p.month} x={x(i)} y={HEIGHT - BOTTOM + 27}
          textAnchor="middle" fontSize={12} fill="currentColor">
          {p.month.slice(5) + '/' + p.month.slice(0, 4)}
        </text>)}
        {series.map(s => <g key={s.key}>
          {paths(s.key).map((path, index) =>
            <path key={index} d={path} stroke={s.color} strokeWidth={2.5}
              fill="none" strokeLinecap="round" strokeLinejoin="round" />)}
          {points.map((p, i) => {
            const raw = p[s.key]
            if (raw == null || !Number.isFinite(Number(raw))) return null
            return <circle key={p.month} cx={x(i)} cy={y(Number(raw))} r={4.5}
              fill={s.color} stroke="white" strokeWidth={1.5}>
              <title>{s.name + ' — ' + formatReportMonth(p.month) + ': ' + formatViNumber(raw) + ' đồng'}</title>
            </circle>
          })}
        </g>)}
      </svg>
      <div className="revenue-trend-legend">
        {series.map(s => <span key={s.key}><i style={{ background: s.color }} />{s.name}</span>)}
      </div>
      <details style={{ marginTop: 12 }}>
        <summary>Đối chiếu số liệu trên biểu đồ</summary>
        <div style={{ overflowX: 'auto', marginTop: 8 }}>
          <table className="revenue-trend-table">
            <thead><tr><th>Tháng</th><th>{trend.primaryLabel}</th>{trend.secondaryLabel && <th>{trend.secondaryLabel}</th>}</tr></thead>
            <tbody>{points.map(p => <tr key={p.month}>
              <td>{formatReportMonth(p.month)}</td>
              <td>{p.primary == null ? '—' : formatViNumber(p.primary)}</td>
              {trend.secondaryLabel && <td>{p.secondary == null ? '—' : formatViNumber(p.secondary)}</td>}
            </tr>)}</tbody>
          </table>
        </div>
      </details>
    </Card>
  )
}
