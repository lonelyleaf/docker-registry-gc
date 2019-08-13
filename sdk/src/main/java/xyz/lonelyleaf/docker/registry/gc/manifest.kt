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

    var lastModified: LocalDateTime? = null

    fun firstV1Compatibility(mapper: ObjectMapper): V1Compatibility {
        return mapper.readValue(history.first().v1Compatibility)
    }

    data class FsLayer(
            val blobSum: String
    )

    data class History(
            val v1Compatibility: String
    )

    data class V1Compatibility(
            val created: LocalDateTime
    )

}

data class ManifestV2(
        val schemaVersion: Int,
        val mediaType: String?,
        val config: Layer?,
        val layers: List<Layer>?
) {

    var contentDigest: String = ""
    var lastModified: LocalDateTime? = null

    data class Layer(
            val mediaType: String,
            val size: Int,
            val digest: String
    )

}


/**
 * simple image info
 */
data class ImageInfo(
        val image: String,
        val tag: String,
        val contentDigest: String,
        val lastModified: LocalDateTime
)