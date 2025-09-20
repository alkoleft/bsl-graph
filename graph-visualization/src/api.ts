export const baseUrl = 'http://localhost:8888/api/graph'

export const api = {
  stats: `${baseUrl}/stats`,
  nodes: (type?: string) => (type ? `${baseUrl}/nodes?type=${encodeURIComponent(type)}` : `${baseUrl}/nodes`),
  related: (id: string, depth = 1) => `${baseUrl}/related/${encodeURIComponent(id)}?depth=${depth}`,
  search: `${baseUrl}/search`
}

export async function fetchJson<T>(input: RequestInfo, init?: RequestInit): Promise<T> {
  const res = await fetch(input, init)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return (await res.json()) as T
}


