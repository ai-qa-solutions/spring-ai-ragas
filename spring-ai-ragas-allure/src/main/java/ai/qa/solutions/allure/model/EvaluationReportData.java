package ai.qa.solutions.allure.model;

import ai.qa.solutions.allure.explanation.RubricsScoreExplanation;
import ai.qa.solutions.allure.explanation.ScoreExplanation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Complete data container for generating an Allure evaluation report.
 * <p>
 * Contains all information needed to render the HTML and Markdown reports:
 * <ul>
 *   <li>Metric metadata and sample data</li>
 *   <li>Execution results and timing</li>
 *   <li>Methodology documentation</li>
 *   <li>Chart visualization data</li>
 * </ul>
 */
@Value
@Builder
public class EvaluationReportData {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    // --- Metric Identification ---

    /**
     * Name of the metric being evaluated.
     */
    String metricName;

    /**
     * Brief description of the metric.
     */
    String metricDescription;

    // --- Sample Data ---

    /**
     * User input/question from the sample.
     */
    String userInput;

    /**
     * AI response being evaluated.
     */
    String response;

    /**
     * Reference/ground truth answer (may be null).
     */
    String reference;

    /**
     * Retrieved contexts for RAG evaluation (may be empty).
     */
    @Builder.Default
    List<String> retrievedContexts = List.of();

    // --- Configuration ---

    /**
     * Metric configuration object.
     */
    Object config;

    /**
     * Pretty-printed JSON of the configuration.
     */
    String configJson;

    // --- Execution Metadata ---

    /**
     * Evaluation start time.
     */
    Instant startTime;

    /**
     * Evaluation end time.
     */
    Instant endTime;

    /**
     * Total evaluation duration.
     */
    Duration totalDuration;

    // --- Models ---

    /**
     * LLM model IDs used in evaluation.
     */
    @Builder.Default
    List<String> modelIds = List.of();

    /**
     * Embedding model IDs used in evaluation.
     */
    @Builder.Default
    List<String> embeddingModelIds = List.of();

    /**
     * Model IDs that were excluded during evaluation.
     */
    @Builder.Default
    List<String> excludedModels = List.of();

    // --- Results ---

    /**
     * Final aggregated score (0.0 to 1.0, may be null if all models failed).
     */
    Double aggregatedScore;

    /**
     * Individual scores per model.
     */
    @Builder.Default
    Map<String, Double> modelScores = Map.of();

    // --- Execution Details ---

    /**
     * Step-by-step execution data.
     */
    @Builder.Default
    List<StepExecutionData> steps = List.of();

    /**
     * Model exclusion events.
     */
    @Builder.Default
    List<ModelExclusionData> exclusions = List.of();

    // --- Methodology ---

    /**
     * Methodology documentation as HTML.
     */
    String methodologyHtml;

    /**
     * Methodology documentation as Markdown.
     */
    String methodologyMarkdown;

    // --- Charts ---

    /**
     * Pre-computed chart visualization data.
     */
    ChartData chartData;

    // --- Score Explanation ---

    /**
     * Explanation of why the metric got a specific score.
     * <p>
     * Contains step-by-step breakdown and metric-specific data
     * to help users understand the score.
     */
    ScoreExplanation scoreExplanation;

    // --- Settings ---

    /**
     * Report language ("en" or "ru").
     */
    @Builder.Default
    String language = "en";

    // --- Utility Methods ---

    /**
     * Gets total duration in milliseconds.
     *
     * @return duration in ms, or 0 if null
     */
    public long getTotalDurationMs() {
        return totalDuration != null ? totalDuration.toMillis() : 0;
    }

    /**
     * Checks if any models were excluded.
     *
     * @return true if at least one model was excluded
     */
    public boolean hasExclusions() {
        return !exclusions.isEmpty();
    }

    /**
     * Checks if embedding models were used.
     *
     * @return true if embedding model IDs are present
     */
    public boolean hasEmbeddingModels() {
        return !embeddingModelIds.isEmpty();
    }

    /**
     * Checks if retrieved contexts are present.
     *
     * @return true if contexts list is not empty
     */
    public boolean hasContexts() {
        return !retrievedContexts.isEmpty();
    }

    /**
     * Checks if reference answer is present.
     *
     * @return true if reference is not null or empty
     */
    public boolean hasReference() {
        return reference != null && !reference.isBlank();
    }

    /**
     * Checks if score explanation is available.
     *
     * @return true if score explanation is present
     */
    public boolean hasScoreExplanation() {
        return scoreExplanation != null;
    }

    /**
     * Gets the score as a formatted percentage string.
     * <p>
     * For rubrics-based metrics, displays level and normalized percentage.
     *
     * @return formatted score (e.g., "85.00%" or "2 / 5 → 25.00%") or "N/A" if null
     */
    public String getFormattedScore() {
        if (aggregatedScore == null) {
            return "N/A";
        }

        // For rubrics metrics, show level and normalized score
        if (isRubricsMetric()) {
            final int minLevel = getRubricsMinLevel();
            final int maxLevel = getRubricsMaxLevel();
            final double normalizedScore =
                    (maxLevel > minLevel) ? (aggregatedScore - minLevel) / (maxLevel - minLevel) : 0.0;
            return String.format(Locale.US, "%.1f / %d → %.2f%%", aggregatedScore, maxLevel, normalizedScore * 100);
        }

        return String.format(Locale.US, "%.2f%%", aggregatedScore * 100);
    }

    /**
     * Gets the score CSS class for styling based on value.
     *
     * @return CSS class name ("excellent", "good", "moderate", "poor")
     */
    public String getScoreClass() {
        if (aggregatedScore == null) {
            return "unknown";
        }

        // Rubrics metrics: use dynamic color based on position in scale
        if (isRubricsMetric()) {
            final int minLevel = getRubricsMinLevel();
            final int maxLevel = getRubricsMaxLevel();
            final double position = (maxLevel > minLevel) ? (aggregatedScore - minLevel) / (maxLevel - minLevel) : 0.0;
            if (position <= 0.33) {
                return "poor";
            }
            if (position <= 0.66) {
                return "moderate";
            }
            return "good";
        }

        // Inverted metrics: lower score is better
        if (isInvertedMetric()) {
            if (aggregatedScore <= 0.1) {
                return "excellent";
            }
            if (aggregatedScore <= 0.3) {
                return "good";
            }
            if (aggregatedScore <= 0.5) {
                return "moderate";
            }
            return "poor";
        }

        // Standard metrics: higher score is better
        if (aggregatedScore >= 0.8) {
            return "excellent";
        }
        if (aggregatedScore >= 0.6) {
            return "good";
        }
        if (aggregatedScore >= 0.4) {
            return "moderate";
        }
        return "poor";
    }

    /**
     * Gets the minimum rubric level from explanation, or default 1.
     *
     * @return minimum level in rubric scale
     */
    public int getRubricsMinLevel() {
        if (scoreExplanation instanceof RubricsScoreExplanation rubrics) {
            return rubrics.getMinLevel();
        }
        return 1;
    }

    /**
     * Gets the maximum rubric level from explanation, or default 5.
     *
     * @return maximum level in rubric scale
     */
    public int getRubricsMaxLevel() {
        if (scoreExplanation instanceof RubricsScoreExplanation rubrics) {
            return rubrics.getMaxLevel();
        }
        return 5;
    }

    /**
     * Checks if this metric uses inverted scale (lower is better).
     * Used by templates to determine color coding.
     *
     * @return true for metrics where 0 is best (e.g., NoiseSensitivity)
     */
    public boolean isInvertedMetric() {
        if (metricName == null) {
            return false;
        }
        final String normalized = metricName.toLowerCase();
        return normalized.contains("noise") || normalized.contains("sensitivity");
    }

    /**
     * Checks if this is a rubrics-based metric.
     * <p>
     * Rubrics metrics use raw levels (1-5, 1-9, etc.) instead of normalized scores (0.0-1.0).
     *
     * @return true for RubricsScoreMetric
     */
    public boolean isRubricsMetric() {
        if (metricName == null) {
            return false;
        }
        return metricName.toLowerCase().contains("rubrics");
    }

    /**
     * Gets the number of successful models.
     *
     * @return count of models with scores
     */
    public int getSuccessfulModelCount() {
        return modelScores.size();
    }

    /**
     * Converts configuration object to JSON string.
     *
     * @param config the configuration object
     * @return pretty-printed JSON or toString fallback
     */
    public static String configToJson(final Object config) {
        if (config == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(config);
        } catch (final JsonProcessingException e) {
            return config.toString();
        }
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
