const nodeTypeIconMap: Record<string, string> = {
  CONFIGURATION: 'folder',
  CATALOG: 'catalog',
  DOCUMENT: 'document',
  ENUM: 'enum',
  CONSTANT: 'constant',
  REGISTER: 'informationRegister',
  BUSINESS_PROCESS: 'businessProcess',
  TASK: 'task',
  COMMON_MODULE: 'commonModule',
  COMMAND_GROUP: 'command',
  COMMAND: 'command',
  ATTRIBUTE: 'attribute',
  DIMENSION: 'dimension',
  RESOURCE: 'resource',
  FORM: 'form',
  TABLE: 'tabularSection',
  QUERY: 'operation',
  REPORT: 'report',
  DATA_PROCESSOR: 'dataProcessor',
  EXTERNAL_DATA_SOURCE: 'externalDataSource',
  EXCHANGE_PLAN: 'exchangePlan',
  CHART_OF_ACCOUNTS: 'chartsOfAccount',
  CHART_OF_CHARACTERISTIC_TYPES: 'chartsOfCharacteristicType',
  CHART_OF_CALCULATION_TYPES: 'chartsOfCalculationType',
  FILTER_CRITERION: 'filterCriteria',
  INFORMATION_REGISTER: 'informationRegister',
  ACCUMULATION_REGISTER: 'accumulationRegister',
  ACCOUNTING_REGISTER: 'accountingRegister',
  CALCULATION_REGISTER: 'calculationRegister',
  DOCUMENT_JOURNAL: 'documentJournal',
  ROLE: 'role',
  SUBSYSTEM: 'subsystem',
  LANGUAGE: 'folder',
  STYLE_ITEM: 'style',
  STYLE: 'style',
  ACCOUNTING_FLAG: 'accountingFlag',
  BOT: 'task',
  COLUMN: 'column',
  COMMON_ATTRIBUTE: 'attribute',
  COMMON_COMMAND: 'command',
  COMMON_FORM: 'form',
  COMMON_PICTURE: 'picture',
  COMMON_TEMPLATE: 'template',
  DEFINED_TYPE: 'enum',
  DOCUMENT_NUMERATOR: 'documentNumerator',
  ENUM_VALUE: 'enum',
  EVENT_SUBSCRIPTION: 'eventSubscription',
  EXTERNAL_DATA_PROCESSOR: 'dataProcessor',
  EXTERNAL_DATA_SOURCE_TABLE: 'externalDataSource',
  EXTERNAL_DATA_SOURCE_TABLE_FIELD: 'column',
  EXTERNAL_REPORT: 'report',
  EXT_DIMENSION_ACCOUNTING_FLAG: 'extDimensionAccountingFlag',
  FUNCTIONAL_OPTION: 'parameter',
  FUNCTIONAL_OPTIONS_PARAMETER: 'parameter',
  HTTP_SERVICE: 'http',
  HTTP_SERVICE_METHOD: 'operation',
  HTTP_SERVICE_URL_TEMPLATE: 'urlTemplate',
  INTEGRATION_SERVICE: 'ws',
  INTEGRATION_SERVICE_CHANNEL: 'ws',
  INTERFACE: 'ws',
  PALETTE_COLOR: 'style',
  RECALCULATION: 'operation',
  SCHEDULED_JOB: 'scheduledJob',
  SEQUENCE: 'sequence',
  SESSION_PARAMETER: 'sessionParameter',
  SETTINGS_STORAGE: 'parameter',
  STANDARD_ATTRIBUTE: 'attribute',
  STANDARD_TABULAR_SECTION: 'tabularSection',
  TABULAR_SECTION: 'tabularSection',
  TASK_ADDRESSING_ATTRIBUTE: 'attribute',
  TEMPLATE: 'template',
  WEB_SERVICE: 'ws',
  WS_OPERATION: 'operation',
  WS_OPERATION_PARAMETER: 'parameter',
  WS_REFERENCE: 'wsLink',
  XDTO_PACKAGE: 'folder',
  UNKNOWN: 'folder'
}

const iconCache = new Map<string, string>()

export function getIconPath(iconName: string): string {
  const baseUrl = import.meta.env.BASE_URL || '/'
  const path = `${baseUrl}assets/icons/${iconName}.svg`
  console.log(`getIconPath: iconName=${iconName}, baseUrl=${baseUrl}, path=${path}`)
  return path
}

export function getNodeIcon(nodeType: string, properties: Record<string, unknown>): string {
  const explicitType = properties?.type as string
  const typeToUse = explicitType || nodeType || 'unknown'
  const normalizedType = String(typeToUse).trim().toUpperCase()
  const cached = iconCache.get(normalizedType)
  if (cached) {
    console.log(`getNodeIcon cached: nodeType=${nodeType}, normalizedType=${normalizedType}, icon=${cached}`)
    return cached
  }
  const mapped = nodeTypeIconMap[normalizedType] || 'folder'
  iconCache.set(normalizedType, mapped)
  console.log(`getNodeIcon: nodeType=${nodeType}, normalizedType=${normalizedType}, icon=${mapped}`)
  return mapped
}


