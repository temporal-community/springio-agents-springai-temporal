// ABOUTME: Serializable DTO for the LLM activity response.
// Contains either a text response or tool calls the agent needs to execute.

package io.temporal.ai.workshop.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LlmResponse(
        @JsonProperty("content") String content,
        @JsonProperty("toolCalls") List<ToolCallInfo> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
