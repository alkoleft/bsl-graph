/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.graph

import ru.alkoleft.context.domain.graph.AccessEdge
import ru.alkoleft.context.domain.graph.AttributeEdge
import ru.alkoleft.context.domain.graph.ConfigurationNode
import ru.alkoleft.context.domain.graph.EdgeType
import ru.alkoleft.context.domain.graph.GraphEdge
import ru.alkoleft.context.domain.graph.GraphNode
import ru.alkoleft.context.domain.graph.MDObjectNode

object QueryMapper {
    fun properties(node: GraphNode) = when (node) {
        is ConfigurationNode -> listOf("uid", "name", "synonym", "type")
        is MDObjectNode -> listOf("uid", "name", "synonym", "type")
    }

    fun properties(clazz: Class<*>) = when (clazz) {
        ConfigurationNode::class.java -> listOf("uid", "name", "synonym", "type")
        MDObjectNode::class.java -> listOf("uid", "name", "synonym", "type")
        AttributeEdge::class.java -> listOf("name")
        AccessEdge::class.java -> listOf("name")
        else -> emptyList()
    }

    fun properties(edgeType: EdgeType) = when (edgeType) {
        EdgeType.ATTRIBUTE -> listOf("name")
        EdgeType.ACCESS -> listOf("name")
        else -> emptyList()
    }

    fun properties(node: GraphEdge) = when (node) {
        is AttributeEdge -> listOf("name")
        is AccessEdge -> listOf("name")
        else -> emptyList()
    }

    fun values(node: GraphNode) = when (node) {
        is ConfigurationNode -> listOf(node.uid, node.name, node.synonym, node.type.toString())
        is MDObjectNode -> listOf(node.uid, node.name, node.synonym, node.type.toString())
    }

    fun values(node: GraphEdge) = when (node) {
        is AttributeEdge -> listOf(node.attributeName)
        is AccessEdge -> listOf(node.rights)
        else -> emptyList()
    }

    fun toPropertiesMap(node: GraphNode): Map<String, String> {
        val properties = properties(node)
        val values = values(node)

        return properties
            .withIndex()
            .associateBy({ it.value }, { values[it.index] })
    }

    fun toPropertiesMap(node: GraphEdge): Map<String, String> {
        val properties = properties(node)
        val values = values(node)

        return properties
            .withIndex()
            .associateBy({ it.value }, { values[it.index] })
    }
}