// ABOUTME: Spring Boot entry point that starts the Temporal worker.
// The temporal-spring-boot-starter auto-configures the worker from application.yaml.

package io.temporal.ai.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = "io.temporal.ai",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "io\\.temporal\\.ai\\.mcp\\..*"))
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
