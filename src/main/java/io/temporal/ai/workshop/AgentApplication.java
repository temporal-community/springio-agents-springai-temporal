// ABOUTME: Spring Boot entry point that runs the agentic loop with a goal from command-line args.
// No web server - just a CLI application that runs the agent and exits.

package io.temporal.ai.workshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    @Component
    static class AgentRunner implements ApplicationRunner {

        private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

        private final AgentLoop agentLoop;

        AgentRunner(AgentLoop agentLoop) {
            this.agentLoop = agentLoop;
        }

        @Override
        public void run(ApplicationArguments args) {
            if (args.getNonOptionArgs().isEmpty()) {
                log.error("Usage: java -jar workshop-agent.jar \"<your goal>\"");
                log.error("Example: java -jar workshop-agent.jar \"What is the weather in Tokyo and New York? Which city is warmer?\"");
                return;
            }

            String goal = String.join(" ", args.getNonOptionArgs());
            log.info("Goal: {}", goal);

            String result = agentLoop.execute(goal);

            System.out.println();
            System.out.println("=== Agent Result ===");
            System.out.println(result);
        }
    }
}
