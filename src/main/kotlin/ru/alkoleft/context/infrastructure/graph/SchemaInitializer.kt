/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.graph

import com.vesoft.nebula.client.graph.net.Session
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.alkoleft.context.domain.graph.EdgeType
import ru.alkoleft.context.domain.graph.MDObjectNode

private val logger = KotlinLogging.logger { }

@Component
class SchemaInitializer {
    fun createSpace(session: Session, space: String) {
        try {
            logger.info { "Создание пространства $space..." }
            val createSpaceQuery =
                """
                CREATE SPACE IF NOT EXISTS $space (
                    partition_num = 10,
                    replica_factor = 1,
                    vid_type = FIXED_STRING(32)
                )
                """.trimIndent()

            val result = session.execute(createSpaceQuery)
            if (result?.isSucceeded == true) {
                logger.info { "Пространство $space создано успешно" }
                // Ждем некоторое время для завершения создания пространства
                Thread.sleep(2000)
            } else {
                error { "Не удалось создать пространство $space: ${result?.errorMessage}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при создании пространства $space" }
        }
    }

    fun createGraphSchema(session: Session) {
        logger.info { "Создание упрощенной схемы графа для метаданных 1С..." }

        // Создаем единый тег для всех объектов метаданных
        val properties = QueryMapper.properties(MDObjectNode::class.java)
        val createTagQuery =
            "CREATE TAG IF NOT EXISTS MDObject (${properties.joinToString(",") { "$it string" }})"

        val tagResult = session.execute(createTagQuery)
        if (tagResult?.isSucceeded == true) {
            logger.debug { "Тег MDObject создан успешно" }
        } else {
            throw IllegalStateException("Не удалось создать тег MDObject: ${tagResult?.errorMessage}")
        }

        // Создаем типы рёбер
        val edges = EdgeType.entries.map { it.name to QueryMapper.properties(it).associateWith { "string" } }

        edges.forEach { (edgeName, properties) ->
            val propertiesStr =
                if (properties.isNotEmpty()) {
                    properties.entries.joinToString(", ") { "${it.key} ${it.value}" }
                } else {
                    ""
                }
            val createEdgeQuery =
                if (propertiesStr.isNotEmpty()) {
                    "CREATE EDGE IF NOT EXISTS $edgeName ($propertiesStr)"
                } else {
                    "CREATE EDGE IF NOT EXISTS $edgeName ()"
                }

            val result = session.execute(createEdgeQuery)
            if (result?.isSucceeded == true) {
                logger.debug { "Тип ребра $edgeName создан успешно" }
            } else {
                logger.warn { "Не удалось создать тип ребра $edgeName: ${result?.errorMessage}" }
            }
        }

        logger.info { "Упрощенная схема графа создана успешно" }
        logger.info { "Создан тег: MDObject" }
        logger.info { "Создано типов рёбер: ${edges.size}" }
    }
}