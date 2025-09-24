package ai.qa.solutions.metrics.general;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metric.AbstractLLMMetric;
import ai.qa.solutions.metric.MetricOutputType;
import ai.qa.solutions.sample.SingleTurnSample;
import java.util.Set;

/**
 * AspectCritic Metric - Binary evaluation based on predefined aspects
 * Based on Ragas AspectCritic implementation
 */
public class AspectCriticMetric extends AbstractLLMMetric {
    private String definition;
    private int strictness = 3; // Default strictness level

    public AspectCriticMetric() {
        super("aspect_critic", MetricOutputType.BINARY, Set.of("user_input", "response"));
        initializePromptTemplate();
    }

    public AspectCriticMetric(String name, String definition, LLMEvaluationService llmService) {
        super(name, MetricOutputType.BINARY, Set.of("user_input", "response"));
        this.definition = definition;
        this.llmService = llmService;
        initializePromptTemplate();
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setStrictness(int strictness) {
        if (strictness < 1 || strictness > 5) {
            throw new IllegalArgumentException("Strictness must be between 1 and 5");
        }
        this.strictness = strictness;
    }

    private void initializePromptTemplate() {
        this.promptTemplate =
                """
            Given a user input and an AI response, evaluate whether the response meets the specified criteria.

            Criteria: {definition}

            User Input: {user_input}

            AI Response: {response}

            Instructions:
            1. Carefully analyze the AI response against the given criteria
            2. Consider the context provided by the user input
            3. Apply a strictness level of {strictness} (1=lenient, 5=very strict)
            4. Provide your evaluation as a JSON object with a 'verdict' field (true/false) and a 'reasoning' field

            Return ONLY the corrected JSON object (RFC8259). No markdown, no comments, no extra text.
            Output your response as valid JSON:
            {
                "criteria": "Criteria from user request"
                "verdict": true/false,
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
                .replace("{strictness}", String.valueOf(strictness));
    }

    @Override
    protected Double parseScore(String llmResponse) {
        return llmService.parseJsonScore(llmResponse);
    }
}
