// ABOUTME: Temporal workflow implementation with weather tools and F1 MCP tools.
// Spring AI's ChatClient handles the tool calling loop; Temporal provides durability.

package io.temporal.ai.workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.ai.chat.client.TemporalChatClient;
import io.temporal.ai.chat.model.ActivityChatModel;
import io.temporal.ai.chat.model.ChatModelActivity;
import io.temporal.ai.mcp.McpToolCallback;
import io.temporal.ai.mcp.client.ActivityMcpClient;
import io.temporal.ai.mcp.client.McpClientActivity;
import io.temporal.ai.workshop.tools.ToolActivities;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AgentWorkflowImpl implements AgentWorkflow {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Use the provided tools to help accomplish the user's goal.
            Always use tools when they would help answer the question accurately.
            You have access to weather tools (geocoding, current weather) and Formula 1 race data tools (schedules, results, standings).
            When you have enough information to fully answer, provide your final response as plain text.
            Today's date is %s.
            """;

    private final ChatClient chatClient;

    @WorkflowInit
    public AgentWorkflowImpl(String goal) {
        ChatModelActivity chatModelActivity = Workflow.newActivityStub(
                ChatModelActivity.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(60))
                        .build());

        ToolActivities toolActivity = Workflow.newActivityStub(
                ToolActivities.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .build());

        McpClientActivity mcpClientActivity = Workflow.newActivityStub(
                McpClientActivity.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .build());

        ActivityChatModel activityChatModel = new ActivityChatModel(chatModelActivity);
        ActivityMcpClient mcpClient = new ActivityMcpClient(mcpClientActivity);
        List<ToolCallback> mcpTools = McpToolCallback.fromMcpTools(mcpClient);

        // Inject current date into system prompt using Temporal's deterministic clock
        String currentDate = Instant.ofEpochMilli(Workflow.currentTimeMillis())
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        String systemPrompt = String.format(SYSTEM_PROMPT, currentDate);

        // Activity-backed tools go through defaultTools() (processed by TemporalToolUtil).
        // MCP tool callbacks go through defaultToolCallbacks() (added directly, already Temporal-safe).
        this.chatClient = TemporalChatClient.builder(activityChatModel)
                .defaultTools(toolActivity)
                .defaultToolCallbacks(mcpTools)
                .defaultSystem(systemPrompt)
                .build();
    }

    @Override
    public String run(String goal) {
        return chatClient
                .prompt()
                .user(goal)
                .call()
                .content();
    }
}
