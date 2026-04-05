// ABOUTME: Temporal activity interface for LLM invocations.
// Wraps the non-deterministic ChatModel call as a Temporal activity.

package io.temporal.ai.workshop;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.ai.workshop.model.LlmMessage;
import io.temporal.ai.workshop.model.LlmResponse;

import java.util.List;

@ActivityInterface
public interface LlmActivities {

    @ActivityMethod
    LlmResponse callLlm(List<LlmMessage> messages);
}
