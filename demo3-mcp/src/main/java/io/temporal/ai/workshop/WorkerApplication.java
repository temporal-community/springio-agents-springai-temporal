// ABOUTME: Spring Boot entry point that starts the Temporal worker.
// The temporal-spring-boot-starter auto-configures the worker from application.yaml.

package io.temporal.ai.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.temporal.ai")
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
