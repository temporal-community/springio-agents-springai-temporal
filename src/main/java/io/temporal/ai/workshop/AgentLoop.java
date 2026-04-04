// ABOUTME: Implements the core agentic loop - calls the model, executes tool calls, and repeats.
// This is the heart of the agent, manually managing the conversation and tool dispatch.

package io.temporal.ai.workshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Component;

import io.temporal.ai.workshop.tools.AgentTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(AgentLoop.class);
    private static final int MAX_ITERATIONS = 10;

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Use the provided tools to help accomplish the user's goal.
            Always use tools when they would help answer the question accurately.
            When you have enough information to fully answer, provide your final response as plain text.
            """;

    private final ChatModel chatModel;
    private final List<ToolCallback> toolCallbackList;
    private final Map<String, ToolCallback> toolMap;

    public AgentLoop(ChatModel chatModel, AgentTools agentTools) {
        this.chatModel = chatModel;

        ToolCallback[] callbacks = ToolCallbacks.from(agentTools);
        this.toolCallbackList = List.of(callbacks);

        this.toolMap = new HashMap<>();
        for (ToolCallback cb : callbacks) {
            toolMap.put(cb.getToolDefinition().name(), cb);
        }

        log.info("Registered {} tools: {}", toolMap.size(), toolMap.keySet());
    }

    public String execute(String goal) {
        log.info("Starting agent with goal: {}", goal);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.add(new UserMessage(goal));

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            log.info("=== Iteration {} ===", iteration);

            var options = OpenAiChatOptions.builder()
                    .toolCallbacks(toolCallbackList)
                    .internalToolExecutionEnabled(false)
                    .build();

            ChatResponse response = chatModel.call(new Prompt(messages, options));
            AssistantMessage assistant = response.getResult().getOutput();
            messages.add(assistant);

            if (!assistant.hasToolCalls()) {
                log.info("Agent completed after {} iteration(s).", iteration);
                return assistant.getText();
            }

            for (var toolCall : assistant.getToolCalls()) {
                log.info("Calling tool: {}({})", toolCall.name(), toolCall.arguments());

                ToolCallback callback = toolMap.get(toolCall.name());
                if (callback == null) {
                    log.error("Unknown tool requested: {}", toolCall.name());
                    continue;
                }

                String result = callback.call(toolCall.arguments());
                log.info("Tool result: {}", result);

                messages.add(new ToolResponseMessage(List.of(
                        new ToolResponseMessage.ToolResponse(
                                toolCall.id(), toolCall.name(), result))));
            }
        }

        log.warn("Agent did not complete within {} iterations.", MAX_ITERATIONS);
        return "Agent did not complete within " + MAX_ITERATIONS + " iterations.";
    }
}
