const predefinedColors = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
  '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9',
  '#F8C471', '#82E0AA', '#F1948A', '#85C1E9', '#D7BDE2'
]

const nodeTypeColorMap: Record<string, string> = {
  CONFIGURATION: '#455A64',
  CATALOG: '#4CAF50',
  DOCUMENT: '#FF9800',
  ENUM: '#9C27B0',
  CONSTANT: '#795548',
  REGISTER: '#009688',
  BUSINESS_PROCESS: '#EF6C00',
  TASK: '#D32F2F',
  COMMON_MODULE: '#3F51B5',
  COMMAND_GROUP: '#512DA8',
  COMMAND: '#7B1FA2',
  ATTRIBUTE: '#0288D1',
  DIMENSION: '#039BE5',
  RESOURCE: '#558B2F',
  FORM: '#00897B',
  TABLE: '#5D4037',
  QUERY: '#3949AB',
  REPORT: '#7E57C2',
  DATA_PROCESSOR: '#AB47BC',
  EXTERNAL_DATA_SOURCE: '#BF360C',
  EXCHANGE_PLAN: '#FBC02D',
  CHART_OF_ACCOUNTS: '#1565C0',
  CHART_OF_CHARACTERISTIC_TYPES: '#0277BD',
  CHART_OF_CALCULATION_TYPES: '#00838F',
  FILTER_CRITERION: '#546E7A',
  INFORMATION_REGISTER: '#26A69A',
  ACCUMULATION_REGISTER: '#00897B',
  ACCOUNTING_REGISTER: '#00695C',
  CALCULATION_REGISTER: '#00796B',
  DOCUMENT_JOURNAL: '#42A5F5',
  ROLE: '#C62828',
  SUBSYSTEM: '#00BCD4',
  LANGUAGE: '#8D6E63',
  STYLE_ITEM: '#EC407A',
  STYLE: '#D81B60',
  ACCOUNTING_FLAG: '#B71C1C',
  BOT: '#43A047',
  COLUMN: '#795548',
  COMMON_ATTRIBUTE: '#1976D2',
  COMMON_COMMAND: '#AD1457',
  COMMON_FORM: '#00ACC1',
  COMMON_PICTURE: '#90A4AE',
  COMMON_TEMPLATE: '#A1887F',
  DEFINED_TYPE: '#26C6DA',
  DOCUMENT_NUMERATOR: '#9E9D24',
  ENUM_VALUE: '#CE93D8',
  EVENT_SUBSCRIPTION: '#EF5350',
  EXTERNAL_DATA_PROCESSOR: '#6A1B9A',
  EXTERNAL_DATA_SOURCE_TABLE: '#D84315',
  EXTERNAL_DATA_SOURCE_TABLE_FIELD: '#FF7043',
  EXTERNAL_REPORT: '#8E24AA',
  EXT_DIMENSION_ACCOUNTING_FLAG: '#880E4F',
  FUNCTIONAL_OPTION: '#26A69A',
  FUNCTIONAL_OPTIONS_PARAMETER: '#80CBC4',
  HTTP_SERVICE: '#1E88E5',
  HTTP_SERVICE_METHOD: '#42A5F5',
  HTTP_SERVICE_URL_TEMPLATE: '#90CAF9',
  INTEGRATION_SERVICE: '#2E7D32',
  INTEGRATION_SERVICE_CHANNEL: '#43A047',
  INTERFACE: '#5C6BC0',
  PALETTE_COLOR: '#F06292',
  RECALCULATION: '#FB8C00',
  SCHEDULED_JOB: '#F9A825',
  SEQUENCE: '#F57C00',
  SESSION_PARAMETER: '#9E9E9E',
  SETTINGS_STORAGE: '#757575',
  STANDARD_ATTRIBUTE: '#1E88E5',
  STANDARD_TABULAR_SECTION: '#8D6E63',
  TABULAR_SECTION: '#6D4C41',
  TASK_ADDRESSING_ATTRIBUTE: '#E53935',
  TEMPLATE: '#B0BEC5',
  WEB_SERVICE: '#1976D2',
  WS_OPERATION: '#64B5F6',
  WS_OPERATION_PARAMETER: '#4FC3F7',
  WS_REFERENCE: '#81D4FA',
  XDTO_PACKAGE: '#0091EA',
  UNKNOWN: '#BDBDBD'
}

const colorCache = new Map<string, string>()

export function generateNodeColor(nodeType: string, properties: Record<string, unknown>): string {
  const explicitType = properties?.type as string
  const typeToUse = explicitType || nodeType || 'unknown'
  const normalizedType = String(typeToUse).trim().toUpperCase()
  const cached = colorCache.get(normalizedType)
  if (cached) return cached
  if (nodeTypeColorMap[normalizedType]) {
    const mapped = nodeTypeColorMap[normalizedType]
    colorCache.set(normalizedType, mapped)
    return mapped
  }
  let hash = 0
  for (let i = 0; i < normalizedType.length; i++) {
    const char = normalizedType.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }
  const colorIndex = Math.abs(hash) % predefinedColors.length
  const color = predefinedColors[colorIndex]
  colorCache.set(normalizedType, color)
  return color
}


