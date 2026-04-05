// ABOUTME: Standalone client that starts an agent workflow and waits for the result.
// Run this in a second terminal window after the worker is running.

package io.temporal.ai.workshop;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.UUID;

public class Starter {

    private static final String TASK_QUEUE = "agent-task-queue";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: Starter \"<your goal>\"");
            System.err.println("Example: Starter \"What is the weather in Tokyo and New York? Which city is warmer?\"");
            System.exit(1);
        }

        String goal = String.join(" ", args);
        System.out.println("Starting agent with goal: " + goal);

        WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(serviceStubs);

        AgentWorkflow workflow = client.newWorkflowStub(
                AgentWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("agent-" + UUID.randomUUID())
                        .setTaskQueue(TASK_QUEUE)
                        .build());

        String result = workflow.run(goal);

        System.out.println();
        System.out.println("=== Agent Result ===");
        System.out.println(result);

        System.exit(0);
    }
}
