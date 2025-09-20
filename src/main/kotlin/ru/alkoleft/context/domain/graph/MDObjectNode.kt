package ru.alkoleft.context.domain.graph

/**
 * Узел графа знаний
 */
sealed interface GraphNode {
    val id: String
    val type: NodeType
}

open class MDObjectNode(
    val uid: String,
    val name: String,
    val synonym: String,
    override val type: NodeType,
    override val id: String
) : GraphNode

class ConfigurationNode(uid: String, name: String, synonym: String, type: NodeType, id: String) :
    MDObjectNode(uid, name, synonym, type, id)

/**
 * Типы узлов в графе знаний
 */
enum class NodeType {
    // Типы узлов для метаданных 1С
    CONFIGURATION, // Конфигурация
    CATALOG, // Справочник
    DOCUMENT, // Документ
    ENUM, // Перечисление
    CONSTANT, // Константа
    REGISTER, // Регистр
    BUSINESS_PROCESS, // Бизнес-процесс
    TASK, // Задача
    COMMON_MODULE, // Общий модуль
    COMMAND_GROUP, // Группа команд
    COMMAND, // Команда
    ATTRIBUTE, // Атрибут
    DIMENSION, // Измерение
    RESOURCE, // Ресурс
    FORM, // Форма
    TABLE, // Таблица
    QUERY, // Запрос
    REPORT, // Отчет
    DATA_PROCESSOR, // Обработка
    EXTERNAL_DATA_SOURCE, // Внешний источник данных
    EXCHANGE_PLAN, // План обмена
    CHART_OF_ACCOUNTS, // План счетов
    CHART_OF_CHARACTERISTIC_TYPES, // План видов характеристик
    CHART_OF_CALCULATION_TYPES, // План видов расчета
    FILTER_CRITERION, // Критерий отбора
    INFORMATION_REGISTER, // Регистр сведений
    ACCUMULATION_REGISTER, // Регистр накопления
    ACCOUNTING_REGISTER, // Регистр бухгалтерии
    CALCULATION_REGISTER, // Регистр расчета
    DOCUMENT_JOURNAL, // Журнал документов
    ROLE, // Роль
    SUBSYSTEM, // Подсистема
    LANGUAGE, // Язык
    STYLE_ITEM, // Элемент стиля
    STYLE, // Стиль

    // Дополнительные типы узлов из MDOType
    ACCOUNTING_FLAG, // Признак учета
    BOT, // Бот
    COLUMN, // Колонка
    COMMON_ATTRIBUTE, // Общий реквизит
    COMMON_COMMAND, // Общая команда
    COMMON_FORM, // Общая форма
    COMMON_PICTURE, // Общая картинка
    COMMON_TEMPLATE, // Общий макет
    DEFINED_TYPE, // Определяемый тип
    DOCUMENT_NUMERATOR, // Нумератор документов
    ENUM_VALUE, // Значение перечисления
    EVENT_SUBSCRIPTION, // Подписка на событие
    EXTERNAL_DATA_PROCESSOR, // Внешняя обработка
    EXTERNAL_DATA_SOURCE_TABLE, // Таблица внешнего источника данных
    EXTERNAL_DATA_SOURCE_TABLE_FIELD, // Поле таблицы внешнего источника данных
    EXTERNAL_REPORT, // Внешний отчет
    EXT_DIMENSION_ACCOUNTING_FLAG, // Признак учета субконто
    FUNCTIONAL_OPTION, // Функциональная опция
    FUNCTIONAL_OPTIONS_PARAMETER, // Параметр функциональных опций
    HTTP_SERVICE, // HTTP сервис
    HTTP_SERVICE_METHOD, // Метод HTTP сервиса
    HTTP_SERVICE_URL_TEMPLATE, // Шаблон URL HTTP сервиса
    INTEGRATION_SERVICE, // Сервис интеграции
    INTEGRATION_SERVICE_CHANNEL, // Канал сервиса интеграции
    INTERFACE, // Интерфейс
    PALETTE_COLOR, // Цвет палитры
    RECALCULATION, // Перерасчет
    SCHEDULED_JOB, // Регламентное задание
    SEQUENCE, // Последовательность
    SESSION_PARAMETER, // Параметр сеанса
    SETTINGS_STORAGE, // Хранилище настроек
    STANDARD_ATTRIBUTE, // Стандартный реквизит
    STANDARD_TABULAR_SECTION, // Стандартная табличная часть
    TABULAR_SECTION, // Табличная часть
    TASK_ADDRESSING_ATTRIBUTE, // Реквизит адресации
    TEMPLATE, // Макет
    WEB_SERVICE, // Web сервис
    WS_OPERATION, // Операция Web сервиса
    WS_OPERATION_PARAMETER, // Параметр операции Web сервиса
    WS_REFERENCE, // WS ссылка
    XDTO_PACKAGE, // Пакет XDTO
    UNKNOWN // Неизвестный тип
}

/**
 * Ребро графа знаний
 */
sealed interface GraphEdge {
    val sourceId: String
    val targetId: String
    val type: EdgeType

    companion object {
        fun create(
            sourceId: String,
            targetId: String,
            type: EdgeType
        ) = BaseEdge(sourceId, targetId, type)

        fun contains(
            sourceId: String,
            targetId: String
        ) = BaseEdge(sourceId, targetId, EdgeType.CONTAINS)

        fun children(
            sourceId: String,
            targetId: String
        ) = BaseEdge(sourceId, targetId, EdgeType.CHILDREN)

        fun related(
            sourceId: String,
            targetId: String
        ) = BaseEdge(sourceId, targetId, EdgeType.RELATED_TO)

        fun attribute(
            sourceId: String,
            targetId: String,
            attributeName: String
        ) = AttributeEdge(sourceId, targetId, attributeName)

        fun access(
            sourceId: String,
            targetId: String,
            rights: String
        ) = AccessEdge(sourceId, targetId, rights)
    }
}

open class BaseEdge(
    override val sourceId: String,
    override val targetId: String,
    override val type: EdgeType
) : GraphEdge

class AttributeEdge(
    override val sourceId: String,
    override val targetId: String,
    val attributeName: String
) : BaseEdge(sourceId, targetId, EdgeType.ATTRIBUTE)

class AccessEdge(
    override val sourceId: String,
    override val targetId: String,
    val rights: String
) : BaseEdge(sourceId, targetId, EdgeType.ACCESS)

/**
 * Типы рёбер в графе знаний
 */
enum class EdgeType {
    CONTAINS, // Содержит
    RELATED_TO, // Связан с
    CHILDREN, // Дочерние элементы (конфигурация - объекты, подсистема - состав)
    ATTRIBUTE, // Связь по типу атрибута
    ACCESS,
}

/**
 * Запрос к графу знаний
 */
data class GraphQuery(
    val nodeTypes: Set<NodeType>? = null,
    val edgeTypes: Set<EdgeType>? = null,
    val properties: Map<String, Any>? = null,
    val limit: Int? = null,
    val offset: Int? = null,
)

/**
 * Результат поиска в графе
 */
data class GraphSearchResult(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val totalCount: Int,
)
