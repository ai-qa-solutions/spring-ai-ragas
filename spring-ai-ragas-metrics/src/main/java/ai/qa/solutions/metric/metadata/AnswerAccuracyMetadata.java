package ai.qa.solutions.metric.metadata;

import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import java.util.Map;

/**
 * Typed metadata for Answer Accuracy (NVIDIA) metric evaluation results.
 * <p>
 * Captures per-model accuracy judgments and optional confirmation results.
 *
 * @param initialJudgments    per-model initial accuracy judgments (score 0-2 and reasoning)
 * @param confirmedJudgments  per-model confirmed judgments (null if dual-judge not used)
 * @param usedDualJudge       whether dual-judge mode was enabled
 */
public record AnswerAccuracyMetadata(
        Map<String, JudgmentSummary> initialJudgments,
        Map<String, JudgmentSummary> confirmedJudgments,
        boolean usedDualJudge)
        implements MetricMetadata {

    /**
     * Summary of an accuracy judgment.
     *
     * @param rawScore  the raw score (0-2) from the LLM
     * @param reasoning the explanation for the assessment
     */
    public record JudgmentSummary(int rawScore, String reasoning) {}
}
