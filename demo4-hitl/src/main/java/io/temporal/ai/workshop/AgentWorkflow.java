// ABOUTME: Temporal workflow interface for the agentic loop with human-in-the-loop.
// Adds signal for user input and query for checking if the agent needs input.

package io.temporal.ai.workshop;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface AgentWorkflow {

    @WorkflowMethod
    String run(String goal);

    @SignalMethod
    void provideUserInput(String input);

    @QueryMethod
    boolean isInputNeeded();

    @QueryMethod
    String getPendingQuestion();
}
