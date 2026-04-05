// ABOUTME: Temporal workflow interface for the agentic loop.
// Accepts a goal string and returns the agent's final response.

package io.temporal.ai.workshop;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AgentWorkflow {

    @WorkflowMethod
    String run(String goal);
}
