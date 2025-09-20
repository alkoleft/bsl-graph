package ru.alkoleft.context.domain.graph

/**
 * Репозиторий для работы с графом знаний
 */
interface GraphKnowledgeRepository {

    fun isConnected(): Boolean
    /**
     * Сохраняет узел в граф знаний
     */
    fun saveNode(node: GraphNode): Boolean

    /**
     * Сохраняет ребро в граф знаний
     */
    fun saveEdge(edge: GraphEdge): Boolean

    /**
     * Находит узлы по запросу
     */
    fun findNodes(query: GraphQuery): GraphSearchResult

    /**
     * Находит узлы по типу
     */
    fun findNodesByType(type: NodeType): List<GraphNode>

    /**
     * Находит узлы по свойствам
     */
    fun findNodesByProperties(properties: Map<String, Any>): List<GraphNode>

    /**
     * Находит связанные узлы
     */
    fun findRelatedNodes(
        nodeId: String,
        maxDepth: Int = 1,
    ): GraphSearchResult

    /**
     * Находит рёбра между узлами
     */
    fun findEdges(
        sourceId: String,
        targetId: String,
    ): List<GraphEdge>

    /**
     * Удаляет узел из графа
     */
    fun deleteNode(nodeId: String): Boolean

    /**
     * Удаляет ребро из графа
     */
    fun deleteEdge(
        sourceId: String,
        targetId: String,
        edgeType: EdgeType,
    ): Boolean

    /**
     * Обновляет свойства узла
     */
    fun updateNodeProperties(
        nodeId: String,
        properties: Map<String, Any>,
    ): Boolean

    /**
     * Проверяет существование узла
     */
    fun nodeExists(nodeId: String): Boolean

    /**
     * Получает статистику графа
     */
    fun getGraphStatistics(): GraphStatistics
}

/**
 * Статистика графа знаний
 */
data class GraphStatistics(
    val totalNodes: Int,
    val totalEdges: Int,
    val nodesByType: Map<NodeType, Int>,
    val edgesByType: Map<EdgeType, Int>,
)
