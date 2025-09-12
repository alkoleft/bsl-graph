package ru.alkoleft.context.domain.graph

/**
 * Узел графа знаний
 */
data class GraphNode(
    val id: String,
    val type: NodeType,
    val properties: Map<String, Any> = emptyMap(),
    val labels: Set<String> = emptySet(),
)

/**
 * Типы узлов в графе знаний
 */
enum class NodeType {
    CONCEPT, // Концепция
    METHOD, // Метод
    PROPERTY, // Свойство
    CLASS, // Класс
    MODULE, // Модуль
    DOCUMENTATION, // Документация
    EXAMPLE, // Пример
    RELATIONSHIP, // Связь
}

/**
 * Ребро графа знаний
 */
data class GraphEdge(
    val sourceId: String,
    val targetId: String,
    val type: EdgeType,
    val properties: Map<String, Any> = emptyMap(),
)

/**
 * Типы рёбер в графе знаний
 */
enum class EdgeType {
    CONTAINS, // Содержит
    IMPLEMENTS, // Реализует
    EXTENDS, // Расширяет
    USES, // Использует
    REFERENCES, // Ссылается
    DEPENDS_ON, // Зависит от
    SIMILAR_TO, // Похож на
    PART_OF, // Часть
    RELATED_TO, // Связан с
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
