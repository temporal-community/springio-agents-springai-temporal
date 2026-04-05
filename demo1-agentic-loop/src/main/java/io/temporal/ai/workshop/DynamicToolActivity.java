// ABOUTME: Generic dynamic Temporal activity that dispatches tool calls via the ToolRegistry.
// Completely decoupled from specific tools - just looks up and executes handlers by name.

package io.temporal.ai.workshop;

import io.temporal.activity.Activity;
import io.temporal.activity.DynamicActivity;
import io.temporal.common.converter.EncodedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicToolActivity implements DynamicActivity {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolActivity.class);

    private final ToolRegistry registry;

    public DynamicToolActivity(ToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object execute(EncodedValues args) {
        String toolName = Activity.getExecutionContext().getInfo().getActivityType();
        String arguments = args.get(0, String.class);

        log.info("Executing tool: {}({})", toolName, arguments);
        String result = registry.getHandler(toolName).call(arguments);
        log.info("Tool result: {}", result);

        return result;
    }
}
