const configuredBaseUrl =
  (import.meta.env.VITE_API_BASE_URL ?? '').trim()

const baseUrl = configuredBaseUrl.replace(/\/+$/, '')

/**
 * Local: use Vite proxy when VITE_API_BASE_URL is empty.
 * Production: use the configured HTTPS backend origin.
 */
export function apiUrl(path: string): string {
  return `${baseUrl}${path}`
}