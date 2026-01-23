package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for TopicAdherenceMetric.
 * <p>
 * Topic Adherence evaluates whether conversation topics adhere to expected reference topics.
 * It supports three modes:
 * <ul>
 *   <li>F1 - Harmonic mean of precision and recall (default)</li>
 *   <li>PRECISION - Focus on avoiding off-topic discussions</li>
 *   <li>RECALL - Focus on covering all reference topics</li>
 * </ul>
 */
@Getter
public class TopicAdherenceExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "topic-adherence";

    private final String mode;
    private final double precision;
    private final double recall;
    private final List<String> extractedTopics;
    private final List<String> referenceTopics;
    private final List<TopicClassificationItem> classifications;

    @Builder
    public TopicAdherenceExplanation(
            final Double score,
            final String language,
            final String mode,
            final double precision,
            final double recall,
            final List<String> extractedTopics,
            final List<String> referenceTopics,
            final List<TopicClassificationItem> classifications) {
        super(score, language);
        this.mode = mode != null ? mode : "F1";
        this.precision = precision;
        this.recall = recall;
        this.extractedTopics = extractedTopics != null ? extractedTopics : List.of();
        this.referenceTopics = referenceTopics != null ? referenceTopics : List.of();
        this.classifications = classifications != null ? classifications : List.of();
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("topicAdherence.description");
    }

    private void buildSteps() {
        // Step 1: Extract topics
        steps.add(StepExplanation.builder()
                .stepName("ExtractTopics")
                .stepNumber(1)
                .title(messages.get("topicAdherence.step.extractTopics.title"))
                .description(messages.get("topicAdherence.step.extractTopics.desc"))
                .inputData(messages.get("topicAdherence.conversationLabel"))
                .outputSummary(String.format(
                        "%s: %d", messages.get("topicAdherence.extractedTopicsCount"), extractedTopics.size()))
                .items(buildExtractedTopicItems())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Classify topics
        steps.add(StepExplanation.builder()
                .stepName("ClassifyTopics")
                .stepNumber(2)
                .title(messages.get("topicAdherence.step.classifyTopics.title"))
                .description(messages.get("topicAdherence.step.classifyTopics.desc"))
                .inputData(String.format(
                        "%s: %s",
                        messages.get("topicAdherence.referenceTopicsLabel"), String.join(", ", referenceTopics)))
                .outputSummary(buildClassificationSummary())
                .items(buildClassificationItems())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Compute score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("topicAdherence.step.computeScore.title"))
                .description(messages.get("topicAdherence.step.computeScore.desc"))
                .inputData(String.format(
                        "%s: %s | %s=%.2f, %s=%.2f",
                        messages.get("topicAdherence.modeLabel"),
                        getLocalizedMode(),
                        messages.get("topicAdherence.precisionLabel"),
                        precision,
                        messages.get("topicAdherence.recallLabel"),
                        recall))
                .outputSummary(String.format("%s = %.2f", getScoreFormulaLabel(), score != null ? score : 0.0))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private String getLocalizedMode() {
        return switch (mode.toUpperCase()) {
            case "PRECISION" -> messages.get("topicAdherence.mode.precision");
            case "RECALL" -> messages.get("topicAdherence.mode.recall");
            default -> messages.get("topicAdherence.mode.f1");
        };
    }

    private String getScoreFormulaLabel() {
        return switch (mode.toUpperCase()) {
            case "PRECISION" -> messages.get("topicAdherence.precisionLabel");
            case "RECALL" -> messages.get("topicAdherence.recallLabel");
            default -> "F1";
        };
    }

    private List<ExplanationItem> buildExtractedTopicItems() {
        return extractedTopics.stream()
                .map(topic -> ExplanationItem.builder()
                        .content(topic)
                        .passed(true)
                        .verdict(messages.get("topicAdherence.extractedLabel"))
                        .build())
                .toList();
    }

    private String buildClassificationSummary() {
        final long onTopic = classifications.stream()
                .filter(TopicClassificationItem::isOnTopic)
                .count();
        return String.format("%s: %d/%d", messages.get("topicAdherence.onTopicLabel"), onTopic, classifications.size());
    }

    private List<ExplanationItem> buildClassificationItems() {
        return classifications.stream()
                .map(c -> ExplanationItem.builder()
                        .content(formatClassification(c))
                        .passed(c.isOnTopic())
                        .verdict(
                                c.isOnTopic()
                                        ? messages.get("topicAdherence.onTopicLabel")
                                        : messages.get("topicAdherence.offTopicLabel"))
                        .build())
                .toList();
    }

    private String formatClassification(final TopicClassificationItem c) {
        final StringBuilder sb = new StringBuilder();
        sb.append(c.getExtractedTopic());
        if (c.isOnTopic()
                && c.getMatchedReferenceTopic() != null
                && !c.getMatchedReferenceTopic().isEmpty()) {
            sb.append(" → ").append(c.getMatchedReferenceTopic());
        }
        if (c.getReasoning() != null && !c.getReasoning().isEmpty()) {
            sb.append(" (").append(c.getReasoning()).append(")");
        }
        return sb.toString();
    }

    private void buildInterpretation() {
        final String formula = getFormula();
        final String calculation = getCalculation();
        final String meaning = getMeaning();

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevel())
                .meaning(meaning)
                .scaleLevels(createStandardScale())
                .currentLevelIndex(getCurrentLevelIndex())
                .build();
    }

    private String getFormula() {
        return switch (mode.toUpperCase()) {
            case "PRECISION" -> messages.get("topicAdherence.formula.precision");
            case "RECALL" -> messages.get("topicAdherence.formula.recall");
            default -> messages.get("topicAdherence.formula.f1");
        };
    }

    private String getCalculation() {
        return switch (mode.toUpperCase()) {
            case "PRECISION" -> String.format(
                    "%s = %d / %d = %.2f",
                    messages.get("topicAdherence.precisionLabel"), countOnTopic(), classifications.size(), precision);
            case "RECALL" -> String.format(
                    "%s = %d / %d = %.2f",
                    messages.get("topicAdherence.recallLabel"),
                    countCoveredReference(),
                    referenceTopics.size(),
                    recall);
            default -> String.format(
                    "2 × (%.2f × %.2f) / (%.2f + %.2f) = %.2f",
                    precision, recall, precision, recall, score != null ? score : 0.0);
        };
    }

    private long countOnTopic() {
        return classifications.stream()
                .filter(TopicClassificationItem::isOnTopic)
                .count();
    }

    private long countCoveredReference() {
        return classifications.stream()
                .filter(TopicClassificationItem::isOnTopic)
                .map(TopicClassificationItem::getMatchedReferenceTopic)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .count();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("topicAdherence.meaning.excellent");
        } else if (score >= 0.7) {
            return messages.get("topicAdherence.meaning.good");
        } else if (score >= 0.5) {
            return messages.get("topicAdherence.meaning.moderate");
        } else {
            return messages.get("topicAdherence.meaning.poor");
        }
    }

    private String getLevel() {
        if (score == null) {
            return messages.get("common.unknown");
        }
        if (score >= 0.9) {
            return messages.get("scale.excellent");
        } else if (score >= 0.7) {
            return messages.get("scale.good");
        } else if (score >= 0.5) {
            return messages.get("scale.moderate");
        } else {
            return messages.get("scale.poor");
        }
    }

    private int getCurrentLevelIndex() {
        if (score == null) {
            return 3;
        }
        if (score >= 0.9) {
            return 0;
        } else if (score >= 0.7) {
            return 1;
        } else if (score >= 0.5) {
            return 2;
        } else {
            return 3;
        }
    }

    private List<ScoreInterpretation.ScaleLevel> createStandardScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("topicAdherence.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-89%")
                        .description(messages.get("topicAdherence.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-69%")
                        .description(messages.get("topicAdherence.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-49%")
                        .description(messages.get("topicAdherence.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    /**
     * Represents a topic classification result for explanation.
     */
    @Getter
    @Builder
    public static class TopicClassificationItem {
        private final String extractedTopic;
        private final boolean onTopic;
        private final String matchedReferenceTopic;
        private final String reasoning;
    }
}
