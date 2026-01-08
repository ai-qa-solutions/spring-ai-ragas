package ai.qa.solutions.allure.explanation;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Explanation for a single step in the metric evaluation process.
 * <p>
 * Contains both human-readable description and extracted data
 * that shows what happened at each step.
 */
@Value
@Builder
public class StepExplanation {

    /**
     * Step name (e.g., "ExtractStatements", "VerifyStatements").
     */
    String stepName;

    /**
     * Step number (1-based index).
     */
    int stepNumber;

    /**
     * Human-readable title for the step.
     * Example: "Разбиение ответа на утверждения"
     */
    String title;

    /**
     * Detailed explanation of what this step does (beginner-friendly).
     */
    String description;

    /**
     * The input data used in this step (e.g., the original response text).
     */
    String inputData;

    /**
     * The output/result of this step.
     */
    String outputSummary;

    /**
     * Detailed items extracted from this step (e.g., list of statements).
     */
    @Builder.Default
    List<ExplanationItem> items = List.of();

    /**
     * Per-model results for multi-model analysis.
     */
    @Builder.Default
    List<ModelStepResult> modelResults = List.of();

    /**
     * Additional metadata for this step.
     */
    @Builder.Default
    Map<String, Object> metadata = Map.of();

    /**
     * Whether there was disagreement between models in this step.
     */
    boolean hasModelDisagreement;

    /**
     * Agreement percentage (0-100) across models.
     */
    double agreementPercent;
}
