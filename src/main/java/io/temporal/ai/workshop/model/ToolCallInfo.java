// ABOUTME: Serializable DTO representing a tool call requested by the LLM.
// Used to pass tool call data between the Temporal workflow and activity.

package io.temporal.ai.workshop.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ToolCallInfo(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("arguments") String arguments) {
}
