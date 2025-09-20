# PROJECT BRIEF

## Project Overview
**Project Name:** 1C Configuration Analysis Server  
**Type:** Kotlin Spring Boot Application with MCP Protocol Support  
**Purpose:** Deep analysis of 1C:Enterprise configurations with graph-based knowledge representation  

## Core Functionality
- **Metadata Analysis:** Complete reading and processing of 1C configuration metadata using bsl-mdclasses
- **Graph Knowledge Base:** NebulaGraph-based storage of configuration relationships and dependencies
- **Code Analysis:** BSL syntax parsing and context extraction
- **Visualization:** Web interface for interactive graph exploration
- **Export Capabilities:** Full metadata export to graph database with relationship mapping
- **MCP Protocol:** Model Context Protocol support for AI integration
- **REST API:** RESTful API for graph operations and metadata access

## Supported Formats
- **Standard Configuration Format:** 1C configuration files export
- **EDT Format:** 1C:Enterprise Development Tools format
- **Real-time Processing:** Live configuration analysis and export

## Technology Stack
- **Language:** Kotlin 2.1.20
- **Framework:** Spring Boot 3.5.0
- **Database:** NebulaGraph 3.0.0
- **1C Libraries:** bsl-mdclasses, bsl-parser, bsl-utils, bsl-common-library
- **Architecture:** Clean Architecture (Domain, Infrastructure, Presentation)
- **Build Tool:** Gradle 8.5 with Kotlin DSL
- **Frontend:** TypeScript, Vite, Sigma.js
- **Protocols:** MCP (Model Context Protocol), REST API

## Key Components
- **Domain Layer:** GraphNode, MDObjectNode, service interfaces
- **Infrastructure Layer:** Complete NebulaGraph integration, full metadata reading services
- **Presentation Layer:** Console application, web visualization, REST API, MCP controller
- **Graph Models:** Support for all 1C metadata types with full relationship mapping

## Metadata Processing Features
- **Complete 1C Metadata Support:** All standard 1C metadata objects (catalogs, documents, constants, etc.)
- **Relationship Mapping:** Full mapping of object relationships, dependencies, and references
- **Graph Export:** Complete export of metadata structure to NebulaGraph
- **Error Handling:** Comprehensive error handling and logging
- **Performance:** Optimized for large configurations

## Current Status
- ✅ Project structure and architecture established
- ✅ Core domain models and interfaces implemented
- ✅ Complete NebulaGraph integration with full CRUD operations
- ✅ Full metadata reading implementation using bsl-mdclasses library
- ✅ Complete graph export functionality with relationship mapping
- ✅ Console application for testing and operations
- ✅ Web visualization interface
- ✅ REST API for graph operations
- ✅ MCP protocol support
- ✅ Comprehensive error handling and logging

## Next Steps
- Enhanced testing with real 1C configurations
- Performance optimization for very large configurations
- API documentation and usage examples
- Advanced graph analysis features
- Integration with additional 1C development tools
