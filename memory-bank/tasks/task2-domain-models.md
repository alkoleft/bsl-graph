# Задача #2: Доменные модели для JSON DB

## Описание
Разработать доменные модели для сериализации объектов метаданных 1С в JSON формат.

## Цель
Создать основу для сериализации/десериализации объектов `bsl-mdclasses` в JSON.

## Требования
1. Разработать базовый интерфейс `SerializedObject` со следующими полями:
   - `id: String` - уникальный идентификатор
   - `type: String` - тип объекта метаданных
   - `serializedData: String` - JSON сериализованный объект
   - `metadata: ObjectMetadata` - метаданные объекта
   - `lastModified: Instant` - время последнего изменения
   - `version: Int` - версия объекта

2. Создать класс `ObjectMetadata` со следующими полями:
   - `name: String` - имя объекта
   - `synonym: String?` - синоним объекта
   - `uuid: String` - UUID объекта
   - `mdoType: String` - тип объекта метаданных
   - `parentId: String?` - ID родительского объекта
   - `configurationId: String` - ID конфигурации
   - `filePath: String?` - путь к файлу
   - `size: Long` - размер объекта
   - `checksum: String` - контрольная сумма

3. Реализовать конкретные классы для каждого типа объектов:
   - `SerializedConfiguration`
   - `SerializedCatalog`
   - `SerializedDocument`
   - `SerializedEnum`
   - `SerializedConstant`
   - `SerializedRegister`
   - `SerializedCommonModule`
   - `SerializedForm`
   - `SerializedQuery`
   - `SerializedReport`
   - `SerializedDataProcessor`

## Технические детали
- Использовать Kotlin data classes
- Реализовать интерфейс `SerializedObject` для всех типов
- Обеспечить совместимость с Jackson для сериализации/десериализации
- Добавить аннотации для MongoDB (@Document, @Id)
- Добавить аннотации для индексирования (@Indexed)

## Критерии готовности
- [ ] Созданы все необходимые классы и интерфейсы
- [ ] Реализованы все требуемые поля и методы
- [ ] Добавлены аннотации для MongoDB
- [ ] Написаны тесты для сериализации/десериализации
- [ ] Проверена совместимость с MongoDB

## Зависимости
- Задача #1: Настройка MongoDB для JSON DB

## Оценка трудозатрат
- 2-3 дня

## Ответственный
- TBD

## Приоритет
- Высокий
