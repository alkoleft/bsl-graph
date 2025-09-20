package ru.alkoleft.context.infrastructure.metadata

import org.springframework.stereotype.Service
import ru.alkoleft.context.domain.metadata.MetadataGraphService
import ru.alkoleft.context.domain.metadata.MetadataLoadResult
import java.nio.file.Path

/**
 * Реализация сервиса интеграции метаданных 1С с графом знаний
 */
@Service
class MetadataGraphServiceImpl(
    private val loader: MetadataLoader,
) : MetadataGraphService {
    override fun loadMetadataToGraph(configurationPath: Path): MetadataLoadResult =
        loader.load(configurationPath)
}
