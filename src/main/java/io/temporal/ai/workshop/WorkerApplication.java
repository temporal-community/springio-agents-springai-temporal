// ABOUTME: Spring Boot entry point that starts the Temporal worker.
// Run this in one terminal window - it stays running, polling for workflow and activity tasks.

package io.temporal.ai.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
