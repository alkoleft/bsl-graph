# Интеграция с Nebula Graph

## Обзор

Проект реализует полную интеграцию с Nebula Graph для хранения и анализа метаданных конфигураций 1С. Метаданные преобразуются в граф знаний, что позволяет выполнять сложные запросы и анализ связей между объектами метаданных.

## Архитектура

### Компоненты

1. **NebulaGraphService** - основной сервис для работы с Nebula Graph
2. **NebulaGraphKnowledgeRepository** - репозиторий для операций с графом знаний
3. **MetadataGraphService** - сервис интеграции метаданных с графом
4. **ConsoleController** - консольное приложение для тестирования

### Схема графа

#### Теги (типы вершин)

- **Configuration** - конфигурация метаданных
- **Constant** - константы
- **Catalog** - справочники
- **Document** - документы
- **Enum** - перечисления
- **EnumValue** - значения перечислений
- **Form** - формы
- **Module** - модули
- **Register** - регистры
- **Report** - отчеты
- **Table** - таблицы

#### Рёбра (типы связей)

- **CONTAINS** - содержит (конфигурация содержит объекты)
- **HAS_MODULE** - имеет модуль
- **HAS_FORM** - имеет форму
- **HAS_TABLE** - имеет таблицу
- **HAS_ATTRIBUTE** - имеет атрибут
- **HAS_DIMENSION** - имеет измерение
- **HAS_RESOURCE** - имеет ресурс

## Настройка

### Конфигурация подключения

В `application.yml`:

```yaml
nebula:
  addresses:
    - host: ${NEBULA_HOST:localhost}
      port: ${NEBULA_PORT:9669}
  username: ${NEBULA_USERNAME:root}
  password: ${NEBULA_PASSWORD:nebula}
  space: ${NEBULA_SPACE:context}
  max-conn-size: ${NEBULA_MAX_CONN_SIZE:10}
  min-conn-size: ${NEBULA_MIN_CONN_SIZE:1}
  timeout: ${NEBULA_TIMEOUT:1000}
```

### Переменные окружения

- `NEBULA_HOST` - хост Nebula Graph (по умолчанию: localhost)
- `NEBULA_PORT` - порт Nebula Graph (по умолчанию: 9669)
- `NEBULA_USERNAME` - имя пользователя (по умолчанию: root)
- `NEBULA_PASSWORD` - пароль (по умолчанию: nebula)
- `NEBULA_SPACE` - имя пространства (по умолчанию: context)
- `NEBULA_MAX_CONN_SIZE` - максимальный размер пула соединений (по умолчанию: 10)
- `NEBULA_MIN_CONN_SIZE` - минимальный размер пула соединений (по умолчанию: 1)
- `NEBULA_TIMEOUT` - таймаут соединения в мс (по умолчанию: 1000)

## Запуск Nebula Graph

### Использование Docker Compose

```bash
# Запуск Nebula Graph
docker-compose -f docker-compose-lite.yaml up -d

# Проверка статуса
docker-compose -f docker-compose-lite.yaml ps

# Остановка
docker-compose -f docker-compose-lite.yaml down
```

### Проверка подключения

```bash
# Тестирование подключения
java -jar app.jar --test-nebula

# Просмотр статистики графа
java -jar app.jar --stats
```

## Использование

### Выгрузка метаданных в граф

```bash
# Выгрузка метаданных из конфигурации
java -jar app.jar /path/to/configuration/src

# Пример
java -jar app.jar /home/user/1c-configuration/src
```

### Программное использование

```kotlin
// Загрузка метаданных в граф
val result = metadataGraphService.loadMetadataToGraph(configurationPath)

if (result.success) {
    println("Загружено ${result.nodesCount} узлов и ${result.edgesCount} рёбер")
}

// Поиск узлов по типу
val configurations = metadataGraphService.findMetadataNodesByType(NodeType.CONFIGURATION)

// Поиск узлов по имени
val nodes = metadataGraphService.findMetadataNodesByName("НазваниеОбъекта")

// Поиск узлов по UUID
val node = metadataGraphService.findMetadataNodesByUuid("uuid-here")
```

## Примеры запросов nGQL

### Поиск всех конфигураций

```ngql
MATCH (n:Configuration) RETURN n
```

### Поиск справочников с их формами

```ngql
MATCH (c:Catalog)-[r:HAS_FORM]->(f:Form) 
RETURN c.name, f.name
```

### Поиск модулей конфигурации

```ngql
MATCH (conf:Configuration)-[r:HAS_MODULE]->(m:Module) 
WHERE conf.name CONTAINS "НазваниеКонфигурации"
RETURN m.name, m.type
```

### Анализ связей между объектами

```ngql
MATCH (n)-[r]->(m) 
RETURN type(r), count(r) as count 
ORDER BY count DESC
```

## Мониторинг и диагностика

### Статистика графа

```bash
# Получение статистики через консольное приложение
java -jar app.jar --stats
```

### Проверка подключения

```bash
# Тестирование подключения
java -jar app.jar --test-nebula
```

### Логирование

Уровень логирования для Nebula Graph можно настроить в `logback-mcp.xml`:

```xml
<logger name="ru.alkoleft.context.infrastructure.graph" level="DEBUG"/>
```

## Производительность

### Рекомендации

1. **Размер пула соединений**: настройте `max-conn-size` в зависимости от нагрузки
2. **Таймауты**: увеличьте `timeout` для больших конфигураций
3. **Партиционирование**: используйте `partition_num` > 10 для больших объемов данных
4. **Индексы**: создайте индексы для часто используемых свойств

### Мониторинг

- Следите за размером графа через статистику
- Мониторьте время выполнения запросов
- Проверяйте использование памяти Nebula Graph

## Безопасность

### Настройки безопасности

1. **Аутентификация**: используйте надежные пароли
2. **Сетевая безопасность**: ограничьте доступ к портам Nebula Graph
3. **Шифрование**: настройте TLS для продакшн среды
4. **Резервное копирование**: регулярно создавайте бэкапы графа

## Устранение неполадок

### Частые проблемы

1. **Подключение не устанавливается**
   - Проверьте, что Nebula Graph запущен
   - Убедитесь в правильности настроек подключения
   - Проверьте сетевую доступность

2. **Медленные запросы**
   - Проверьте размер графа
   - Оптимизируйте nGQL запросы
   - Увеличьте размер пула соединений

3. **Ошибки схемы**
   - Убедитесь, что схема создана корректно
   - Проверьте совместимость версий Nebula Graph

### Диагностические команды

```bash
# Проверка статуса сервисов
docker-compose -f docker-compose-lite.yaml ps

# Просмотр логов
docker-compose -f docker-compose-lite.yaml logs graphd
docker-compose -f docker-compose-lite.yaml logs metad0
docker-compose -f docker-compose-lite.yaml logs storaged0

# Тестирование подключения
java -jar app.jar --test-nebula
```

## Разработка

### Добавление новых типов узлов

1. Добавьте новый тип в `NodeType` enum
2. Создайте соответствующий тег в `createGraphSchema()`
3. Обновите маппинг в `getTagNameForNodeType()`
4. Добавьте тесты для нового типа

### Расширение схемы

1. Определите новые свойства для тегов
2. Обновите `createTag()` методы
3. Добавьте новые типы рёбер при необходимости
4. Обновите документацию

## Лицензия

Проект использует Nebula Graph Java Client под лицензией Apache 2.0.
