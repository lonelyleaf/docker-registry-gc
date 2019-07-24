package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import xyz.lonelyleaf.docker.registry.gc.config.DockerRegistryProperties
import xyz.lonelyleaf.docker.registry.gc.config.RegistryCleanupRuleDto
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

        matchers.parallelStream()
                .flatMap { (imageName, rule) ->
                    val tags = client.tagList(imageName).tags
                    val matchedTags = findMatchedTags(tags, rule)
                    return@flatMap matchedTags.map { Triple(imageName, it, rule) }.stream()
                }
                .filter { (imageName, tag, rule) ->
                    try {
                        val created = client.manifest(imageName, tag).firstV1Compatibility(mapper).created
                        return@filter LocalDateTime.now().isAfter(created.plus(rule.durationToKeep))
                    } catch (e: Exception) {
                        logger.error("get image manifest fail", e)
                        return@filter false
                    }
                }
                .forEach { (imageName, tag, _) ->
                    try {
                        client.deleteManifest(imageName, tag)
                        logger.info("delete image $imageName:$tag success")
                    } catch (e: Exception) {
                        logger.error("delete image fail", e)
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
