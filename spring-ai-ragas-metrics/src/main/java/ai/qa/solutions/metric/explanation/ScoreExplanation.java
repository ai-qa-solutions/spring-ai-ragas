package ai.qa.solutions.metric.explanation;

import java.util.List;

/**
 * Base interface for metric-specific score explanations.
 * <p>
 * Each metric type provides its own implementation with data
 * extracted from the intermediate results (resultJson).
 * The explanation helps users understand WHY the metric got a specific score.
 */
public interface ScoreExplanation {

    /**
     * Gets the metric type identifier used for template selection.
     * <p>
     * This value is used in FreeMarker templates to switch between
     * different visualization layouts for each metric type.
     *
     * @return metric type identifier (e.g., "faithfulness", "context-precision")
     */
    String getMetricType();

    /**
     * Gets a simple, beginner-friendly description of what the metric measures.
     * <p>
     * This description should be understandable by someone without
     * deep knowledge of the evaluation framework.
     *
     * @return simple description of the metric
     */
    String getSimpleDescription();

    /**
     * Gets the step-by-step breakdown of how the score was calculated.
     * <p>
     * Each step contains the step name, explanation, and extracted data.
     *
     * @return list of step explanations
     */
    List<StepExplanation> getSteps();

    /**
     * Gets the interpretation of the final score.
     * <p>
     * For metrics with fixed interpretation (e.g., NoiseSensitivity where 0 = good),
     * this provides the correct context. For relative metrics, it shows the scale.
     *
     * @return score interpretation
     */
    ScoreInterpretation getInterpretation();

    /**
     * Checks if this metric has a fixed interpretation (0=good/bad vs relative scale).
     *
     * @return true if the metric has fixed good/bad interpretation
     */
    default boolean hasFixedInterpretation() {
        return false;
    }
}
