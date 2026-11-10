import type { ReportColumn } from './reportApi'

/** Display-only normalization. Never calculate financial values in React. */
const monthPattern = /^(\d{4})-(0[1-9]|1[0-2])$/
const datePattern = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?)?(Z|[+-]\d{2}:?\d{2})?$/
const numericPattern = /^([+-]?)(\d+)(?:\.(\d+))?$/

export function formatReportMonth(month: string): string {
  const match = monthPattern.exec(month)
  return match ? 'Tháng ' + Number(match[2]) + ' năm ' + match[1] : month
}

export function normalizeSearch(text: string): string {
  return text.normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D')
    .toLocaleLowerCase('vi-VN').trim()
}

/** Keep the exact numeric string from PostgreSQL. Do not convert money to float. */
export function formatViNumber(value: unknown): string {
  const raw = String(value).trim()
  const match = numericPattern.exec(raw)
  if (!match) return raw
  const sign = match[1], whole = match[2], fraction = match[3]
  return sign + whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.') +
    (fraction === undefined ? '' : ',' + fraction)
}

function validDate(year: number, month: number, day: number): boolean {
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year &&
    date.getUTCMonth() + 1 === month && date.getUTCDate() === day
}

/** With timezone: show Vietnam clock; without timezone: preserve database clock. */
export function formatViTemporal(value: unknown, type: 'date' | 'datetime'): string {
  const raw = String(value).trim()
  const match = datePattern.exec(raw)
  if (!match) return raw
  let [, year, month, day, hour, minute, second, offset] = match
  if (!validDate(Number(year), Number(month), Number(day))) return raw
  if (hour !== undefined && (Number(hour) > 23 || Number(minute) > 59 || Number(second ?? '0') > 59))
    return raw
  if (offset && hour !== undefined) {
    const instant = new Date(raw.replace(' ', 'T'))
    if (Number.isNaN(instant.getTime())) return raw
    const parts = new Intl.DateTimeFormat('en-GB', {
      timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit',
      day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(instant)
    const part = (key: string) => parts.find(p => p.type === key)?.value ?? ''
    year = part('year'); month = part('month'); day = part('day')
    hour = part('hour'); minute = part('minute'); second = part('second')
  }
  const date = day + '/' + month + '/' + year
  return type === 'datetime' && hour !== undefined
    ? date + ' ' + hour + ':' + minute + ':' + (second ?? '00') : date
}

export function formatGroupHeader(title: string): string {
  return datePattern.test(title) ? formatViTemporal(title, 'date') : title
}

export function formatReportValue(value: unknown, column: Pick<ReportColumn, 'type'>): string {
  if (value === null || value === undefined || value === '') return '—'
  if (column.type === 'decimal' || column.type === 'integer') return formatViNumber(value)
  if (column.type === 'date' || column.type === 'datetime') return formatViTemporal(value, column.type)
  return String(value)
}
