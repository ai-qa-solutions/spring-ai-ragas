package ai.qa.solutions.execution.listener.dto;

import ai.qa.solutions.execution.listener.MetricExecutionListener;
import lombok.Builder;
import lombok.Value;

/**
 * Context for a step that is about to execute.
 * <p>
 * This class is passed to {@link MetricExecutionListener#beforeStep(StepContext)}
 * before each step starts executing on all models.
 * <p>
 * Contains step metadata but no execution results (those come in {@link StepResults}).
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public void beforeStep(StepContext context) {
 *     log.info("Starting step {}/{}: {}",
 *         context.getStepIndex() + 1,
 *         context.getTotalSteps(),
 *         context.getStepName());
 * }
 * }</pre>
 */
@Value
@Builder
public class StepContext {

    /**
     * The name of the step about to execute.
     * <p>
     * Examples: "GenerateStatements", "EvaluateFaithfulness", "ComputeScore"
     */
    String stepName;

    /**
     * The zero-based index of this step in the evaluation chain.
     */
    int stepIndex;

    /**
     * Total number of steps in the evaluation.
     */
    int totalSteps;
}
