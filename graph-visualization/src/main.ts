import Graph from 'graphology'
import Sigma from 'sigma'
import { GraphEdgeResponse, GraphNodeResponse, GraphSearchResponse } from './types'
import { setDebugMode, log } from './logger'
import { api, fetchJson } from './api'
import { renderGraph, fitCameraToGraph } from './graphRender'
import { TreeView } from './tree'

setDebugMode(true)

const statsEl = document.getElementById('stats') as HTMLSpanElement
const typeInput = document.getElementById('typeInput') as HTMLInputElement
const loadBtn = document.getElementById('loadBtn') as HTMLButtonElement
const resetBtn = document.getElementById('resetBtn') as HTMLButtonElement
const fitCameraBtn = document.getElementById('fitCameraBtn') as HTMLButtonElement
const container = document.getElementById('container') as HTMLDivElement
const treeContainer = document.getElementById('tree-container') as HTMLDivElement
const edgeLegendContainer = document.getElementById('edge-legend-container') as HTMLDivElement

let sigmaInstance: Sigma | null = null
let graph: Graph | null = null
let initialData: { nodes: GraphNodeResponse[]; edges: GraphEdgeResponse[] } | null = null
let isLoading = false
let isInitialized = false
const treeView = new TreeView(treeContainer)

async function loadStats(): Promise<void> {
  try {
    const s = await fetchJson<{ totalNodes: number; totalEdges: number }>(api.stats)
    statsEl.textContent = `Nodes: ${s.totalNodes}  Edges: ${s.totalEdges}`
  } catch {
    statsEl.textContent = 'Stats unavailable'
  }
}

function renderGraphFacade(data: GraphSearchResponse | { nodes: GraphNodeResponse[]; edges: GraphEdgeResponse[] }, rebuildTree: boolean = true): void {
  renderGraph(
    container,
    treeContainer,
    edgeLegendContainer,
    data,
    { current: sigmaInstance },
    { current: graph },
    initialData,
    rebuildTree,
    (nodes) => {
      treeView.onTypeSelect = (type) => rerenderGraphForType(type, /*rebuildTree*/ false)
      treeView.onNodeSelect = (nodeId, fallbackNode) => rerenderGraphForNode(nodeId, 1, /*rebuildTree*/ false, fallbackNode)
      treeView.render(nodes)
    }
  )
}

function resetGraphFilter(): void {
  if (!graph || !sigmaInstance) return
  graph.forEachNode((nodeId) => { graph!.setNodeAttribute(nodeId, 'hidden', false) })
  graph.forEachEdge((edgeId) => { graph!.setEdgeAttribute(edgeId, 'hidden', false) })
  document.querySelectorAll('.tree-node-header.active').forEach(el => el.classList.remove('active'))
  document.querySelectorAll('.tree-child.selected').forEach(el => el.classList.remove('selected'))
  sigmaInstance.refresh()
}

async function rerenderGraphForType(type: string, rebuildTree: boolean = false): Promise<void> {
  try {
    const data = await fetchJson<GraphSearchResponse>(api.search, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nodeTypes: [type], limit: 9999 })
    })
    const nodes = data.nodes || []
    if (nodes.length > 0) {
      renderGraphFacade(data, rebuildTree)
      return
    }
    const fallbackNodes = (initialData?.nodes || []).filter(n => n.type === type)
    const nodeIdSet = new Set(fallbackNodes.map(n => String(n.id)))
    const fallbackEdges: GraphEdgeResponse[] = (initialData?.edges || []).filter(e => nodeIdSet.has(String(e.sourceId)) && nodeIdSet.has(String(e.targetId)))
    renderGraphFacade({ nodes: fallbackNodes, edges: fallbackEdges }, rebuildTree)
  } catch (e) {
    console.error('Failed to rerender by type', e)
  }
}

async function rerenderGraphForNode(
  nodeId: string,
  depth: number = 1,
  rebuildTree: boolean = false,
  fallbackNode?: GraphNodeResponse
): Promise<void> {
  try {
    const uidToSearch = (fallbackNode?.properties && (fallbackNode.properties as any).uid as string) || nodeId
    const nodeSearch = await fetchJson<GraphSearchResponse>(api.search, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ properties: { uid: uidToSearch }, limit: 1 })
    }).catch(() => ({ nodes: [], edges: [], totalCount: 0 } as GraphSearchResponse))
    const mainNode = nodeSearch.nodes?.[0] || fallbackNode
    const relatedResponse = await fetchJson<GraphSearchResponse>(api.related(nodeId, depth)).catch(() => ({ nodes: [], edges: [], totalCount: 0 } as GraphSearchResponse))
    const allNodes: GraphNodeResponse[] = []
    if (mainNode) allNodes.push(mainNode)
    for (const n of relatedResponse.nodes || []) allNodes.push(n)
    const allEdges: GraphEdgeResponse[] = relatedResponse.edges || []
    renderGraphFacade({ 
      nodes: allNodes.length > 0 && mainNode ? allNodes : (fallbackNode ? [fallbackNode] : []), 
      edges: allEdges 
    }, rebuildTree)
  } catch (e) {
    console.error('Failed to rerender by node', e)
  }
}

async function loadInitial(): Promise<void> {
  if (isLoading) { log('Already loading, skipping...'); return }
  if (isInitialized) { log('Already initialized, skipping...'); return }
  log('Loading initial data...')
  isLoading = true
  try {
    await loadStats()
    try {
      log('Fetching data from search API...')
      const data = await fetchJson<GraphSearchResponse>(api.search, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ limit: 9999 })
      })
      log('Search API response:', data)
      if (data.nodes && data.nodes.length > 0) {
        initialData = { nodes: data.nodes || [], edges: data.edges || [] }
        log('Initial data set:', initialData)
        renderGraphFacade(data, true)
        isInitialized = true
        log('Successfully loaded and rendered initial data')
        return
      } else {
        log('Search API returned empty data, trying nodes API...')
      }
    } catch(e) { log('Search API failed, trying nodes API:', e) }
    try {
      const nodes = await fetchJson<GraphNodeResponse[]>(api.nodes())
      log('Nodes API response:', nodes)
      if (nodes && nodes.length > 0) {
        initialData = { nodes, edges: [] }
        renderGraphFacade({ nodes, edges: [] }, true)
        isInitialized = true
        log('Successfully loaded and rendered nodes data')
        return
      } else { log('Nodes API returned empty data') }
    } catch(e) { log('Nodes API failed:', e) }
    log('All APIs failed or returned empty data, using empty data')
    initialData = { nodes: [], edges: [] }
    renderGraphFacade({ nodes: [], edges: [] }, true)
    isInitialized = true
  } finally {
    isLoading = false
  }
}

// Инициализация обработчиков событий после загрузки DOM
document.addEventListener('DOMContentLoaded', () => {
  log('DOM loaded, setting up event listeners')
  loadBtn.addEventListener('click', async () => {
    log('Load button clicked')
    const t = (typeInput.value || '').trim()
    if (!t) {
      if (!isInitialized) { log('Loading initial data...'); await loadInitial() } else { log('Already initialized, skipping load') }
      return
    }
    try {
      log('Loading filtered data for type:', t)
      const data = await fetchJson<GraphSearchResponse>(api.search, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ nodeTypes: [t], limit: 9999 })
      })
      initialData = { nodes: data.nodes || [], edges: data.edges || [] }
      renderGraphFacade(data, true)
    } catch (e) { console.error('Error loading filtered data:', e) }
    await loadStats()
  })

  resetBtn.addEventListener('click', async () => {
    log('Reset button clicked')
    typeInput.value = ''
    resetGraphFilter()
    isInitialized = false
    await loadInitial()
  })

  fitCameraBtn.addEventListener('click', () => { if (sigmaInstance && graph) fitCameraToGraph(sigmaInstance, graph, container) })
  log('Auto-loading initial data...')
  loadInitial()
  container.addEventListener('dblclick', () => { if (sigmaInstance && graph) fitCameraToGraph(sigmaInstance, graph, container) })
}) 
