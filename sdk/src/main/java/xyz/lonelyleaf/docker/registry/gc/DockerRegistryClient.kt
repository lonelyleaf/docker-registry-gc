package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.lang.RuntimeException
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Suppress("JoinDeclarationAndAssignment")
class DockerRegistryClient(
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
        client = HttpClient.newBuilder()
                .authenticator(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(user, pass?.toCharArray())
                    }
                })
                .build()
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
            throw RuntimeException("request fail ${response.statusCode()} ${response.body()}")
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     */
    fun tagList(name: String): TagList {
        val uri =
                URI("$baseUrl/v2/$name/tags/list")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            //成功
            return mapper.readValue(response.body())
        } else {
            throw RuntimeException("request fail ${response.statusCode()} ${response.body()}")
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     */
    fun manifest(name: String, tag: String): Manifest {
        val uri =
                URI("$baseUrl/v2/$name/manifests/$tag")
        val request = HttpRequest.newBuilder(uri).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            //成功
            return mapper.readValue(response.body())
        } else {
            throw RuntimeException("request fail ${response.statusCode()} ${response.body()}")
        }
    }

    /**
     * @param name like 'mysql','openjdk' without tag
     * @param tag like 'v1.0'
     */
    fun deleteManifest(name: String, tag: String): String {
        val uri =
                URI("$baseUrl/v2/$name/manifests/$tag")
        val request = HttpRequest.newBuilder(uri).DELETE().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return when (response.statusCode()) {
            in 200..299 -> {
                println("delete success $name:$tag")
                "ok"
            }
            404 -> {
                println("can't find image $name:$tag")
                "not found"
            }
            else -> {
                throw RuntimeException("request fail ${response.statusCode()} ${response.body()}")
            }
        }
    }

}