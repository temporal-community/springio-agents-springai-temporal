// ABOUTME: Collects tool objects and provides schemas (for the LLM) and handlers (for execution).
// Bridges Spring AI's @Tool annotation system with Temporal's dynamic activity dispatch.

package io.temporal.ai.workshop;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    private final List<ToolCallback> allCallbacks = new ArrayList<>();
    private final Map<String, ToolCallback> handlersByName = new HashMap<>();

    public ToolRegistry(Object... toolObjects) {
        for (Object toolObject : toolObjects) {
            for (ToolCallback callback : ToolCallbacks.from(toolObject)) {
                allCallbacks.add(callback);
                handlersByName.put(callback.getToolDefinition().name(), callback);
            }
        }
    }

    public List<ToolCallback> getSchemas() {
        return allCallbacks;
    }

    public ToolCallback getHandler(String toolName) {
        ToolCallback handler = handlersByName.get(toolName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        return handler;
    }
}
