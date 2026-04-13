// ABOUTME: A deterministic tool that pauses the agent to ask the user a question.
// Runs inside the workflow, sets state variables, and blocks until the user responds via signal.

package io.temporal.ai.workshop;

import io.temporal.workflow.Workflow;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class AskUserTool {

    private boolean inputNeeded = false;
    private String question = "";
    private String userInput = "";

    @Tool(description = "Ask the user a question when you need more information to complete the task. Use this when the user's request is ambiguous or you need clarification.")
    public String askUser(@ToolParam(description = "The question to ask the user") String question) {
        this.question = question;
        this.inputNeeded = true;

        Workflow.await(() -> !inputNeeded);

        return userInput;
    }

    public void provideInput(String input) {
        this.userInput = input;
        this.inputNeeded = false;
    }

    public boolean isInputNeeded() {
        return inputNeeded;
    }

    public String getQuestion() {
        return question;
    }
}
