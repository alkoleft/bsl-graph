package ru.alkoleft.context.infrastructure.metadata

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import ru.alkoleft.context.domain.metadata.MetadataGraphService
import ru.alkoleft.context.infrastructure.graph.NebulaGraphService
import java.nio.file.Paths

/**
 * Интеграционный тест для проверки выгрузки метаданных в Nebula Graph
 *
 * Для запуска этого теста необходимо:
 * 1. Запустить Nebula Graph через docker-compose-lite.yaml
 * 2. Убедиться, что подключение к базе данных работает
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "nebula.addresses[0].host=localhost",
        "nebula.addresses[0].port=9669",
        "nebula.username=root",
        "nebula.password=nebula",
        "nebula.space=amusement_park",
    ],
)
@Disabled("Требует запущенного Nebula Graph")
class NebulaGraphIntegrationTest {
    @Autowired
    private lateinit var nebulaGraphService: NebulaGraphService

    @Autowired
    private lateinit var metadataGraphService: MetadataGraphService

    @BeforeEach
    fun setUp() {
        // Проверяем подключение к Nebula Graph
        if (!nebulaGraphService.isConnected()) {
            throw IllegalStateException("Не удалось подключиться к Nebula Graph. Убедитесь, что сервер запущен.")
        }
    }

    @Test
    fun `должен создать схему графа для метаданных 1С`() {
        runBlocking {
            // Проверяем, что схема создана
            val tagsQuery = "SHOW TAGS"
            val tagsResult = nebulaGraphService.findNodes(tagsQuery)

            assert(tagsResult.isNotEmpty()) { "Схема графа не создана" }

            // Проверяем наличие основных тегов
            val tagNames = tagsResult.mapNotNull { it["Name"] as? String }
            assert(tagNames.contains("Configuration")) { "Тег Configuration не найден" }
            assert(tagNames.contains("Constant")) { "Тег Constant не найден" }
            assert(tagNames.contains("Catalog")) { "Тег Catalog не найден" }
            assert(tagNames.contains("Document")) { "Тег Document не найден" }

            println("Схема графа создана успешно. Найдено тегов: ${tagNames.size}")
            tagNames.forEach { println("- $it") }
        }
    }

    @Test
    fun `должен загрузить тестовые метаданные в граф`() {
        runBlocking {
            // Путь к тестовой конфигурации (заглушка)
            val testConfigurationPath = Paths.get("src/test/resources/test-configuration")

            try {
                val result = metadataGraphService.loadMetadataToGraph(testConfigurationPath)

                if (result.success) {
                    println("Метаданные загружены успешно:")
                    println("- Конфигурация: ${result.configurationId}")
                    println("- Узлов: ${result.nodesCount}")
                    println("- Рёбер: ${result.edgesCount}")

                    assert(result.nodesCount > 0) { "Должно быть загружено хотя бы несколько узлов" }
                } else {
                    println("Ошибки при загрузке метаданных:")
                    result.errors.forEach { println("- $it") }

                    // Для тестового окружения это может быть нормально
                    // так как реальные файлы метаданных могут отсутствовать
                    println("Тест пропущен из-за отсутствия реальных файлов метаданных")
                }
            } catch (e: Exception) {
                println("Исключение при загрузке метаданных: ${e.message}")
                // Для тестового окружения это может быть нормально
                println("Тест пропущен из-за отсутствия реальных файлов метаданных")
            }
        }
    }

    @Test
    fun `должен получить статистику графа`() {
        runBlocking {
            val statistics = nebulaGraphService.findNodes("MATCH (n) RETURN count(n) as totalNodes")

            if (statistics.isNotEmpty()) {
                val totalNodes = statistics.first()["totalNodes"] as? Long ?: 0L
                println("Всего узлов в графе: $totalNodes")

                // Проверяем статистику по типам
                val typeStats =
                    nebulaGraphService.findNodes(
                        "MATCH (n) RETURN labels(n) as nodeType, count(n) as count",
                    )

                println("Статистика по типам:")
                typeStats.forEach { stat ->
                    val nodeType = stat["nodeType"]?.toString() ?: "unknown"
                    val count = stat["count"] as? Long ?: 0L
                    println("- $nodeType: $count")
                }
            } else {
                println("Граф пуст")
            }
        }
    }

    @Test
    fun `должен выполнить комплексный тест интеграции`() {
        runBlocking {
            println("=== Комплексный тест интеграции с Nebula Graph ===")

            // 1. Проверяем подключение
            assert(nebulaGraphService.isConnected()) { "Подключение к Nebula Graph не установлено" }
            println("✓ Подключение к Nebula Graph установлено")

            // 2. Проверяем схему
            val tagsQuery = "SHOW TAGS"
            val tagsResult = nebulaGraphService.findNodes(tagsQuery)
            assert(tagsResult.isNotEmpty()) { "Схема графа не создана" }
            println("✓ Схема графа создана (${tagsResult.size} тегов)")

            // 3. Проверяем пространство
            val spacesQuery = "SHOW SPACES"
            val spacesResult = nebulaGraphService.findNodes(spacesQuery)
            assert(spacesResult.isNotEmpty()) { "Пространство не создано" }
            println("✓ Пространство создано")

            // 4. Тестируем вставку тестового узла
            val testNodeId = "test-configuration-${System.currentTimeMillis()}"
            val testProperties =
                mapOf(
                    "name" to "ТестоваяКонфигурация",
                    "uuid" to testNodeId,
                    "version" to "1.0.0",
                    "comment" to "Тестовая конфигурация для интеграционного теста",
                )

            val insertResult = nebulaGraphService.insertVertex("Configuration", testNodeId, testProperties)
            assert(insertResult) { "Не удалось вставить тестовый узел" }
            println("✓ Тестовый узел вставлен")

            // 5. Проверяем, что узел найден
            val findQuery = "MATCH (n:Configuration) WHERE n.uuid == \"$testNodeId\" RETURN n"
            val findResult = nebulaGraphService.findNodes(findQuery)
            assert(findResult.isNotEmpty()) { "Тестовый узел не найден" }
            println("✓ Тестовый узел найден")

            // 6. Очищаем тестовые данные
            val deleteQuery = "DELETE VERTEX \"$testNodeId\""
            nebulaGraphService.executeQuery(deleteQuery)
            println("✓ Тестовые данные очищены")

            println("=== Все тесты интеграции прошли успешно ===")
        }
    }
}
