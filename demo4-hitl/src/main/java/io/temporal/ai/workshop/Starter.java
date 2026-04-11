// ABOUTME: Standalone client that starts an agent workflow and handles human-in-the-loop interaction.
// Polls the workflow for questions, prompts the user, and sends responses via signal.

package io.temporal.ai.workshop;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowQueryException;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Starter {

    private static final String TASK_QUEUE = "agent-task-queue";
    private static final long POLL_INTERVAL_MS = 2000;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: Starter \"<your goal>\"");
            System.err.println("       Starter --workflow-id <id>");
            System.err.println("Example: Starter \"Should I bring rain gear to the next F1 race?\"");
            System.exit(1);
        }

        WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(serviceStubs);

        AgentWorkflow workflow;

        if (args[0].equals("--workflow-id")) {
            if (args.length < 2) {
                System.err.println("Error: --workflow-id requires a workflow ID argument");
                System.exit(1);
            }
            String workflowId = args[1];
            System.out.println("Reconnecting to workflow: " + workflowId);
            workflow = client.newWorkflowStub(AgentWorkflow.class, workflowId);
        } else {
            String goal = String.join(" ", args);
            System.out.println("Starting agent with goal: " + goal);
            workflow = client.newWorkflowStub(
                    AgentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId("agent-" + UUID.randomUUID())
                            .setTaskQueue(TASK_QUEUE)
                            .build());
            WorkflowClient.start(workflow::run, goal);
        }

        // Wait for result on a background thread
        WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
        CompletableFuture<String> resultFuture = CompletableFuture.supplyAsync(
                () -> untypedStub.getResult(String.class));

        Scanner scanner = new Scanner(System.in);
        System.out.println("Agent is working...");

        // Poll for questions until the workflow completes
        while (!resultFuture.isDone()) {
            try {
                if (workflow.isInputNeeded()) {
                    String question = workflow.getPendingQuestion();
                    System.out.println();
                    System.out.println("Agent asks: " + question);
                    System.out.print("Your response: ");
                    String response = scanner.nextLine();
                    workflow.provideUserInput(response);
                    System.out.println("Agent is working...");
                }
            } catch (WorkflowQueryException e) {
                // Workflow not ready for queries yet (still initializing)
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }

        // Print the result
        try {
            String result = resultFuture.get();
            System.out.println();
            System.out.println("=== Agent Result ===");
            System.out.println(result);
        } catch (Exception e) {
            System.err.println("Workflow failed: " + e.getCause().getMessage());
        }

        scanner.close();
        System.exit(0);
    }
}
