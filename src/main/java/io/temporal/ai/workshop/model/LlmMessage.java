// ABOUTME: Serializable DTO representing a single message in the LLM conversation.
// Supports system, user, assistant, and tool response message roles.

package io.temporal.ai.workshop.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LlmMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content,
        @JsonProperty("toolCalls") List<ToolCallInfo> toolCalls,
        @JsonProperty("toolCallId") String toolCallId,
        @JsonProperty("toolName") String toolName) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null, null);
    }

    public static LlmMessage assistant(String content, List<ToolCallInfo> toolCalls) {
        return new LlmMessage("assistant", content, toolCalls, null, null);
    }

    public static LlmMessage toolResponse(String toolCallId, String toolName, String content) {
        return new LlmMessage("tool", content, null, toolCallId, toolName);
    }
}
