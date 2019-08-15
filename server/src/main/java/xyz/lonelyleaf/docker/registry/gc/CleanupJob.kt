package xyz.lonelyleaf.docker.registry.gc

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger
import xyz.lonelyleaf.docker.registry.gc.config.DockerRegistry
import xyz.lonelyleaf.docker.registry.gc.config.DockerRegistryGc

//@Component
class CleanupJob(
        private val prop: DockerRegistryGc,
        private val objectMapper: ObjectMapper,
        private val taskScheduler: TaskScheduler
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val maids: List<RegistryMaid>

    init {
        val maids = ArrayList<RegistryMaid>(prop.registry.size)

        if (prop.registry.isEmpty()) {
            throw IllegalArgumentException("no registry found,check your config file")
        }

        for (registry: DockerRegistry in prop.registry) {
            if (registry.baseUrl.isEmpty()) {
                throw IllegalArgumentException("registry baseUrl can't be null or empty")
            }

            val client = DockerRegistryClient(registry.baseUrl, registry.user, registry.pass, objectMapper)
            val maid = RegistryMaid(registry, client)
            maids.add(maid)

            val scheduler = registry.scheduler.scheduler
            val task = Runnable {
                logger.info("******start clean up ${registry.baseUrl}******")
                maid.cleanup()
                logger.info("******clean up finish ${registry.baseUrl}******")
            }
            if (!scheduler.cron.isNullOrEmpty()) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                taskScheduler.schedule(task, CronTrigger(scheduler.cron!!))
            } else {
                taskScheduler.scheduleWithFixedDelay(task, scheduler.fix!!)
            }
            logger.info("load config for ${registry.baseUrl}")

        }
        this.maids = maids.toList()
    }

}
