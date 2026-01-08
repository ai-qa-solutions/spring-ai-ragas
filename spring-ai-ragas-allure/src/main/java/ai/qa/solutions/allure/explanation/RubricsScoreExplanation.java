package ai.qa.solutions.allure.explanation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Score explanation for RubricsScoreMetric.
 * <p>
 * RubricsScore provides rubric-based evaluation where the user defines
 * multiple levels with descriptions. The LLM selects the appropriate level.
 */
@Getter
public class RubricsScoreExplanation extends AbstractScoreExplanation {

    private static final String METRIC_TYPE = "rubrics-score";

    private final List<RubricLevel> rubricLevels;
    private final int selectedLevel;
    private final String reasoning;

    @Builder
    public RubricsScoreExplanation(
            final Double score,
            final String language,
            final List<RubricLevel> rubricLevels,
            final int selectedLevel,
            final String reasoning) {
        super(score, language);
        this.rubricLevels = rubricLevels != null ? rubricLevels : List.of();
        this.selectedLevel = selectedLevel;
        this.reasoning = reasoning != null ? reasoning : "";
        buildSteps();
        buildInterpretation();
    }

    @Override
    public String getMetricType() {
        return METRIC_TYPE;
    }

    @Override
    public String getSimpleDescription() {
        return messages.get("rubricsScore.description");
    }

    private void buildSteps() {
        // Step 1: Show all rubric levels
        steps.add(StepExplanation.builder()
                .stepName("DefineRubric")
                .stepNumber(1)
                .title(messages.get("rubricsScore.step1.title"))
                .description(messages.get("rubricsScore.step1.desc"))
                .items(rubricLevels.stream()
                        .map((r) -> ExplanationItem.builder()
                                .content(r.description)
                                .passed(r.level == selectedLevel)
                                .verdict(messages.get("rubricsScore.level", r.level))
                                .index(r.level)
                                .build())
                        .toList())
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 2: LLM selection
        final String selectedDesc = rubricLevels.stream()
                .filter(r -> r.level == selectedLevel)
                .map(RubricLevel::getDescription)
                .findFirst()
                .orElse("");
        steps.add(StepExplanation.builder()
                .stepName("SelectLevel")
                .stepNumber(2)
                .title(messages.get("rubricsScore.step2.title"))
                .description(messages.get("rubricsScore.step2.desc"))
                .outputSummary(messages.get("rubricsScore.step2.output", selectedLevel, selectedDesc))
                .items(List.of(ExplanationItem.builder()
                        .content(reasoning)
                        .passed(true)
                        .index(1)
                        .build()))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());

        // Step 3: Calculate score
        steps.add(StepExplanation.builder()
                .stepName("ComputeScore")
                .stepNumber(3)
                .title(messages.get("rubricsScore.step3.title"))
                .description(messages.get("rubricsScore.step3.desc"))
                .outputSummary(formatPercent(score))
                .hasModelDisagreement(false)
                .agreementPercent(100.0)
                .build());
    }

    private void buildInterpretation() {
        final int minLevel =
                rubricLevels.stream().mapToInt(RubricLevel::getLevel).min().orElse(1);
        final int maxLevel =
                rubricLevels.stream().mapToInt(RubricLevel::getLevel).max().orElse(5);

        final String formula =
                String.format("(%s - %d) / (%d - %d)", messages.get("common.level"), minLevel, maxLevel, minLevel);

        // Score is aggregated across models, so show simplified calculation
        final String calculation = formatPercent(score);

        final String selectedDesc = rubricLevels.stream()
                .filter(r -> r.level == selectedLevel)
                .map(RubricLevel::getDescription)
                .findFirst()
                .orElse("");

        final String meaning = messages.get("rubricsScore.meaning", selectedLevel, selectedDesc);

        // Convert rubric levels to scale levels
        final List<ScoreInterpretation.ScaleLevel> scaleLevels = rubricLevels.stream()
                .sorted((a, b) -> Integer.compare(b.level, a.level)) // Descending order
                .map(r -> ScoreInterpretation.ScaleLevel.builder()
                        .name(messages.get("rubricsScore.level", r.level))
                        .range(String.valueOf(r.level))
                        .description(r.description)
                        .current(r.level == selectedLevel)
                        .build())
                .toList();

        final int currentLevelIndex = rubricLevels.stream()
                .filter(r -> r.level == selectedLevel)
                .findFirst()
                .map(rubricLevels::indexOf)
                .orElse(-1);

        interpretation = ScoreInterpretation.builder()
                .formula(formula)
                .calculation(calculation)
                .numerator(selectedLevel - minLevel)
                .denominator(maxLevel - minLevel)
                .score(score)
                .scorePercent(formatPercent(score))
                .level(messages.get("rubricsScore.level", selectedLevel))
                .meaning(meaning)
                .scaleLevels(scaleLevels)
                .currentLevelIndex(currentLevelIndex)
                .build();
    }

    /**
     * A single level in the rubric.
     */
    @Builder
    @Getter
    public static class RubricLevel {
        private final int level;
        private final String description;
    }
}
