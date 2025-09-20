package ru.alkoleft.context.presentation.rest

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.alkoleft.context.domain.graph.EdgeType
import ru.alkoleft.context.domain.graph.GraphEdge
import ru.alkoleft.context.domain.graph.GraphKnowledgeRepository
import ru.alkoleft.context.domain.graph.GraphNode
import ru.alkoleft.context.domain.graph.GraphQuery
import ru.alkoleft.context.domain.graph.GraphSearchResult
import ru.alkoleft.context.domain.graph.NodeType
import ru.alkoleft.context.infrastructure.graph.QueryMapper

@RestController
@RequestMapping("/api/graph")
class GraphRestController(
    private val graphKnowledgeRepository: GraphKnowledgeRepository,
) {
    @GetMapping("/stats")
    suspend fun getStatistics(): GraphStatisticsResponse {
        val stats = graphKnowledgeRepository.getGraphStatistics()
        return GraphStatisticsResponse(
            totalNodes = stats.totalNodes,
            totalEdges = stats.totalEdges,
        )
    }

    @GetMapping("/nodes")
    suspend fun getNodesByType(
        @RequestParam(required = false) type: String?,
    ): List<GraphNodeResponse> {
        val nodes: List<GraphNode> =
            if (type.isNullOrBlank()) {
                // Если тип не указан, попробуем вернуть ограниченный список через поиск без фильтров
                val result = graphKnowledgeRepository.findNodes(GraphQuery(limit = 100))
                result.nodes
            } else {
                val nodeType = NodeType.valueOf(type.trim().uppercase())
                graphKnowledgeRepository.findNodesByType(nodeType)
            }
        return nodes.map { it.toResponse() }
    }

    @GetMapping("/related/{id}")
    suspend fun getRelatedNodes(
        @PathVariable("id") id: String,
        @RequestParam(name = "depth", required = false, defaultValue = "1") depth: Int,
    ): GraphSearchResponse {
        val result = graphKnowledgeRepository.findRelatedNodes(id, depth)
        return GraphSearchResponse(
            nodes = result.nodes.map { it.toResponse() },
            edges = result.edges.map { it.toResponse() },
            totalCount = result.totalCount,
        )
    }

    @GetMapping("/edges")
    suspend fun getEdges(
        @RequestParam sourceId: String,
        @RequestParam targetId: String,
    ): List<GraphEdgeResponse> {
        val edges = graphKnowledgeRepository.findEdges(sourceId, targetId)
        return edges.map { it.toResponse() }
    }

    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    suspend fun search(
        @RequestBody request: GraphSearchRequest,
    ): GraphSearchResponse {
        val query =
            GraphQuery(
                nodeTypes = request.nodeTypes?.map { NodeType.valueOf(it.uppercase()) }?.toSet(),
                edgeTypes = request.edgeTypes?.map { EdgeType.valueOf(it.uppercase()) }?.toSet(),
                properties = request.properties,
                limit = request.limit,
                offset = request.offset,
            )
        val result: GraphSearchResult = graphKnowledgeRepository.findNodes(query)
        return GraphSearchResponse(
            nodes = result.nodes.map { it.toResponse() },
            edges = result.edges.map { it.toResponse() },
            totalCount = result.totalCount,
        )
    }
}

data class GraphStatisticsResponse(
    val totalNodes: Int,
    val totalEdges: Int,
)

data class GraphNodeResponse(
    val id: String,
    val type: String,
    val properties: Map<String, Any>
)

data class GraphEdgeResponse(
    val sourceId: String,
    val targetId: String,
    val type: String,
    val properties: Map<String, Any>,
)

data class GraphSearchRequest(
    val nodeTypes: Set<String>?,
    val edgeTypes: Set<String>?,
    val properties: Map<String, Any>?,
    val limit: Int? = 100,
    val offset: Int? = 0,
)

data class GraphSearchResponse(
    val nodes: List<GraphNodeResponse>,
    val edges: List<GraphEdgeResponse>,
    val totalCount: Int,
)

private fun GraphNode.toResponse(): GraphNodeResponse =
    GraphNodeResponse(
        id = id,
        type = type.name,
        properties = QueryMapper.toPropertiesMap(this)
    )

private fun GraphEdge.toResponse(): GraphEdgeResponse =
    GraphEdgeResponse(
        sourceId = sourceId,
        targetId = targetId,
        type = type.name,
        properties = QueryMapper.toPropertiesMap(this),
    )
