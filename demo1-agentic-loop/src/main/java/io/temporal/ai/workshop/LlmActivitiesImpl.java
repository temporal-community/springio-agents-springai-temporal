// ABOUTME: Temporal activity implementation that calls the LLM via Spring AI's ChatModel.
// Converts between serializable DTOs and Spring AI message types.

package io.temporal.ai.workshop;

import io.temporal.ai.workshop.model.LlmMessage;
import io.temporal.ai.workshop.model.LlmResponse;
import io.temporal.ai.workshop.model.ToolCallInfo;
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
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LlmActivitiesImpl implements LlmActivities {

    private static final Logger log = LoggerFactory.getLogger(LlmActivitiesImpl.class);

    private final ChatModel chatModel;
    private final List<ToolCallback> toolCallbackList;

    public LlmActivitiesImpl(ChatModel chatModel, ToolRegistry toolRegistry) {
        this.chatModel = chatModel;
        this.toolCallbackList = toolRegistry.getSchemas();
    }

    @Override
    public LlmResponse callLlm(List<LlmMessage> messages) {
        List<Message> springMessages = toSpringMessages(messages);

        var options = AnthropicChatOptions.builder()
                .toolCallbacks(toolCallbackList)
                .internalToolExecutionEnabled(false)  // Let Temporal handle tool execution
                .build();

        ChatResponse response = chatModel.call(new Prompt(springMessages, options));
        AssistantMessage assistant = response.getResult().getOutput();

        List<ToolCallInfo> toolCalls = null;
        if (assistant.hasToolCalls()) {
            toolCalls = assistant.getToolCalls().stream()
                    .map(tc -> new ToolCallInfo(tc.id(), tc.name(), tc.arguments()))
                    .toList();
        }

        return new LlmResponse(assistant.getText(), toolCalls);
    }

    private List<Message> toSpringMessages(List<LlmMessage> messages) {
        List<Message> result = new ArrayList<>();
        for (LlmMessage msg : messages) {
            switch (msg.role()) {
                case "system" -> result.add(new SystemMessage(msg.content()));
                case "user" -> result.add(new UserMessage(msg.content()));
                case "assistant" -> result.add(toAssistantMessage(msg));
                case "tool" -> result.add(new ToolResponseMessage(List.of(
                        new ToolResponseMessage.ToolResponse(
                                msg.toolCallId(), msg.toolName(), msg.content()))));
            }
        }
        return result;
    }

    private AssistantMessage toAssistantMessage(LlmMessage msg) {
        List<AssistantMessage.ToolCall> toolCalls = List.of();
        if (msg.toolCalls() != null) {
            toolCalls = msg.toolCalls().stream()
                    .map(tc -> new AssistantMessage.ToolCall(tc.id(), "function", tc.name(), tc.arguments()))
                    .toList();
        }
        return new AssistantMessage(msg.content(), Map.of(), toolCalls);
    }
}
