package xyz.lonelyleaf.docker.registry.gc.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule


/**
 * 方便序列化json
 */
object JsonUtil {

    @JvmStatic
    private var objectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    @JvmStatic
    fun setObjectMapper(objectMapper: ObjectMapper) {
        JsonUtil.objectMapper = objectMapper
    }

    @JvmStatic
    fun toPrettyJson(obj: Any): String {
        try {
            return objectMapper!!.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }

    }

}
