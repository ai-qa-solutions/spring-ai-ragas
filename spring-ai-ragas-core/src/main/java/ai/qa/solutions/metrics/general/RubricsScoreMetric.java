package ai.qa.solutions.metrics.general;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Map;
import java.util.Set;

/**
 * RubricsScore Metric - Detailed rubric-based evaluation
 * Based on Ragas RubricsScore implementation
 */
public class RubricsScoreMetric extends AbstractLLMMetric {
    private Map<String, String> rubrics;

    public RubricsScoreMetric() {
        super("rubrics_score", MetricOutputType.DISCRETE, Set.of("user_input", "response", "reference"));
        initializePromptTemplate();
    }

    public RubricsScoreMetric(String name, Map<String, String> rubrics, LLMEvaluationService llmService) {
        super(name, MetricOutputType.DISCRETE, Set.of("user_input", "response", "reference"));
        this.rubrics = rubrics;
        this.llmService = llmService;
        initializePromptTemplate();
    }

    public void setRubrics(Map<String, String> rubrics) {
        this.rubrics = rubrics;
    }

    private void initializePromptTemplate() {
        this.promptTemplate =
                """
            Evaluate the AI response using the provided detailed rubrics.

            User Input: {user_input}
            AI Response: {response}
            Reference Answer: {reference}

            Evaluation Rubrics:
            {rubrics}

            Instructions:
            1. Compare the AI response with the reference answer
            2. Evaluate the response against each rubric level
            3. Select the rubric level that best describes the response quality
            4. Provide the corresponding score and detailed reasoning

            Return ONLY the corrected JSON object (RFC8259). No markdown, no comments, no extra text.
            Output your response as valid JSON:
            {
                "score": <numerical_score>,
                "rubric_level": "<selected_rubric_key>",
                "reasoning": "Your detailed explanation here"
            }
            """;
    }

    @Override
    protected String buildPrompt(SingleTurnSample sample) {
        if (rubrics == null || rubrics.isEmpty()) {
            throw new IllegalStateException("Rubrics must be set before scoring");
        }

        StringBuilder rubricsText = new StringBuilder();
        rubrics.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String scoreKey = entry.getKey(); // e.g., "score1_description"
            String score = scoreKey.replaceAll("[^0-9]", ""); // Extract number
            rubricsText
                    .append("Score ")
                    .append(score)
                    .append(": ")
                    .append(entry.getValue())
                    .append("\n");
        });

        return promptTemplate
                .replace("{user_input}", sample.getUserInput())
                .replace("{response}", sample.getResponse())
                .replace("{reference}", sample.getReference() != null ? sample.getReference() : "")
                .replace("{rubrics}", rubricsText.toString());
    }

    @Override
    protected Double parseScore(String llmResponse) {
        return llmService.parseJsonScore(llmResponse);
    }
}
