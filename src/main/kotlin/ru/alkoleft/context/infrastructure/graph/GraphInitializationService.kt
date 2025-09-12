package ru.alkoleft.context.infrastructure.graph

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.alkoleft.context.domain.graph.EdgeType
import ru.alkoleft.context.domain.graph.NodeType

/**
 * Сервис для инициализации графа знаний
 */
@Service
class GraphInitializationService(
    private val nebulaGraphService: NebulaGraphService,
    private val nebulaProperties: NebulaGraphProperties,
) {
    private val logger = LoggerFactory.getLogger(GraphInitializationService::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun initializeGraph() {
        logger.info("Начинаем инициализацию графа знаний...")

        try {
            // Создаем пространство, если оно не существует
            if (nebulaGraphService.createSpaceIfNotExists(nebulaProperties.space)) {
                logger.info("Пространство '${nebulaProperties.space}' создано или уже существует")
            } else {
                logger.warn("Не удалось создать пространство '${nebulaProperties.space}'")
                return
            }

            // Создаем теги (вершины)
            createNodeTags()

            // Создаем рёбра
            createEdgeTypes()

            logger.info("Инициализация графа знаний завершена успешно")
        } catch (e: Exception) {
            logger.error("Ошибка при инициализации графа знаний: ${e.message}", e)
        }
    }

    private fun createNodeTags() {
        val nodeProperties =
            mapOf(
                "id" to "string",
                "type" to "string",
                "name" to "string",
                "description" to "string",
                "labels" to "string",
                "created_at" to "timestamp",
                "updated_at" to "timestamp",
            )

        if (nebulaGraphService.createTag("Node", nodeProperties)) {
            logger.info("Тег 'Node' создан успешно")
        } else {
            logger.warn("Не удалось создать тег 'Node'")
        }
    }

    private fun createEdgeTypes() {
        val edgeProperties =
            mapOf(
                "type" to "string",
                "weight" to "double",
                "created_at" to "timestamp",
            )

        if (nebulaGraphService.createEdge("Edge", edgeProperties)) {
            logger.info("Тип рёбер 'Edge' создан успешно")
        } else {
            logger.warn("Не удалось создать тип рёбер 'Edge'")
        }
    }

    /**
     * Создает базовую структуру графа знаний для 1С
     */
    fun createBasicStructure() {
        logger.info("Создаем базовую структуру графа знаний для 1С...")

        try {
            // Создаем корневой узел конфигурации
            val configNode =
                ru.alkoleft.context.domain.graph.GraphNode(
                    id = "config_root",
                    type = NodeType.CONCEPT,
                    properties =
                        mapOf(
                            "name" to "Конфигурация 1С",
                            "description" to "Корневой узел конфигурации 1С:Предприятие",
                        ),
                )

            // Создаем узлы для основных типов объектов
            val objectTypes =
                listOf(
                    "Справочники" to "Справочники системы",
                    "Документы" to "Документы системы",
                    "Регистры" to "Регистры системы",
                    "Отчеты" to "Отчеты системы",
                    "Обработки" to "Обработки системы",
                    "ПланыСчетов" to "Планы счетов",
                    "ПланыВидовХарактеристик" to "Планы видов характеристик",
                    "ПланыВидовРасчета" to "Планы видов расчета",
                    "ПланыОбмена" to "Планы обмена",
                    "БизнесПроцессы" to "Бизнес-процессы",
                    "Задачи" to "Задачи",
                )

            objectTypes.forEach { (name, description) ->
                val nodeId = "object_type_${name.lowercase().replace(" ", "_")}"
                val node =
                    ru.alkoleft.context.domain.graph.GraphNode(
                        id = nodeId,
                        type = NodeType.CONCEPT,
                        properties =
                            mapOf(
                                "name" to name,
                                "description" to description,
                                "object_type" to name,
                            ),
                    )

                // Сохраняем узел
                if (nebulaGraphService.insertVertex("Node", nodeId, node.properties)) {
                    logger.debug("Создан узел: $name")

                    // Создаем связь с корневым узлом
                    val edge =
                        ru.alkoleft.context.domain.graph.GraphEdge(
                            sourceId = "config_root",
                            targetId = nodeId,
                            type = EdgeType.CONTAINS,
                            properties = mapOf("weight" to 1.0),
                        )

                    nebulaGraphService.insertEdge("Edge", "config_root", nodeId, edge.properties)
                }
            }

            logger.info("Базовая структура графа знаний создана")
        } catch (e: Exception) {
            logger.error("Ошибка при создании базовой структуры: ${e.message}", e)
        }
    }
}
