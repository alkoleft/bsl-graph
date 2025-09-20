package ru.alkoleft.context.infrastructure.graph

import com.vesoft.nebula.Value
import com.vesoft.nebula.client.graph.NebulaPoolConfig
import com.vesoft.nebula.client.graph.data.HostAddress
import com.vesoft.nebula.client.graph.data.ResultSet
import com.vesoft.nebula.client.graph.data.ValueWrapper
import com.vesoft.nebula.client.graph.exception.AuthFailedException
import com.vesoft.nebula.client.graph.exception.IOErrorException
import com.vesoft.nebula.client.graph.exception.NotValidConnectionException
import com.vesoft.nebula.client.graph.net.NebulaPool
import com.vesoft.nebula.client.graph.net.Session
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.alkoleft.context.domain.graph.GraphEdge
import ru.alkoleft.context.domain.graph.GraphNode

/**
 * Сервис для работы с NebulaGraph
 * Реализация с использованием официального Java клиента
 */
@Service
class NebulaGraphService(
    private val nebulaProperties: NebulaGraphProperties,
    private val initializer: SchemaInitializer
) {
    private val logger = LoggerFactory.getLogger(NebulaGraphService::class.java)

    private var isConnected = false
    private var nebulaPool: NebulaPool? = null
    private var session: Session? = null

    init {
        initializeConnection()
    }

    fun connected() = isConnected && session != null


    /**
     * Инициализирует подключение к NebulaGraph
     */
    private fun initializeConnection() {
        try {
            logger.info("Инициализация подключения к NebulaGraph...")
            logger.info("Настройки подключения:")
            logger.info("- Хост: ${nebulaProperties.addresses.firstOrNull()?.host ?: "localhost"}")
            logger.info("- Порт: ${nebulaProperties.addresses.firstOrNull()?.port ?: 9669}")
            logger.info("- Пользователь: ${nebulaProperties.username}")
            logger.info("- Пространство: ${nebulaProperties.space}")

            // Создаем список адресов хостов
            val addresses =
                nebulaProperties.addresses.map { address ->
                    HostAddress(address.host, address.port)
                }

            // Создаем конфигурацию пула соединений
            val config =
                NebulaPoolConfig().apply {
                    maxConnSize = nebulaProperties.maxConnSize
                    minConnSize = nebulaProperties.minConnSize
                    timeout = nebulaProperties.timeout
                }

            // Создаем и инициализируем пул соединений
            nebulaPool = NebulaPool()
            val initResult = nebulaPool?.init(addresses, config)
            if (initResult != true) {
                throw RuntimeException("Не удалось инициализировать пул соединений NebulaGraph")
            }

            // Получаем сессию
            session = nebulaPool?.getSession(nebulaProperties.username, nebulaProperties.password, false)
            if (session == null) {
                throw RuntimeException("Не удалось получить сессию NebulaGraph")
            }

            // Привязываемся к пространству
            val useSpaceResult = setSpace()
            if (useSpaceResult?.isSucceeded != true) {
                logger.warn("Не удалось переключиться на пространство ${nebulaProperties.space}, создаем его...")
                initializer.createSpace(session!!, nebulaProperties.space)
                for (i in 0..10) {
                    Thread.sleep(1000)
                    val retryResult = setSpace()
                    if (retryResult?.isSucceeded != true) {
                        if (i == 10) {
                            throw RuntimeException("Не удалось переключиться на пространство ${nebulaProperties.space}. ${retryResult.errorMessage}")
                        } else {
                            logger.info("Не удалось переключиться на пространство ${nebulaProperties.space}. ${retryResult.errorMessage}")
                        }
                    } else {
                        break
                    }
                }
                // Создаем схему графа
                initializer.createGraphSchema(session!!)
            }

            isConnected = true

            logger.info("Успешно подключились к NebulaGraph")
        } catch (e: AuthFailedException) {
            logger.error("Ошибка аутентификации при подключении к NebulaGraph", e)
            isConnected = false
        } catch (e: NotValidConnectionException) {
            logger.error("Неверные параметры подключения к NebulaGraph", e)
            isConnected = false
//        } catch (e: BindSpaceFailedException) {
//            logger.error("Ошибка привязки к пространству NebulaGraph", e)
//            isConnected = false
        } catch (e: IOErrorException) {
            logger.error("Ошибка ввода-вывода при подключении к NebulaGraph", e)
            isConnected = false
        } catch (e: Exception) {
            logger.error("Неожиданная ошибка при инициализации подключения к NebulaGraph", e)
            isConnected = false
        }
    }

    private fun setSpace() =
        session!!.execute("USE ${nebulaProperties.space}").also {
            if (it.isSucceeded) {
                logger.warn("Переключились на пространство ${nebulaProperties.space}")
            } else {
                logger.warn("Не удалось переключиться на пространство ${nebulaProperties.space}")
            }
        }

    /**
     * Выполняет запрос к NebulaGraph
     */
    fun executeQuery(query: String): Boolean =
        try {
            logger.debug("Выполнение запроса к NebulaGraph: $query")
            checkConnection()

            val result = session?.execute(query)
            val success = result?.isSucceeded == true

            if (success) {
                logger.debug("Запрос выполнен успешно")
            } else {
                logger.error("Ошибка выполнения запроса: ${result?.errorMessage}")
            }

            success
        } catch (e: Exception) {
            logger.error("Исключение при выполнении запроса: $query", e)
            false
        }

    /**
     * Выполняет запрос с параметрами
     */
    fun executeQueryWithParams(
        query: String,
        params: Map<String, Any>,
    ): Boolean =
        try {
            logger.debug("Выполнение параметризованного запроса к NebulaGraph: $query с параметрами: $params")
            checkConnection()

            // Простая замена параметров в строке запроса
            var finalQuery = query
            params.forEach { (key, value) ->
                val placeholder = "$$key"
                val valueStr =
                    when (value) {
                        is String -> "'$value'"
                        is Boolean -> value.toString()
                        is Number -> value.toString()
                        else -> "'$value'"
                    }
                finalQuery = finalQuery.replace(placeholder, valueStr)
            }

            executeQuery(finalQuery)
        } catch (e: Exception) {
            logger.error("Исключение при выполнении параметризованного запроса: $query", e)
            false
        }

    /**
     * Проверяет подключение к NebulaGraph
     */
    fun isConnected(): Boolean =
        try {
            logger.debug("Проверка подключения к NebulaGraph")
            isConnected
        } catch (e: Exception) {
            logger.warn("Проверка подключения к NebulaGraph не удалась", e)
            false
        }

    /**
     * Проверяет подключение к NebulaGraph и выбрасывает исключение при отсутствии соединения
     * @throws IllegalStateException если нет активного соединения
     */
    private fun checkConnection() {
        if (!connected()) {
            throw IllegalStateException("Нет активного соединения с NebulaGraph")
        }
    }

    /**
     * Вставляет вершину в граф
     */
    fun insertVertex(
        tagName: String,
        vertexId: String,
        properties: Map<String, Any>,
    ): Boolean =
        try {
            logger.debug("Вставка вершины NebulaGraph: $tagName с ID: $vertexId и свойствами: $properties")
            checkConnection()

            // Формируем строку свойств для INSERT запроса
            val propertiesStr =
                if (properties.isNotEmpty()) {
                    properties.keys.joinToString(", ")
                } else {
                    ""
                }

            // Создаем INSERT запрос для вершины
            val insertQuery =
                if (propertiesStr.isNotEmpty()) {
                    "INSERT VERTEX $tagName ($propertiesStr) VALUES '$vertexId':(${
                        properties.values.joinToString(", ") {
                            when (it) {
                                is String -> "'$it'"
                                is Boolean -> it.toString()
                                is Number -> it.toString()
                                else -> "'$it'"
                            }
                        }
                    })"
                } else {
                    "INSERT VERTEX $tagName VALUES '$vertexId'"
                }

            val result = session?.execute(insertQuery)
            val success = result?.isSucceeded == true

            if (success) {
                logger.debug("Вершина $vertexId вставлена успешно")
            } else {
                logger.error("Ошибка вставки вершины $vertexId: ${result?.errorMessage}")
            }

            success
        } catch (e: Exception) {
            logger.error("Исключение при вставке вершины: $vertexId", e)
            false
        }

    /**
     * Вставляет вершину в граф
     */
    fun insertVertex(
        tagName: String,
        node: GraphNode,
    ): Boolean =
        try {
            logger.debug("Вставка вершины NebulaGraph: $tagName с ID: ${node.id}")
            checkConnection()

            // Формируем строку свойств для INSERT запроса
            val propertiesStr = QueryMapper.properties(node).joinToString(", ")

            // Создаем INSERT запрос для вершины
            val insertQuery =
                if (propertiesStr.isNotEmpty()) {
                    "INSERT VERTEX $tagName ($propertiesStr) VALUES '${node.id}':(${
                        QueryMapper.values(node).joinToString(", ") {
                            when (it) {
                                is String -> "'$it'"
                                is Boolean -> it.toString()
                                is Number -> it.toString()
                                else -> "'$it'"
                            }
                        }
                    })"
                } else {
                    "INSERT VERTEX $tagName VALUES '${node.id}'"
                }

            val result = session?.execute(insertQuery)
            val success = result?.isSucceeded == true

            if (success) {
                logger.debug("Вершина ${node.id} вставлена успешно")
            } else {
                logger.error("Ошибка вставки вершины ${node.id}: ${result?.errorMessage}")
            }

            success
        } catch (e: Exception) {
            logger.error("Исключение при вставке вершины: ${node.id}", e)
            false
        }


    /**
     * Вставляет ребро в граф
     */
    fun insertEdge(
        edgeName: String,
        edge: GraphEdge,
    ): Boolean = try {
        logger.debug("Вставка ребра NebulaGraph: $edgeName от ${edge.sourceId} к ${edge.targetId}")
        checkConnection()

        val keys = QueryMapper.properties(edge)
        // Формируем строку свойств для INSERT запроса
        val propertiesStr =
            if (keys.isNotEmpty()) {
                keys.joinToString(", ")
            } else {
                ""
            }

        // Создаем INSERT запрос для ребра
        val insertQuery =
            if (keys.isNotEmpty()) {
                "INSERT EDGE $edgeName($propertiesStr) VALUES '${edge.sourceId}'->'${edge.targetId}':(${
                    QueryMapper.values(edge).joinToString(", ") {
                        when (it) {
                            is String -> "'$it'"
                            is Boolean -> it.toString()
                            is Number -> it.toString()
                            else -> "'$it'"
                        }
                    }
                })"
            } else {
                "INSERT EDGE $edgeName() VALUES '${edge.sourceId}'->'${edge.targetId}':()"
            }

        val result = session?.execute(insertQuery)
        val success = result?.isSucceeded == true

        if (success) {
            logger.debug("Ребро '${edge.sourceId}'->'${edge.targetId}' вставлено успешно")
        } else {
            logger.error("Ошибка вставки ребра '${edge.sourceId}'->'${edge.targetId}': ${result?.errorMessage}")
        }

        success
    } catch (e: Exception) {
        logger.error("Исключение при вставке ребра: '${edge.sourceId}'->'${edge.targetId}'", e)
        false
    }

    fun execute(query: String): ResultSet? {
        try {
            checkConnection()
            val result = session?.execute(query)
            if (result?.isSucceeded != true) {
                logger.error("Ошибка выполнения поиска: ${result?.errorMessage}")
                return null
            }
            return result
        } catch (e: Exception) {
            logger.error("Исключение при поиске узлов: $query", e)
        }
        return null
    }

    fun executeAsSequence(query: String): Sequence<Map<String, ValueWrapper>> = sequence {
        val result = execute(query)

        if (result != null) {
            val values = mutableMapOf<String, ValueWrapper>()

            for (i in 0 until result.rowsSize()) {
                val record = result.rowValues(i)
                result.keys().forEach { values.put(it, record.get(it)) }
                yield(values)
            }
        }
    }


    /**
     * Выполняет поиск узлов
     */
    fun findNodes(query: String): List<Map<String, Any>> {
        try {
            logger.debug("Поиск узлов: $query")
            val result = execute(query)

            if (result == null) {
                return emptyList()
            }
            val nodes = mutableListOf<Map<String, Any>>()

            // Обрабатываем результат запроса
            result.rows?.forEach { row ->
                val nodeData = mutableMapOf<String, Any>()

                // Получаем имена колонок
                val columnNames = result.columnNames

                // Обрабатываем каждую колонку
                columnNames.forEachIndexed { index, columnName ->
                    val value = row.values[index]
                    nodeData[columnName] = extractValue(value)
                }

                if (nodeData.isNotEmpty()) {
                    nodes.add(nodeData)
                }
            }

            logger.debug("Найдено узлов: ${nodes.size}")
            return nodes
        } catch (e: Exception) {
            logger.error("Исключение при поиске узлов: $query", e)
        }
        return emptyList()
    }

    /**
     * Извлекает значение из Value объекта Nebula Graph
     */
    private fun extractValue(value: Value): Any =
        try {
            when (value.setField) {
                Value.NVAL -> "NULL" // Null value - возвращаем строку вместо null для совместимости
                Value.BVAL -> value.fieldValue as Boolean // Boolean
                Value.IVAL -> value.iVal // Long
                Value.FVAL -> value.fVal // Double
                Value.SVAL -> String(value.sVal, Charsets.UTF_8) // String from byte array
                Value.DVAL -> value.dVal.toString() // Date
                Value.TVAL -> value.tVal.toString() // Time
                Value.DTVAL -> value.dtVal.toString() // DateTime
                Value.LVAL -> { // List
                    value.lVal.values.map { extractValue(it) }
                }

                Value.UVAL -> { // Set
                    value.uVal.values
                        .map { extractValue(it) }
                        .toSet()
                }

                Value.MVAL -> { // Map
                    val resultMap = mutableMapOf<String, Any>()
                    value.mVal.kvs.forEach { (key, v) ->
                        resultMap[String(key, Charsets.UTF_8)] = extractValue(v)
                    }
                    resultMap
                }

                Value.VVAL -> { // Vertex
                    val vertex = value.vVal
                    val resultMap =
                        mutableMapOf<String, Any>(
                            "vid" to extractValue(vertex.vid),
                        )

                    // Добавляем теги и их свойства
                    if (vertex.tags.isNotEmpty()) {
                        val tagsMap = mutableMapOf<String, Map<String, Any>>()
                        vertex.tags.forEach { tag ->
                            val tagProps = mutableMapOf<String, Any>()
                            tag.props.forEach { (propKey, propValue) ->
                                tagProps[String(propKey, Charsets.UTF_8)] = extractValue(propValue)
                            }
                            tagsMap[String(tag.name, Charsets.UTF_8)] = tagProps
                        }
                        resultMap["tags"] = tagsMap
                    }

                    resultMap
                }

                Value.EVAL -> { // Edge
                    val edge = value.eVal
                    mapOf(
                        // Преобразуем бинарные идентификаторы в строки, как и для вершин
                        "src" to String(edge.src.sVal, Charsets.UTF_8),
                        "dst" to String(edge.dst.sVal, Charsets.UTF_8),
                        "type" to edge.type,
                        "name" to String(edge.name, Charsets.UTF_8),
                        "ranking" to edge.ranking,
                        "props" to
                            edge.props
                                .mapKeys { String(it.key, Charsets.UTF_8) }
                                .mapValues { extractValue(it.value) },
                    )
                }

                Value.PVAL -> { // Path
                    val path = value.pVal
                    mapOf(
                        "src" to extractValue(path.src.vid),
                        "steps" to
                            path.steps.map { step ->
                                mapOf(
                                    "dst" to extractValue(step.dst.vid),
                                    "type" to step.type,
                                    "name" to String(step.name, Charsets.UTF_8),
                                    "ranking" to step.ranking,
                                    "props" to
                                        step.props
                                            .mapKeys { String(it.key, Charsets.UTF_8) }
                                            .mapValues { extractValue(it.value) },
                                )
                            },
                    )
                }

                Value.GVAL -> { // DataSet
                    val dataset = value.gVal
                    mapOf(
                        "column_names" to dataset.column_names.map { String(it, Charsets.UTF_8) },
                        "rows" to
                            dataset.rows.map { row ->
                                row.values.map { extractValue(it) }
                            },
                    )
                }

                Value.GGVAL -> { // Geography
                    value.ggVal.toString()
                }

                Value.DUVAL -> { // Duration
                    val duration = value.duVal
                    mapOf(
                        "seconds" to duration.seconds,
                        "microseconds" to duration.microseconds,
                        "months" to duration.months,
                    )
                }

                else -> {
                    logger.warn("Неизвестный тип Value: ${value.setField}")
                    value.toString()
                }
            }
        } catch (e: Exception) {
            logger.warn("Ошибка при извлечении значения из Value: ${e.message}", e)
            "ERROR: ${e.message}"
        }

    /**
     * Выполняет поиск вершин по тегу
     */
    fun findVerticesByTag(
        tagName: String,
        limit: Int = 100,
    ): List<Map<String, Any>> =
        try {
            val query = "MATCH (v:$tagName) RETURN v LIMIT $limit"
            findNodes(query)
        } catch (e: Exception) {
            logger.error("Исключение при поиске вершин по тегу: $tagName", e)
            emptyList()
        }

    /**
     * Выполняет поиск рёбер между вершинами
     */
    fun findEdgesBetweenVertices(
        srcId: String,
        dstId: String,
        edgeType: String? = null,
    ): List<Map<String, Any>> =
        try {
            val edgeFilter = if (edgeType != null) ":$edgeType" else ""
            val query =
                "MATCH (src)-[e$edgeFilter]->(dst) WHERE id(src) == '$srcId' AND id(dst) == '$dstId' RETURN e"
            findNodes(query)
        } catch (e: Exception) {
            logger.error("Исключение при поиске рёбер между вершинами: $srcId -> $dstId", e)
            emptyList()
        }

    /**
     * Выполняет поиск соседних вершин
     */
    fun findNeighbors(
        vertexId: String,
        edgeType: String? = null,
        direction: String = "BOTH",
        limit: Int = 100,
    ): List<Map<String, Any>> =
        try {
            val edgeFilter = if (edgeType != null) ":$edgeType" else ""
            val query =
                when (direction.uppercase()) {
                    "OUT" -> "MATCH (v)-[e$edgeFilter]->(n) WHERE id(v) == '$vertexId' RETURN n LIMIT $limit"
                    "IN" -> "MATCH (v)<-[e$edgeFilter]-(n) WHERE id(v) == '$vertexId' RETURN n LIMIT $limit"
                    else -> "MATCH (v)-[e$edgeFilter]-(n) WHERE id(v) == '$vertexId' RETURN n LIMIT $limit"
                }
            findNodes(query)
        } catch (e: Exception) {
            logger.error("Исключение при поиске соседних вершин: $vertexId", e)
            emptyList()
        }

    /**
     * Обновляет свойства вершины
     */
    fun updateVertexProperties(
        vertexId: String,
        properties: Map<String, Any>,
    ): Boolean =
        try {
            logger.debug("Обновление свойств вершины: $vertexId с свойствами: $properties")
            checkConnection()

            val propertiesStr =
                properties.entries.joinToString(", ") { (key, value) ->
                    val valueStr =
                        when (value) {
                            is String -> "'$value'"
                            is Boolean -> value.toString()
                            is Number -> value.toString()
                            else -> "'$value'"
                        }
                    "$key: $valueStr"
                }

            val updateQuery = "UPDATE VERTEX '$vertexId' SET $propertiesStr"
            val result = session?.execute(updateQuery)
            val success = result?.isSucceeded() == true

            if (success) {
                logger.debug("Свойства вершины $vertexId обновлены успешно")
            } else {
                logger.error("Ошибка обновления свойств вершины $vertexId: ${result?.errorMessage}")
            }

            success
        } catch (e: Exception) {
            logger.error("Исключение при обновлении свойств вершины: $vertexId", e)
            false
        }


    /**
     * Удаляет вершину
     */
    fun deleteVertex(vertexId: String): Boolean =
        try {
            logger.debug("Удаление вершины: $vertexId")
            checkConnection()

            val deleteQuery = "DELETE VERTEX '$vertexId'"
            val result = session?.execute(deleteQuery)
            val success = result?.isSucceeded == true

            if (success) {
                logger.debug("Вершина $vertexId удалена успешно")
            } else {
                logger.error("Ошибка удаления вершины $vertexId: ${result?.errorMessage}")
            }

            success
        } catch (e: Exception) {
            logger.error("Исключение при удалении вершины: $vertexId", e)
            false
        }

    /**
     * Удаляет ребро
     */
    fun deleteEdge(
        srcId: String,
        dstId: String,
        edgeType: String,
    ): Boolean =
        try {
            logger.debug("Удаление ребра: $srcId -> $dstId ($edgeType)")
            checkConnection()

            val deleteQuery = "DELETE EDGE $edgeType '$srcId' -> '$dstId'"
            val result = session?.execute(deleteQuery)
            val success = result?.isSucceeded() == true

            if (success) {
                logger.debug("Ребро $srcId -> $dstId удалено успешно")
            } else {
                logger.error("Ошибка удаления ребра $srcId -> $dstId: ${result?.errorMessage}")
            }

            success
        } catch (e: Exception) {
            logger.error("Исключение при удалении ребра: $srcId -> $dstId", e)
            false
        }

    /**
     * Выполняет произвольный запрос и возвращает результат
     */
    fun executeQueryWithResult(query: String): List<Map<String, Any>> {
        try {
            logger.debug("Выполнение запроса с результатом: $query")
            checkConnection()

            val result = session?.execute(query)
            if (result?.isSucceeded != true) {
                logger.error("Ошибка выполнения запроса: ${result?.errorMessage}")
                return emptyList()
            }

            val nodes = mutableListOf<Map<String, Any>>()

            result.rows?.forEach { row ->
                val nodeData = mutableMapOf<String, Any>()
                val columnNames = result.columnNames

                columnNames.forEachIndexed { index, columnName ->
                    val value = row.values[index]
                    nodeData[columnName] = extractValue(value)
                }

                if (nodeData.isNotEmpty()) {
                    nodes.add(nodeData)
                }
            }

            logger.debug("Запрос выполнен успешно, найдено записей: ${nodes.size}")
            return nodes
        } catch (e: Exception) {
            logger.error("Исключение при выполнении запроса: $query", e)

        }
        return emptyList()
    }

    /**
     * Закрывает пул соединений
     */
    @PreDestroy
    fun close() {
        try {
            logger.info("Закрытие пула соединений NebulaGraph")

            // Закрываем сессию
            session?.let { session ->
                try {
                    session.release()
                    logger.debug("Сессия NebulaGraph закрыта")
                } catch (e: Exception) {
                    logger.warn("Ошибка при закрытии сессии NebulaGraph", e)
                }
            }
            session = null

            // Закрываем пул соединений
            nebulaPool?.let { pool ->
                try {
                    pool.close()
                    logger.debug("Пул соединений NebulaGraph закрыт")
                } catch (e: Exception) {
                    logger.warn("Ошибка при закрытии пула соединений NebulaGraph", e)
                }
            }
            nebulaPool = null

            isConnected = false

            logger.info("Пул соединений NebulaGraph закрыт успешно")
        } catch (e: Exception) {
            logger.error("Ошибка при закрытии пула соединений NebulaGraph", e)
        }
    }
}
