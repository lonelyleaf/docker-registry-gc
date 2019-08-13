package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import xyz.lonelyleaf.docker.registry.gc.config.DockerRegistryProperties
import xyz.lonelyleaf.docker.registry.gc.config.RegistryCleanupRuleDto
import java.lang.RuntimeException
import java.time.Duration
import java.time.LocalDateTime

/**
 * clean up your registry
 */
@Component
class RegistryMaid {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var prop: DockerRegistryProperties
    @Autowired
    private lateinit var client: DockerRegistryClient
    @Autowired
    private lateinit var mapper: ObjectMapper

    fun cleanup() {
        val images = client.catalog().repositories

        val matchers = ArrayList<Pair<String, RegistryCleanupRuleDto.Rule>>()

        for (imageName in images) {
            for (dto in prop.cleanup) {
                if (dto.rule.image.matcher(imageName).matches()) {
                    matchers.add(imageName to dto.rule)
                }
            }
        }
        val imageListStr = matchers.map { it.first }.joinToString(separator = ",")
        logger.info("find matched images $imageListStr")

        matchers.parallelStream()
                .flatMap { (imageName, rule) ->
                    val tags = client.tagList(imageName).tags
                    val matchedTags = findMatchedTags(tags, rule)
                    return@flatMap matchedTags.map { Triple(imageName, it, rule) }.stream()
                }
                .map { (imageName, tag, rule) ->
                    try {
                        //use HEAD to get image info is much quicker than get the whole image manifest
                        return@map Pair(client.imageInfo(imageName, tag), rule)
                    } catch (e: Exception) {
                        logger.error("get image manifest fail", e)
                        return@map Pair(null, rule)
                    }
                }
                .filter { (imageInfo, rule) ->
                    if (imageInfo == null) {
                        return@filter false
                    } else {
                        val created = imageInfo.lastModified
                        val isBefore = Duration.between(created, LocalDateTime.now()) > rule.durationToKeep
                        return@filter isBefore
                    }
                }
                //.sequential()
                .forEach { (imageInfo: ImageInfo?, _) ->
                    val (image, tag, digest, _) = imageInfo!!
                    try {
                        //val digest = client.manifestV2(name, tag).contentDigest
                        //no need to do this again,delete by digest is better
                        //val resultTag = client.deleteManifest(imageName, tag)
                        val result = client.deleteManifest(image, digest)
                        when (result) {
                            DeleteResult.OK -> logger.info("delete image success $image:$tag digest:$digest ")
                            DeleteResult.NOT_FOUND -> logger.warn("delete image not found $image:$tag digest:$digest")
                            else -> throw RuntimeException("bad result")
                        }
                    } catch (e: Exception) {
                        logger.error("delete image fail $imageInfo", e)
                    }
                }
    }

    fun findMatchedTags(tags: List<String>, rule: RegistryCleanupRuleDto.Rule): List<String> {
        val matchedTags: MutableList<String>
        if (rule.tag.pattern() == ".*") {
            //matched all
            matchedTags = tags.toMutableList()
        } else {
            matchedTags = ArrayList(tags.size)
            for (tag in tags) {
                if (rule.tag.matcher(tag).matches()) {
                    matchedTags.add(tag)
                }
            }
        }
        return matchedTags
    }

}
