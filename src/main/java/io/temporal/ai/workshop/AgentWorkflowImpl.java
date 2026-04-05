// ABOUTME: Temporal workflow implementation of the agentic loop.
// Orchestrates LLM calls and tool execution (both as activities) until the goal is achieved.

package io.temporal.ai.workshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.ActivityOptions;
import io.temporal.ai.workshop.model.LlmMessage;
import io.temporal.ai.workshop.model.LlmResponse;
import io.temporal.ai.workshop.model.ToolCallInfo;
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

    private final ToolActivities toolActivities = Workflow.newActivityStub(
            ToolActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build());

    private final ObjectMapper objectMapper = new ObjectMapper();

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

            // Execute each tool call as an activity and add results
            for (ToolCallInfo toolCall : response.toolCalls()) {
                logger.info("Calling tool: {}({})", toolCall.name(), toolCall.arguments());
                String result = executeTool(toolCall.name(), toolCall.arguments());
                logger.info("Tool result: {}", result);
                messages.add(LlmMessage.toolResponse(toolCall.id(), toolCall.name(), result));
            }
        }

        logger.warn("Agent did not complete within {} iterations.", MAX_ITERATIONS);
        return "Agent did not complete within " + MAX_ITERATIONS + " iterations.";
    }

    private String executeTool(String name, String arguments) {
        try {
            JsonNode args = objectMapper.readTree(arguments);
            return switch (name) {
                case "getIpAddress" -> toolActivities.getIpAddress();
                case "getLocationInfo" -> toolActivities.getLocationInfo(args.get("ipAddress").asText());
                case "getCoordinates" -> toolActivities.getCoordinates(args.get("city").asText());
                case "getWeather" -> toolActivities.getWeather(args.get("latitude").asDouble(), args.get("longitude").asDouble());
                default -> "Unknown tool: " + name;
            };
        } catch (Exception e) {
            return "Error executing tool " + name + ": " + e.getMessage();
        }
    }
}
