package xyz.lonelyleaf.docker.registry.gc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RegistryGcApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryGcApplication.class, args);
    }

}
