package ai.qa.solutions.metrics.agent;

import ai.qa.solutions.execution.ModelResult;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.StepResults;
import ai.qa.solutions.execution.listener.dto.StepType;
import ai.qa.solutions.metric.AbstractMultiTurnMetric;
import ai.qa.solutions.metric.metadata.ToolCallAccuracyMetadata;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool Call Accuracy Metric - Evaluates accuracy of agent's tool calls.
 * <p>
 * This metric compares actual tool calls made by an agent against expected reference
 * tool calls. It supports two modes:
 * <ul>
 *   <li>{@code STRICT} - Exact matching of tool names and arguments</li>
 *   <li>{@code FLEXIBLE} - Allows partial argument matching based on threshold</li>
 * </ul>
 * <p>
 * The score is computed as:
 * - Precision: correct calls / total actual calls
 * - Recall: correct calls / total reference calls
 * - F1: harmonic mean of precision and recall
 * <p>
 * Required sample fields:
 * <ul>
 *   <li>{@code toolCalls} - Actual tool calls made by the agent</li>
 *   <li>{@code referenceToolCalls} - Expected/correct tool calls</li>
 * </ul>
 */
@Slf4j
public class ToolCallAccuracyMetric extends AbstractMultiTurnMetric<ToolCallAccuracyMetric.ToolCallAccuracyConfig> {

    @Builder(toBuilder = true)
    protected ToolCallAccuracyMetric(final MultiModelExecutor executor) {
        super(executor);
    }

    @Override
    public Double multiTurnScore(final ToolCallAccuracyConfig config, final Sample sample) {
        return multiTurnScoreAsync(config, sample).join();
    }

    @Override
    public CompletableFuture<Double> multiTurnScoreAsync(final ToolCallAccuracyConfig config, final Sample sample) {
        final Instant startTime = Instant.now();
        final List<String> modelIds =
                config.models != null && !config.models.isEmpty() ? config.models : executor.getModelIds();

        // Extract tool calls from typed messages if not provided directly
        List<Sample.ToolCall> extractedToolCalls = sample.getToolCalls();
        if ((extractedToolCalls == null || extractedToolCalls.isEmpty()) && sample.getUserInputMessages() != null) {
            extractedToolCalls = extractToolCalls(sample.getUserInputMessages()).stream()
                    .map(tc -> new Sample.ToolCall(tc.name(), tc.arguments()))
                    .toList();
        }
        final List<Sample.ToolCall> actualCalls = extractedToolCalls;

        // Validate input
        if (sample.getReferenceToolCalls() == null
                || sample.getReferenceToolCalls().isEmpty()) {
            log.warn("No reference tool calls provided for Tool Call Accuracy evaluation");
            return CompletableFuture.completedFuture(null);
        }

        // If no actual tool calls but reference calls exist, agent failed to make required calls
        if (actualCalls == null || actualCalls.isEmpty()) {
            log.info(
                    "No tool calls made by agent but {} reference calls expected - returning 0.0",
                    sample.getReferenceToolCalls().size());
            return CompletableFuture.completedFuture(0.0);
        }

        final EvaluationNotifier notifier = createEvaluationNotifier();
        final Mode mode = config.mode != null ? config.mode : Mode.STRICT;
        final double threshold = config.argumentMatchThreshold != null ? config.argumentMatchThreshold : 0.8;

        notifier.beforeMetricEvaluation(MetricEvaluationContext.builder()
                .metricName(getName())
                .sample(sample)
                .config(config)
                .modelIds(modelIds)
                .totalSteps(3)
                .build());

        return executor.runAsync(() -> {
            final List<StepResults> accumulatedSteps = new ArrayList<>();
            final List<Sample.ToolCall> referenceCalls = sample.getReferenceToolCalls();

            // Step 1: Align tool calls
            final List<ToolCallMatch> matches = alignToolCalls(actualCalls, referenceCalls, mode, threshold);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("AlignToolCalls")
                    .stepIndex(0)
                    .totalSteps(3)
                    .stepType(StepType.COMPUTE)
                    .results(List.of())
                    .build());

            // Step 2: Compute precision and recall
            final int truePositives =
                    (int) matches.stream().filter(ToolCallMatch::isMatched).count();
            final int falsePositives = actualCalls.size() - truePositives;
            final int falseNegatives = referenceCalls.size() - truePositives;

            final double precision = actualCalls.isEmpty() ? 0.0 : (double) truePositives / actualCalls.size();
            final double recall = referenceCalls.isEmpty() ? 0.0 : (double) truePositives / referenceCalls.size();

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputePrecisionRecall")
                    .stepIndex(1)
                    .totalSteps(3)
                    .stepType(StepType.COMPUTE)
                    .results(List.of(ModelResult.success(
                            modelIds.isEmpty() ? "default" : modelIds.get(0),
                            Map.of("precision", precision, "recall", recall),
                            Duration.ZERO,
                            "compute")))
                    .build());

            // Step 3: Compute F1 score
            final double f1Score = (precision + recall) == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

            accumulatedSteps.add(StepResults.builder()
                    .stepName("ComputeScore")
                    .stepIndex(2)
                    .totalSteps(3)
                    .stepType(StepType.COMPUTE)
                    .results(List.of(ModelResult.success(
                            modelIds.isEmpty() ? "default" : modelIds.get(0), f1Score, Duration.ZERO, "compute")))
                    .build());

            final Duration duration = Duration.between(startTime, Instant.now());
            final Map<String, Double> modelScores = new HashMap<>();
            modelScores.put(modelIds.isEmpty() ? "default" : modelIds.get(0), f1Score);

            // Build match summaries for metadata
            final List<ToolCallAccuracyMetadata.ToolCallMatchSummary> matchSummaries = matches.stream()
                    .map(m -> new ToolCallAccuracyMetadata.ToolCallMatchSummary(
                            m.actualCall != null ? m.actualCall.name() : null,
                            m.referenceCall != null ? m.referenceCall.name() : null,
                            m.matched,
                            m.matchScore))
                    .toList();

            notifier.afterMetricEvaluation(MetricEvaluationResult.builder()
                    .metricName(getName())
                    .sample(sample)
                    .config(config)
                    .modelIds(modelIds)
                    .aggregatedScore(f1Score)
                    .modelScores(modelScores)
                    .excludedModels(List.of())
                    .totalDuration(duration)
                    .steps(accumulatedSteps)
                    .exclusions(List.of())
                    .metadata(new ToolCallAccuracyMetadata(
                            mode.name(),
                            threshold,
                            actualCalls.size(),
                            referenceCalls.size(),
                            truePositives,
                            falsePositives,
                            falseNegatives,
                            precision,
                            recall,
                            matchSummaries))
                    .build());

            return f1Score;
        });
    }

    private List<ToolCallMatch> alignToolCalls(
            final List<Sample.ToolCall> actualCalls,
            final List<Sample.ToolCall> referenceCalls,
            final Mode mode,
            final double threshold) {

        final List<ToolCallMatch> matches = new ArrayList<>();
        final List<Sample.ToolCall> unmatchedReferences = new ArrayList<>(referenceCalls);

        for (final Sample.ToolCall actual : actualCalls) {
            Sample.ToolCall bestMatch = null;
            double bestScore = 0.0;

            for (final Sample.ToolCall reference : unmatchedReferences) {
                final double matchScore = computeMatchScore(actual, reference, mode, threshold);
                if (matchScore > bestScore) {
                    bestScore = matchScore;
                    bestMatch = reference;
                }
            }

            final boolean isMatched = mode == Mode.STRICT ? bestScore >= 1.0 : bestScore >= threshold;

            if (isMatched && bestMatch != null) {
                unmatchedReferences.remove(bestMatch);
                matches.add(ToolCallMatch.builder()
                        .actualCall(actual)
                        .referenceCall(bestMatch)
                        .matched(true)
                        .matchScore(bestScore)
                        .build());
            } else {
                matches.add(ToolCallMatch.builder()
                        .actualCall(actual)
                        .referenceCall(bestMatch)
                        .matched(false)
                        .matchScore(bestScore)
                        .build());
            }
        }

        return matches;
    }

    private double computeMatchScore(
            final Sample.ToolCall actual, final Sample.ToolCall reference, final Mode mode, final double threshold) {

        // Tool names must match
        if (!Objects.equals(actual.name(), reference.name())) {
            return 0.0;
        }

        final Map<String, Object> actualArgs = actual.arguments() != null ? actual.arguments() : Map.of();
        final Map<String, Object> refArgs = reference.arguments() != null ? reference.arguments() : Map.of();

        if (mode == Mode.STRICT) {
            // Exact match required
            return actualArgs.equals(refArgs) ? 1.0 : 0.0;
        }

        // Flexible mode: compute argument overlap
        if (refArgs.isEmpty() && actualArgs.isEmpty()) {
            return 1.0;
        }

        if (refArgs.isEmpty()) {
            return 0.5; // No reference args but actual has args
        }

        int matchedArgs = 0;
        for (final Map.Entry<String, Object> entry : refArgs.entrySet()) {
            final Object actualValue = actualArgs.get(entry.getKey());
            if (actualValue != null && Objects.equals(actualValue, entry.getValue())) {
                matchedArgs++;
            }
        }

        return (double) matchedArgs / refArgs.size();
    }

    /**
     * Represents a match between actual and reference tool calls.
     */
    @Data
    @Builder
    public static class ToolCallMatch {
        private final Sample.ToolCall actualCall;
        private final Sample.ToolCall referenceCall;
        private final boolean matched;
        private final double matchScore;
    }

    /**
     * Matching mode for tool call comparison.
     */
    public enum Mode {
        /** Exact matching of tool names and arguments. */
        STRICT,
        /** Flexible matching allowing partial argument matches based on threshold. */
        FLEXIBLE
    }

    @Data
    @Builder
    public static class ToolCallAccuracyConfig implements MetricConfiguration {
        @Singular
        private List<String> models;

        /** Matching mode: STRICT or FLEXIBLE. */
        @Builder.Default
        private Mode mode = Mode.STRICT;

        /** Threshold for argument matching in FLEXIBLE mode (0.0-1.0). */
        @Builder.Default
        private Double argumentMatchThreshold = 0.8;

        @Builder.Default
        private String language = "en";
    }
}
