package ru.alkoleft.context.infrastructure.graph

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import ru.alkoleft.context.domain.graph.EdgeType
import ru.alkoleft.context.domain.graph.GraphEdge
import ru.alkoleft.context.domain.graph.GraphNode
import ru.alkoleft.context.domain.graph.MDObjectNode
import ru.alkoleft.context.domain.graph.NodeType

/**
 * Тест для NebulaRepository с фокусом на метод findRelatedNodes
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
class NebulaRepositoryTest {
    @Autowired
    private lateinit var nebulaRepository: NebulaRepository

    @Autowired
    private lateinit var nebulaGraphService: NebulaGraphService

    @BeforeEach
    fun setUp() {
        // Проверяем подключение к Nebula Graph
        if (!nebulaGraphService.isConnected()) {
            throw IllegalStateException("Не удалось подключиться к Nebula Graph. Убедитесь, что сервер запущен.")
        }
    }

    /**
     * Создает тестовый узел
     */
    private fun createTestNode(id: String, name: String, type: NodeType): GraphNode {
        return MDObjectNode(
            uid = id,
            name = name,
            synonym = name,
            type = type,
            id = id
        )
    }

    /**
     * Очищает тестовые данные
     */
    private fun cleanupTestData(nodeIds: List<String>) {
        try {
            nodeIds.forEach { nodeId ->
                val deleteQuery = "DELETE VERTEX \"$nodeId\""
                nebulaGraphService.executeQuery(deleteQuery)
            }
            println("✓ Тестовые данные очищены")
        } catch (e: Exception) {
            println("Предупреждение: не удалось очистить тестовые данные: ${e.message}")
        }
    }
}
