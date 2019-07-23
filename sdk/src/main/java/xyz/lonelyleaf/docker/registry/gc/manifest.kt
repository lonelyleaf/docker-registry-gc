package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime

data class Manifest(
        val schemaVersion: Int,
        val name: String,
        val tag: String,
        val architecture: String,
        val fsLayers: List<FsLayer>,
        val history: List<History>
) {

    fun firstV1Compatibility(mapper: ObjectMapper): V1Compatibility {
        return mapper.readValue(history.first().v1Compatibility)
    }

    data class FsLayer(
        val blobSum: String
    )

    data class History(
        val v1Compatibility: String
    )

}

data class V1Compatibility(
    val created: LocalDateTime
)
