package ru.alkoleft.context.infrastructure.graph

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

/**
 * Конфигурация для подключения к NebulaGraph
 */
@Configuration
class NebulaGraphConfiguration

/**
 * Свойства конфигурации NebulaGraph
 */
@Component
@ConfigurationProperties(prefix = "nebula")
data class NebulaGraphProperties(
    var addresses: List<AddressConfig> = emptyList(),
    var username: String = "root",
    var password: String = "nebula",
    var space: String = "context",
    var maxConnSize: Int = 10,
    var minConnSize: Int = 1,
    var timeout: Int = 1000,
)

/**
 * Конфигурация адреса NebulaGraph
 */
data class AddressConfig(
    var host: String = "localhost",
    var port: Int = 9669,
)
