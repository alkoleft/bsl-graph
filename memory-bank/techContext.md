# TECHNICAL CONTEXT

## Architecture Overview
**Pattern:** Clean Architecture with Domain-Driven Design  
**Layers:** Domain, Infrastructure, Presentation  
**Status:** 95% Complete - Full metadata reading and graph export implemented

## Domain Layer
- **GraphNode.kt** - Core graph node entity with metadata support
- **MDObjectNode.kt** - Specific node type for 1C metadata objects
- **GraphRepository.kt** - Repository interface for graph operations
- **MetadataGraphService.kt** - Service interface for metadata integration

## Infrastructure Layer  
- **NebulaGraphService.kt** - Graph database service with full CRUD operations
- **NebulaGraphConfiguration.kt** - Database configuration and connection management
- **NebulaRepository.kt** - Repository implementation for NebulaGraph
- **SchemaInitializer.kt** - Graph schema creation and management
- **QueryMapper.kt** - nGQL query mapping and execution
- **MetadataExporter.kt** - Complete metadata export to graph functionality
- **MetadataExporterServiceImpl.kt** - Full implementation of metadata reading and export
- **Mapper.kt** - Metadata to graph node mapping utilities

## Presentation Layer
- **McpServerApplication.kt** - Main Spring Boot application (console mode)
- **ConsoleController.kt** - Console interface for testing and operations
- **ContextMcpController.kt** - MCP protocol controller
- **GraphRestController.kt** - REST API for graph operations
- **WebConfiguration.kt** - Web configuration and CORS setup
- Graph visualization web application (TypeScript/Vite)

## Technology Dependencies
- **Kotlin:** 2.1.20 with JVM 17
- **Spring Boot:** 3.5.0
- **NebulaGraph:** 3.0.0 Java Driver
- **1C Libraries:** bsl-mdclasses, bsl-parser, bsl-utils, bsl-common-library
- **Build:** Gradle 8.5 with Kotlin DSL
- **Testing:** JUnit 5, AssertJ
- **Logging:** KotlinLogging

## Build Configuration
- Gradle with Kotlin DSL
- Java 17 toolchain
- Ktlint code formatting
- JaCoCo test coverage
- Git versioning integration

## Database Schema
- **Graph Storage:** NebulaGraph for knowledge graph
- **Node Types:** Configuration, Catalog, Document, Constant, Enum, Form, Module, Register, Report, Table
- **Relationships:** CONTAINS, HAS_MODULE, HAS_FORM, HAS_TABLE, HAS_ATTRIBUTE, HAS_DIMENSION, HAS_RESOURCE, CHILDREN, ACCESS
- **Schema:** Supports all 1C metadata types with full relationship mapping

## Metadata Processing
- **Real 1C Configuration Reading:** Full implementation using bsl-mdclasses
- **Metadata Types Support:** All standard 1C metadata objects
- **Relationship Mapping:** Complete mapping of object relationships and dependencies
- **Graph Export:** Full export of metadata structure to NebulaGraph
- **Error Handling:** Comprehensive error handling and logging

## Current Implementation Status
- ✅ Domain models and interfaces
- ✅ NebulaGraph integration with full CRUD operations
- ✅ Complete metadata reading using bsl-mdclasses library
- ✅ Full graph export functionality with relationship mapping
- ✅ Console application for testing and operations
- ✅ Web visualization interface
- ✅ REST API for graph operations
- ✅ MCP protocol support
- ✅ Comprehensive error handling and logging
