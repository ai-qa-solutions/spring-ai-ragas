package ai.qa.solutions.metrics.general;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Set;

/**
 * SimpleCriteriaScore Metric - Continuous scoring based on simple criteria
 * Based on Ragas SimpleCriteriaScore implementation
 */
public class SimpleCriteriaScoreMetric extends AbstractLLMMetric {
    private String definition;
    private double minScore = 0.0;
    private double maxScore = 5.0;

    public SimpleCriteriaScoreMetric() {
        super("simple_criteria_score", MetricOutputType.DISCRETE, Set.of("user_input", "response", "reference"));
        initializePromptTemplate();
    }

    public SimpleCriteriaScoreMetric(String name, String definition, LLMEvaluationService llmService) {
        super(name, MetricOutputType.DISCRETE, Set.of("user_input", "response", "reference"));
        this.definition = definition;
        this.llmService = llmService;
        initializePromptTemplate();
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setScoreRange(double minScore, double maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    private void initializePromptTemplate() {
        this.promptTemplate =
                """
            Evaluate the AI response based on the given criteria and score it accordingly.

            Evaluation Criteria: {definition}

            User Input: {user_input}
            AI Response: {response}
            Reference Answer: {reference}

            Instructions:
            1. Compare the AI response with the reference answer
            2. Evaluate based on the specified criteria: {definition}
            3. Provide a score between {min_score} and {max_score}
            4. Higher scores indicate better alignment with the criteria
            5. Provide detailed reasoning for your score

            Return ONLY the corrected JSON object (RFC8259). No markdown, no comments, no extra text.
            Output your response as valid JSON:
            {
                "score": <numerical_score>,
                "reasoning": "Your detailed explanation here"
            }
            """;
    }

    @Override
    protected String buildPrompt(SingleTurnSample sample) {
        if (definition == null || definition.trim().isEmpty()) {
            throw new IllegalStateException("Definition must be set before scoring");
        }

        return promptTemplate
                .replace("{definition}", definition)
                .replace("{user_input}", sample.getUserInput())
                .replace("{response}", sample.getResponse())
                .replace("{reference}", sample.getReference() != null ? sample.getReference() : "")
                .replace("{min_score}", String.valueOf(minScore))
                .replace("{max_score}", String.valueOf(maxScore));
    }

    @Override
    protected Double parseScore(String llmResponse) {
        return llmService.parseJsonScore(llmResponse);
    }
}
