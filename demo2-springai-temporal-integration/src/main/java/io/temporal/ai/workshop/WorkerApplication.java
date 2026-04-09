// ABOUTME: Spring Boot entry point that starts the Temporal worker.
// The temporal-spring-boot-starter auto-configures the worker from application.yaml.
// The temporal-spring-ai plugin auto-registers ChatModelActivity via auto-configuration.

package io.temporal.ai.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
