# Описание библиотеки bsl-mdclasses 0.15.1

## Общая информация

**bsl-mdclasses** - это Java-библиотека для чтения и работы с метаданными конфигураций 1С:Предприятие. Библиотека предоставляет API для парсинга XML-файлов конфигураций и создания объектной модели метаданных.

## Основные компоненты

### 1. Основные классы

#### Configuration
- **Пакет**: `com.github._1c_syntax.bsl.mdclasses.Configuration`
- **Назначение**: Корневой класс конфигурации 1С
- **Ключевые поля**:
  - `name: String` - имя конфигурации
  - `uuid: String` - уникальный идентификатор
  - `version: String` - версия конфигурации
  - `comment: String` - комментарий
  - `synonym: MultiLanguageString` - синоним
  - `children: List<MD>` - дочерние объекты метаданных

#### MDClass
- **Пакет**: `com.github._1c_syntax.bsl.mdclasses.MDClass`
- **Назначение**: Базовый класс для всех объектов метаданных

### 2. Ридеры (Readers)

#### MDReader (интерфейс)
- **Пакет**: `com.github._1c_syntax.bsl.reader.MDReader`
- **Основные методы**:
  - `readConfiguration(): MDClass` - чтение конфигурации
  - `readExternalSource(): ExternalSource` - чтение внешних источников
  - `read(String fullName): Object` - чтение объекта по имени
  - `read(Path path): Object` - чтение объекта по пути

#### DesignerReader
- **Пакет**: `com.github._1c_syntax.bsl.reader.designer.DesignerReader`
- **Назначение**: Чтение конфигураций в формате Конфигуратора 1С
- **Конструктор**: `DesignerReader(Path path, boolean skipSupport)`
- **Константа**: `CONFIGURATION_MDO_PATH = "Configuration.xml"`

#### EDTReader
- **Пакет**: `com.github._1c_syntax.bsl.reader.edt.EDTReader`
- **Назначение**: Чтение конфигураций в формате EDT (Eclipse Development Tools)
- **Конструктор**: `EDTReader(Path path, boolean skipSupport)`
- **Константа**: `CONFIGURATION_MDO_PATH = "src/Configuration/Configuration.mdo"`

### 3. Типы объектов метаданных (MDO)

Библиотека поддерживает все основные типы объектов 1С:

#### Справочники и документы
- `Catalog` - Справочники
- `Document` - Документы
- `Enum` - Перечисления
- `Constant` - Константы

#### Регистры
- `InformationRegister` - Регистры сведений
- `AccumulationRegister` - Регистры накопления
- `AccountingRegister` - Регистры бухгалтерии
- `CalculationRegister` - Регистры расчета

#### Отчеты и обработки
- `Report` - Отчеты
- `DataProcessor` - Обработки
- `ExternalReport` - Внешние отчеты
- `ExternalDataProcessor` - Внешние обработки

#### Другие объекты
- `CommonModule` - Общие модули
- `CommonForm` - Общие формы
- `Role` - Роли
- `Subsystem` - Подсистемы
- `Language` - Языки
- `Style` - Стили
- `WebService` - Веб-сервисы
- `HTTPService` - HTTP-сервисы

### 4. Утилиты и вспомогательные классы

#### ExtendXStream
- **Пакет**: `com.github._1c_syntax.bsl.reader.common.xstream.ExtendXStream`
- **Назначение**: Расширенный XStream для парсинга XML

#### ConfigurationSource
- **Пакет**: `com.github._1c_syntax.bsl.types.ConfigurationSource`
- **Назначение**: Определение источника конфигурации (Designer/EDT)

## Примеры использования

### Базовое чтение конфигурации

```java
// Для конфигурации в формате Конфигуратора
Path configPath = Paths.get("/path/to/configuration");
DesignerReader reader = new DesignerReader(configPath, false);
Configuration configuration = (Configuration) reader.readConfiguration();

// Для конфигурации в формате EDT
EDTReader edtReader = new EDTReader(configPath, false);
Configuration configuration = (Configuration) edtReader.readConfiguration();
```

### Получение информации о конфигурации

```java
String name = configuration.getName();
String uuid = configuration.getUuid();
String version = configuration.getVersion();
String comment = configuration.getComment();
MultiLanguageString synonym = configuration.getSynonym();
```

### Работа с объектами метаданных

```java
// Получение всех каталогов
List<Catalog> catalogs = configuration.getCatalogs();

// Получение всех документов
List<Document> documents = configuration.getDocuments();

// Получение всех общих модулей
List<CommonModule> commonModules = configuration.getCommonModules();

// Получение всех ролей
List<Role> roles = configuration.getRoles();
```

### Чтение конкретного объекта

```java
// Чтение объекта по полному имени
Object catalog = reader.read("Справочник.Номенклатура");

// Чтение объекта по пути к файлу
Path mdoPath = Paths.get("/path/to/Catalog.Номенклатура.xml");
Object catalog = reader.read(mdoPath);
```

## Особенности работы

### 1. Поддержка двух форматов
- **Designer** - формат выгрузки из Конфигуратора 1С
- **EDT** - формат Eclipse Development Tools

### 2. Ленивая загрузка
Библиотека использует ленивую загрузку для оптимизации производительности:
- `Lazy<List<Module>> allModules`
- `Lazy<List<MD>> plainChildren`
- `Lazy<Map<URI, ModuleType>> modulesByType`

### 3. Поддержка многоязычности
- `MultiLanguageString` для синонимов и комментариев
- Поддержка различных языков интерфейса

### 4. Обработка ошибок
- Graceful handling отсутствующих файлов
- Возврат `null` для несуществующих объектов
- Логирование через SLF4J

## Зависимости

- **XStream** - для парсинга XML
- **Lombok** - для генерации кода
- **Apache Commons IO** - для работы с файлами
- **SLF4J** - для логирования

## Версия в проекте

В проекте используется версия **0.15.1** библиотеки bsl-mdclasses, которая является стабильной и поддерживает все основные функции для работы с метаданными 1С.
