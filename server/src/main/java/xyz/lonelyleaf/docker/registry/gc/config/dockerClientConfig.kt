package xyz.lonelyleaf.docker.registry.gc.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.lonelyleaf.docker.registry.gc.DockerRegistryClient

@Configuration
@EnableConfigurationProperties(DockerClientProperties::class)
class DockerClientConfig {

    @Autowired
    lateinit var prop: DockerClientProperties

    @Bean
    fun dockerRegistryClient(mapper: ObjectMapper): DockerRegistryClient {
        return DockerRegistryClient(prop.baseUrl, prop.user, prop.pass, mapper)
    }

}

@ConfigurationProperties(prefix = "docker.registry")
data class DockerClientProperties(
        val baseUrl: String,
        val user: String? = null,
        val pass: String? = null
)