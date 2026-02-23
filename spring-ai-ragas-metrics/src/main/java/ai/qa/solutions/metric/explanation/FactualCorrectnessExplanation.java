package ai.qa.solutions.metric.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for FactualCorrectnessMetric.
 * <p>
 * Factual Correctness measures how factually accurate the AI response is
 * compared to the reference answer. It decomposes both texts into atomic claims
 * and uses Natural Language Inference (NLI) to verify which claims are supported.
 * <p>
 * Supports three scoring modes: F1, PRECISION, and RECALL.
 */
@Getter
public class FactualCorrectnessExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "factual-correctness";

    private final String response;
    private final String reference;
    private final List<String> responseClaims;
    private final List<String> referenceClaims;
    private final List<ClaimVerdict> precisionVerdicts;
    private final List<ClaimVerdict> recallVerdicts;
    private final String mode;
    private final int supportedCount;
    private final int totalClaims;
    private final double precision;
    private final double recall;

    @Builder
    public FactualCorrectnessExplanation(
            final Double score,
            final String language,
            final String response,
            final String reference,
            final List<String> responseClaims,
            final List<String> referenceClaims,
            final List<ClaimVerdict> precisionVerdicts,
            final List<ClaimVerdict> recallVerdicts,
            final String mode) {
        super(score, language);
        this.response = response != null ? response : "";
        this.reference = reference != null ? reference : "";
        this.responseClaims = responseClaims != null ? responseClaims : List.of();
        this.referenceClaims = referenceClaims != null ? referenceClaims : List.of();
        this.precisionVerdicts = precisionVerdicts != null ? precisionVerdicts : List.of();
        this.recallVerdicts = recallVerdicts != null ? recallVerdicts : List.of();
        this.mode = mode != null ? mode : "F1";

        // Calculate counts
        this.supportedCount = (int) this.precisionVerdicts.stream()
                .filter(v -> "SUPPORTED".equalsIgnoreCase(v.verdict))
                .count();
        this.totalClaims = this.precisionVerdicts.size();

        // Calculate precision and recall
        this.precision = calculateSupportedRatio(this.precisionVerdicts);
        this.recall = calculateSupportedRatio(this.recallVerdicts);

        buildSteps();
        buildInterpretation();
    }

    private double calculateSupportedRatio(final List<ClaimVerdict> verdicts) {
        if (verdicts == null || verdicts.isEmpty()) {
            return 0.0;
        }
        final long supported = verdicts.stream()
                .filter(v -> "SUPPORTED".equalsIgnoreCase(v.verdict))
                .count();
        return (double) supported / verdicts.size();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("factualCorrectness.description");
    }

    private void buildSteps() {
        // Step 1: Decompose response into claims
        steps.add(StepExplanation.builder()
                .stepName("DecomposeResponseClaims")
                .stepNumber(1)
                .title(messages.get("factualCorrectness.step1.title"))
                .description(messages.get("factualCorrectness.step1.desc"))
                .inputData(truncate(response, 300))
                .outputSummary(messages.get("factualCorrectness.step1.output", responseClaims.size()))
                .items(responseClaims.stream()
                        .map(claim -> ExplanationItem.builder()
                                .content(claim)
                                .index(responseClaims.indexOf(claim) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: Decompose reference into claims
        steps.add(StepExplanation.builder()
                .stepName("DecomposeReferenceClaims")
                .stepNumber(2)
                .title(messages.get("factualCorrectness.step2.title"))
                .description(messages.get("factualCorrectness.step2.desc"))
                .inputData(truncate(reference, 300))
                .outputSummary(messages.get("factualCorrectness.step2.output", referenceClaims.size()))
                .items(referenceClaims.stream()
                        .map(claim -> ExplanationItem.builder()
                                .content(claim)
                                .index(referenceClaims.indexOf(claim) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Verify claims with NLI
        final int precisionSupported = (int) precisionVerdicts.stream()
                .filter(v -> "SUPPORTED".equalsIgnoreCase(v.verdict))
                .count();
        final int recallSupported = (int) recallVerdicts.stream()
                .filter(v -> "SUPPORTED".equalsIgnoreCase(v.verdict))
                .count();

        steps.add(StepExplanation.builder()
                .stepName("VerifyClaimsNLI")
                .stepNumber(3)
                .title(messages.get("factualCorrectness.step3.title"))
                .description(messages.get("factualCorrectness.step3.desc"))
                .outputSummary(messages.get(
                        "factualCorrectness.step3.output",
                        precisionSupported,
                        precisionVerdicts.size(),
                        recallSupported,
                        recallVerdicts.size()))
                .items(precisionVerdicts.stream()
                        .map(v -> ExplanationItem.builder()
                                .content(v.claim)
                                .passed("SUPPORTED".equalsIgnoreCase(v.verdict))
                                .verdict(getVerdictLabel(v.verdict))
                                .reason(v.reason)
                                .index(precisionVerdicts.indexOf(v) + 1)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 4: Compute score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(4)
                .title(messages.get("factualCorrectness.step4.title"))
                .description(messages.get("factualCorrectness.step4.desc", mode))
                .outputSummary(
                        String.format("%s (P=%.2f, R=%.2f, mode=%s)", formatPercent(score), precision, recall, mode))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private String getVerdictLabel(final String verdict) {
        if ("SUPPORTED".equalsIgnoreCase(verdict)) {
            return messages.get("verdict.supported");
        }
        if ("CONTRADICTED".equalsIgnoreCase(verdict)) {
            return messages.get("verdict.contradicted");
        }
        return messages.get("verdict.neutral");
    }

    private void buildInterpretation() {
        final String formula = messages.get("factualCorrectness.formula." + mode.toLowerCase());

        interpretation = ScoreInterpretation.builder()
                .formula(formula != null ? formula : messages.get("factualCorrectness.formula"))
                .calculation(formatPercent(score))
                .score(score)
                .scorePercent(formatPercent(score))
                .level(getLevelName(score))
                .meaning(getMeaning())
                .scaleLevels(createFactualCorrectnessScale())
                .currentLevelIndex(getCurrentLevelIndex(score))
                .build();
    }

    private String getMeaning() {
        if (score == null) {
            return messages.get("common.scoreNotCalculated");
        }
        if (score >= 0.9) {
            return messages.get("factualCorrectness.meaning.excellent");
        }
        if (score >= 0.7) {
            return messages.get("factualCorrectness.meaning.good");
        }
        if (score >= 0.5) {
            return messages.get("factualCorrectness.meaning.moderate");
        }
        return messages.get("factualCorrectness.meaning.poor");
    }

    private List<ScoreInterpretation.ScaleLevel> createFactualCorrectnessScale() {
        return List.of(
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.excellent"))
                        .range("90-100%")
                        .description(messages.get("factualCorrectness.scale.excellent"))
                        .current(score != null && score >= 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.good"))
                        .range("70-90%")
                        .description(messages.get("factualCorrectness.scale.good"))
                        .current(score != null && score >= 0.7 && score < 0.9)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.moderate"))
                        .range("50-70%")
                        .description(messages.get("factualCorrectness.scale.moderate"))
                        .current(score != null && score >= 0.5 && score < 0.7)
                        .build(),
                ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("scale.poor"))
                        .range("0-50%")
                        .description(messages.get("factualCorrectness.scale.poor"))
                        .current(score != null && score < 0.5)
                        .build());
    }

    private String truncate(final String text, final int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Individual claim verification verdict from NLI.
     */
    @Builder
    @Getter
    public static class ClaimVerdict {
        private final String claim;
        private final String verdict;
        private final String reason;
    }
}
