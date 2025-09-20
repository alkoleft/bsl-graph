package ru.alkoleft.context.domain.metadata

import ru.alkoleft.context.domain.graph.GraphNode
import ru.alkoleft.context.domain.graph.NodeType
import java.nio.file.Path

/**
 * Сервис для интеграции метаданных 1С с графом знаний
 */
interface MetadataGraphService {
    /**
     * Загружает метаданные из конфигурации и сохраняет в граф знаний
     */
    fun loadMetadataToGraph(configurationPath: Path): MetadataLoadResult
}

/**
 * Результат загрузки метаданных в граф
 */
data class MetadataLoadResult(
    val success: Boolean,
    val configurationId: String?,
    val nodesCount: Int,
    val edgesCount: Int,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)
