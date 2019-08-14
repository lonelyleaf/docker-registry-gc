package xyz.lonelyleaf.docker.registry.gc.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import xyz.lonelyleaf.docker.registry.gc.CleanupJob
import java.time.Duration
import java.util.regex.Pattern
import javax.validation.constraints.NotEmpty

@Configuration
@EnableConfigurationProperties(DockerRegistryGc::class)
class DockerRegistryGcConfig {

    @Autowired
    lateinit var prop: DockerRegistryGc

    @Bean
    fun cleanupJob(taskScheduler: TaskScheduler, mapper: ObjectMapper): CleanupJob {
        return CleanupJob(prop, mapper, taskScheduler)
    }

}

@ConfigurationProperties(prefix = "docker.gc")
class DockerRegistryGc {
    var registry: List<DockerRegistry> = emptyList()
}

class DockerRegistry {
    @NotEmpty
    var baseUrl: String = ""
    var user: String? = null
    var pass: String? = null
    var scheduler: SchedulerDto = SchedulerDto()
    var cleanup: List<RegistryCleanupRuleDto> = emptyList()
}

/**
 * define when to clean up
 */
class SchedulerDto {
    var fix: String? = null
    var cron: String? = null

    val scheduler: Scheduler by lazy {
        if (cron.isNullOrEmpty() && fix.isNullOrEmpty()) {
            return@lazy Scheduler(Duration.ofHours(1), null)
        } else if (fix.isNullOrEmpty()) {
            return@lazy Scheduler(null, this.cron)
        } else {
            return@lazy Scheduler(Duration.parse(fix), this.cron)
        }
    }

    data class Scheduler(
            var fix: Duration? = null,
            var cron: String? = null
    )

}

/**
 * define which image to clean up
 */
class RegistryCleanupRuleDto {

    /**
     * image name regexp [Pattern],matched image will be deleted
     */
    var image: String = ""
    /**
     * image tag regexp [Pattern],matched image will be deleted.If is null,all tag will
     * be matched.
     * */
    var tag: String? = ""
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
    var durationToKeep: String = ""

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
