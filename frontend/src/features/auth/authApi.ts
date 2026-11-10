import { apiUrl } from '../../config/api'

export interface AdminIdentity { username: string; role: 'ADMIN' }
interface CsrfResponse { token: string; headerName: string }

async function checked(response: Response): Promise<Response> {
  if (response.ok) return response
  if (response.status === 401) throw new Error('Chưa đăng nhập hoặc tài khoản admin không hợp lệ.')
  if (response.status === 403) throw new Error('Không có quyền hoặc phiên bảo mật đã hết hạn.')
  if (response.status === 429) throw new Error('Thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.')
  throw new Error(`Lỗi máy chủ (HTTP ${response.status}).`)
}

/** CSRF is obtained fresh before each POST, and is NEVER stored in localStorage. */
export async function csrfHeader(): Promise<Record<string, string>> {
  const response = await checked(await fetch(apiUrl('/api/auth/csrf'), {
    credentials: 'same-origin', cache: 'no-store',
  }))
  const csrf = await response.json() as CsrfResponse
  if (!csrf.token || !['X-XSRF-TOKEN', 'X-CSRF-TOKEN'].includes(csrf.headerName)) {
    throw new Error('Không nhận được mã xác thực CSRF.')
  }
  return { [csrf.headerName]: csrf.token }
}

export async function currentAdmin(): Promise<AdminIdentity | null> {
  const response = await fetch(apiUrl('/api/auth/me'), {
    credentials: 'same-origin', cache: 'no-store',
  })
  if (response.status === 401 || response.status === 403) return null
  await checked(response)
  const identity = await response.json() as AdminIdentity
  if (identity.role !== 'ADMIN' || !identity.username) return null
  return identity
}

export async function loginAdmin(username: string, password: string): Promise<void> {
  const form = new URLSearchParams({ username, password })
  await checked(await fetch(apiUrl('/api/auth/login'), {
    method: 'POST',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', ...(await csrfHeader()) },
    body: form.toString(),
  }))
}

export async function logoutAdmin(): Promise<void> {
  await checked(await fetch(apiUrl('/api/auth/logout'), {
    method: 'POST',
    credentials: 'same-origin',
    cache: 'no-store',
    headers: await csrfHeader(),
  }))
}
