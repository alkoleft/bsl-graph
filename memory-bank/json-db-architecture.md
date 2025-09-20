# JSON DB Архитектура для хранения объектов 1С

## 🎯 Концепция

Использование JSON DB для хранения полных сериализованных объектов из `bsl-mdclasses` вместо создания отдельных DTO. Это обеспечивает:
- Полное сохранение всех свойств объектов без потери данных
- Простоту интеграции с существующими библиотеками 1С
- Гибкость структуры данных
- Возможность инкрементальных обновлений

## 🏗️ Архитектура хранения

### **Структура JSON документов**

```kotlin
// Базовый интерфейс для всех сериализованных объектов
interface SerializedObject {
    val id: String
    val type: String
    val serializedData: String
    val metadata: ObjectMetadata
    val lastModified: Instant
    val version: Int
}

// Метаданные объекта
data class ObjectMetadata(
    val name: String,
    val synonym: String?,
    val uuid: String,
    val mdoType: String,
    val parentId: String?,
    val configurationId: String,
    val filePath: String?,
    val size: Long,
    val checksum: String
)

// Конкретные типы объектов
data class SerializedConfiguration(
    override val id: String,
    override val serializedData: String, // JSON сериализованная Configuration
    override val metadata: ObjectMetadata,
    override val lastModified: Instant,
    override val version: Int
) : SerializedObject

data class SerializedCatalog(
    override val id: String,
    override val serializedData: String, // JSON сериализованный Catalog
    override val metadata: ObjectMetadata,
    override val lastModified: Instant,
    override val version: Int
) : SerializedObject

// ... аналогично для всех типов объектов
```

### **Схема JSON DB**

```json
// Коллекция: configurations
{
  "_id": "config_12345",
  "type": "Configuration",
  "serializedData": {
    "uuid": "12345-67890-...",
    "name": "Управление торговлей",
    "synonym": "УТ",
    "children": [...],
    "subsystems": [...],
    "roles": [...],
    // ... все свойства Configuration
  },
  "metadata": {
    "name": "Управление торговлей",
    "synonym": "УТ",
    "uuid": "12345-67890-...",
    "mdoType": "Configuration",
    "parentId": null,
    "configurationId": "config_12345",
    "filePath": "/path/to/config",
    "size": 1024000,
    "checksum": "abc123..."
  },
  "lastModified": "2024-01-15T10:30:00Z",
  "version": 1
}

// Коллекция: catalogs
{
  "_id": "catalog_67890",
  "type": "Catalog",
  "serializedData": {
    "uuid": "67890-12345-...",
    "name": "Номенклатура",
    "synonym": "Номенклатура",
    "attributes": [...],
    "tabularSections": [...],
    "forms": [...],
    "modules": [...],
    // ... все свойства Catalog
  },
  "metadata": {
    "name": "Номенклатура",
    "synonym": "Номенклатура",
    "uuid": "67890-12345-...",
    "mdoType": "Catalog",
    "parentId": "config_12345",
    "configurationId": "config_12345",
    "filePath": "/path/to/catalog",
    "size": 51200,
    "checksum": "def456..."
  },
  "lastModified": "2024-01-15T10:30:00Z",
  "version": 1
}
```

## 🔧 Реализация

### **Сервис сериализации**

```kotlin
@Service
class ObjectSerializationService(
    private val objectMapper: ObjectMapper,
    private val jsonDbRepository: JsonDbRepository
) {
    
    fun serializeAndSave(mdObject: MD): SerializedObject {
        val serializedData = objectMapper.writeValueAsString(mdObject)
        val metadata = buildMetadata(mdObject)
        
        val serializedObject = when (mdObject) {
            is Configuration -> SerializedConfiguration(
                id = generateId(mdObject),
                serializedData = serializedData,
                metadata = metadata,
                lastModified = Instant.now(),
                version = 1
            )
            is Catalog -> SerializedCatalog(/*...*/)
            is Document -> SerializedDocument(/*...*/)
            // ... для всех типов объектов
        }
        
        jsonDbRepository.save(serializedObject)
        return serializedObject
    }
    
    fun deserializeObject(serializedObject: SerializedObject): MD {
        return when (serializedObject.type) {
            "Configuration" -> objectMapper.readValue(serializedObject.serializedData, Configuration::class.java)
            "Catalog" -> objectMapper.readValue(serializedObject.serializedData, Catalog::class.java)
            "Document" -> objectMapper.readValue(serializedObject.serializedData, Document::class.java)
            // ... для всех типов
            else -> throw IllegalArgumentException("Unknown object type: ${serializedObject.type}")
        }
    }
    
    private fun buildMetadata(mdObject: MD): ObjectMetadata {
        return ObjectMetadata(
            name = mdObject.name,
            synonym = mdObject.synonym.any,
            uuid = mdObject.uuid.toString(),
            mdoType = mdObject.mdoType.name,
            parentId = findParentId(mdObject),
            configurationId = findConfigurationId(mdObject),
            filePath = findFilePath(mdObject),
            size = calculateSize(mdObject),
            checksum = calculateChecksum(mdObject)
        )
    }
}
```

### **JSON DB Repository**

```kotlin
@Repository
interface JsonDbRepository {
    
    // Базовые операции
    fun save(serializedObject: SerializedObject): Boolean
    fun findById(id: String): SerializedObject?
    fun findByType(type: String): List<SerializedObject>
    fun findByConfigurationId(configurationId: String): List<SerializedObject>
    fun deleteById(id: String): Boolean
    
    // Поиск по метаданным
    fun findByName(name: String): List<SerializedObject>
    fun findByUuid(uuid: String): SerializedObject?
    fun findByParentId(parentId: String): List<SerializedObject>
    
    // Полнотекстовый поиск
    fun searchByContent(query: String): List<SerializedObject>
    fun searchByContentInType(type: String, query: String): List<SerializedObject>
    
    // Агрегации
    fun getStatistics(): DatabaseStatistics
    fun getObjectCountByType(): Map<String, Int>
    fun getConfigurationSizes(): List<ConfigurationSize>
}

// Реализация с MongoDB
@Repository
class MongoJsonDbRepository(
    private val mongoTemplate: MongoTemplate
) : JsonDbRepository {
    
    override fun save(serializedObject: SerializedObject): Boolean {
        return try {
            val collection = getCollection(serializedObject.type)
            mongoTemplate.save(serializedObject, collection)
            true
        } catch (e: Exception) {
            logger.error(e) { "Error saving object ${serializedObject.id}" }
            false
        }
    }
    
    override fun findById(id: String): SerializedObject? {
        return try {
            // Поиск по всем коллекциям
            val allTypes = listOf("Configuration", "Catalog", "Document", /*...*/)
            for (type in allTypes) {
                val collection = getCollection(type)
                val result = mongoTemplate.findById(id, SerializedObject::class.java, collection)
                if (result != null) return result
            }
            null
        } catch (e: Exception) {
            logger.error(e) { "Error finding object $id" }
            null
        }
    }
    
    override fun searchByContent(query: String): List<SerializedObject> {
        val textQuery = TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(query))
        val results = mutableListOf<SerializedObject>()
        
        val allTypes = listOf("Configuration", "Catalog", "Document", /*...*/)
        for (type in allTypes) {
            val collection = getCollection(type)
            val typeResults = mongoTemplate.find(textQuery, SerializedObject::class.java, collection)
            results.addAll(typeResults)
        }
        
        return results
    }
    
    private fun getCollection(type: String): String {
        return when (type) {
            "Configuration" -> "configurations"
            "Catalog" -> "catalogs"
            "Document" -> "documents"
            "Enum" -> "enums"
            "Constant" -> "constants"
            "Register" -> "registers"
            "CommonModule" -> "common_modules"
            "Form" -> "forms"
            "Query" -> "queries"
            "Report" -> "reports"
            "DataProcessor" -> "data_processors"
            else -> "other_objects"
        }
    }
}
```

### **Интеграция с существующей системой**

```kotlin
@Service
class EnhancedMetadataService(
    private val metadataExporter: MetadataExporter,
    private val objectSerializationService: ObjectSerializationService,
    private val jsonDbRepository: JsonDbRepository,
    private val graphRepository: GraphKnowledgeRepository
) {
    
    fun processConfiguration(configuration: Configuration): ProcessingResult {
        val results = mutableListOf<String>()
        
        // 1. Экспорт в граф (существующая функциональность)
        val graphResult = metadataExporter.exportMetadata(configuration)
        results.add("Graph export: ${graphResult.nodesCount} nodes, ${graphResult.edgesCount} edges")
        
        // 2. Сериализация и сохранение в JSON DB
        val serializedObjects = mutableListOf<SerializedObject>()
        
        // Сериализуем конфигурацию
        val configSerialized = objectSerializationService.serializeAndSave(configuration)
        serializedObjects.add(configSerialized)
        
        // Сериализуем все дочерние объекты
        configuration.children.forEach { child ->
            val childSerialized = objectSerializationService.serializeAndSave(child)
            serializedObjects.add(childSerialized)
        }
        
        results.add("JSON DB: ${serializedObjects.size} objects serialized")
        
        return ProcessingResult(
            success = true,
            graphResult = graphResult,
            serializedObjectsCount = serializedObjects.size,
            messages = results
        )
    }
    
    fun getObjectFullData(objectId: String): ObjectFullData? {
        val serializedObject = jsonDbRepository.findById(objectId) ?: return null
        val mdObject = objectSerializationService.deserializeObject(serializedObject)
        
        return ObjectFullData(
            id = objectId,
            metadata = serializedObject.metadata,
            serializedObject = serializedObject,
            mdObject = mdObject
        )
    }
}
```

## 📊 Преимущества JSON DB подхода

### **Технические преимущества**
- ✅ **Полнота данных:** Сохранение всех свойств объектов без потери
- ✅ **Простота:** Прямая сериализация объектов `bsl-mdclasses`
- ✅ **Гибкость:** Легкое добавление новых типов объектов
- ✅ **Отладка:** Возможность просмотра полных данных в JSON
- ✅ **Версионность:** Легкое отслеживание изменений

### **Функциональные преимущества**
- ✅ **Полнотекстовый поиск:** По всем свойствам объектов
- ✅ **Агрегации:** Статистика и аналитика по объектам
- ✅ **Инкрементальные обновления:** Обновление только измененных объектов
- ✅ **Кэширование:** Быстрый доступ к часто используемым объектам

### **Интеграционные преимущества**
- ✅ **Совместимость:** Работа с существующими библиотеками 1С
- ✅ **Расширяемость:** Легкое добавление новых типов анализа
- ✅ **Производительность:** Оптимизированные запросы к JSON данным

## 🔧 Конфигурация

```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/bsl_context
      database: bsl_context
      
json-db:
  collections:
    configurations: "configurations"
    catalogs: "catalogs"
    documents: "documents"
    enums: "enums"
    constants: "constants"
    registers: "registers"
    common-modules: "common_modules"
    forms: "forms"
    queries: "queries"
    reports: "reports"
    data-processors: "data_processors"
    other-objects: "other_objects"
  
  indexing:
    full-text-search: true
    metadata-indexing: true
    content-indexing: true
    
  serialization:
    pretty-print: false
    include-null-values: false
    date-format: "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
```

Этот подход обеспечивает максимальную гибкость и полноту данных при сохранении простоты интеграции с существующими библиотеками 1С.
