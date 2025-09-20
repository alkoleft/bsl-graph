/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.presentation.console

import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import ru.alkoleft.context.domain.metadata.MetadataGraphService
import ru.alkoleft.context.infrastructure.graph.NebulaGraphService
import java.nio.file.Path

@Component
class ConsoleController(
    private val metadataGraphService: MetadataGraphService,
    private val nebulaGraphService: NebulaGraphService,
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        println("=== BSL Context Application ===")
        println("Приложение для чтения метаданных конфигураций 1С и выгрузки в Nebula Graph")

        // Проверяем подключение к Nebula Graph
        runBlocking {
            try {
                if (nebulaGraphService.isConnected()) {
                    println("✓ Подключение к Nebula Graph установлено")

                    // Показываем статистику графа
                    val statsQuery = "MATCH (n) RETURN count(n) as totalNodes"
                    val stats = nebulaGraphService.findNodes(statsQuery)
                    if (stats.isNotEmpty()) {
                        val totalNodes = stats.first()["totalNodes"] as? Long ?: 0L
                        println("✓ Всего узлов в графе: $totalNodes")
                    }
                } else {
                    println("⚠️  Не удалось подключиться к Nebula Graph")
                    println("   Убедитесь, что Nebula Graph запущен и доступен")
                }
            } catch (e: Exception) {
                println("⚠️  Ошибка при проверке подключения к Nebula Graph: ${e.message}")
            }
        }

        if (args.isNotEmpty() && args[0] != null) {
            val configurationPath = args[0]!!
            println("\nЧитаем метаданные из: $configurationPath")
            println("Выгружаем в Nebula Graph...")

            try {
                val result =
                    runBlocking {
                        metadataGraphService.loadMetadataToGraph(Path.of(configurationPath))
                    }

                println("\n=== Результат выгрузки метаданных в Nebula Graph ===")
                println("Успешно: ${result.success}")
                println("ID конфигурации: ${result.configurationId}")
                println("Количество узлов: ${result.nodesCount}")
                println("Количество ребер: ${result.edgesCount}")

                if (result.errors.isNotEmpty()) {
                    println("\nОшибки:")
                    result.errors.forEach { error -> println("  - $error") }
                }

                if (result.warnings.isNotEmpty()) {
                    println("\nПредупреждения:")
                    result.warnings.forEach { warning -> println("  - $warning") }
                }

                if (result.success) {
                    println("\n✓ Метаданные успешно выгружены в Nebula Graph!")

                    // Показываем обновленную статистику
                    runBlocking {
                        try {
                            val statsQuery = "MATCH (n) RETURN count(n) as totalNodes"
                            val stats = nebulaGraphService.findNodes(statsQuery)
                            if (stats.isNotEmpty()) {
                                val totalNodes = stats.first()["totalNodes"] as? Long ?: 0L
                                println("✓ Общее количество узлов в графе: $totalNodes")
                            }

                            // Показываем статистику по типам
                            val typeStatsQuery =
                                """
                                MATCH (n) 
                                RETURN labels(n)[0] as nodeType, count(n) as count 
                                ORDER BY count DESC
                                """.trimIndent()

                            val typeStats = nebulaGraphService.findNodes(typeStatsQuery)
                            if (typeStats.isNotEmpty()) {
                                println("\nСтатистика по типам узлов:")
                                typeStats.forEach { stat ->
                                    val nodeType = stat["nodeType"]?.toString() ?: "unknown"
                                    val count = stat["count"] as? Long ?: 0L
                                    println("  - $nodeType: $count")
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️  Ошибка при получении статистики: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Ошибка при выгрузке метаданных: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("\nИспользование: java -jar app.jar <путь_к_конфигурации>")
            println("Пример: java -jar app.jar /path/to/configuration/src")
            println("\nДополнительные команды:")
            println("  --test-nebula     - Тестирование подключения к Nebula Graph")
            println("  --stats           - Показать статистику графа")
            println("  --demo            - Запустить демонстрацию работы с Nebula Graph")

            // Проверяем дополнительные аргументы
            if (args.isNotEmpty()) {
                when (args[0]) {
                    "--test-nebula" -> testNebulaConnection()
                    "--stats" -> showGraphStats()
                }
            }
        }
    }

    private fun testNebulaConnection() {
        runBlocking {
            println("\n=== Тестирование подключения к Nebula Graph ===")

            try {
                if (nebulaGraphService.isConnected()) {
                    println("✓ Подключение к Nebula Graph работает")

                    // Тестируем выполнение запроса
                    val testQuery = "SHOW SPACES"
                    val result = nebulaGraphService.executeQuery(testQuery)

                    if (result) {
                        println("✓ Выполнение запросов работает")

                        // Показываем пространства
                        val spaces = nebulaGraphService.findNodes(testQuery)
                        if (spaces.isNotEmpty()) {
                            println("\nДоступные пространства:")
                            spaces.forEach { space ->
                                val spaceName = space["Name"]?.toString() ?: "unknown"
                                println("  - $spaceName")
                            }
                        }
                    } else {
                        println("⚠️  Ошибка при выполнении запросов")
                    }
                } else {
                    println("❌ Не удалось подключиться к Nebula Graph")
                    println("   Проверьте настройки подключения в application.yml")
                }
            } catch (e: Exception) {
                println("❌ Ошибка при тестировании подключения: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showGraphStats() {
        runBlocking {
            println("\n=== Статистика графа знаний ===")

            try {
                if (!nebulaGraphService.isConnected()) {
                    println("❌ Нет подключения к Nebula Graph")
                    return@runBlocking
                }

                // Общая статистика
                val totalNodesQuery = "MATCH (n) RETURN count(n) as totalNodes"
                val totalEdgesQuery = "MATCH ()-[r]->() RETURN count(r) as totalEdges"

                val totalNodes = nebulaGraphService.findNodes(totalNodesQuery)
                val totalEdges = nebulaGraphService.findNodes(totalEdgesQuery)

                val nodeCount = totalNodes.firstOrNull()?.get("totalNodes") as? Long ?: 0L
                val edgeCount = totalEdges.firstOrNull()?.get("totalEdges") as? Long ?: 0L

                println("Всего узлов: $nodeCount")
                println("Всего рёбер: $edgeCount")

                // Статистика по типам узлов
                val typeStatsQuery =
                    """
                    MATCH (n) 
                    RETURN labels(n)[0] as nodeType, count(n) as count 
                    ORDER BY count DESC
                    """.trimIndent()

                val typeStats = nebulaGraphService.findNodes(typeStatsQuery)
                if (typeStats.isNotEmpty()) {
                    println("\nУзлы по типам:")
                    typeStats.forEach { stat ->
                        val nodeType = stat["nodeType"]?.toString() ?: "unknown"
                        val count = stat["count"] as? Long ?: 0L
                        println("  - $nodeType: $count")
                    }
                }

                // Статистика по типам рёбер
                val edgeTypeStatsQuery =
                    """
                    MATCH ()-[r]->() 
                    RETURN type(r) as edgeType, count(r) as count 
                    ORDER BY count DESC
                    """.trimIndent()

                val edgeTypeStats = nebulaGraphService.findNodes(edgeTypeStatsQuery)
                if (edgeTypeStats.isNotEmpty()) {
                    println("\nРёбра по типам:")
                    edgeTypeStats.forEach { stat ->
                        val edgeType = stat["edgeType"]?.toString() ?: "unknown"
                        val count = stat["count"] as? Long ?: 0L
                        println("  - $edgeType: $count")
                    }
                }
            } catch (e: Exception) {
                println("❌ Ошибка при получении статистики: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
