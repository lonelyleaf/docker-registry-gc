package xyz.lonelyleaf.docker.registry.gc.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.lonelyleaf.docker.registry.gc.DockerRegistryClient
import java.time.Duration
import java.util.regex.Pattern

@Configuration
@EnableConfigurationProperties(DockerRegistryProperties::class)
class DockerClientConfig {

    @Autowired
    lateinit var prop: DockerRegistryProperties

    @Bean
    fun dockerRegistryClient(mapper: ObjectMapper): DockerRegistryClient {
        return DockerRegistryClient(prop.baseUrl, prop.user, prop.pass, mapper)
    }

}

@ConfigurationProperties(prefix = "docker.registry")
class DockerRegistryProperties {
    var baseUrl: String = ""
    var user: String? = null
    var pass: String? = null
    var cleanup: List<RegistryCleanupRuleDto> = emptyList()
}

data class RegistryCleanupRuleDto(
        /**
         * image name regexp [Pattern],matched image will be deleted
         */
        val image: String,
        /**
         * image tag regexp [Pattern],matched image will be deleted.If is null,all tag will
         * be matched.
         * */
        val tag: String?,
        /**
         * ISO 8601 duration format string.Any image match [image] and [tag]
         * before this duration will be deleted.
         *
         * For example:
         *
         * - "PT20.345S" -- parses as "20.345 seconds"
         * - "PT15M"     -- parses as "15 minutes"
         * - "PT10H"     -- parses as "10 hours"
         * - "P2D"       -- parses as "2 days"
         * - "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"
         * - "PT-6H3M"    -- parses as "-6 hours and +3 minutes"
         * - "-PT6H3M"    -- parses as "-6 hours and -3 minutes"
         * - "-PT-6H+3M"  -- parses as "+6 hours and -3 minutes"
         *
         */
        val durationToKeep: String
) {

    val rule: Rule by lazy {
        return@lazy Rule(Pattern.compile(this@RegistryCleanupRuleDto.image),
                Pattern.compile(this@RegistryCleanupRuleDto.tag ?: ".*"),
                Duration.parse(this@RegistryCleanupRuleDto.durationToKeep))
    }

    data class Rule(
            val image: Pattern,
            val tag: Pattern,
            val durationToKeep: Duration
    )

}
