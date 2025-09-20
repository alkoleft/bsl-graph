package ru.alkoleft.context.infrastructure.metadata

import com.github._1c_syntax.bsl.mdclasses.Configuration
import com.github._1c_syntax.bsl.reader.MDOReader
import com.github._1c_syntax.bsl.types.ConfigurationSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.alkoleft.context.domain.metadata.MetadataGraphService
import ru.alkoleft.context.domain.metadata.MetadataLoadResult
import ru.alkoleft.context.infrastructure.graph.MetadataExporter
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Реализация сервиса интеграции метаданных 1С с графом знаний
 */
@Service
class MetadataExporterServiceImpl(
    private val exporter: MetadataExporter,
) : MetadataGraphService {
    override fun loadMetadataToGraph(configurationPath: Path): MetadataLoadResult =
        try {
            loadConfiguration(configurationPath).let { exporter.exportMetadata(it) }
        } catch (ex: Exception) {
            MetadataLoadResult(
                success = false,
                configurationId = null,
                nodesCount = 0,
                edgesCount = 0,
                errors = listOf(ex.message!!),
            )
        }

    private fun loadConfiguration(configurationPath: Path): Configuration {
        logger.info { "${"Начинаем загрузку метаданных в граф из пути: {}"} $configurationPath" }

        return (MDOReader.readConfiguration(configurationPath) as Configuration).also {
            if (it.configurationSource == ConfigurationSource.EMPTY) {
                throw IllegalArgumentException("Не удалось прочитать конфигурацию по пути $configurationPath")
            }
        }
    }
}
