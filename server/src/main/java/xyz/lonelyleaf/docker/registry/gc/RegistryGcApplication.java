package xyz.lonelyleaf.docker.registry.gc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RegistryGcApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryGcApplication.class, args);
    }

}
