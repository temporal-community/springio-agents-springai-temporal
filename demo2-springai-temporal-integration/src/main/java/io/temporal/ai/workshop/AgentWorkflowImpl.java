// ABOUTME: Temporal workflow implementation using the temporal-spring-ai integration.
// Spring AI's ChatClient handles the tool calling loop; Temporal provides durability.

package io.temporal.ai.workshop;

import io.temporal.activity.ActivityOptions;
import io.temporal.ai.chat.client.TemporalChatClient;
import io.temporal.ai.chat.model.ActivityChatModel;
import io.temporal.ai.chat.model.ChatModelActivity;
import io.temporal.ai.workshop.tools.ToolActivities;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInit;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Duration;

public class AgentWorkflowImpl implements AgentWorkflow {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Use the provided tools to help accomplish the user's goal.
            Always use tools when they would help answer the question accurately.
            When you have enough information to fully answer, provide your final response as plain text.
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

        ActivityChatModel activityChatModel = new ActivityChatModel(chatModelActivity);

        this.chatClient = TemporalChatClient.builder(activityChatModel)
                .defaultTools(toolActivity)
                .defaultSystem(SYSTEM_PROMPT)
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
