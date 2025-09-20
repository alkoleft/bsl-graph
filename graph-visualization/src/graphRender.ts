import Graph from 'graphology'
import Sigma from 'sigma'
import forceAtlas2 from 'graphology-layout-forceatlas2'
import { createNodeImageProgram } from '@sigma/node-image'
import { GraphEdgeResponse, GraphNodeResponse, GraphSearchResponse } from './types'
import { getIconPath, getNodeIcon } from './icons'
import { generateNodeColor } from './colors'
import { renderEdgeLegend, groupEdgesByType } from './legend'
import { log } from './logger'

export function ensurePositions(g: Graph): void {
  g.forEachNode((n, attr) => {
    if (typeof attr.x !== 'number' || typeof attr.y !== 'number') {
      g.setNodeAttribute(n, 'x', Math.random())
      g.setNodeAttribute(n, 'y', Math.random())
    }
  })

  try {
    const settings = forceAtlas2.inferSettings(g)
    const compactSettings = {
      ...settings,
      gravity: 1.0,
      strongGravity: true,
      scalingRatio: 0.1,
      slowDown: 10,
      outboundAttractionDistribution: false,
      linLogMode: false,
      adjustSizes: false,
      edgeWeightInfluence: 0.1,
      iterationsPerRender: 1,
      maxIterations: 500,
      jitterTolerance: 0.1,
      delta: 0.1,
      epsilon: 0.1
    }
    forceAtlas2.assign(g, { iterations: 500, settings: compactSettings })
  } catch { }
}

export function fitCameraToGraph(sigma: Sigma, graph: Graph, container: HTMLElement): void {
  if (graph.order === 0) return
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  graph.forEachNode((nodeId, attributes) => {
    const x = (attributes as any).x as number
    const y = (attributes as any).y as number
    const size = ((attributes as any).size as number) || 6
    minX = Math.min(minX, x - size)
    maxX = Math.max(maxX, x + size)
    minY = Math.min(minY, y - size)
    maxY = Math.max(maxY, y + size)
  })
  const centerX = (minX + maxX) / 2
  const centerY = (minY + maxY) / 2
  const width = maxX - minX
  const height = maxY - minY
  const padding = Math.max(width, height) * 0.2
  const paddedWidth = width + padding * 2
  const paddedHeight = height + padding * 2
  const containerRect = container.getBoundingClientRect()
  const containerWidth = containerRect.width
  const containerHeight = containerRect.height
  const scaleX = containerWidth / paddedWidth
  const scaleY = containerHeight / paddedHeight
  const scale = Math.min(scaleX, scaleY, 1)
  const camera = sigma.getCamera()
  camera.animate({ x: centerX, y: centerY, ratio: 1 / scale }, { duration: 500 })
}

export function renderGraph(
  container: HTMLDivElement,
  treeContainer: HTMLDivElement,
  edgeLegendContainer: HTMLDivElement,
  data: GraphSearchResponse | { nodes: GraphNodeResponse[]; edges: GraphEdgeResponse[] },
  sigmaInstanceRef: { current: Sigma | null },
  graphRef: { current: Graph | null },
  initialData: { nodes: GraphNodeResponse[]; edges: GraphEdgeResponse[] } | null,
  rebuildTree: boolean,
  renderTreeFn: (nodes: GraphNodeResponse[]) => void
): void {
  log('Rendering graph with data:', data)
  log('Rebuild tree:', rebuildTree)
  if (sigmaInstanceRef.current) {
    // Очищаем холст перед уничтожением экземпляра
    const canvas = container.querySelector('canvas')
    if (canvas) {
      const ctx = canvas.getContext('2d')
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height)
      }
    }
    sigmaInstanceRef.current.kill()
    sigmaInstanceRef.current = null
  }

  // Дополнительная очистка контейнера от всех дочерних элементов
  while (container.firstChild) {
    container.removeChild(container.firstChild)
  }
  const g = new Graph()
  graphRef.current = g
  renderEdgeLegend(edgeLegendContainer, (data as any).edges || [])
  const edgeTypeColors = groupEdgesByType((data as any).edges || [])
  const nodeIds = new Set<string>()
  for (const n of (data as any).nodes || []) {
    const id = String(n.id)
    if (!nodeIds.has(id)) {
      nodeIds.add(id)
      const nodeColor = generateNodeColor(n.type, n.properties || {})
      const nodeIcon = getNodeIcon(n.type, n.properties || {})
      const iconPath = getIconPath(nodeIcon)
      log(`Node ${id} (type: ${n.type}): icon=${nodeIcon}, path=${iconPath}`)
      g.addNode(id, {
        entityType: n.type,
        properties: n.properties || {},
        labels: n.labels || [],
        label: (() => {
          const p = (n.properties || {}) as any
          const type = (p.type as string) || ''
          const synonym = (p.synonym as string) || (p.name as string) || ''
          const computed = [type, synonym].filter(Boolean).join(' ').trim()
          return computed || `${n.type}:${id}`
        })(),
        color: nodeColor,
        type: 'image',
        image: iconPath,
        size: 20
      })
    }
  }
  for (const e of (data as any).edges || []) {
    const s = String(e.sourceId)
    const t = String(e.targetId)
    if (nodeIds.has(s) && nodeIds.has(t)) {
      const key = `${s}->${t}:${e.type}`
      if (!g.hasEdge(key)) {
        const edgeColor = edgeTypeColors.get(e.type)?.color || '#c44e52'
        const edgeLabel = (e.properties && (e.properties as any).name as string) || e.type
        g.addDirectedEdgeWithKey(key, s, t, { relationType: e.type, properties: e.properties || {}, size: 1, color: edgeColor, label: edgeLabel, type: 'arrow' })
      }
    }
  }
  ensurePositions(g)

  sigmaInstanceRef.current = new Sigma(g, container, {
    renderEdgeLabels: true,
    edgeLabelSize: 12,
    edgeLabelColor: { attribute: 'color' },
    edgeLabelFont: 'Arial',
    edgeLabelWeight: 'bold',
    nodeProgramClasses: {
      image: createNodeImageProgram()
    }
  })
  // Принудительно обновляем отображение после создания
  setTimeout(() => {
    if (sigmaInstanceRef.current) {
      sigmaInstanceRef.current.refresh()
      log('Sigma instance refreshed')
    }
  }, 100)

  if (rebuildTree) {
    const treeSourceNodes = initialData?.nodes || ((data as any).nodes || [])
    renderTreeFn(treeSourceNodes)
  }
}


