/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.metadata

import com.github._1c_syntax.bsl.mdclasses.Configuration
import com.github._1c_syntax.bsl.types.MDOType
import ru.alkoleft.context.domain.graph.ConfigurationNode
import ru.alkoleft.context.domain.graph.NodeType

fun nodeId(uuid: String) = uuid.replace("-", "")

fun ConfigurationNode(configuration: Configuration) = ConfigurationNode(
    configuration.uuid,
    configuration.name,
    configuration.synonym.any,
    NodeType.CONFIGURATION,
    nodeId(configuration.uuid)
)

fun MDOType.toNodeType() = NodeType.valueOf(toString())