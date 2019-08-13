package xyz.lonelyleaf.docker.registry.gc

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CleanupJob {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Autowired
    private lateinit var registryMaid: RegistryMaid

    //every 15min
    @Scheduled(fixedDelay = (1000 * 60 * 30).toLong())
    fun cleanup() {
        logger.info("*****start cleanup*****")
        registryMaid.cleanup()
        logger.info("****cleanup done****")
    }

}
