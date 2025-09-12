# SYSTEM PATTERNS

## Architectural Patterns

### Clean Architecture
- **Domain Layer:** Pure business logic, no external dependencies
- **Infrastructure Layer:** External concerns (database, APIs)
- **Presentation Layer:** User interface and external interfaces

### Repository Pattern
- **Interface:** `GraphKnowledgeRepository` in domain layer
- **Implementation:** `NebulaGraphKnowledgeRepository` in infrastructure layer
- **Purpose:** Abstract data access, enable testing

### Service Layer Pattern
- **NebulaGraphService:** Encapsulates graph database operations
- **Separation:** Business logic separate from data access

## Design Patterns

### Builder Pattern
- Used in `GraphNode` for flexible object construction
- Fluent interface for complex object creation

### Configuration Pattern
- **NebulaGraphConfiguration:** Centralized configuration management
- Environment-specific settings

### Factory Pattern
- Service instantiation through Spring configuration
- Dependency injection for loose coupling

## Kotlin-Specific Patterns

### Data Classes
- `GraphNode` as immutable data class
- Automatic equals, hashCode, toString

### Extension Functions
- Additional functionality without inheritance
- Domain-specific operations

### Coroutines
- Asynchronous operations
- Non-blocking I/O for database operations

### Sealed Classes
- Type-safe hierarchies
- Pattern matching with when expressions

## Testing Patterns

### Repository Testing
- Interface-based testing
- Mock implementations for unit tests

### Integration Testing
- Real database connections
- End-to-end workflow validation

## Error Handling Patterns

### Result Types
- Explicit error handling
- No exceptions for business logic errors

### Logging Strategy
- Structured logging with context
- Separate MCP-specific logging configuration
