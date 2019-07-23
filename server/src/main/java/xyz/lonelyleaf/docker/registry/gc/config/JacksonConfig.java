package xyz.lonelyleaf.docker.registry.gc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.lonelyleaf.docker.registry.gc.util.JsonUtil;

@Configuration
public class JacksonConfig {

    @Bean
    public KotlinModule kotlinModule() {
        return new KotlinModule();
    }

    /*
     * Jackson Afterburner module to speed up serialization/deserialization.
     */
    @Bean
    public AfterburnerModule afterburnerModule() {
        AfterburnerModule module = new AfterburnerModule();
        // make Afterburner generate bytecode only for public getters/setter and fields
        // without this, Java 9+ complains of "Illegal reflective access"
        module.setUseValueClassLoader(false);
        return module;
    }

    @Bean
    public JsonUtil jsonUtil(ObjectMapper objectMapper) {
        JsonUtil.INSTANCE.setObjectMapper(objectMapper);
        return new JsonUtil();
    }

}
