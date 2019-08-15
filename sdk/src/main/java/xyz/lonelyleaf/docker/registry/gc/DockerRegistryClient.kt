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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Suppress("JoinDeclarationAndAssignment")
open class DockerRegistryClient(
        private val baseUrl: String,
        private val user: String? = null,
        private val pass: String? = null,
        private val mapper: ObjectMapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(Jdk8Module())
                .registerModule(JavaTimeModule())
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
) {

    private val client: OkHttpClient

    init {
        @Suppress("UselessCallOnNotNull")
        if (baseUrl.isNullOrEmpty()) {
            throw IllegalArgumentException("baseUrl can not be null")
        }

        val builder = OkHttpClient.Builder()
        if (!user.isNullOrEmpty() || !pass.isNullOrEmpty()) {
            builder.authenticator(authenticator = object : okhttp3.Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    val credential = Credentials.basic(user!!, pass!!)
                    return response.request.newBuilder().header("Authorization", credential).build()
                }
            })
        }

        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)
        builder.connectionPool(ConnectionPool(1, 30, TimeUnit.SECONDS))
        client = builder.build()

        //todo may check here
        //val response = check()
        //if (!response.isSuccessful) {
        //    throw IllegalStateException("");
        //}

    }

    /**
     * check has permission or is conective
     */
    fun check(): Response {
        val request = Request.Builder()
                .url("$baseUrl/v2/").get().build()
        val response = client.newCall(request).execute()
        return response
    }

    /**
     * get a list of image from registry
     */
    fun catalog(): Catalog {
        val request = Request.Builder()
                .url("$baseUrl/v2/_catalog").get().build()
        val response = client.newCall(request).execute()
        response.use {
            if (response.isSuccessful) {
                //成功
                return mapper.readValue<Catalog>(response.body!!.byteStream())
            } else {
                throw HttpRequestFailException("request fail ${response.code} ${response.body?.string()}", response)
            }
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     */
    fun tagList(name: String): TagResponse {
        val request = Request.Builder()
                .url("$baseUrl/v2/$name/tags/list").get().build()
        val response = client.newCall(request).execute()
        response.use {
            if (response.isSuccessful) {
                //成功
                return mapper.readValue(response.body!!.byteStream())
            } else {
                throw HttpRequestFailException("request fail ${response.code} ${response.body?.string()}", response)
            }
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     */
    fun manifest(name: String, tag: String): Manifest {
        val request = Request.Builder()
                .url("$baseUrl/v2/$name/manifests/$tag").get()
                .header("Accept", "application/vnd.docker.distribution.manifest.v1+json")
                .build()
        val response = client.newCall(request).execute()
        response.use {
            if (response.isSuccessful) {
                val manifest = mapper.readValue<Manifest>(response.body!!.byteStream())

                val lastModified = response.header("last-modified")
                if (lastModified != null) {
                    val time = LocalDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME)
                    manifest.lastModified = time
                }

                return manifest
            } else {
                throw HttpRequestFailException("request fail ${response.code} ${response.body?.string()}", response)
            }
        }
    }

    /**
     * get simple image info but not the whole image manifest by request a HEAD request
     */
    fun imageInfo(name: String, tag: String): ImageInfo {
        val request = Request.Builder()
                .url("$baseUrl/v2/$name/manifests/$tag").head()
                .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                .build()
        val response = client.newCall(request).execute()
        response.use {
            if (response.isSuccessful) {

                val digest = response.header("docker-content-digest")
                        ?: throw IllegalStateException("can't get image digest $name:$tag")

                val lastModifiedStr = response.header("last-modified")
                        ?: throw IllegalStateException("can't get image lastModified $name:$tag")
                val lastModified = LocalDateTime.parse(lastModifiedStr, DateTimeFormatter.RFC_1123_DATE_TIME)

                return ImageInfo(name, tag, digest, lastModified)
            } else {
                throw HttpRequestFailException("request fail ${response.code} ${response.body?.string()}", response)
            }
        }
    }


    /**
     * add Accept: application/vnd.docker.distribution.manifest.v2+json in header，and get manifest with digest
     *
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     */
    fun manifestV2(name: String, tag: String): ManifestV2 {
        val request = Request.Builder()
                .url("$baseUrl/v2/$name/manifests/$tag").get()
                .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                .build()
        val response = client.newCall(request).execute()
        response.use {
            if (response.isSuccessful) {
                //成功
                val manifest = mapper.readValue<ManifestV2>(response.body!!.byteStream())

                val digest = response.header("docker-content-digest")
                        ?: throw IllegalStateException("can't get image digest $name:$tag")

                val lastModifiedStr = response.header("last-modified")
                        ?: throw IllegalStateException("can't get image lastModified $name:$tag")
                val lastModified = LocalDateTime.parse(lastModifiedStr, DateTimeFormatter.RFC_1123_DATE_TIME)

                manifest.contentDigest = digest
                manifest.lastModified = lastModified

                return manifest
            } else {
                throw HttpRequestFailException("request fail ${response.code} ${response.body?.string()}", response)
            }
        }
    }

    /**
     * delete image by tag or digest
     *
     * @param name like 'mysql','openjdk' without tag
     * @param tag tag or digest,like 'v1.0' or sha256:aa4410529538c24e5816e770e4a67c97992c21cdf8dae0dfa481e1db70a3aa2b
     *
     * @return 'ok' if success, 'not found' if no image found to delete.
     */
    fun deleteManifest(name: String, tag: String): DeleteResult {
        val request = Request.Builder()
                .url("$baseUrl/v2/$name/manifests/$tag").delete()
                .build()

        val response = client.newCall(request).execute()
        response.use {
            return when {
                response.isSuccessful -> {
                    DeleteResult.OK
                }
                response.code == 404 -> {
                    DeleteResult.NOT_FOUND
                }
                else -> {
                    throw HttpRequestFailException("request fail ${response.code} ${response.body?.string()}", response)
                }
            }
        }
    }

}

enum class DeleteResult {
    OK, NOT_FOUND
}