/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.presentation.mcp

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import ru.alkoleft.context.domain.metadata.MetadataGraphService
import java.nio.file.Path

@Service
class ContextMcpController(
    private val metadataGraphService: MetadataGraphService,
) {
    fun readMetadata(configurationPath: String): String =
        try {
            val path = Path.of(configurationPath)
            val result = runBlocking { metadataGraphService.loadMetadataToGraph(path) }

            """
            Результат чтения метаданных:
            - Успешно: ${result.success}
            - ID конфигурации: ${result.configurationId}
            - Количество узлов: ${result.nodesCount}
            - Количество ребер: ${result.edgesCount}
            - Ошибки: ${result.errors.joinToString(", ")}
            - Предупреждения: ${result.warnings.joinToString(", ")}
            """.trimIndent()
        } catch (e: Exception) {
            "Ошибка при чтении метаданных: ${e.message}"
        }

    fun searchMetadata(
        query: String,
        type: String? = null,
    ): String {
        return try {
//            val results =
//                runBlocking {
//                    if (type != null) {
//                        // Парсим тип узла из строки
//                        val nodeType =
//                            try {
//                                ru.alkoleft.context.domain.graph.NodeType
//                                    .valueOf(type.uppercase())
//                            } catch (e: IllegalArgumentException) {
//                                return@runBlocking emptyList<ru.alkoleft.context.domain.graph.MDObjectNode>()
//                            }
//                        metadataGraphService.findMetadataNodesByType(nodeType)
//                    } else {
//                        metadataGraphService.findMetadataNodesByName(query)
//                    }
//                }
//
//            if (results.isEmpty()) {
//                "Узлы метаданных не найдены для запроса: $query"
//            } else {
//                "Найдено узлов: ${results.size}\n" +
//                    results.joinToString("\n") { node ->
//                        "- ID: ${node.id} (тип: ${node.type})"
//                    }
//            }
            ""
        } catch (e: Exception) {
            "Ошибка при поиске метаданных: ${e.message}"
        }
    }
}
