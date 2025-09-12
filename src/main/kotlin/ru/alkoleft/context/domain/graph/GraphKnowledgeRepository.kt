package ru.alkoleft.context.domain.graph

/**
 * Репозиторий для работы с графом знаний
 */
interface GraphKnowledgeRepository {
    /**
     * Сохраняет узел в граф знаний
     */
    suspend fun saveNode(node: GraphNode): Boolean

    /**
     * Сохраняет ребро в граф знаний
     */
    suspend fun saveEdge(edge: GraphEdge): Boolean

    /**
     * Находит узлы по запросу
     */
    suspend fun findNodes(query: GraphQuery): GraphSearchResult

    /**
     * Находит узлы по типу
     */
    suspend fun findNodesByType(type: NodeType): List<GraphNode>

    /**
     * Находит узлы по свойствам
     */
    suspend fun findNodesByProperties(properties: Map<String, Any>): List<GraphNode>

    /**
     * Находит связанные узлы
     */
    suspend fun findRelatedNodes(
        nodeId: String,
        maxDepth: Int = 1,
    ): List<GraphNode>

    /**
     * Находит рёбра между узлами
     */
    suspend fun findEdges(
        sourceId: String,
        targetId: String,
    ): List<GraphEdge>

    /**
     * Удаляет узел из графа
     */
    suspend fun deleteNode(nodeId: String): Boolean

    /**
     * Удаляет ребро из графа
     */
    suspend fun deleteEdge(
        sourceId: String,
        targetId: String,
        edgeType: EdgeType,
    ): Boolean

    /**
     * Обновляет свойства узла
     */
    suspend fun updateNodeProperties(
        nodeId: String,
        properties: Map<String, Any>,
    ): Boolean

    /**
     * Проверяет существование узла
     */
    suspend fun nodeExists(nodeId: String): Boolean

    /**
     * Получает статистику графа
     */
    suspend fun getGraphStatistics(): GraphStatistics
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
