import { useCallback, useEffect, useState } from 'react'

type HealthStatus = 'loading' | 'up' | 'down'

/** Encapsulate API lifecycle; ignore aborted requests when dashboard unmounts. */
export function useBackendHealth() {
  const [health, setHealth] = useState<HealthStatus>('loading')

  const checkHealth = useCallback(async (signal?: AbortSignal) => {
    setHealth('loading')
    try {
      const response = await fetch('/api/health', { signal })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const data: { status?: string } = await response.json()
      if (!signal?.aborted) setHealth(data.status === 'UP' ? 'up' : 'down')
    } catch {
      if (!signal?.aborted) setHealth('down')
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void checkHealth(controller.signal)
    return () => controller.abort()
  }, [checkHealth])

  return { health, checkHealth }
}
