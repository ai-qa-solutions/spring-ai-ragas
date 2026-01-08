package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for ResponseRelevancyMetric.
 * <p>
 * ResponseRelevancy measures how well the AI response addresses
 * the user's question. It generates hypothetical questions that
 * the response could answer and compares them to the original.
 */
@Getter
public class ResponseRelevancyExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "response-relevancy";

    private final String originalQuestion;
    private final String aiResponse;
    private final List<GeneratedQuestion> generatedQuestions;
    private final List<ModelSimilarityResult> modelResults;
    private final double averageSimilarity;

    @Builder
    public ResponseRelevancyExplanation(
            final Double score,
            final String language,
            final String originalQuestion,
            final String aiResponse,
            final List<GeneratedQuestion> generatedQuestions,
            final List<ModelSimilarityResult> modelResults) {
        super(score, language);
        this.originalQuestion = originalQuestion != null ? originalQuestion : "";
        this.aiResponse = aiResponse != null ? aiResponse : "";
        this.generatedQuestions = generatedQuestions != null ? generatedQuestions : List.of();
        this.modelResults = modelResults != null ? modelResults : List.of();
        this.averageSimilarity = score != null ? score : 0.0;
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("responseRelevancy.description");
    }

    private void buildSteps() {
        // Step 1: Show original question
        steps.add(StepExplanation.builder()
                .stepName("OriginalQuestion")
                .stepNumber(1)
                .title(messages.get("responseRelevancy.step1.title"))
                .description(messages.get("responseRelevancy.step1.desc"))
                .outputSummary(originalQuestion)
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Generate questions from response
        steps.add(StepExplanation.builder()
                .stepName("GenerateQuestions")
                .stepNumber(2)
                .title(messages.get("responseRelevancy.step2.title"))
                .description(messages.get("responseRelevancy.step2.desc"))
                .inputData(aiResponse)
                .outputSummary(messages.get("responseRelevancy.step2.output"))
                .items(generatedQuestions.stream()
                        .map((q) -> ExplanationItem.builder()
                                .content(q.question)
                                .numericValue(q.similarity)
                                .index(generatedQuestions.indexOf(q) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Calculate similarity
        steps.add(StepExplanation.builder()
                .stepName("EmbedAndCompare")
                .stepNumber(3)
                .title(messages.get("responseRelevancy.step3.title"))
                .description(messages.get("responseRelevancy.step3.desc"))
                .items(generatedQuestions.stream()
                        .map((q) -> ExplanationItem.builder()
                                .content(q.question)
                                .numericValue(q.similarity)
                                .verdict(String.format("%.2f", q.similarity))
                                .index(generatedQuestions.indexOf(q) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Calculate average - show per-model results
        final List<ModelStepResult> stepModelResults = modelResults.stream()
                .map(m -> ModelStepResult.builder()
                        .modelId(m.modelId)
                        .success(true)
                        .numericResult(m.similarity)
                        .build())
                .toList();

        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(4)
                .title(messages.get("responseRelevancy.step4.title"))
                .description(messages.get("responseRelevancy.step4.desc"))
                .outputSummary(formatPercent(score))
                .modelResults(stepModelResults)
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final String formula = messages.get("responseRelevancy.formula");

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createRelevancyScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("responseRelevancy.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("responseRelevancy.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("responseRelevancy.meaning.moderate");
        }
        return messages.get("responseRelevancy.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createRelevancyScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("responseRelevancy.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("responseRelevancy.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("responseRelevancy.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("responseRelevancy.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    /**
     * A generated question with its similarity score.
     */
    @Builder
    @Getter
    public static class GeneratedQuestion {
        private final String question;
        private final double similarity;
    }

    /**
     * Similarity result from a single model.
     */
    @Builder
    @Getter
    public static class ModelSimilarityResult {
        private final String modelId;
        private final double similarity;
    }
}
