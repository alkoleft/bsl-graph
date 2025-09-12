# TECHNICAL CONTEXT

## Architecture Overview
**Pattern:** Clean Architecture with Domain-Driven Design  
**Layers:** Domain, Infrastructure, Presentation  

## Domain Layer
- **GraphNode.kt** - Core graph node entity
- **GraphKnowledgeRepository.kt** - Repository interface

## Infrastructure Layer  
- **NebulaGraphService.kt** - Graph database service
- **NebulaGraphConfiguration.kt** - Database configuration
- **NebulaGraphKnowledgeRepository.kt** - Repository implementation

## Presentation Layer
- **McpServerApplication.kt** - Main Spring Boot application
- MCP server endpoints for AI model integration

## Technology Dependencies
- Spring Boot 3.x
- NebulaGraph Java Driver
- Kotlin Coroutines
- Jackson for JSON processing
- Logback for logging

## Build Configuration
- Gradle with Kotlin DSL
- Multi-environment configuration (dev, production)
- Custom logging configuration for MCP

## Database Schema
- Graph-based storage using NebulaGraph
- Nodes represent BSL syntax elements
- Relationships represent context connections
