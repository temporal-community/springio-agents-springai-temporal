// ABOUTME: Spring configuration that sets up Temporal client, worker factory, and worker.
// Registers the agent workflow and LLM activity with the worker.

package io.temporal.ai.workshop;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    public static final String TASK_QUEUE = "agent-task-queue";

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newLocalServiceStubs();
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        return WorkflowClient.newInstance(serviceStubs);
    }

    @Bean(initMethod = "start")
    public WorkerFactory workerFactory(WorkflowClient workflowClient, ChatModel chatModel) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(AgentWorkflowImpl.class);
        worker.registerActivitiesImplementations(new LlmActivitiesImpl(chatModel), new ToolActivitiesImpl());
        return factory;
    }
}
