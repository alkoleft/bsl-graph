/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.graph

import com.github._1c_syntax.bsl.mdclasses.Configuration
import com.github._1c_syntax.bsl.mdo.Attribute
import com.github._1c_syntax.bsl.mdo.AttributeOwner
import com.github._1c_syntax.bsl.mdo.CommonPicture
import com.github._1c_syntax.bsl.mdo.MD
import com.github._1c_syntax.bsl.mdo.Role
import com.github._1c_syntax.bsl.mdo.SessionParameter
import com.github._1c_syntax.bsl.mdo.StyleItem
import com.github._1c_syntax.bsl.mdo.Subsystem
import com.github._1c_syntax.bsl.mdo.TabularSectionOwner
import com.github._1c_syntax.bsl.mdo.support.MetadataValueType
import com.github._1c_syntax.bsl.types.MDOType
import com.github._1c_syntax.bsl.types.MdoReference
import com.github._1c_syntax.bsl.types.ValueTypeDescription
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.alkoleft.context.domain.graph.GraphEdge
import ru.alkoleft.context.domain.graph.GraphKnowledgeRepository
import ru.alkoleft.context.domain.graph.GraphNode
import ru.alkoleft.context.domain.graph.MDObjectNode
import ru.alkoleft.context.domain.metadata.MetadataLoadResult
import ru.alkoleft.context.infrastructure.metadata.ConfigurationNode
import ru.alkoleft.context.infrastructure.metadata.nodeId
import ru.alkoleft.context.infrastructure.metadata.toNodeType
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger { }

@Component
class MetadataExporter(
    private val graphKnowledgeRepository: GraphKnowledgeRepository,
) {
    fun exportMetadata(configuration: Configuration): MetadataLoadResult {
        try {
            // Сохраняем узлы в граф
            var savedNodesCount = 0
            var savedEdgesCount = 0

            for (node in nodes(configuration)) {
                if (graphKnowledgeRepository.saveNode(node)) {
                    savedNodesCount++
                }
            }

            // Сохраняем ребра в граф
            for (edge in edges(configuration)) {
                if (graphKnowledgeRepository.saveEdge(edge)) {
                    savedEdgesCount++
                }
            }

            logger.info { "Успешно загружено метаданных в граф: $savedNodesCount узлов, $savedEdgesCount ребер, конфигурация: ${configuration.name}" }

            return MetadataLoadResult(
                success = true,
                configurationId = nodeId(configuration.uuid),
                nodesCount = savedNodesCount,
                edgesCount = savedEdgesCount
            )
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при загрузке метаданных в граф" }
            return MetadataLoadResult(
                success = false,
                configurationId = null,
                nodesCount = 0,
                edgesCount = 0,
                errors = listOf("Ошибка загрузки метаданных: ${e.message}"),
            )
        }
    }

    fun nodes(configuration: Configuration): Sequence<GraphNode> = sequence {
        yield(configuration.toNode())
        val children =
            configuration.children.filter { it !is CommonPicture && it !is SessionParameter && it !is StyleItem }
        children.forEach { yield(it.toNode()) }
        configuration.subsystems.forEach { appendChildrenSubsystemNodes(it) }
    }

    fun edges(configuration: Configuration): Sequence<GraphEdge> = sequence {
        configuration.subsystems.forEach { appendChildrenSubsystemEdges(it) }
        configuration.subsystems.forEach { appendSubsystemContent(configuration, it) }
        configuration.roles.forEach { appendRoleEdges(it, configuration) }
        configuration.functionalOptions.forEach { option ->
            appendContainsEdges(
                nodeId(option.uuid),
                option.content,
                configuration
            )
        }

        configuration.exchangePlans.forEach { plan ->
            appendContainsEdges(
                nodeId(plan.uuid),
                plan.content.map { it.metadata },
                configuration
            )
        }

        configuration.eventSubscriptions.forEach { subscription ->
            subscription.valueType.MDObjects(configuration)
                .forEach { GraphEdge.contains(nodeId(subscription.uuid), nodeId(it.uuid)) }
        }

        configuration.children
            .filterIsInstance<AttributeOwner>()
            .forEach { md ->
                appendAttributeEdges(md, configuration)
                if (md is TabularSectionOwner) {
                    md.tabularSections.forEach {
                        appendAttributeEdges(
                            nodeId((md as MD).uuid),
                            it.attributes,
                            configuration,
                            "${it.name}."
                        )
                    }
                }
            }
    }
}

private suspend fun SequenceScope<GraphNode>.appendChildrenSubsystemNodes(subsystem: Subsystem) {
    subsystem.subsystems.forEach {
        yield(it.toNode())
        appendChildrenSubsystemNodes(it)
    }
}

private suspend fun SequenceScope<GraphEdge>.appendChildrenSubsystemEdges(subsystem: Subsystem) {
    subsystem.subsystems.forEach {
        yield(GraphEdge.children(nodeId(subsystem.uuid), nodeId(it.uuid)))
        appendChildrenSubsystemEdges(it)
    }
}

private suspend fun SequenceScope<GraphEdge>.appendSubsystemContent(
    configuration: Configuration,
    subsystem: Subsystem
) {
    subsystem.content
        .mapNotNull { configuration.findChild(it).getOrNull() }
        .forEach {
            yield(GraphEdge.contains(nodeId(subsystem.uuid), nodeId(it.uuid)))
        }
    subsystem.subsystems.forEach { appendSubsystemContent(configuration, it) }
}

suspend fun SequenceScope<GraphEdge>.appendAttributeEdges(
    owner: AttributeOwner,
    configuration: Configuration,
) = appendAttributeEdges(nodeId((owner as MD).uuid), owner.allAttributes, configuration)

suspend fun SequenceScope<GraphEdge>.appendAttributeEdges(
    srcId: String,
    attributes: List<Attribute>,
    configuration: Configuration,
    prefix: String = ""
) {
    attributes.forEach { attribute ->
        attribute.valueType.MDObjects(configuration)
            .forEach { dest ->
                yield(
                    GraphEdge.attribute(
                        srcId,
                        nodeId(dest.uuid),
                        prefix + attribute.name
                    )
                )
            }
    }
}

fun ValueTypeDescription.MDObjects(configuration: Configuration) =
    types.filterIsInstance<MetadataValueType>()
        .mapNotNull {
            if (it.kind != MDOType.DEFINED_TYPE) {
                it.findMD(configuration)
            } else {
                null
            }
        }

private suspend fun SequenceScope<GraphEdge>.appendRoleEdges(role: Role, configuration: Configuration) {
    val srcId = nodeId(role.uuid)
    role.data.objectRights.forEach { it ->
        it.name.findMD(configuration)?.let { dest ->
            yield(
                GraphEdge.access(
                    srcId, nodeId(dest.uuid),
                    it.rights.filter { r -> r.isValue }.joinToString(", ") { r -> r.name.value() })
            )
        }
    }
}

private suspend fun SequenceScope<GraphEdge>.appendContainsEdges(
    srcId: String,
    references: List<MdoReference>,
    configuration: Configuration
) {
    references.forEach { ref ->
        ref.findMD(configuration)?.let { mdo ->
            yield(GraphEdge.contains(srcId, nodeId(mdo.uuid)))
        }
    }
}

fun Configuration.toNode() = ConfigurationNode(this)

fun MD.toNode() = MDObjectNode(
    uuid,
    name,
    synonym.any,
    mdoType.toNodeType(),
    nodeId(uuid)
)

fun MetadataValueType.findMD(configuration: Configuration): MD? {
    val chunks = name.split(".")
    return if (chunks.size == 2) {
        configuration.findChild(MdoReference.create("${kind.getName()}.${chunks[1]}")).getOrNull()
    } else {
        null
    }
}

fun MdoReference.findMD(configuration: Configuration): MD? =
    configuration.findChild(this).getOrNull()
