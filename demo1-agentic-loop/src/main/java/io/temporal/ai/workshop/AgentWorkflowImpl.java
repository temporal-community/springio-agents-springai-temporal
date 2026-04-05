// ABOUTME: Temporal workflow implementation of the agentic loop.
// Orchestrates LLM calls and tool execution (both as activities) until the goal is achieved.

package io.temporal.ai.workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.ai.workshop.model.LlmMessage;
import io.temporal.ai.workshop.model.LlmResponse;
import io.temporal.ai.workshop.model.ToolCallInfo;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AgentWorkflowImpl implements AgentWorkflow {

    private static final int MAX_ITERATIONS = 10;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Use the provided tools to help accomplish the user's goal.
            Always use tools when they would help answer the question accurately.
            When you have enough information to fully answer, provide your final response as plain text.
            """;

    private final LlmActivities llmActivities = Workflow.newActivityStub(
            LlmActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .build());

    private final ActivityStub toolActivity = Workflow.newUntypedActivityStub(
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build());

    @Override
    public String run(String goal) {
        var logger = Workflow.getLogger(AgentWorkflowImpl.class);
        logger.info("Starting agent with goal: {}", goal);

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.system(SYSTEM_PROMPT));
        messages.add(LlmMessage.user(goal));

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            logger.info("=== Iteration {} ===", iteration);

            LlmResponse response = llmActivities.callLlm(messages);

            if (!response.hasToolCalls()) {
                logger.info("Agent completed after {} iteration(s).", iteration);
                return response.content();
            }

            // Add assistant message (with tool calls) to conversation
            messages.add(LlmMessage.assistant(response.content(), response.toolCalls()));

            // Execute each tool call as a dynamic activity and add results
            for (ToolCallInfo toolCall : response.toolCalls()) {
                logger.info("Calling tool: {}({})", toolCall.name(), toolCall.arguments());
                String result = toolActivity.execute(toolCall.name(), String.class, toolCall.arguments());
                logger.info("Tool result: {}", result);
                messages.add(LlmMessage.toolResponse(toolCall.id(), toolCall.name(), result));
            }
        }

        logger.warn("Agent did not complete within {} iterations.", MAX_ITERATIONS);
        return "Agent did not complete within " + MAX_ITERATIONS + " iterations.";
    }
}
