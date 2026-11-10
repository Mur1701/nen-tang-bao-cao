/**
 * Authentication uses same-origin session + CSRF cookies.
 * Never call a cross-origin Render URL directly from the browser:
 * - Local development: Vite proxies /api to 127.0.0.1:8081.
 * - Vercel: vercel.json rewrites /api/* to the Render backend.
 * Ignore legacy VITE_API_BASE_URL to avoid cross-origin credential failures.
 */
export function apiUrl(path: string): string {
  if (!path.startsWith('/api/')) throw new Error('Only same-origin API endpoints are allowed')
  return path
}
