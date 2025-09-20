import { GraphNodeResponse } from './types'
import { log } from './logger'
import { getIconPath, getNodeIcon } from './icons'
import { generateNodeColor } from './colors'

let currentFocusedElement: HTMLElement | null = null

export function groupNodesByType(nodes: GraphNodeResponse[]): Map<string, GraphNodeResponse[]> {
  const grouped = new Map<string, GraphNodeResponse[]>()
  for (const node of nodes) {
    const type = node.type || 'unknown'
    if (!grouped.has(type)) grouped.set(type, [])
    grouped.get(type)!.push(node)
  }
  return new Map([...grouped.entries()].sort((a, b) => a[0].localeCompare(b[0])))
}

function getAllFocusableElements(container: HTMLDivElement): HTMLElement[] {
  return Array.from(container.querySelectorAll('.tree-node-header, .tree-child')) as HTMLElement[]
}

function focusElement(element: HTMLElement): void {
  if (currentFocusedElement) currentFocusedElement.classList.remove('focused')
  element.focus()
  element.classList.add('focused')
  currentFocusedElement = element
  element.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

function toggleNodeExpansion(header: HTMLElement): void {
  const children = header.parentElement?.querySelector('.tree-children') as HTMLElement
  const toggle = header.querySelector('.tree-toggle') as HTMLElement
  if (children) {
    const isCurrentlyExpanded = children.style.display === 'block'
    children.style.display = isCurrentlyExpanded ? 'none' : 'block'
    toggle.className = `tree-toggle ${isCurrentlyExpanded ? 'collapsed' : 'expanded'}`
    header.setAttribute('aria-expanded', (!isCurrentlyExpanded).toString())
  }
}

function handleKeyNavigation(container: HTMLDivElement, onTypeSelect: (type: string) => void, onNodeSelect: (nodeId: string, fallbackNode?: GraphNodeResponse) => void, currentTreeData: Map<string, GraphNodeResponse[]>, event: KeyboardEvent): void {
  if (!currentFocusedElement) return
  switch (event.key) {
    case 'ArrowDown': {
      event.preventDefault()
      const elements = getAllFocusableElements(container)
      const currentIndex = elements.indexOf(currentFocusedElement)
      const next = currentIndex < elements.length - 1 ? elements[currentIndex + 1] : null
      if (next) focusElement(next)
      break
    }
    case 'ArrowUp': {
      event.preventDefault()
      const elements = getAllFocusableElements(container)
      const currentIndex = elements.indexOf(currentFocusedElement)
      const prev = currentIndex > 0 ? elements[currentIndex - 1] : null
      if (prev) focusElement(prev)
      break
    }
    case 'ArrowRight': {
      event.preventDefault()
      if (currentFocusedElement.classList.contains('tree-node-header')) toggleNodeExpansion(currentFocusedElement)
      break
    }
    case 'ArrowLeft': {
      event.preventDefault()
      if (currentFocusedElement.classList.contains('tree-node-header')) toggleNodeExpansion(currentFocusedElement)
      break
    }
    case 'Enter':
    case ' ': {
      event.preventDefault()
      if (currentFocusedElement.classList.contains('tree-node-header')) {
        const type = currentFocusedElement.parentElement?.getAttribute('data-type')
        if (type) {
          document.querySelectorAll('.tree-node-header.active').forEach(el => el.classList.remove('active'))
          currentFocusedElement.classList.add('active')
          onTypeSelect(type)
        }
      } else if (currentFocusedElement.classList.contains('tree-child')) {
        const nodeId = currentFocusedElement.getAttribute('data-node-id')
        if (nodeId) {
          document.querySelectorAll('.tree-child.selected').forEach(el => el.classList.remove('selected'))
          currentFocusedElement.classList.add('selected')
          let fallbackNode: GraphNodeResponse | undefined
          for (const [, ns] of currentTreeData) {
            const found = ns.find((n: GraphNodeResponse) => n.id === nodeId)
            if (found) { fallbackNode = found; break }
          }
          onNodeSelect(nodeId, fallbackNode)
        }
      }
      break
    }
    case 'Escape': {
      event.preventDefault()
      document.querySelectorAll('.tree-node-header.active').forEach(el => el.classList.remove('active'))
      document.querySelectorAll('.tree-child.selected').forEach(el => el.classList.remove('selected'))
      break
    }
  }
}

export class TreeView {
  private container: HTMLDivElement
  private currentTreeData: Map<string, GraphNodeResponse[]> = new Map()
  private isKeyboardSetup: boolean = false
  onTypeSelect: (type: string) => void = () => {}
  onNodeSelect: (nodeId: string, fallbackNode?: GraphNodeResponse) => void = () => {}

  constructor(container: HTMLDivElement) {
    this.container = container
  }

  render(nodes: GraphNodeResponse[]): void {
    log('Rendering tree with nodes:', nodes)
    this.currentTreeData = groupNodesByType(nodes)
    this.container.innerHTML = ''
    for (const [type, typeNodes] of this.currentTreeData) {
      const nodeElement = document.createElement('div')
      nodeElement.className = 'tree-node'
      nodeElement.setAttribute('data-type', type)
      const header = document.createElement('div')
      header.className = 'tree-node-header'
      header.setAttribute('tabindex', '0')
      header.setAttribute('role', 'button')
      header.setAttribute('aria-expanded', 'false')
      const toggle = document.createElement('span')
      toggle.className = 'tree-toggle collapsed'
      // Legend: color + icon
      const legendColor = document.createElement('span')
      legendColor.className = 'tree-legend-color'
      legendColor.style.display = 'inline-block'
      legendColor.style.width = '10px'
      legendColor.style.height = '10px'
      legendColor.style.marginRight = '6px'
      legendColor.style.borderRadius = '2px'
      legendColor.style.backgroundColor = generateNodeColor(type, { type })
      const legendIcon = document.createElement('img')
      legendIcon.className = 'tree-legend-icon'
      legendIcon.src = getIconPath(getNodeIcon(type, { type }))
      legendIcon.alt = `${type}`
      legendIcon.width = 14
      legendIcon.height = 14
      legendIcon.style.marginRight = '6px'
      const label = document.createElement('span')
      label.textContent = type
      const count = document.createElement('span')
      count.className = 'node-count'
      count.textContent = typeNodes.length.toString()
      header.appendChild(toggle)
      header.appendChild(legendColor)
      header.appendChild(legendIcon)
      header.appendChild(label)
      header.appendChild(count)
      const children = document.createElement('div')
      children.className = 'tree-children'
      children.style.display = 'none'
      const sortedNodes = [...typeNodes].sort((a, b) => {
        const nameA = (a.properties?.name as string) || (a.properties?.synonym as string) || a.id
        const nameB = (b.properties?.name as string) || (b.properties?.synonym as string) || b.id
        return nameA.localeCompare(nameB)
      })
      for (const node of sortedNodes) {
        const childElement = document.createElement('div')
        childElement.className = 'tree-child'
        childElement.setAttribute('tabindex', '0')
        childElement.setAttribute('role', 'button')
        childElement.setAttribute('data-node-id', node.id)
        const nodeName = (node.properties?.name as string) || (node.properties?.synonym as string) || node.id
        childElement.textContent = nodeName
        childElement.title = `ID: ${node.id}\nType: ${node.type}`
        childElement.addEventListener('click', (e) => {
          e.stopPropagation()
          document.querySelectorAll('.tree-child.selected').forEach(el => el.classList.remove('selected'))
          childElement.classList.add('selected')
          this.onNodeSelect(node.id, node)
        })
        children.appendChild(childElement)
      }
      header.addEventListener('click', (e) => {
        const target = e.target as HTMLElement
        const clickedToggle = target === toggle
        const isCurrentlyExpanded = children.style.display === 'block'
        children.style.display = isCurrentlyExpanded ? 'none' : 'block'
        toggle.className = `tree-toggle ${isCurrentlyExpanded ? 'collapsed' : 'expanded'}`
        header.setAttribute('aria-expanded', (!isCurrentlyExpanded).toString())
        if (!clickedToggle) {
          document.querySelectorAll('.tree-node-header.active').forEach(el => el.classList.remove('active'))
          header.classList.add('active')
          this.onTypeSelect(type)
        }
      })
      nodeElement.appendChild(header)
      nodeElement.appendChild(children)
      this.container.appendChild(nodeElement)
    }
    if (!this.isKeyboardSetup) {
      this.container.addEventListener('keydown', (e) => handleKeyNavigation(this.container, (t) => this.onTypeSelect(t), (id, n) => this.onNodeSelect(id, n), this.currentTreeData, e))
      this.isKeyboardSetup = true
    }
    const firstElement = this.container.querySelector('.tree-node-header') as HTMLElement
    if (firstElement) focusElement(firstElement)
  }
}


