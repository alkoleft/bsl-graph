export type GraphNodeResponse = {
  id: string
  type: string
  properties: Record<string, unknown>
  labels: string[]
}

export type GraphEdgeResponse = {
  sourceId: string
  targetId: string
  type: string
  properties: Record<string, unknown>
}

export type GraphSearchResponse = {
  nodes: GraphNodeResponse[]
  edges: GraphEdgeResponse[]
  totalCount: number
}


