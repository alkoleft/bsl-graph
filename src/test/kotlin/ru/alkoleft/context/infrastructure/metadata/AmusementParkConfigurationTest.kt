package ru.alkoleft.context.infrastructure.metadata

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import ru.alkoleft.context.domain.metadata.MetadataGraphService
import ru.alkoleft.context.infrastructure.graph.NebulaGraphService
import java.nio.file.Paths

/**
 * Тест для загрузки реальной конфигурации "УправлениеПаркомАттракционов" в Nebula Graph
 *
 * Этот тест загружает полноценную конфигурацию 1С с множеством объектов:
 * - Справочники (46 объектов)
 * - Документы (9 объектов)
 * - Перечисления (50 объектов)
 * - Константы (77 объектов)
 * - Общие модули (279 объектов)
 * - Регистры сведений (84 объекта)
 * - Отчеты (31 объект)
 * - И многие другие объекты метаданных
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
class AmusementParkConfigurationTest {
    @Autowired
    private lateinit var nebulaGraphService: NebulaGraphService

    @Autowired
    private lateinit var metadataGraphService: MetadataGraphService

    private val configurationPath = "/home/alko/develop/open-source/amusement-park-vibe/УправлениеПаркомАттракционов"
//    private val configurationPath = "/home/alko/develop/bia/tms/src/configuration/"

    @BeforeEach
    fun setUp() {
        println("\n=== Тест конфигурации 'УправлениеПаркомАттракционов' ===")
        println("Путь к конфигурации: $configurationPath")

        // Проверяем подключение к Nebula Graph
        if (!nebulaGraphService.isConnected()) {
            println("⚠️  Не удалось подключиться к Nebula Graph")
            println("   Убедитесь, что Nebula Graph запущен: docker-compose -f docker-compose-lite.yaml up -d")
            return
        }

        println("✓ Подключение к Nebula Graph установлено")

        // Показываем начальную статистику
        runBlocking {
            try {
                val statsQuery = "MATCH (n) RETURN count(n) as totalNodes"
                val stats = nebulaGraphService.findNodes(statsQuery)
                if (stats.isNotEmpty()) {
                    val totalNodes = stats.first()["totalNodes"] as? Long ?: 0L
                    println("✓ Начальное количество узлов в графе: $totalNodes")
                }
            } catch (e: Exception) {
                println("⚠️  Ошибка при получении статистики: ${e.message}")
            }
        }
    }

    @Test
    fun `загружает конфигурацию УправлениеПаркомАттракционов в Nebula Graph`() {
        runBlocking {
            println("\n--- Загрузка конфигурации 'УправлениеПаркомАттракционов' ---")

            try {
                val startTime = System.currentTimeMillis()

                // Загружаем метаданные конфигурации
                val result = metadataGraphService.loadMetadataToGraph(Paths.get(configurationPath))

                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                println("\n=== Результат загрузки ===")
                println("Успешно: ${result.success}")
                println("ID конфигурации: ${result.configurationId}")
                println("Загружено узлов: ${result.nodesCount}")
                println("Загружено рёбер: ${result.edgesCount}")
                println("Время выполнения: ${duration}ms")

                if (result.errors.isNotEmpty()) {
                    println("\nОшибки:")
                    result.errors.forEach { error -> println("  ❌ $error") }
                }

                if (result.warnings.isNotEmpty()) {
                    println("\nПредупреждения:")
                    result.warnings.forEach { warning -> println("  ⚠️  $warning") }
                }

                if (result.success) {
                    println("\n✓ Конфигурация успешно загружена в Nebula Graph!")
                } else {
                    println("\n❌ Загрузка конфигурации не удалась")
                }
            } catch (e: Exception) {
                println("\n❌ Исключение при загрузке конфигурации: ${e.message}")
                e.printStackTrace()
            }
        }
    }

}
