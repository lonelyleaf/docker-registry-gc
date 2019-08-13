package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    private val client: HttpClient

    init {
        @Suppress("UselessCallOnNotNull")
        if (baseUrl.isNullOrEmpty()) {
            throw IllegalArgumentException("baseUrl can not be null")
        }

        client = if (!user.isNullOrEmpty() || !pass.isNullOrEmpty()) {
            HttpClient.newBuilder()
                    .authenticator(object : Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication {
                            return PasswordAuthentication(user, pass?.toCharArray())
                        }
                    })
                    .build()
        } else {
            HttpClient.newBuilder().build()
        }
    }

    /**
     * check has permission or is conective
     */
    fun check(): Boolean {
        val uri = URI("$baseUrl/v2/")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.statusCode() in 200..299
    }

    /**
     * get a list of image from registry
     */
    fun catalog(): Catalog {
        val uri = URI("$baseUrl/v2/_catalog")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            //成功
            return mapper.readValue(response.body())
        } else {
            throw HttpRequestFailException("request fail ${response.statusCode()} ${response.body()}", response)
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     */
    fun tagList(name: String): TagResponse {
        val uri =
                URI("$baseUrl/v2/$name/tags/list")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            //成功
            return mapper.readValue(response.body())
        } else {
            throw HttpRequestFailException("request fail ${response.statusCode()} ${response.body()}", response)
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     */
    fun manifest(name: String, tag: String): Manifest {
        val uri = URI("$baseUrl/v2/$name/manifests/$tag")
        val request = HttpRequest.newBuilder(uri).GET()
                .header("Accept", "application/vnd.docker.distribution.manifest.v1+json")
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            val manifest = mapper.readValue<Manifest>(response.body())

            val lastModified = response.headers().firstValue("last-modified")
            if (lastModified.isPresent) {
                val time = LocalDateTime.parse(lastModified.get(), DateTimeFormatter.RFC_1123_DATE_TIME)
                manifest.lastModified = time
            }

            return manifest
        } else {
            throw HttpRequestFailException("request fail ${response.statusCode()} ${response.body()}", response)
        }
    }

    /**
     * get simple image info but not the whole image manifest by request a HEAD request
     */
    fun imageInfo(name: String, tag: String): ImageInfo {
        val uri = URI("$baseUrl/v2/$name/manifests/$tag")
        val request = HttpRequest.newBuilder(uri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {

            val digest = response.headers().firstValue("docker-content-digest")
                    .orElseThrow { IllegalStateException("can't get image digest $name:$tag") }

            val lastModifiedStr = response.headers().firstValue("last-modified")
                    .orElseThrow { IllegalStateException("can't get image lastModified $name:$tag") }
            val lastModified = LocalDateTime.parse(lastModifiedStr, DateTimeFormatter.RFC_1123_DATE_TIME)

            return ImageInfo(name, tag, digest, lastModified)
        } else {
            throw HttpRequestFailException("request fail ${response.statusCode()} ${response.body()}", response)
        }
    }


    /**
     * add Accept: application/vnd.docker.distribution.manifest.v2+json in header，and get manifest with digest
     *
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     */
    fun manifestV2(name: String, tag: String): ManifestV2 {
        val uri =
                URI("$baseUrl/v2/$name/manifests/$tag")
        val request = HttpRequest.newBuilder(uri).GET()
                .header("Accept", "application/vnd.docker.distribution.manifest.v2+json")
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            //成功
            val manifest = mapper.readValue<ManifestV2>(response.body())

            manifest.contentDigest = response.headers().firstValue("docker-content-digest")
                    .orElseThrow { IllegalStateException("can't get image digest $name:$tag") }

            val lastModified = response.headers().firstValue("last-modified")
            if (lastModified.isPresent) {
                val time = LocalDateTime.parse(lastModified.get(), DateTimeFormatter.RFC_1123_DATE_TIME)
                manifest.lastModified = time
            }

            return manifest
        } else {
            throw HttpRequestFailException("request fail ${response.statusCode()} ${response.body()}", response)
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     *
     * @return 'ok' if success, 'not found' if no image found to delete.
     */
    fun deleteManifest(name: String, tag: String): String {
        val uri =
                URI("$baseUrl/v2/$name/manifests/$tag")
        val request = HttpRequest.newBuilder(uri).DELETE().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return when (response.statusCode()) {
            in 200..299 -> {
                "ok"
            }
            404 -> {
                "not found"
            }
            else -> {
                throw HttpRequestFailException("request fail ${response.statusCode()} ${response.body()}", response)
            }
        }
    }

}