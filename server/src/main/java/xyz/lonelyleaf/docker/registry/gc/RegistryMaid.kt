/*
 * Copyright (c) [2019] [lonelyleaf]
 * [docker-registry-gc] is licensed under the Mulan PSL v1.
 * You can use this software according to the terms and conditions of the Mulan PSL v1.
 * You may obtain a copy of Mulan PSL v1 at:
 *    http://license.coscl.org.cn/MulanPSL
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 *
 * See the Mulan PSL v1 for more details.
 */

package xyz.lonelyleaf.docker.registry.gc

import org.slf4j.LoggerFactory
import xyz.lonelyleaf.docker.registry.gc.config.DockerRegistry
import xyz.lonelyleaf.docker.registry.gc.config.RegistryCleanupRuleDto
import java.time.Duration
import java.time.LocalDateTime

/**
 * clean up your registry
 */
class RegistryMaid(
        private val prop: DockerRegistry,
        private val client: DockerRegistryClient
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

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
