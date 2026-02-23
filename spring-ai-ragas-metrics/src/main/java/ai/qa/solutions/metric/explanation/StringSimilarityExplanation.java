package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for StringSimilarityMetric.
 * <p>
 * String similarity measures edit distance between response and reference
 * using various algorithms (Levenshtein, Jaro, Jaro-Winkler, Hamming).
 * This is a non-LLM metric.
 */
@Getter
public class StringSimilarityExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "string-similarity";

    private final String response;
    private final String reference;
    private final String distanceMeasure;
    private final boolean caseSensitive;

    @Builder
    public StringSimilarityExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final String distanceMeasure,
            final boolean caseSensitive) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.distanceMeasure = distanceMeasure != null ? distanceMeasure : "JARO_WINKLER";
        this.caseSensitive = caseSensitive;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("stringSimilarity.description");
    }

    private void buildSteps() {
        // Step 1: Show response and reference
        steps.add(StepExplanation.builder()
                .stepName("InputTexts")
                .stepNumber(1)
                .title(messages.get("stringSimilarity.step1.title"))
                .description(messages.get("stringSimilarity.step1.desc"))
                .inputData(String.format(
                        "%s: %s\n%s: %s",
                        messages.get("common.response"),
                        truncate(response, 200),
                        messages.get("common.reference"),
                        truncate(reference, 200)))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Configuration
        final String algorithmName = formatAlgorithmName(distanceMeasure);
        final String caseSensitiveStr = caseSensitive
                ? messages.get("stringSimilarity.caseSensitive.yes")
                : messages.get("stringSimilarity.caseSensitive.no");
        steps.add(StepExplanation.builder()
                .stepName("Configuration")
                .stepNumber(2)
                .title(messages.get("stringSimilarity.step2.title"))
                .description(messages.get("stringSimilarity.step2.desc"))
                .outputSummary(messages.get("stringSimilarity.step2.output", algorithmName, caseSensitiveStr))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute distance/similarity
        steps.add(StepExplanation.builder()
                .stepName("ComputeSimilarity")
                .stepNumber(3)
                .title(messages.get("stringSimilarity.step3.title", algorithmName))
                .description(getAlgorithmDescription())
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private String formatAlgorithmName(final String measure) {
        if (measure == null) {
            return "Jaro-Winkler";
        }
        return switch (measure.toUpperCase()) {
            case "LEVENSHTEIN" -> "Levenshtein";
            case "HAMMING" -> "Hamming";
            case "JARO" -> "Jaro";
            default -> "Jaro-Winkler";
        };
    }

    private String getAlgorithmDescription() {
        if (distanceMeasure == null) {
            return messages.get("stringSimilarity.algorithm.jaroWinkler");
        }
        return switch (distanceMeasure.toUpperCase()) {
            case "LEVENSHTEIN" -> messages.get("stringSimilarity.algorithm.levenshtein");
            case "HAMMING" -> messages.get("stringSimilarity.algorithm.hamming");
            case "JARO" -> messages.get("stringSimilarity.algorithm.jaro");
            default -> messages.get("stringSimilarity.algorithm.jaroWinkler");
        };
    }

    private void buildInterpretation() {
        final String formula = getFormula();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createSimilarityScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getFormula() {
        if (distanceMeasure == null) {
            return messages.get("stringSimilarity.formula.jaroWinkler");
        }
        return switch (distanceMeasure.toUpperCase()) {
            case "LEVENSHTEIN" -> messages.get("stringSimilarity.formula.levenshtein");
            case "HAMMING" -> messages.get("stringSimilarity.formula.hamming");
            case "JARO" -> messages.get("stringSimilarity.formula.jaro");
            default -> messages.get("stringSimilarity.formula.jaroWinkler");
        };
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("stringSimilarity.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("stringSimilarity.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("stringSimilarity.meaning.moderate");
        }
        return messages.get("stringSimilarity.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createSimilarityScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("stringSimilarity.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("stringSimilarity.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("stringSimilarity.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("stringSimilarity.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    private String truncate(final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
