package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import xyz.lonelyleaf.docker.registry.gc.config.DockerRegistryProperties
import xyz.lonelyleaf.docker.registry.gc.config.RegistryCleanupRuleDto
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [RegistryGcApplication::class])
class DockerRegistryGcApplicationTests {

    private val logger = LoggerFactory.getLogger(this::class.java)
    @Autowired
    private lateinit var client: DockerRegistryClient
    @Autowired
    private lateinit var mapper: ObjectMapper

    @Test
    fun contextLoads() {
    }

//    @Test
//    fun testHead(){
//        val manifestV2 = client.manifestV2("gmt/9176-admin-server", "11-test-670f2e")
//        println(manifestV2)
//    }

}
