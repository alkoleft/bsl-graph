package ru.alkoleft.context.infrastructure.graph

import com.vesoft.nebula.client.graph.data.Node
import com.vesoft.nebula.client.graph.data.Relationship
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import ru.alkoleft.context.domain.graph.EdgeType
import ru.alkoleft.context.domain.graph.GraphEdge
import ru.alkoleft.context.domain.graph.GraphKnowledgeRepository
import ru.alkoleft.context.domain.graph.GraphNode
import ru.alkoleft.context.domain.graph.GraphQuery
import ru.alkoleft.context.domain.graph.GraphSearchResult
import ru.alkoleft.context.domain.graph.GraphStatistics
import ru.alkoleft.context.domain.graph.MDObjectNode
import ru.alkoleft.context.domain.graph.NodeType
import ru.alkoleft.context.infrastructure.metadata.nodeId

/**
 * Реализация репозитория графа знаний с использованием NebulaGraph (заглушка)
 */
@Repository
class NebulaRepository(
    private val nebulaGraphService: NebulaGraphService,
) : GraphKnowledgeRepository {
    private val logger = LoggerFactory.getLogger(NebulaRepository::class.java)

    override fun isConnected(): Boolean = nebulaGraphService.connected()

    override fun saveNode(node: GraphNode): Boolean =
        try {
            // Используем единый тег MDObject для всех узлов метаданных
            val tagName = "MDObject"
            nebulaGraphService.insertVertex(tagName, node)
        } catch (e: Exception) {
            logger.error("Ошибка при сохранении узла ${node.id}: ${e.message}")
            false
        }

    override fun saveEdge(edge: GraphEdge): Boolean =
        try {
            nebulaGraphService.insertEdge(edge.type.toString(), edge)
        } catch (e: Exception) {
            logger.error("Ошибка при сохранении ребра ${edge.sourceId} -> ${edge.targetId}: ${e.message}")
            false
        }

    override fun findNodes(query: GraphQuery): GraphSearchResult =
        try {
            logger.info("Поиск узлов по запросу: $query")

            // Преобразуем GraphQuery в nGQL запрос
            val nGqlQuery = buildNGqlQuery(query)
            val results = nebulaGraphService.findNodes(nGqlQuery)

            val nodes = results.mapNotNull { nodeMap ->
                try {
                    convertNodeMapToGraphNode(nodeMap)
                } catch (e: Exception) {
                    logger.warn("Ошибка преобразования узла: $nodeMap", e)
                    null
                }
            }

            // Загружаем рёбра только между найденными узлами
            val nodeIds = nodes.map { it.id }
            val edges = fetchEdgesForNodes(nodeIds, query.edgeTypes)

            GraphSearchResult(nodes, edges, nodes.size)
        } catch (e: Exception) {
            logger.error("Ошибка при поиске узлов: ${e.message}")
            GraphSearchResult(emptyList(), emptyList(), 0)
        }

    override fun findNodesByType(type: NodeType): List<GraphNode> =
        try {
            logger.info("Поиск узлов по типу: ${type.name}")

            // Используем единый тег MDObject и фильтруем по свойству type
            val query = "MATCH (n:MDObject) WHERE n.type == \"${type.name}\" RETURN n"
            val results = nebulaGraphService.findNodes(query)

            results.mapNotNull { nodeMap ->
                try {
                    convertNodeMapToGraphNode(nodeMap)
                } catch (e: Exception) {
                    logger.warn("Ошибка преобразования узла: $nodeMap", e)
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске узлов по типу ${type.name}: ${e.message}")
            emptyList()
        }

    override fun findNodesByProperties(properties: Map<String, Any>): List<GraphNode> =
        try {
            logger.info("Поиск узлов по свойствам: $properties")

            val whereClause =
                properties.entries.joinToString(" AND ") { "n.${it.key} == ${formatValueForQuery(it.value)}" }
            val query = "MATCH (n:MDObject) WHERE $whereClause RETURN n"
            val results = nebulaGraphService.findNodes(query)

            results.mapNotNull { nodeMap ->
                try {
                    convertNodeMapToGraphNode(nodeMap)
                } catch (e: Exception) {
                    logger.warn("Ошибка преобразования узла: $nodeMap", e)
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске узлов по свойствам: ${e.message}")
            emptyList()
        }

    override fun findRelatedNodes(
        nodeId: String,
        maxDepth: Int,
    ): GraphSearchResult {
        return try {
            logger.info("Поиск связанных узлов для $nodeId с глубиной $maxDepth")

            if (maxDepth <= 0) {
                logger.warn("Глубина поиска должна быть больше 0, получено: $maxDepth")
                GraphSearchResult(emptyList(), emptyList(), 0)
            } else {
                // Строим nGQL запрос для поиска связанных узлов
                val query = buildRelatedNodesQuery(nodeId, maxDepth)
                val relatedNodes = mutableListOf<GraphNode>()
                val edges = mutableListOf<GraphEdge>()
                nebulaGraphService.executeAsSequence(query).forEach {
                    relatedNodes.add(it["related"]!!.asNode().toMDNode())
                    edges.add(it["ref"]!!.asRelationship().toEdge())
                }

                // Убираем дубликаты по ID и исключаем исходный узел
                val uniqueNodes = relatedNodes
                    .filter { it.id != nodeId }
                    .distinctBy { it.id }

                logger.info("Найдено ${uniqueNodes.size} связанных узлов для $nodeId")
                GraphSearchResult(uniqueNodes, edges, uniqueNodes.size)
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске связанных узлов для $nodeId: ${e.message}")
            GraphSearchResult(emptyList(), emptyList(), 0)
        }
    }

    fun Node.toMDNode(): MDObjectNode {
        val values = values("MDObject")
        val properties = keys("MDObject").withIndex().associate { it.value to values[it.index].asString() }
        return MDObjectNode(
            properties["uid"]!!,
            properties["name"]!!,
            properties["synonym"]!!,
            NodeType.valueOf(properties["type"]!!),
            this.id.asString()
        )
    }

    fun Relationship.toEdge(): GraphEdge {
        return when (this.edgeName()) {
            EdgeType.ATTRIBUTE.name -> GraphEdge.attribute(
                this.srcId().asString(),
                dstId().asString(),
                properties().get("name")?.asString() ?: ""
            )

            EdgeType.ACCESS.name -> GraphEdge.access(
                this.srcId().asString(),
                dstId().asString(),
                properties().get("name")?.asString() ?: ""
            )

            EdgeType.CHILDREN.name -> GraphEdge.children(this.srcId().asString(), dstId().asString())
            EdgeType.CONTAINS.name -> GraphEdge.contains(this.srcId().asString(), dstId().asString())
            else -> GraphEdge.related(this.srcId().asString(), dstId().asString())
        }
    }

    override fun findEdges(
        sourceId: String,
        targetId: String,
    ): List<GraphEdge> =
        try {
            logger.info("Поиск рёбер между $sourceId и $targetId")
            // В реальной реализации здесь будет поиск в NebulaGraph
            emptyList()
        } catch (e: Exception) {
            logger.error("Ошибка при поиске рёбер между $sourceId и $targetId: ${e.message}")
            emptyList()
        }

    override fun deleteNode(nodeId: String): Boolean =
        try {
            logger.info("Удаление узла: $nodeId")
            // В реальной реализации здесь будет удаление из NebulaGraph
            true
        } catch (e: Exception) {
            logger.error("Ошибка при удалении узла $nodeId: ${e.message}")
            false
        }

    override fun deleteEdge(
        sourceId: String,
        targetId: String,
        edgeType: EdgeType,
    ): Boolean =
        try {
            logger.info("Удаление ребра: $sourceId -> $targetId типа ${edgeType.name}")
            // В реальной реализации здесь будет удаление из NebulaGraph
            true
        } catch (e: Exception) {
            logger.error("Ошибка при удалении ребра $sourceId -> $targetId: ${e.message}")
            false
        }

    override fun updateNodeProperties(
        nodeId: String,
        properties: Map<String, Any>,
    ): Boolean = try {
        logger.info("Обновление свойств узла $nodeId: $properties")
        // В реальной реализации здесь будет обновление в NebulaGraph
        true
    } catch (e: Exception) {
        logger.error("Ошибка при обновлении свойств узла $nodeId: ${e.message}")
        false
    }

    override fun nodeExists(nodeId: String): Boolean = try {
        logger.info("Проверка существования узла: $nodeId")
        // В реальной реализации здесь будет проверка в NebulaGraph
        false
    } catch (e: Exception) {
        logger.error("Ошибка при проверке существования узла $nodeId: ${e.message}")
        false
    }


    override fun getGraphStatistics(): GraphStatistics {
        try {
            logger.info("Получение статистики графа")

            // Получаем количество вершин
            val verticesQuery = "MATCH (n) RETURN count(n) as vertexCount"
            val verticesResult = nebulaGraphService.findNodes(verticesQuery)
            val vertexCount = verticesResult.firstOrNull()?.get("vertexCount") as? Long ?: 0L

            // Получаем количество рёбер
            val edgesQuery = "MATCH ()-[r]->() RETURN count(r) as edgeCount"
            val edgesResult = nebulaGraphService.findNodes(edgesQuery)
            val edgeCount = edgesResult.firstOrNull()?.get("edgeCount") as? Long ?: 0L

            return GraphStatistics(
                totalNodes = vertexCount.toInt(),
                totalEdges = edgeCount.toInt(),
                nodesByType = emptyMap(), // TODO: Реализовать подсчет по типам
                edgesByType = emptyMap(), // TODO: Реализовать подсчет по типам
            )
        } catch (e: Exception) {
            logger.error("Ошибка при получении статистики графа: ${e.message}")
            return GraphStatistics(0, 0, emptyMap(), emptyMap())
        }
    }

    /**
     * Определяет тег NebulaGraph для типа узла (упрощенная схема)
     */
    private fun getTagNameForNodeType(nodeType: NodeType): String = "MDObject"

    /**
     * Преобразует GraphQuery в nGQL запрос (упрощенная схема)
     */
    private fun buildNGqlQuery(query: GraphQuery): String = when {
        !query.nodeTypes.isNullOrEmpty() -> {
            val types = query.nodeTypes.joinToString("|") { "\"${it.name}\"" }
            "MATCH (n:MDObject) WHERE n.type IN [$types] RETURN n"
        }

        !query.properties.isNullOrEmpty() -> {
            val whereClause = query.properties.entries.joinToString(" AND ") {
                "n.${it.key} == ${formatValueForQuery(it.value)}"
            }
            "MATCH (n:MDObject) WHERE $whereClause RETURN n"
        }

        else -> "MATCH (n:MDObject) RETURN n LIMIT ${query.limit}"
    }

    /**
     * Преобразует GraphQuery в nGQL запрос (упрощенная схема)
     */
    private fun buildEdgesNGqlQuery(query: GraphQuery): String = when {
        !query.nodeTypes.isNullOrEmpty() -> {
            val types = query.nodeTypes.joinToString("|") { "\"${it.name}\"" }
            "MATCH (:MDObject)-[e]-(:MDObject) WHERE e.type IN [$types] RETURN e"
        }

        !query.properties.isNullOrEmpty() -> {
            val whereClause = query.properties.entries.joinToString(" AND ") {
                "n.${it.key} == ${formatValueForQuery(it.value)}"
            }
            "MATCH (:MDObject)-[e]-(:MDObject) WHERE $whereClause RETURN e"
        }

        else -> "MATCH (:MDObject)-[e]-(:MDObject) RETURN e LIMIT ${query.limit}"
    }

    /**
     * Форматирует значение для использования в nGQL запросе
     */
    private fun formatValueForQuery(value: Any): String = when (value) {
        is String -> "\"$value\""
        is Boolean -> value.toString()
        is Number -> value.toString()
        else -> "\"$value\""
    }

    /**
     * Преобразует результат из NebulaGraph в GraphNode (упрощенная схема)
     */
    private fun convertNodeMapToGraphNode(nodeMap: Map<String, Any>, key: String = "n"): GraphNode? {
        try {
            val data = nodeMap[key] as? Map<*, *> ?: return null
            val tags = data["tags"] as? Map<*, *> ?: return null
            val mdObjectData = tags["MDObject"] as? Map<*, *> ?: return null

            val vid = data["vid"]?.toString() ?: return null
            val uid = mdObjectData["uid"]?.toString() ?: vid
            val name = mdObjectData["name"]?.toString() ?: ""
            val synonym = mdObjectData["synonym"]?.toString() ?: ""
            val typeStr = mdObjectData["type"]?.toString() ?: "UNKNOWN"

            // Определяем тип узла по свойству type
            val nodeType = try {
                NodeType.valueOf(typeStr.uppercase())
            } catch (e: IllegalArgumentException) {
                NodeType.UNKNOWN // Fallback для неизвестных типов
            }

            val properties = mapOf(
                "uid" to uid,
                "name" to name,
                "synonym" to synonym,
                "type" to typeStr,
            )

            return MDObjectNode(uid, name, synonym, nodeType, nodeId(uid))
        } catch (e: Exception) {
            logger.warn("Ошибка преобразования узла: $nodeMap", e)
            return null
        }
    }

    /**
     * Загружает рёбра между набором вершин (в обе стороны), опционально фильтруя по типам рёбер
     */
    private fun fetchEdges(
        query: String
    ): List<GraphEdge> {
        val rows = nebulaGraphService.findNodes(query)
        if (rows.isEmpty()) return emptyList()

        return rows.mapNotNull { convertEdgeRowToGraphEdge(it) }
    }

    /**
     * Загружает рёбра между набором вершин (в обе стороны), опционально фильтруя по типам рёбер
     */
    private fun fetchEdgesForNodes(
        nodeIds: List<String>,
        edgeTypes: Set<EdgeType>?,
    ): List<GraphEdge> {
        if (nodeIds.isEmpty()) return emptyList()

        val ids = nodeIds.joinToString(",") { "'${it}'" }
        val edgeFilter = buildEdgeTypeFilter(edgeTypes)
        val query = """
            MATCH (n)-[e$edgeFilter]->(m)
            WHERE id(n) IN [$ids] AND id(m) IN [$ids]
            RETURN e
            """.trimIndent()

        val rows = nebulaGraphService.findNodes(query)
        if (rows.isEmpty()) return emptyList()

        return rows.mapNotNull { convertEdgeRowToGraphEdge(it) }
    }

    /**
     * Строит фильтр типов рёбер для шаблона MATCH
     */
    private fun buildEdgeTypeFilter(edgeTypes: Set<EdgeType>?): String {
        if (edgeTypes.isNullOrEmpty()) return ""
        // Поддерживаем только существующие в схеме типы рёбер
        val supported = edgeTypes.map { it.toString() }
        if (supported.isEmpty()) return ""
        return ":" + supported.joinToString("|")
    }

    /**
     * Преобразует строку результата с колонкой e (Edge) в GraphEdge
     */
    private fun convertEdgeRowToGraphEdge(row: Map<String, Any>, key: String = "e"): GraphEdge? {
        val edgeData = row[key] as? Map<*, *> ?: return null

        val sourceId = edgeData["src"]?.toString() ?: return null
        val targetId = edgeData["dst"]?.toString() ?: return null
        val edgeName = edgeData["name"]?.toString() ?: ""

        return when (edgeName.uppercase()) {
            EdgeType.ATTRIBUTE.name -> GraphEdge.attribute(
                sourceId,
                targetId,
                (edgeData["props"] as? Map<*, *>)?.get("name")?.toString() ?: ""
            )

            EdgeType.ACCESS.name -> GraphEdge.access(
                sourceId,
                targetId,
                (edgeData["props"] as? Map<*, *>)?.get("name")?.toString() ?: ""
            )

            EdgeType.CHILDREN.name -> GraphEdge.children(sourceId, targetId)
            EdgeType.CONTAINS.name -> GraphEdge.contains(sourceId, targetId)
            else -> GraphEdge.related(sourceId, targetId)
        }
    }

    /**
     * Строит nGQL запрос для поиска связанных узлов с заданной глубиной
     */
    private fun buildRelatedNodesQuery(nodeId: String, maxDepth: Int): String {
        return when (maxDepth) {
            1 -> {
                // Для глубины 1 используем простой запрос по ID вершины
                """
                MATCH (related)-[ref]-(start)
                WHERE id(start) == "$nodeId"
                RETURN DISTINCT related, ref
                """.trimIndent()
            }

            else -> {
                // Для большей глубины используем рекурсивный запрос по ID вершины
                """
                MATCH (start)
                WHERE id(start) == "$nodeId"
                WITH start
                MATCH p = (start)-[*1..$maxDepth]-(related)
                WHERE related <> start
                RETURN DISTINCT related
                """.trimIndent()
            }
        }
    }
}
