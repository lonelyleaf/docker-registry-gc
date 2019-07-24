package xyz.lonelyleaf.docker.registry.gc

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CleanupJob {

    @Autowired
    private lateinit var registryMaid: RegistryMaid

    //every 15min
    @Scheduled(fixedDelay = (1000 * 60 * 15).toLong())
    fun cleanup() {
        registryMaid.cleanup()
    }

}
