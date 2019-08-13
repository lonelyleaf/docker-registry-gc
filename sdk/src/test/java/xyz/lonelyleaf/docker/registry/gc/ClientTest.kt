package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class ClientTest {

    private lateinit var client: DockerRegistryClient

    @Before
    fun init() {
        //only need properties for test
        val prop = Properties()
        prop.load(ClassLoader.getSystemClassLoader().getResourceAsStream("docker-client.properties"))

        client = DockerRegistryClient(
                baseUrl = prop.getProperty("baseUrl"),
                user = prop.getProperty("user"),
                pass = prop.getProperty("pass")
        )
    }


    @Test
    fun testCatalog() {
        val catalog = client.catalog()
        Assert.assertTrue(!catalog.repositories.isNullOrEmpty())
        println(JsonUtil.toPrettyJson(catalog))
    }

    @Test
    fun testTagList() {
        val tagList = client.tagList("gmt/gmt-oss-ocs-bp-srv")
        Assert.assertTrue(!tagList.tags.isNullOrEmpty())
        println(JsonUtil.toPrettyJson(tagList))
    }

    @Test
    fun testManifest() {
        val manifest = client.manifest("gmt/gmt-oss-ocs-bp-srv", "193-test-b4e4d2")
        println(manifest)
        val v1Compatibility = manifest.firstV1Compatibility(JsonUtil.objectMapper)
        println()
        println(v1Compatibility)
    }

    @Test
    fun testDelete(){
        val manifest = client.manifest("gmt/gmt-csp-api", "3-new-jenkins-file-1a0d9f")
        val manifestV2 = client.manifestV2("gmt/gmt-csp-api", "3-new-jenkins-file-1a0d9f")
        println(manifest)
        println(manifestV2)
//        cae79479c785eaa74ba2a6d6126c225d89aa99bd8bdcabf506f49072e86c914c
    }
}
