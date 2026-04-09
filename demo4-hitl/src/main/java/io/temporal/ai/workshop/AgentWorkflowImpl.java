// ABOUTME: Temporal workflow implementation with human-in-the-loop support.
// The agent can ask the user questions via the AskUserTool, pausing until a signal arrives.

package io.temporal.ai.workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.springai.chat.TemporalChatClient;
import io.temporal.springai.model.ActivityChatModel;
import io.temporal.springai.activity.ChatModelActivity;
import io.temporal.springai.mcp.McpToolCallback;
import io.temporal.springai.mcp.ActivityMcpClient;
import io.temporal.springai.mcp.McpClientActivity;
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
            If the user's request is ambiguous or you need more information, use the askUser tool to ask them a question.
            When you have enough information to fully answer, provide your final response as plain text.
            Today's date is %s.
            """;

    private final AskUserTool askUserTool = new AskUserTool();

    // Activity stubs (non-blocking to create)
    private final ChatModelActivity chatModelActivity;
    private final ToolActivities toolActivity;
    private final McpClientActivity mcpClientActivity;

    @WorkflowInit
    public AgentWorkflowImpl(String goal) {
        this.chatModelActivity = Workflow.newActivityStub(
                ChatModelActivity.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(60))
                        .build());

        this.toolActivity = Workflow.newActivityStub(
                ToolActivities.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .build());

        this.mcpClientActivity = Workflow.newActivityStub(
                McpClientActivity.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .build());
    }

    @Override
    public String run(String goal) {
        // Build the ChatClient here (not in constructor) because MCP tool discovery
        // executes an activity, which would block the constructor and delay
        // signal/query handler registration.
        ActivityChatModel activityChatModel = new ActivityChatModel(chatModelActivity);
        ActivityMcpClient mcpClient = new ActivityMcpClient(mcpClientActivity);
        List<ToolCallback> mcpTools = McpToolCallback.fromMcpClient(mcpClient);

        String currentDate = Instant.ofEpochMilli(Workflow.currentTimeMillis())
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        String systemPrompt = String.format(SYSTEM_PROMPT, currentDate);

        ChatClient chatClient = TemporalChatClient.builder(activityChatModel)
                .defaultTools(toolActivity, askUserTool)
                .defaultToolCallbacks(mcpTools)
                .defaultSystem(systemPrompt)
                .build();

        return chatClient
                .prompt()
                .user(goal)
                .call()
                .content();
    }

    @Override
    public void provideUserInput(String input) {
        askUserTool.provideInput(input);
    }

    @Override
    public boolean isInputNeeded() {
        return askUserTool.isInputNeeded();
    }

    @Override
    public String getPendingQuestion() {
        return askUserTool.getQuestion();
    }
}
