import { GraphEdgeResponse } from './types'

export function groupEdgesByType(edges: GraphEdgeResponse[]): Map<string, { count: number; color: string }> {
  const grouped = new Map<string, { count: number; color: string }>()
  const edgeColors = ['#1F77B4','#FF7F0E','#2CA02C','#D62728','#9467BD','#8C564B','#E377C2','#7F7F7F','#BCBD22','#17BECF','#2B8CBE','#6A3D9A','#00695C','#B22222','#225EA8','#238B45']
  for (const edge of edges) {
    const type = edge.type || 'unknown'
    if (!grouped.has(type)) {
      let hash = 0
      for (let i = 0; i < type.length; i++) {
        const char = type.charCodeAt(i)
        hash = ((hash << 5) - hash) + char
        hash = hash & hash
      }
      const colorIndex = Math.abs(hash) % edgeColors.length
      const color = edgeColors[colorIndex]
      grouped.set(type, { count: 0, color })
    }
    grouped.get(type)!.count++
  }
  return grouped
}

export function renderEdgeLegend(edgeLegendContainer: HTMLDivElement, edges: GraphEdgeResponse[]): void {
  const edgeTypes = groupEdgesByType(edges)
  edgeLegendContainer.innerHTML = ''
  const sortedTypes = [...edgeTypes.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  for (const [type, data] of sortedTypes) {
    const legendItem = document.createElement('div')
    legendItem.className = 'edge-legend-item'
    const colorBox = document.createElement('div')
    colorBox.className = 'edge-legend-color'
    colorBox.style.backgroundColor = data.color
    const label = document.createElement('div')
    label.className = 'edge-legend-label'
    label.textContent = type
    const count = document.createElement('div')
    count.className = 'edge-legend-count'
    count.textContent = data.count.toString()
    legendItem.appendChild(colorBox)
    legendItem.appendChild(label)
    legendItem.appendChild(count)
    edgeLegendContainer.appendChild(legendItem)
  }
}


