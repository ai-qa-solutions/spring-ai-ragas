package ai.qa.solutions.allure.i18n;

import java.util.Map;
import lombok.Getter;

/**
 * Localized messages for score explanation components.
 * <p>
 * Provides translations for all UI strings in explanation blocks.
 * Supports English (en) and Russian (ru) languages.
 */
public final class ExplanationMessages {

    private static final String DEFAULT_LANGUAGE = "en";

    @Getter
    private final String language;

    private final Map<String, String> messages;

    /**
     * Creates an ExplanationMessages instance for the specified language.
     *
     * @param language language code (en, ru)
     */
    public ExplanationMessages(final String language) {
        this.language = language != null ? language : DEFAULT_LANGUAGE;
        this.messages = "ru".equalsIgnoreCase(this.language) ? russianMessages() : englishMessages();
    }

    /**
     * Gets a localized message by key.
     *
     * @param key message key
     * @return localized string, or key if not found
     */
    public String get(final String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * Gets a localized message with placeholder substitution.
     *
     * @param key message key
     * @param args arguments to substitute
     * @return localized string with substitutions
     */
    public String get(final String key, final Object... args) {
        final String template = messages.getOrDefault(key, key);
        return String.format(template, args);
    }

    /**
     * Checks if the current language is Russian.
     *
     * @return true if language is "ru"
     */
    public boolean isRussian() {
        return "ru".equalsIgnoreCase(language);
    }

    private static Map<String, String> englishMessages() {
        return Map.ofEntries(
                // Common
                Map.entry("common.na", "N/A"),
                Map.entry("common.unknown", "Unknown"),
                Map.entry("common.scoreNotCalculated", "Score not calculated"),
                Map.entry("common.score", "score"),
                Map.entry("common.level", "level"),
                Map.entry("common.models", "models"),

                // Scale levels (standard)
                Map.entry("scale.excellent", "Excellent"),
                Map.entry("scale.good", "Good"),
                Map.entry("scale.moderate", "Moderate"),
                Map.entry("scale.poor", "Poor"),
                Map.entry("scale.excellent.desc", "Very high score"),
                Map.entry("scale.good.desc", "Good score"),
                Map.entry("scale.moderate.desc", "Moderate score"),
                Map.entry("scale.poor.desc", "Low score"),

                // Verdicts
                Map.entry("verdict.faithful", "FAITHFUL"),
                Map.entry("verdict.unfaithful", "UNFAITHFUL"),
                Map.entry("verdict.relevant", "Relevant"),
                Map.entry("verdict.notRelevant", "Not relevant"),
                Map.entry("verdict.found", "Found"),
                Map.entry("verdict.missing", "Missing"),
                Map.entry("verdict.pass", "PASS"),
                Map.entry("verdict.fail", "FAIL"),
                Map.entry("verdict.ok", "OK"),
                Map.entry("verdict.error", "ERROR"),

                // Step titles - common
                Map.entry("step.computeScore", "Calculating final score"),

                // Faithfulness
                Map.entry(
                        "faithfulness.description",
                        "This metric checks: did the AI make up anything? "
                                + "We break down the response into individual facts and verify each against the context."),
                Map.entry("faithfulness.step1.title", "Breaking down the response"),
                Map.entry(
                        "faithfulness.step1.desc",
                        "We took the AI response and broke it down into individual facts (statements)."),
                Map.entry("faithfulness.step1.output", "Extracted %d statements"),
                Map.entry("faithfulness.step2.title", "Verifying statements"),
                Map.entry(
                        "faithfulness.step2.desc",
                        "For each statement, we checked: can it be found in the provided context?"),
                Map.entry("faithfulness.step2.output", "%d of %d statements verified"),
                Map.entry("faithfulness.step3.title", "Calculating final score"),
                Map.entry("faithfulness.step3.desc", "Divide the number of verified statements by the total count."),
                Map.entry("faithfulness.formula", "avg((verified statements) / (total statements) per model)"),
                Map.entry("faithfulness.meaning.excellent", "Excellent - AI relies only on facts from the context"),
                Map.entry("faithfulness.meaning.good", "Good - most statements are supported by the context"),
                Map.entry("faithfulness.meaning.moderate", "Moderate - some information is not supported by context"),
                Map.entry("faithfulness.meaning.poor", "Poor - many hallucinations (made-up facts)"),
                Map.entry("faithfulness.scale.excellent", "All statements verified by context"),
                Map.entry("faithfulness.scale.good", "Most statements verified"),
                Map.entry("faithfulness.scale.moderate", "Some statements not verified"),
                Map.entry("faithfulness.scale.poor", "Many hallucinations"),

                // AspectCritic
                Map.entry(
                        "aspectCritic.description",
                        "Binary check: does the response meet the specified criteria? Result: PASS or FAIL."),
                Map.entry("aspectCritic.step1.title", "User-defined aspect"),
                Map.entry("aspectCritic.step1.desc", "The criteria defined by the user for evaluation."),
                Map.entry("aspectCritic.step2.title", "Aspect evaluation"),
                Map.entry("aspectCritic.step2.desc", "LLM analyzed the response for criteria compliance."),
                Map.entry("aspectCritic.step3.majority.title", "Majority Voting (strictness)"),
                Map.entry(
                        "aspectCritic.step3.majority.desc",
                        "Each model was called %d times. Final result determined by majority voting."),
                Map.entry("aspectCritic.step3.title", "Score calculation"),
                Map.entry("aspectCritic.step3.desc", "PASS → 1.0, FAIL → 0.0"),
                Map.entry("aspectCritic.meaning.pass", "Criteria \"%s\" PASSED"),
                Map.entry("aspectCritic.meaning.fail", "Criteria \"%s\" FAILED"),
                Map.entry("aspectCritic.scale.pass", "Criteria met"),
                Map.entry("aspectCritic.scale.fail", "Criteria not met"),
                Map.entry("aspectCritic.defaultAspect", "Custom Aspect"),

                // ContextPrecision
                Map.entry(
                        "contextPrecision.description",
                        "This metric checks: are contexts ranked correctly? "
                                + "Relevant contexts should appear first in the list."),
                Map.entry("contextPrecision.step1.title", "Evaluating context relevance"),
                Map.entry(
                        "contextPrecision.step1.desc",
                        "For each context, we check: is it relevant for answering the question?"),
                Map.entry("contextPrecision.step1.output", "%d of %d are relevant"),
                Map.entry("contextPrecision.step2.title", "Calculating Precision@K"),
                Map.entry("contextPrecision.step2.desc", "Precision@K = (relevant in top-K) / K"),
                Map.entry("contextPrecision.step2.output", "Precision calculated for %d contexts (%d relevant)"),
                Map.entry("contextPrecision.step3.title", "Calculating Average Precision"),
                Map.entry(
                        "contextPrecision.step3.desc",
                        "AP = average Precision@K only at positions with relevant contexts"),
                Map.entry("contextPrecision.formula", "AP = Σ(Precision@k × relevance@k) / (relevant count)"),
                Map.entry("contextPrecision.meaning.excellent", "Excellent - relevant contexts appear first"),
                Map.entry("contextPrecision.meaning.good", "Good - most relevant contexts are at the top"),
                Map.entry("contextPrecision.meaning.moderate", "Moderate - ranking needs improvement"),
                Map.entry("contextPrecision.meaning.poor", "Poor - relevant contexts are lost among irrelevant ones"),

                // ContextRecall
                Map.entry(
                        "contextRecall.description",
                        "This metric checks: do contexts cover all information from the reference? "
                                + "Each sentence from reference should be found in contexts."),
                Map.entry("contextRecall.step1.title", "Checking reference coverage by contexts"),
                Map.entry(
                        "contextRecall.step1.desc",
                        "For each sentence from the reference answer, we check: "
                                + "can it be found in the provided contexts?"),
                Map.entry("contextRecall.step1.output", "%d of %d found in contexts"),
                Map.entry("contextRecall.step2.title", "Calculating coverage"),
                Map.entry("contextRecall.step2.desc", "Proportion of reference sentences found in contexts."),
                Map.entry("contextRecall.formula", "avg((found sentences) / (total sentences) per model)"),
                Map.entry("contextRecall.meaning.excellent", "Excellent - contexts fully cover the reference answer"),
                Map.entry("contextRecall.meaning.good", "Good - most of the reference is found in contexts"),
                Map.entry("contextRecall.meaning.moderate", "Moderate - some information is missing from contexts"),
                Map.entry("contextRecall.meaning.poor", "Poor - contexts don't contain significant parts of reference"),
                Map.entry("contextRecall.scale.excellent", "Full reference coverage"),
                Map.entry("contextRecall.scale.good", "Good coverage"),
                Map.entry("contextRecall.scale.moderate", "Partial coverage"),
                Map.entry("contextRecall.scale.poor", "Insufficient coverage"),

                // ContextEntityRecall
                Map.entry(
                        "contextEntityRecall.description",
                        "This metric checks: do contexts contain all important entities from reference? "
                                + "Entities are names, dates, places, organizations, etc."),
                Map.entry("contextEntityRecall.step1.title", "Extracting entities from reference"),
                Map.entry(
                        "contextEntityRecall.step1.desc",
                        "Finding all important entities in reference: names, dates, places, etc."),
                Map.entry("contextEntityRecall.step1.output", "Found %d entities"),
                Map.entry("contextEntityRecall.step2.title", "Extracting entities from contexts"),
                Map.entry("contextEntityRecall.step2.desc", "Finding all entities in provided contexts."),
                Map.entry("contextEntityRecall.step3.title", "Comparing entities"),
                Map.entry("contextEntityRecall.step3.desc", "Which entities from reference are found in contexts?"),
                Map.entry("contextEntityRecall.step3.output", "%d of %d found"),
                Map.entry("contextEntityRecall.step4.title", "Calculating entity coverage"),
                Map.entry("contextEntityRecall.step4.desc", "Proportion of reference entities found in contexts."),
                Map.entry("contextEntityRecall.formula", "avg((found entities) / (total entities) per model)"),
                Map.entry(
                        "contextEntityRecall.meaning.excellent", "Excellent - contexts contain all important entities"),
                Map.entry("contextEntityRecall.meaning.good", "Good - most entities are present"),
                Map.entry("contextEntityRecall.meaning.moderate", "Moderate - some entities are missing"),
                Map.entry(
                        "contextEntityRecall.meaning.poor", "Poor - many important entities are missing from contexts"),

                // NoiseSensitivity
                Map.entry(
                        "noiseSensitivity.description",
                        "This metric checks: does irrelevant context influence the AI response? "
                                + "Lower values are better - the system ignores noisy context."),
                Map.entry("noiseSensitivity.step1.title", "Breaking down reference into statements"),
                Map.entry("noiseSensitivity.step1.desc", "The reference answer is broken down into individual facts."),
                Map.entry("noiseSensitivity.step1.output", "Reference statements"),
                Map.entry("noiseSensitivity.step2.title", "Breaking down AI response into statements"),
                Map.entry("noiseSensitivity.step2.desc", "The AI response is broken down into individual facts."),
                Map.entry("noiseSensitivity.step2.output", "Response statements"),
                Map.entry("noiseSensitivity.step3.title", "Analyzing matches and sources"),
                Map.entry(
                        "noiseSensitivity.step3.desc",
                        "Checking if response statements match reference and where information came from."),
                Map.entry("noiseSensitivity.step3.output", "%d errors out of %d checks"),
                Map.entry("noiseSensitivity.step4.title", "Calculating noise sensitivity"),
                Map.entry(
                        "noiseSensitivity.step4.desc",
                        "Proportion of errors caused by irrelevant context. Lower = better."),
                Map.entry("noiseSensitivity.formula", "avg((noise errors / total checks) per model)"),
                Map.entry(
                        "noiseSensitivity.meaning.excellent",
                        "EXCELLENT - System completely ignores irrelevant context"),
                Map.entry("noiseSensitivity.meaning.good", "GOOD - Minimal influence from irrelevant context"),
                Map.entry(
                        "noiseSensitivity.meaning.moderate", "MODERATE - Noticeable influence from irrelevant context"),
                Map.entry(
                        "noiseSensitivity.meaning.poor",
                        "POOR - Response is heavily distorted by irrelevant information"),
                Map.entry("noiseSensitivity.scale.excellent", "No noise influence (EXCELLENT)"),
                Map.entry("noiseSensitivity.scale.good", "Minimal influence"),
                Map.entry("noiseSensitivity.scale.moderate", "Noticeable influence"),
                Map.entry("noiseSensitivity.scale.poor", "Heavy response distortion"),

                // ResponseRelevancy
                Map.entry(
                        "responseRelevancy.description",
                        "This metric checks: does the AI response actually answer the user's question? "
                                + "We generate questions the response could answer and compare to the original."),
                Map.entry("responseRelevancy.step1.title", "Original user question"),
                Map.entry("responseRelevancy.step1.desc", "The question that the AI response should answer."),
                Map.entry("responseRelevancy.step2.title", "Generating questions from response"),
                Map.entry(
                        "responseRelevancy.step2.desc",
                        "LLM generates questions that the given response could answer."),
                Map.entry("responseRelevancy.step2.output", "Generated questions"),
                Map.entry("responseRelevancy.step3.title", "Calculating semantic similarity"),
                Map.entry(
                        "responseRelevancy.step3.desc",
                        "Comparing generated questions with the original via embeddings. "
                                + "The more similar the questions - the more relevant the response."),
                Map.entry("responseRelevancy.step4.title", "Calculating average similarity"),
                Map.entry(
                        "responseRelevancy.step4.desc", "Final score = average similarity of all generated questions."),
                Map.entry("responseRelevancy.formula", "mean(cosine_similarity(original, generated))"),
                Map.entry("responseRelevancy.meaning.excellent", "Excellent - response directly answers the question"),
                Map.entry("responseRelevancy.meaning.good", "Good - response is mostly relevant to the question"),
                Map.entry("responseRelevancy.meaning.moderate", "Moderate - response partially answers the question"),
                Map.entry("responseRelevancy.meaning.poor", "Poor - response is off-topic"),
                Map.entry("responseRelevancy.scale.excellent", "Response directly answers the question"),
                Map.entry("responseRelevancy.scale.good", "Response is mostly relevant"),
                Map.entry("responseRelevancy.scale.moderate", "Response partially answers"),
                Map.entry("responseRelevancy.scale.poor", "Response is off-topic"),

                // SimpleCriteria
                Map.entry(
                        "simpleCriteria.description",
                        "Evaluation by user-defined criteria on a continuous scale. "
                                + "Interpretation depends on your criteria requirements."),
                Map.entry("simpleCriteria.step1.title", "User-defined criteria"),
                Map.entry("simpleCriteria.step1.desc", "The criteria defined by the user for evaluation."),
                Map.entry("simpleCriteria.step2.title", "LLM evaluation"),
                Map.entry("simpleCriteria.step2.desc", "LLM evaluates the response on a scale from %d to %d."),
                Map.entry("simpleCriteria.step3.title", "Score normalization"),
                Map.entry("simpleCriteria.step3.desc", "Normalizing to 0-1 scale."),
                Map.entry(
                        "simpleCriteria.meaning",
                        "Score: %d out of %d for criteria \"%s\". Interpretation depends on your requirements."),
                Map.entry("simpleCriteria.defaultCriteria", "Custom Criteria"),

                // RubricsScore
                Map.entry(
                        "rubricsScore.description",
                        "Rubric-based evaluation - LLM selects a level from a predefined scale "
                                + "with descriptions for each level."),
                Map.entry("rubricsScore.step1.title", "Evaluation rubric"),
                Map.entry("rubricsScore.step1.desc", "User defined a scale with description for each level."),
                Map.entry("rubricsScore.step2.title", "LLM as a Judge"),
                Map.entry("rubricsScore.step2.desc", "LLM analyzed the response and selected the appropriate level."),
                Map.entry("rubricsScore.step2.output", "Level %d: %s"),
                Map.entry("rubricsScore.step3.title", "Score calculation"),
                Map.entry("rubricsScore.step3.desc", "Normalizing selected level to 0-1 scale."),
                Map.entry("rubricsScore.meaning", "Selected level %d: %s"),
                Map.entry("rubricsScore.level", "Level %d"),

                // SemanticSimilarity
                Map.entry(
                        "semanticSimilarity.description",
                        "This metric measures how semantically similar the AI response is to the reference. "
                                + "It uses embedding models to compute vector representations and cosine similarity."),
                Map.entry("semanticSimilarity.response", "Response"),
                Map.entry("semanticSimilarity.reference", "Reference"),
                Map.entry("semanticSimilarity.step1.title", "Input texts"),
                Map.entry("semanticSimilarity.step1.desc", "The response and reference texts to compare."),
                Map.entry("semanticSimilarity.step2.title", "Computing embeddings"),
                Map.entry(
                        "semanticSimilarity.step2.desc",
                        "Converting response and reference to vector representations using embedding models."),
                Map.entry("semanticSimilarity.step2.output", "Embeddings computed by %d models"),
                Map.entry("semanticSimilarity.step3.title", "Computing cosine similarity"),
                Map.entry(
                        "semanticSimilarity.step3.desc",
                        "Calculating cosine similarity between response and reference embeddings."),
                Map.entry("semanticSimilarity.step3.outputThreshold", "%s (threshold: %.2f)"),
                Map.entry("semanticSimilarity.formula", "cosine_similarity(embed(response), embed(reference))"),
                Map.entry(
                        "semanticSimilarity.meaning.excellent",
                        "Excellent - response is semantically nearly identical to reference"),
                Map.entry("semanticSimilarity.meaning.good", "Good - response conveys similar meaning to reference"),
                Map.entry(
                        "semanticSimilarity.meaning.moderate", "Moderate - response is somewhat related to reference"),
                Map.entry("semanticSimilarity.meaning.poor", "Poor - response differs significantly from reference"),
                Map.entry("semanticSimilarity.meaning.passThreshold", "PASS - similarity >= threshold (%.2f)"),
                Map.entry("semanticSimilarity.meaning.failThreshold", "FAIL - similarity < threshold (%.2f)"),
                Map.entry("semanticSimilarity.scale.excellent", "Nearly identical meaning"),
                Map.entry("semanticSimilarity.scale.good", "Similar meaning"),
                Map.entry("semanticSimilarity.scale.moderate", "Somewhat related"),
                Map.entry("semanticSimilarity.scale.poor", "Different meanings"),

                // FactualCorrectness
                Map.entry(
                        "factualCorrectness.description",
                        "This metric checks: are the facts in the AI response correct? "
                                + "We decompose response and reference into atomic claims and verify each using NLI."),
                Map.entry("factualCorrectness.step1.title", "Decomposing response into claims"),
                Map.entry(
                        "factualCorrectness.step1.desc",
                        "The AI response is broken down into individual atomic claims that can be verified."),
                Map.entry("factualCorrectness.step1.output", "Extracted %d claims from response"),
                Map.entry("factualCorrectness.step2.title", "Decomposing reference into claims"),
                Map.entry(
                        "factualCorrectness.step2.desc",
                        "The reference answer is broken down into individual atomic claims."),
                Map.entry("factualCorrectness.step2.output", "Extracted %d claims from reference"),
                Map.entry("factualCorrectness.step3.title", "Verifying claims with NLI"),
                Map.entry(
                        "factualCorrectness.step3.desc",
                        "Each claim is verified using Natural Language Inference: SUPPORTED, CONTRADICTED, or NEUTRAL."),
                Map.entry("factualCorrectness.step3.output", "Precision: %d/%d supported, Recall: %d/%d supported"),
                Map.entry("factualCorrectness.step4.title", "Computing final score"),
                Map.entry("factualCorrectness.step4.desc", "Calculating %s score from precision and recall."),
                Map.entry("factualCorrectness.formula", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry("factualCorrectness.formula.f1", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry(
                        "factualCorrectness.formula.precision",
                        "Precision = supported response claims / total response claims"),
                Map.entry(
                        "factualCorrectness.formula.recall",
                        "Recall = supported reference claims / total reference claims"),
                Map.entry(
                        "factualCorrectness.meaning.excellent",
                        "Excellent - all claims are factually correct and complete"),
                Map.entry("factualCorrectness.meaning.good", "Good - most claims are correct with minor omissions"),
                Map.entry("factualCorrectness.meaning.moderate", "Moderate - some facts are incorrect or missing"),
                Map.entry("factualCorrectness.meaning.poor", "Poor - many factual errors or significant omissions"),
                Map.entry("factualCorrectness.scale.excellent", "All claims verified as correct"),
                Map.entry("factualCorrectness.scale.good", "Most claims are correct"),
                Map.entry("factualCorrectness.scale.moderate", "Some claims incorrect or missing"),
                Map.entry("factualCorrectness.scale.poor", "Many factual errors"),

                // NLI Verdicts
                Map.entry("verdict.supported", "SUPPORTED"),
                Map.entry("verdict.contradicted", "CONTRADICTED"),
                Map.entry("verdict.neutral", "NEUTRAL"),

                // AnswerCorrectness
                Map.entry(
                        "answerCorrectness.description",
                        "This metric combines factual correctness and semantic similarity to evaluate answer quality. "
                                + "It weighs both whether facts are correct and whether the meaning is preserved."),
                Map.entry("answerCorrectness.step1.title", "Input texts"),
                Map.entry("answerCorrectness.step1.desc", "The response and reference texts to compare."),
                Map.entry("answerCorrectness.step2.title", "Computing factual correctness"),
                Map.entry(
                        "answerCorrectness.step2.desc",
                        "Evaluating factual correctness using claims decomposition and NLI verification."),
                Map.entry("answerCorrectness.step3.title", "Computing semantic similarity"),
                Map.entry(
                        "answerCorrectness.step3.desc",
                        "Calculating semantic similarity using embeddings and cosine similarity."),
                Map.entry("answerCorrectness.step4.title", "Combining scores"),
                Map.entry("answerCorrectness.step4.desc", "Computing weighted average of factual and semantic scores."),
                Map.entry("answerCorrectness.factualScore", "Factual correctness"),
                Map.entry("answerCorrectness.semanticScore", "Semantic similarity"),
                Map.entry("answerCorrectness.factual", "factual"),
                Map.entry("answerCorrectness.semantic", "semantic"),
                Map.entry("common.response", "Response"),
                Map.entry("common.reference", "Reference"),
                Map.entry(
                        "answerCorrectness.meaning.excellent",
                        "Excellent - response is both factually correct and semantically aligned"),
                Map.entry("answerCorrectness.meaning.good", "Good - response is mostly correct with high similarity"),
                Map.entry("answerCorrectness.meaning.moderate", "Moderate - some factual issues or semantic drift"),
                Map.entry("answerCorrectness.meaning.poor", "Poor - significant factual errors or semantic mismatch"),
                Map.entry("answerCorrectness.scale.excellent", "Both factually correct and semantically aligned"),
                Map.entry("answerCorrectness.scale.good", "Mostly correct with good similarity"),
                Map.entry("answerCorrectness.scale.moderate", "Some issues with facts or meaning"),
                Map.entry("answerCorrectness.scale.poor", "Significant errors or mismatch"),

                // Message types for multi-turn conversations
                Map.entry("message.type.human", "User"),
                Map.entry("message.type.ai", "Assistant"),
                Map.entry("message.type.tool", "Tool Result"),
                Map.entry("message.toolCalls", "Tool Calls"),

                // AgentGoalAccuracy
                Map.entry(
                        "agentGoalAccuracy.description",
                        "This metric evaluates whether an AI agent achieved its intended goal. "
                                + "It analyzes multi-turn conversations to determine goal completion."),
                Map.entry("agentGoalAccuracy.step.inferGoal.title", "Inferring goal from conversation"),
                Map.entry(
                        "agentGoalAccuracy.step.inferGoal.desc",
                        "LLM analyzes the conversation to infer what the user's primary goal was."),
                Map.entry("agentGoalAccuracy.step.evaluateOutcome.title", "Evaluating outcome"),
                Map.entry(
                        "agentGoalAccuracy.step.evaluateOutcome.desc",
                        "Determining if the agent's actions successfully achieved the inferred goal."),
                Map.entry("agentGoalAccuracy.step.compareOutcome.title", "Comparing with expected outcome"),
                Map.entry(
                        "agentGoalAccuracy.step.compareOutcome.desc",
                        "Comparing the conversation outcome with the provided expected goal."),
                Map.entry("agentGoalAccuracy.goal", "Goal"),
                Map.entry("agentGoalAccuracy.expectedOutcome", "Expected outcome"),
                Map.entry("agentGoalAccuracy.formula", "Binary: 1.0 if goal achieved, 0.0 otherwise"),
                Map.entry("agentGoalAccuracy.verdict.achieved", "GOAL ACHIEVED"),
                Map.entry("agentGoalAccuracy.verdict.notAchieved", "GOAL NOT ACHIEVED"),
                Map.entry("agentGoalAccuracy.level.achieved", "Achieved"),
                Map.entry("agentGoalAccuracy.level.notAchieved", "Not Achieved"),
                Map.entry("agentGoalAccuracy.meaning.achieved", "The agent successfully completed the intended goal"),
                Map.entry("agentGoalAccuracy.meaning.notAchieved", "The agent failed to complete the intended goal"),
                Map.entry("agentGoalAccuracy.scale.achieved", "Goal fully accomplished by the agent"),
                Map.entry("agentGoalAccuracy.scale.notAchieved", "Goal not accomplished - action incomplete or failed"),
                Map.entry("common.notAvailable", "Not available"),

                // ToolCallAccuracy
                Map.entry(
                        "toolCallAccuracy.description",
                        "This metric evaluates accuracy of agent's tool calls against expected reference calls. "
                                + "It compares actual tool invocations with expected ones using F1 score."),
                Map.entry("toolCallAccuracy.step.alignToolCalls.title", "Aligning tool calls"),
                Map.entry(
                        "toolCallAccuracy.step.alignToolCalls.desc",
                        "Matching actual tool calls with reference tool calls based on tool name and arguments."),
                Map.entry("toolCallAccuracy.step.computePrecisionRecall.title", "Computing precision and recall"),
                Map.entry(
                        "toolCallAccuracy.step.computePrecisionRecall.desc",
                        "Calculating precision (correct calls / total actual) and recall (correct calls / total reference)."),
                Map.entry("toolCallAccuracy.step.computeScore.title", "Computing F1 score"),
                Map.entry(
                        "toolCallAccuracy.step.computeScore.desc",
                        "Computing the final F1 score as harmonic mean of precision and recall."),
                Map.entry("toolCallAccuracy.formula", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry("toolCallAccuracy.mode.strict", "STRICT"),
                Map.entry("toolCallAccuracy.mode.flexible", "FLEXIBLE"),
                Map.entry("toolCallAccuracy.modeLabel", "Mode"),
                Map.entry("toolCallAccuracy.precisionLabel", "Precision"),
                Map.entry("toolCallAccuracy.recallLabel", "Recall"),
                Map.entry("toolCallAccuracy.matchedLabel", "Matched"),
                Map.entry("toolCallAccuracy.notMatchedLabel", "Not Matched"),
                Map.entry("toolCallAccuracy.toolName", "Tool"),
                Map.entry("toolCallAccuracy.arguments", "Arguments"),
                Map.entry("toolCallAccuracy.matchScore", "Match Score"),
                Map.entry(
                        "toolCallAccuracy.meaning.excellent",
                        "Excellent - all tool calls match expected calls perfectly"),
                Map.entry("toolCallAccuracy.meaning.good", "Good - most tool calls are correct"),
                Map.entry("toolCallAccuracy.meaning.moderate", "Moderate - some tool calls are incorrect or missing"),
                Map.entry("toolCallAccuracy.meaning.poor", "Poor - many tool calls are incorrect or missing"),
                Map.entry("toolCallAccuracy.scale.excellent", "All tool calls match"),
                Map.entry("toolCallAccuracy.scale.good", "Most tool calls match"),
                Map.entry("toolCallAccuracy.scale.moderate", "Some tool calls match"),
                Map.entry("toolCallAccuracy.scale.poor", "Few tool calls match"),

                // TopicAdherence
                Map.entry(
                        "topicAdherence.description",
                        "This metric evaluates whether conversation topics adhere to expected reference topics. "
                                + "It extracts topics from the conversation and classifies them against reference topics."),
                Map.entry("topicAdherence.step.extractTopics.title", "Extracting topics"),
                Map.entry(
                        "topicAdherence.step.extractTopics.desc",
                        "LLM analyzes the conversation to extract all discussed topics."),
                Map.entry("topicAdherence.step.classifyTopics.title", "Classifying topics"),
                Map.entry(
                        "topicAdherence.step.classifyTopics.desc",
                        "Each extracted topic is classified as on-topic or off-topic against reference topics."),
                Map.entry("topicAdherence.step.computeScore.title", "Computing score"),
                Map.entry(
                        "topicAdherence.step.computeScore.desc",
                        "Computing the final score based on the selected mode (F1, Precision, or Recall)."),
                Map.entry("topicAdherence.conversationLabel", "Conversation"),
                Map.entry("topicAdherence.referenceTopicsLabel", "Reference topics"),
                Map.entry("topicAdherence.extractedTopicsCount", "Topics extracted"),
                Map.entry("topicAdherence.extractedLabel", "Extracted"),
                Map.entry("topicAdherence.onTopicLabel", "On topic"),
                Map.entry("topicAdherence.offTopicLabel", "Off topic"),
                Map.entry("topicAdherence.modeLabel", "Mode"),
                Map.entry("topicAdherence.precisionLabel", "Precision"),
                Map.entry("topicAdherence.recallLabel", "Recall"),
                Map.entry("topicAdherence.mode.f1", "F1 (balanced)"),
                Map.entry("topicAdherence.mode.precision", "Precision"),
                Map.entry("topicAdherence.mode.recall", "Recall"),
                Map.entry("topicAdherence.formula.f1", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry("topicAdherence.formula.precision", "Precision = on-topic / total extracted"),
                Map.entry("topicAdherence.formula.recall", "Recall = covered reference / total reference"),
                Map.entry(
                        "topicAdherence.meaning.excellent",
                        "Excellent - conversation stayed perfectly on topic with all reference topics covered"),
                Map.entry("topicAdherence.meaning.good", "Good - conversation mostly on topic"),
                Map.entry("topicAdherence.meaning.moderate", "Moderate - some off-topic discussions"),
                Map.entry("topicAdherence.meaning.poor", "Poor - significant topic drift"),
                Map.entry("topicAdherence.scale.excellent", "All topics aligned with reference"),
                Map.entry("topicAdherence.scale.good", "Most topics on target"),
                Map.entry("topicAdherence.scale.moderate", "Some topic drift"),
                Map.entry("topicAdherence.scale.poor", "Significant off-topic discussions"),

                // ContextRelevance (NVIDIA-style)
                Map.entry(
                        "contextRelevance.description",
                        "This metric evaluates whether the retrieved contexts are relevant to the user's question. "
                                + "Each context is scored on a 0-2 scale, then normalized to 0-1."),
                Map.entry("contextRelevance.step.evaluateContext.title", "Evaluating context"),
                Map.entry(
                        "contextRelevance.step.evaluateContext.desc",
                        "LLM evaluates context relevance for answering the question."),
                Map.entry("contextRelevance.rawScoreLabel", "Relevance"),
                Map.entry("contextRelevance.verdict.fullyRelevant", "Fully relevant (2/2)"),
                Map.entry("contextRelevance.verdict.partiallyRelevant", "Partially relevant (1/2)"),
                Map.entry("contextRelevance.verdict.notRelevant", "Not relevant (0/2)"),
                Map.entry("contextRelevance.formula", "Average of normalized scores (score/2) across all contexts"),
                Map.entry(
                        "contextRelevance.meaning.excellent", "Excellent - all retrieved contexts are highly relevant"),
                Map.entry("contextRelevance.meaning.good", "Good - most contexts contain relevant information"),
                Map.entry(
                        "contextRelevance.meaning.moderate",
                        "Moderate - contexts have mixed relevance to the question"),
                Map.entry("contextRelevance.meaning.poor", "Poor - contexts are mostly irrelevant to the question"),
                Map.entry("contextRelevance.scale.excellent", "All contexts fully relevant"),
                Map.entry("contextRelevance.scale.good", "Most contexts relevant"),
                Map.entry("contextRelevance.scale.moderate", "Mixed relevance"),
                Map.entry("contextRelevance.scale.poor", "Mostly irrelevant contexts"),
                Map.entry("contextRelevance.avgLabel", "Average"),

                // ResponseGroundedness (NVIDIA-style)
                Map.entry(
                        "responseGroundedness.description",
                        "This metric evaluates whether the response is grounded in (supported by) the retrieved contexts. "
                                + "It uses a 0-2 scoring scale normalized to 0-1."),
                Map.entry("responseGroundedness.step.heuristics.title", "Apply heuristics"),
                Map.entry(
                        "responseGroundedness.step.heuristics.desc",
                        "Check for exact matches or response contained in context."),
                Map.entry("responseGroundedness.step.evaluate.title", "Evaluate groundedness"),
                Map.entry(
                        "responseGroundedness.step.evaluate.desc",
                        "LLM evaluates how well the response is supported by the context."),
                Map.entry("responseGroundedness.rawScoreLabel", "Groundedness"),
                Map.entry("responseGroundedness.heuristics.match", "Heuristic match found - response is grounded"),
                Map.entry("responseGroundedness.heuristics.noMatch", "No heuristic match - using LLM evaluation"),
                Map.entry("responseGroundedness.verdict.fullyGrounded", "Fully grounded (2/2)"),
                Map.entry("responseGroundedness.verdict.partiallyGrounded", "Partially grounded (1/2)"),
                Map.entry("responseGroundedness.verdict.notGrounded", "Not grounded (0/2)"),
                Map.entry("responseGroundedness.formula", "Normalized score = raw_score / 2"),
                Map.entry("responseGroundedness.calculation.heuristic", "Heuristic match = 1.0"),
                Map.entry(
                        "responseGroundedness.meaning.excellent",
                        "Excellent - response is completely supported by context"),
                Map.entry("responseGroundedness.meaning.good", "Good - response is mostly supported by context"),
                Map.entry(
                        "responseGroundedness.meaning.moderate",
                        "Moderate - response is partially supported by context"),
                Map.entry(
                        "responseGroundedness.meaning.poor",
                        "Poor - response contains significant unsupported information"),
                Map.entry("responseGroundedness.scale.excellent", "Response fully grounded in context"),
                Map.entry("responseGroundedness.scale.good", "Response mostly grounded"),
                Map.entry("responseGroundedness.scale.moderate", "Response partially grounded"),
                Map.entry("responseGroundedness.scale.poor", "Response contains unsupported claims"),

                // AnswerAccuracy (NVIDIA-style)
                Map.entry(
                        "answerAccuracy.description",
                        "This metric evaluates whether the AI response accurately matches the reference answer. "
                                + "It uses a 0-2 scoring scale normalized to 0-1."),
                Map.entry("answerAccuracy.step.initial.title", "Initial judgment"),
                Map.entry("answerAccuracy.step.initial.desc", "LLM evaluates accuracy of response against reference."),
                Map.entry("answerAccuracy.step.confirm.title", "Confirmation judgment"),
                Map.entry("answerAccuracy.step.confirm.desc", "Second LLM confirms or adjusts the initial assessment."),
                Map.entry("answerAccuracy.rawScoreLabel", "Accuracy"),
                Map.entry("answerAccuracy.initialAssessmentLabel", "Initial assessment"),
                Map.entry("answerAccuracy.finalScoreLabel", "Final score"),
                Map.entry("answerAccuracy.verdict.fullyCorrect", "Fully correct (2/2)"),
                Map.entry("answerAccuracy.verdict.partiallyCorrect", "Partially correct (1/2)"),
                Map.entry("answerAccuracy.verdict.incorrect", "Incorrect (0/2)"),
                Map.entry("answerAccuracy.formula", "Normalized score = raw_score / 2"),
                Map.entry("answerAccuracy.meaning.excellent", "Excellent - response accurately matches the reference"),
                Map.entry("answerAccuracy.meaning.good", "Good - response mostly matches the reference"),
                Map.entry("answerAccuracy.meaning.moderate", "Moderate - response partially matches the reference"),
                Map.entry("answerAccuracy.meaning.poor", "Poor - response is inaccurate or contradicts the reference"),
                Map.entry("answerAccuracy.scale.excellent", "Response fully matches reference"),
                Map.entry("answerAccuracy.scale.good", "Response mostly matches reference"),
                Map.entry("answerAccuracy.scale.moderate", "Response partially matches reference"),
                Map.entry("answerAccuracy.scale.poor", "Response is inaccurate"),

                // BLEU Score (NLP metric)
                Map.entry(
                        "bleuScore.description",
                        "BLEU (Bilingual Evaluation Understudy) measures n-gram overlap between "
                                + "the response and reference texts. Higher scores indicate better text similarity."),
                Map.entry("bleuScore.step1.title", "Input texts"),
                Map.entry("bleuScore.step1.desc", "The response and reference texts to compare."),
                Map.entry("bleuScore.step2.title", "Configuration"),
                Map.entry("bleuScore.step2.desc", "BLEU metric configuration parameters."),
                Map.entry("bleuScore.step2.output", "Max n-gram: %d, Smoothing: %s"),
                Map.entry("bleuScore.step3.title", "Computing n-gram precision"),
                Map.entry("bleuScore.step3.desc", "Calculating precision for n-grams from 1 to %d."),
                Map.entry("bleuScore.step4.title", "Computing BLEU score"),
                Map.entry("bleuScore.step4.desc", "Combining n-gram precisions with brevity penalty."),
                Map.entry("bleuScore.formula", "BLEU = BP × exp(Σ wₙ × log(pₙ))"),
                Map.entry("bleuScore.meaning.excellent", "Excellent - texts are nearly identical"),
                Map.entry("bleuScore.meaning.good", "Good - high n-gram overlap between texts"),
                Map.entry("bleuScore.meaning.moderate", "Moderate - partial n-gram overlap"),
                Map.entry("bleuScore.meaning.poor", "Poor - low text similarity"),
                Map.entry("bleuScore.scale.excellent", "Near-identical texts"),
                Map.entry("bleuScore.scale.good", "High similarity"),
                Map.entry("bleuScore.scale.moderate", "Moderate similarity"),
                Map.entry("bleuScore.scale.poor", "Low similarity"),

                // ROUGE Score (NLP metric)
                Map.entry(
                        "rougeScore.description",
                        "ROUGE (Recall-Oriented Understudy for Gisting Evaluation) measures overlap "
                                + "between response and reference using unigrams, bigrams, or longest common subsequence."),
                Map.entry("rougeScore.step1.title", "Input texts"),
                Map.entry("rougeScore.step1.desc", "The response and reference texts to compare."),
                Map.entry("rougeScore.step2.title", "Configuration"),
                Map.entry("rougeScore.step2.desc", "ROUGE metric configuration parameters."),
                Map.entry("rougeScore.step2.output", "Type: %s, Mode: %s"),
                Map.entry("rougeScore.step3.title", "Computing %s overlap"),
                Map.entry("rougeScore.rouge1.desc", "Counting unigram (single word) matches between texts."),
                Map.entry("rougeScore.rouge2.desc", "Counting bigram (two consecutive words) matches between texts."),
                Map.entry("rougeScore.rougeL.desc", "Finding longest common subsequence between texts."),
                Map.entry("rougeScore.step4.title", "Computing score"),
                Map.entry("rougeScore.step4.desc", "Calculating %s from overlap statistics."),
                Map.entry("rougeScore.mode.precision", "Precision"),
                Map.entry("rougeScore.mode.recall", "Recall"),
                Map.entry("rougeScore.mode.fmeasure", "F-measure"),
                Map.entry("rougeScore.formula.precision", "Precision = matched / response_length"),
                Map.entry("rougeScore.formula.recall", "Recall = matched / reference_length"),
                Map.entry("rougeScore.formula.fmeasure", "F1 = 2 × (P × R) / (P + R)"),
                Map.entry("rougeScore.meaning.excellent", "Excellent - very high text overlap"),
                Map.entry("rougeScore.meaning.good", "Good - significant text overlap"),
                Map.entry("rougeScore.meaning.moderate", "Moderate - partial text overlap"),
                Map.entry("rougeScore.meaning.poor", "Poor - low text overlap"),
                Map.entry("rougeScore.scale.excellent", "Very high overlap"),
                Map.entry("rougeScore.scale.good", "Good overlap"),
                Map.entry("rougeScore.scale.moderate", "Partial overlap"),
                Map.entry("rougeScore.scale.poor", "Low overlap"),

                // chrF Score (NLP metric)
                Map.entry(
                        "chrfScore.description",
                        "chrF (Character n-gram F-score) measures character-level overlap between texts. "
                                + "chrF++ also includes word n-grams for improved accuracy."),
                Map.entry("chrfScore.step1.title", "Input texts"),
                Map.entry("chrfScore.step1.desc", "The response and reference texts to compare."),
                Map.entry("chrfScore.step2.title", "Configuration"),
                Map.entry("chrfScore.step2.desc", "chrF metric configuration parameters."),
                Map.entry("chrfScore.step2.output", "Variant: %s, Char n-gram: %d, Word n-gram: %d, Beta: %.1f"),
                Map.entry("chrfScore.step3.title", "Computing character n-gram overlap"),
                Map.entry("chrfScore.step3.desc", "Calculating character n-gram matches (1 to %d)."),
                Map.entry("chrfScore.step4.title", "Computing word n-gram overlap"),
                Map.entry("chrfScore.step4.desc", "Calculating word n-gram matches (1 to %d)."),
                Map.entry("chrfScore.step5.title", "Computing chrF score"),
                Map.entry("chrfScore.step5.desc", "Combining character and word scores with beta=%.1f."),
                Map.entry("chrfScore.formula", "chrF = (1 + β²) × (P × R) / (β² × P + R)"),
                Map.entry("chrfScore.meaning.excellent", "Excellent - very high character-level similarity"),
                Map.entry("chrfScore.meaning.good", "Good - high character overlap"),
                Map.entry("chrfScore.meaning.moderate", "Moderate - partial character overlap"),
                Map.entry("chrfScore.meaning.poor", "Poor - low character similarity"),
                Map.entry("chrfScore.scale.excellent", "Near-identical characters"),
                Map.entry("chrfScore.scale.good", "High character similarity"),
                Map.entry("chrfScore.scale.moderate", "Partial character overlap"),
                Map.entry("chrfScore.scale.poor", "Low character similarity"),

                // String Similarity (NLP metric)
                Map.entry(
                        "stringSimilarity.description",
                        "String similarity measures edit distance between texts using algorithms like "
                                + "Levenshtein, Jaro, Jaro-Winkler, or Hamming distance."),
                Map.entry("stringSimilarity.step1.title", "Input texts"),
                Map.entry("stringSimilarity.step1.desc", "The response and reference texts to compare."),
                Map.entry("stringSimilarity.step2.title", "Configuration"),
                Map.entry("stringSimilarity.step2.desc", "String similarity configuration parameters."),
                Map.entry("stringSimilarity.step2.output", "Algorithm: %s, Case sensitive: %s"),
                Map.entry("stringSimilarity.step3.title", "Computing %s similarity"),
                Map.entry("stringSimilarity.caseSensitive.yes", "Yes"),
                Map.entry("stringSimilarity.caseSensitive.no", "No"),
                Map.entry(
                        "stringSimilarity.algorithm.levenshtein",
                        "Counting minimum edits (insert/delete/replace) to transform one text into another."),
                Map.entry(
                        "stringSimilarity.algorithm.hamming",
                        "Counting positions where characters differ (requires equal length strings)."),
                Map.entry(
                        "stringSimilarity.algorithm.jaro",
                        "Computing similarity based on matching characters and transpositions."),
                Map.entry(
                        "stringSimilarity.algorithm.jaroWinkler",
                        "Jaro similarity with prefix bonus for strings that match from the beginning."),
                Map.entry("stringSimilarity.formula.levenshtein", "Similarity = 1 - (edit_distance / max_length)"),
                Map.entry("stringSimilarity.formula.hamming", "Similarity = 1 - (different_positions / length)"),
                Map.entry("stringSimilarity.formula.jaro", "Jaro = (m/|s₁| + m/|s₂| + (m-t)/m) / 3"),
                Map.entry("stringSimilarity.formula.jaroWinkler", "JW = Jaro + (prefix × p × (1 - Jaro))"),
                Map.entry("stringSimilarity.meaning.excellent", "Excellent - strings are nearly identical"),
                Map.entry("stringSimilarity.meaning.good", "Good - strings are very similar"),
                Map.entry("stringSimilarity.meaning.moderate", "Moderate - strings have some similarity"),
                Map.entry("stringSimilarity.meaning.poor", "Poor - strings are quite different"),
                Map.entry("stringSimilarity.scale.excellent", "Near-identical strings"),
                Map.entry("stringSimilarity.scale.good", "Very similar strings"),
                Map.entry("stringSimilarity.scale.moderate", "Some similarity"),
                Map.entry("stringSimilarity.scale.poor", "Different strings"));
    }

    private static Map<String, String> russianMessages() {
        return Map.ofEntries(
                // Common
                Map.entry("common.na", "Н/Д"),
                Map.entry("common.unknown", "Неизвестно"),
                Map.entry("common.scoreNotCalculated", "Скор не вычислен"),
                Map.entry("common.score", "оценка"),
                Map.entry("common.level", "уровень"),
                Map.entry("common.models", "моделей"),

                // Scale levels (standard)
                Map.entry("scale.excellent", "Отлично"),
                Map.entry("scale.good", "Хорошо"),
                Map.entry("scale.moderate", "Удовлетворительно"),
                Map.entry("scale.poor", "Плохо"),
                Map.entry("scale.excellent.desc", "Очень высокий результат"),
                Map.entry("scale.good.desc", "Хороший результат"),
                Map.entry("scale.moderate.desc", "Средний результат"),
                Map.entry("scale.poor.desc", "Низкий результат"),

                // Verdicts
                Map.entry("verdict.faithful", "ВЕРНО"),
                Map.entry("verdict.unfaithful", "НЕВЕРНО"),
                Map.entry("verdict.relevant", "Релевантен"),
                Map.entry("verdict.notRelevant", "Нерелевантен"),
                Map.entry("verdict.found", "Найдено"),
                Map.entry("verdict.missing", "Не найдено"),
                Map.entry("verdict.pass", "PASS"),
                Map.entry("verdict.fail", "FAIL"),
                Map.entry("verdict.ok", "OK"),
                Map.entry("verdict.error", "Ошибка"),

                // Step titles - common
                Map.entry("step.computeScore", "Расчёт итогового скора"),

                // Faithfulness
                Map.entry(
                        "faithfulness.description",
                        "Метрика проверяет: не выдумал ли AI что-то от себя? "
                                + "Мы разбиваем ответ на отдельные факты и проверяем каждый по контексту."),
                Map.entry("faithfulness.step1.title", "Разбиение ответа на утверждения"),
                Map.entry(
                        "faithfulness.step1.desc", "Мы взяли ответ AI и разбили его на отдельные факты (утверждения)."),
                Map.entry("faithfulness.step1.output", "Получено %d утверждений"),
                Map.entry("faithfulness.step2.title", "Проверка утверждений по контексту"),
                Map.entry(
                        "faithfulness.step2.desc",
                        "Для каждого утверждения проверили: можно ли его найти в предоставленном контексте?"),
                Map.entry("faithfulness.step2.output", "%d из %d утверждений подтверждены"),
                Map.entry("faithfulness.step3.title", "Расчёт итогового скора"),
                Map.entry("faithfulness.step3.desc", "Делим количество верных утверждений на общее количество."),
                Map.entry("faithfulness.formula", "среднее((верные утверждения) / (всего утверждений) по моделям)"),
                Map.entry("faithfulness.meaning.excellent", "Отлично - AI опирается только на факты из контекста"),
                Map.entry("faithfulness.meaning.good", "Хорошо - большинство утверждений подтверждены контекстом"),
                Map.entry("faithfulness.meaning.moderate", "Средне - часть информации не подтверждена контекстом"),
                Map.entry("faithfulness.meaning.poor", "Плохо - много \"галлюцинаций\" (выдуманных фактов)"),
                Map.entry("faithfulness.scale.excellent", "Все утверждения подтверждены контекстом"),
                Map.entry("faithfulness.scale.good", "Большинство утверждений подтверждены"),
                Map.entry("faithfulness.scale.moderate", "Часть утверждений не подтверждена"),
                Map.entry("faithfulness.scale.poor", "Много галлюцинаций"),

                // AspectCritic
                Map.entry(
                        "aspectCritic.description",
                        "Бинарная проверка: соответствует ли ответ заданному критерию? Результат: PASS или FAIL."),
                Map.entry("aspectCritic.step1.title", "Пользовательский аспект"),
                Map.entry("aspectCritic.step1.desc", "Критерий, заданный пользователем для проверки."),
                Map.entry("aspectCritic.step2.title", "Оценка аспекта"),
                Map.entry("aspectCritic.step2.desc", "LLM проанализировал ответ на соответствие критерию."),
                Map.entry("aspectCritic.step3.majority.title", "Majority Voting (strictness)"),
                Map.entry(
                        "aspectCritic.step3.majority.desc",
                        "Каждая модель вызывалась %d раз(а). Финальный результат определяется большинством голосов."),
                Map.entry("aspectCritic.step3.title", "Расчёт скора"),
                Map.entry("aspectCritic.step3.desc", "PASS → 1.0, FAIL → 0.0"),
                Map.entry("aspectCritic.meaning.pass", "Критерий \"%s\" ВЫПОЛНЕН"),
                Map.entry("aspectCritic.meaning.fail", "Критерий \"%s\" НЕ ВЫПОЛНЕН"),
                Map.entry("aspectCritic.scale.pass", "Критерий выполнен"),
                Map.entry("aspectCritic.scale.fail", "Критерий не выполнен"),
                Map.entry("aspectCritic.defaultAspect", "Пользовательский аспект"),

                // ContextPrecision
                Map.entry(
                        "contextPrecision.description",
                        "Метрика проверяет: правильно ли отранжированы контексты? "
                                + "Релевантные контексты должны идти первыми в списке."),
                Map.entry("contextPrecision.step1.title", "Оценка релевантности контекстов"),
                Map.entry(
                        "contextPrecision.step1.desc",
                        "Для каждого контекста проверяем: релевантен ли он для ответа на вопрос?"),
                Map.entry("contextPrecision.step1.output", "%d из %d релевантны"),
                Map.entry("contextPrecision.step2.title", "Расчёт Precision@K"),
                Map.entry("contextPrecision.step2.desc", "Precision@K = (релевантных в топ-K) / K"),
                Map.entry("contextPrecision.step2.output", "Precision рассчитан для %d контекстов (%d релевантных)"),
                Map.entry("contextPrecision.step3.title", "Расчёт Average Precision"),
                Map.entry(
                        "contextPrecision.step3.desc",
                        "AP = среднее Precision@K только для позиций с релевантными контекстами"),
                Map.entry("contextPrecision.formula", "AP = Σ(Precision@k × relevance@k) / (кол-во релевантных)"),
                Map.entry("contextPrecision.meaning.excellent", "Отлично - релевантные контексты идут первыми"),
                Map.entry(
                        "contextPrecision.meaning.good", "Хорошо - большинство релевантных контекстов в начале списка"),
                Map.entry("contextPrecision.meaning.moderate", "Средне - ранжирование требует улучшения"),
                Map.entry(
                        "contextPrecision.meaning.poor", "Плохо - релевантные контексты затеряны среди нерелевантных"),

                // ContextRecall
                Map.entry(
                        "contextRecall.description",
                        "Метрика проверяет: покрывают ли контексты всю информацию из эталонного ответа? "
                                + "Каждое предложение эталона должно найтись в контекстах."),
                Map.entry("contextRecall.step1.title", "Проверка покрытия эталона контекстами"),
                Map.entry(
                        "contextRecall.step1.desc",
                        "Для каждого предложения из эталонного ответа проверяем: "
                                + "можно ли его найти в предоставленных контекстах?"),
                Map.entry("contextRecall.step1.output", "%d из %d найдены в контекстах"),
                Map.entry("contextRecall.step2.title", "Расчёт покрытия"),
                Map.entry("contextRecall.step2.desc", "Доля предложений эталона, найденных в контекстах."),
                Map.entry("contextRecall.formula", "среднее((найденные предложения) / (всего предложений) по моделям)"),
                Map.entry("contextRecall.meaning.excellent", "Отлично - контексты полностью покрывают эталонный ответ"),
                Map.entry("contextRecall.meaning.good", "Хорошо - большая часть эталона найдена в контекстах"),
                Map.entry("contextRecall.meaning.moderate", "Средне - часть информации отсутствует в контекстах"),
                Map.entry("contextRecall.meaning.poor", "Плохо - контексты не содержат значительную часть эталона"),
                Map.entry("contextRecall.scale.excellent", "Полное покрытие эталона"),
                Map.entry("contextRecall.scale.good", "Хорошее покрытие"),
                Map.entry("contextRecall.scale.moderate", "Частичное покрытие"),
                Map.entry("contextRecall.scale.poor", "Недостаточное покрытие"),

                // ContextEntityRecall
                Map.entry(
                        "contextEntityRecall.description",
                        "Метрика проверяет: содержат ли контексты все важные сущности из эталона? "
                                + "Сущности - это имена, даты, места, организации и т.д."),
                Map.entry("contextEntityRecall.step1.title", "Извлечение сущностей из эталона"),
                Map.entry(
                        "contextEntityRecall.step1.desc",
                        "Находим все важные сущности в эталонном ответе: имена, даты, места и т.д."),
                Map.entry("contextEntityRecall.step1.output", "Найдено %d сущностей"),
                Map.entry("contextEntityRecall.step2.title", "Извлечение сущностей из контекстов"),
                Map.entry("contextEntityRecall.step2.desc", "Находим все сущности в предоставленных контекстах."),
                Map.entry("contextEntityRecall.step3.title", "Сравнение сущностей"),
                Map.entry("contextEntityRecall.step3.desc", "Какие сущности из эталона найдены в контекстах?"),
                Map.entry("contextEntityRecall.step3.output", "%d из %d найдены"),
                Map.entry("contextEntityRecall.step4.title", "Расчёт покрытия сущностей"),
                Map.entry("contextEntityRecall.step4.desc", "Доля сущностей эталона, найденных в контекстах."),
                Map.entry(
                        "contextEntityRecall.formula", "среднее((найденные сущности) / (всего сущностей) по моделям)"),
                Map.entry("contextEntityRecall.meaning.excellent", "Отлично - контексты содержат все важные сущности"),
                Map.entry("contextEntityRecall.meaning.good", "Хорошо - большинство сущностей присутствует"),
                Map.entry("contextEntityRecall.meaning.moderate", "Средне - часть сущностей отсутствует"),
                Map.entry(
                        "contextEntityRecall.meaning.poor", "Плохо - много важных сущностей отсутствует в контекстах"),

                // NoiseSensitivity
                Map.entry(
                        "noiseSensitivity.description",
                        "Метрика проверяет: влияет ли нерелевантная информация на ответ Агентной системы? "
                                + "Чем ниже значение, тем лучше система игнорирует посторонний контекст."),
                Map.entry("noiseSensitivity.step1.title", "Разбиение эталона на утверждения"),
                Map.entry("noiseSensitivity.step1.desc", "Эталонный ответ разбивается на отдельные факты."),
                Map.entry("noiseSensitivity.step1.output", "Утверждения эталона"),
                Map.entry("noiseSensitivity.step2.title", "Разбиение ответа AI на утверждения"),
                Map.entry("noiseSensitivity.step2.desc", "Ответ Агентной системы разбивается на отдельные факты."),
                Map.entry("noiseSensitivity.step2.output", "Утверждения ответа"),
                Map.entry("noiseSensitivity.step3.title", "Анализ соответствия и источников"),
                Map.entry(
                        "noiseSensitivity.step3.desc",
                        "Проверяем, совпадают ли утверждения ответа с эталоном и откуда взята информация."),
                Map.entry("noiseSensitivity.step3.output", "%d ошибок из %d проверок"),
                Map.entry("noiseSensitivity.step4.title", "Расчёт чувствительности к шуму"),
                Map.entry(
                        "noiseSensitivity.step4.desc",
                        "Доля ошибок, вызванных нерелевантным контекстом. Меньше = лучше."),
                Map.entry("noiseSensitivity.formula", "среднее((ошибки от шума / всего проверок) по моделям)"),
                Map.entry(
                        "noiseSensitivity.meaning.excellent",
                        "ОТЛИЧНО - Система полностью игнорирует нерелевантный контекст"),
                Map.entry("noiseSensitivity.meaning.good", "ХОРОШО - Минимальное влияние постороннего контекста"),
                Map.entry("noiseSensitivity.meaning.moderate", "СРЕДНЕ - Заметное влияние постороннего контекста"),
                Map.entry("noiseSensitivity.meaning.poor", "ПЛОХО - Ответ сильно искажён нерелевантной информацией"),
                Map.entry("noiseSensitivity.scale.excellent", "Нет влияния шума (ОТЛИЧНО)"),
                Map.entry("noiseSensitivity.scale.good", "Минимальное влияние"),
                Map.entry("noiseSensitivity.scale.moderate", "Заметное влияние"),
                Map.entry("noiseSensitivity.scale.poor", "Сильное искажение ответа"),

                // ResponseRelevancy
                Map.entry(
                        "responseRelevancy.description",
                        "Метрика проверяет: действительно ли ответ AI отвечает на вопрос пользователя? "
                                + "Генерируем вопросы на основе ответа и сравниваем их с оригинальным запросом."),
                Map.entry("responseRelevancy.step1.title", "Исходный вопрос пользователя"),
                Map.entry("responseRelevancy.step1.desc", "Запрос от пользователя к Агентной системе."),
                Map.entry("responseRelevancy.step2.title", "Генерация вопросов из ответа"),
                Map.entry(
                        "responseRelevancy.step2.desc",
                        "LLM генерирует вопросы, на которые данный ответ мог бы являться ответом."),
                Map.entry("responseRelevancy.step2.output", "Сгенерированные вопросы"),
                Map.entry("responseRelevancy.step3.title", "Расчёт семантического сходства"),
                Map.entry(
                        "responseRelevancy.step3.desc",
                        "Сравниваем сгенерированные вопросы с оригинальным через эмбеддинги. "
                                + "Чем больше похожи вопросы - тем релевантнее ответ."),
                Map.entry("responseRelevancy.step4.title", "Расчёт среднего сходства"),
                Map.entry(
                        "responseRelevancy.step4.desc",
                        "Итоговый скор = среднее значение сходства всех сгенерированных вопросов."),
                Map.entry("responseRelevancy.formula", "mean(cosine_similarity(оригинал, сгенерированные))"),
                Map.entry("responseRelevancy.meaning.excellent", "Отлично - ответ напрямую отвечает на вопрос"),
                Map.entry("responseRelevancy.meaning.good", "Хорошо - ответ в основном релевантен вопросу"),
                Map.entry("responseRelevancy.meaning.moderate", "Средне - ответ частично отвечает на вопрос"),
                Map.entry("responseRelevancy.meaning.poor", "Плохо - ответ не по теме"),
                Map.entry("responseRelevancy.scale.excellent", "Ответ напрямую отвечает на вопрос"),
                Map.entry("responseRelevancy.scale.good", "Ответ в основном релевантен"),
                Map.entry("responseRelevancy.scale.moderate", "Ответ частично отвечает на вопрос"),
                Map.entry("responseRelevancy.scale.poor", "Ответ не по теме"),

                // SimpleCriteria
                Map.entry(
                        "simpleCriteria.description",
                        "Оценка по пользовательскому критерию на непрерывной шкале. "
                                + "Интерпретация зависит от ваших требований к критерию."),
                Map.entry("simpleCriteria.step1.title", "Пользовательский критерий"),
                Map.entry("simpleCriteria.step1.desc", "Критерий, заданный пользователем для оценки."),
                Map.entry("simpleCriteria.step2.title", "Оценка LLM"),
                Map.entry("simpleCriteria.step2.desc", "LLM оценивает ответ по критерию на шкале от %d до %d."),
                Map.entry("simpleCriteria.step3.title", "Нормализация скора"),
                Map.entry("simpleCriteria.step3.desc", "Приводим оценку к шкале 0-1."),
                Map.entry(
                        "simpleCriteria.meaning",
                        "Оценка: %d из %d по критерию \"%s\". Интерпретация зависит от ваших требований."),
                Map.entry("simpleCriteria.defaultCriteria", "Пользовательский критерий"),

                // RubricsScore
                Map.entry(
                        "rubricsScore.description",
                        "Оценка по рубрикам - LLM выбирает уровень из заранее определённой шкалы "
                                + "с описанием каждого уровня."),
                Map.entry("rubricsScore.step1.title", "Шкала оценки (рубрика)"),
                Map.entry("rubricsScore.step1.desc", "Пользователь определил шкалу с описанием каждого уровня."),
                Map.entry("rubricsScore.step2.title", "LLM as a Judge"),
                Map.entry("rubricsScore.step2.desc", "LLM проанализировал ответ и выбрал подходящий уровень."),
                Map.entry("rubricsScore.step2.output", "Уровень %d: %s"),
                Map.entry("rubricsScore.step3.title", "Расчёт скора"),
                Map.entry("rubricsScore.step3.desc", "Нормализуем выбранный уровень к шкале 0-1."),
                Map.entry("rubricsScore.meaning", "Выбран уровень %d: %s"),
                Map.entry("rubricsScore.level", "Уровень %d"),

                // SemanticSimilarity
                Map.entry(
                        "semanticSimilarity.description",
                        "Метрика измеряет семантическое сходство между ответом AI и эталоном. "
                                + "Используются модели эмбеддингов для векторного представления и косинусного сходства."),
                Map.entry("semanticSimilarity.response", "Ответ"),
                Map.entry("semanticSimilarity.reference", "Эталон"),
                Map.entry("semanticSimilarity.step1.title", "Входные тексты"),
                Map.entry("semanticSimilarity.step1.desc", "Ответ и эталонный текст для сравнения."),
                Map.entry("semanticSimilarity.step2.title", "Вычисление эмбеддингов"),
                Map.entry(
                        "semanticSimilarity.step2.desc",
                        "Преобразование ответа и эталона в векторные представления с помощью моделей эмбеддингов."),
                Map.entry("semanticSimilarity.step2.output", "Эмбеддинги вычислены %d моделями"),
                Map.entry("semanticSimilarity.step3.title", "Вычисление косинусного сходства"),
                Map.entry(
                        "semanticSimilarity.step3.desc",
                        "Расчёт косинусного сходства между эмбеддингами ответа и эталона."),
                Map.entry("semanticSimilarity.step3.outputThreshold", "%s (порог: %.2f)"),
                Map.entry("semanticSimilarity.formula", "cosine_similarity(embed(ответ), embed(эталон))"),
                Map.entry(
                        "semanticSimilarity.meaning.excellent", "Отлично - ответ семантически почти идентичен эталону"),
                Map.entry("semanticSimilarity.meaning.good", "Хорошо - ответ передаёт схожий смысл с эталоном"),
                Map.entry("semanticSimilarity.meaning.moderate", "Средне - ответ частично связан с эталоном"),
                Map.entry("semanticSimilarity.meaning.poor", "Плохо - ответ значительно отличается от эталона"),
                Map.entry("semanticSimilarity.meaning.passThreshold", "PASS - сходство >= порога (%.2f)"),
                Map.entry("semanticSimilarity.meaning.failThreshold", "FAIL - сходство < порога (%.2f)"),
                Map.entry("semanticSimilarity.scale.excellent", "Почти идентичный смысл"),
                Map.entry("semanticSimilarity.scale.good", "Схожий смысл"),
                Map.entry("semanticSimilarity.scale.moderate", "Частично связаны"),
                Map.entry("semanticSimilarity.scale.poor", "Разный смысл"),

                // FactualCorrectness
                Map.entry(
                        "factualCorrectness.description",
                        "Метрика проверяет: верны ли факты в ответе AI? "
                                + "Мы разбиваем ответ и эталон на атомарные утверждения и проверяем каждое с помощью NLI."),
                Map.entry("factualCorrectness.step1.title", "Разбиение ответа на утверждения"),
                Map.entry(
                        "factualCorrectness.step1.desc",
                        "Ответ AI разбивается на отдельные атомарные утверждения, которые можно проверить."),
                Map.entry("factualCorrectness.step1.output", "Извлечено %d утверждений из ответа"),
                Map.entry("factualCorrectness.step2.title", "Разбиение эталона на утверждения"),
                Map.entry(
                        "factualCorrectness.step2.desc",
                        "Эталонный ответ разбивается на отдельные атомарные утверждения."),
                Map.entry("factualCorrectness.step2.output", "Извлечено %d утверждений из эталона"),
                Map.entry("factualCorrectness.step3.title", "Проверка утверждений с помощью NLI"),
                Map.entry(
                        "factualCorrectness.step3.desc",
                        "Каждое утверждение проверяется методом NLI: ПОДТВЕРЖДЕНО, ПРОТИВОРЕЧИТ или НЕЙТРАЛЬНО."),
                Map.entry(
                        "factualCorrectness.step3.output", "Precision: %d/%d подтверждено, Recall: %d/%d подтверждено"),
                Map.entry("factualCorrectness.step4.title", "Расчёт итогового скора"),
                Map.entry("factualCorrectness.step4.desc", "Вычисляем %s скор из precision и recall."),
                Map.entry("factualCorrectness.formula", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry("factualCorrectness.formula.f1", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry(
                        "factualCorrectness.formula.precision",
                        "Precision = подтверждённые утверждения ответа / всего утверждений ответа"),
                Map.entry(
                        "factualCorrectness.formula.recall",
                        "Recall = подтверждённые утверждения эталона / всего утверждений эталона"),
                Map.entry("factualCorrectness.meaning.excellent", "Отлично - все утверждения фактически верны и полны"),
                Map.entry(
                        "factualCorrectness.meaning.good",
                        "Хорошо - большинство утверждений верны с незначительными пропусками"),
                Map.entry("factualCorrectness.meaning.moderate", "Средне - часть фактов неверна или отсутствует"),
                Map.entry(
                        "factualCorrectness.meaning.poor",
                        "Плохо - много фактических ошибок или значительные пропуски"),
                Map.entry("factualCorrectness.scale.excellent", "Все утверждения подтверждены как верные"),
                Map.entry("factualCorrectness.scale.good", "Большинство утверждений верны"),
                Map.entry("factualCorrectness.scale.moderate", "Часть утверждений неверна или отсутствует"),
                Map.entry("factualCorrectness.scale.poor", "Много фактических ошибок"),

                // NLI Verdicts
                Map.entry("verdict.supported", "ПОДТВЕРЖДЕНО"),
                Map.entry("verdict.contradicted", "ПРОТИВОРЕЧИТ"),
                Map.entry("verdict.neutral", "НЕЙТРАЛЬНО"),

                // AnswerCorrectness
                Map.entry(
                        "answerCorrectness.description",
                        "Метрика объединяет фактическую корректность и семантическое сходство для оценки качества ответа. "
                                + "Учитывает как правильность фактов, так и сохранение смысла."),
                Map.entry("answerCorrectness.step1.title", "Входные тексты"),
                Map.entry("answerCorrectness.step1.desc", "Ответ и эталонный текст для сравнения."),
                Map.entry("answerCorrectness.step2.title", "Вычисление фактической корректности"),
                Map.entry(
                        "answerCorrectness.step2.desc",
                        "Оценка фактической корректности с помощью декомпозиции утверждений и NLI-верификации."),
                Map.entry("answerCorrectness.step3.title", "Вычисление семантического сходства"),
                Map.entry(
                        "answerCorrectness.step3.desc",
                        "Расчёт семантического сходства с использованием эмбеддингов и косинусного сходства."),
                Map.entry("answerCorrectness.step4.title", "Комбинирование оценок"),
                Map.entry(
                        "answerCorrectness.step4.desc",
                        "Вычисление взвешенного среднего фактической и семантической оценок."),
                Map.entry("answerCorrectness.factualScore", "Фактическая корректность"),
                Map.entry("answerCorrectness.semanticScore", "Семантическое сходство"),
                Map.entry("answerCorrectness.factual", "факт"),
                Map.entry("answerCorrectness.semantic", "семант"),
                Map.entry("common.response", "Ответ"),
                Map.entry("common.reference", "Эталон"),
                Map.entry(
                        "answerCorrectness.meaning.excellent",
                        "Отлично - ответ фактически корректен и семантически согласован"),
                Map.entry("answerCorrectness.meaning.good", "Хорошо - ответ в основном корректен с высоким сходством"),
                Map.entry("answerCorrectness.meaning.moderate", "Средне - некоторые проблемы с фактами или семантикой"),
                Map.entry(
                        "answerCorrectness.meaning.poor",
                        "Плохо - значительные фактические ошибки или семантическое несоответствие"),
                Map.entry("answerCorrectness.scale.excellent", "Фактически корректен и семантически согласован"),
                Map.entry("answerCorrectness.scale.good", "В основном корректен с хорошим сходством"),
                Map.entry("answerCorrectness.scale.moderate", "Проблемы с фактами или смыслом"),
                Map.entry("answerCorrectness.scale.poor", "Значительные ошибки или несоответствие"),

                // Message types for multi-turn conversations
                Map.entry("message.type.human", "Пользователь"),
                Map.entry("message.type.ai", "Ассистент"),
                Map.entry("message.type.tool", "Результат инструмента"),
                Map.entry("message.toolCalls", "Вызовы инструментов"),

                // AgentGoalAccuracy
                Map.entry(
                        "agentGoalAccuracy.description",
                        "Метрика оценивает, достиг ли AI-агент поставленной цели. "
                                + "Анализирует многоходовые диалоги для определения достижения цели."),
                Map.entry("agentGoalAccuracy.step.inferGoal.title", "Вывод цели из диалога"),
                Map.entry(
                        "agentGoalAccuracy.step.inferGoal.desc",
                        "LLM анализирует диалог, чтобы понять, какова была основная цель пользователя."),
                Map.entry("agentGoalAccuracy.step.evaluateOutcome.title", "Оценка результата"),
                Map.entry(
                        "agentGoalAccuracy.step.evaluateOutcome.desc",
                        "Определение того, успешно ли действия агента достигли выведенной цели."),
                Map.entry("agentGoalAccuracy.step.compareOutcome.title", "Сравнение с ожидаемым результатом"),
                Map.entry(
                        "agentGoalAccuracy.step.compareOutcome.desc",
                        "Сравнение результата диалога с предоставленной ожидаемой целью."),
                Map.entry("agentGoalAccuracy.goal", "Цель"),
                Map.entry("agentGoalAccuracy.expectedOutcome", "Ожидаемый результат"),
                Map.entry("agentGoalAccuracy.formula", "Бинарно: 1.0 если цель достигнута, 0.0 иначе"),
                Map.entry("agentGoalAccuracy.verdict.achieved", "ЦЕЛЬ ДОСТИГНУТА"),
                Map.entry("agentGoalAccuracy.verdict.notAchieved", "ЦЕЛЬ НЕ ДОСТИГНУТА"),
                Map.entry("agentGoalAccuracy.level.achieved", "Достигнуто"),
                Map.entry("agentGoalAccuracy.level.notAchieved", "Не достигнуто"),
                Map.entry("agentGoalAccuracy.meaning.achieved", "Агент успешно выполнил поставленную цель"),
                Map.entry("agentGoalAccuracy.meaning.notAchieved", "Агент не смог выполнить поставленную цель"),
                Map.entry("agentGoalAccuracy.scale.achieved", "Цель полностью выполнена агентом"),
                Map.entry("agentGoalAccuracy.scale.notAchieved", "Цель не выполнена - действие неполное или неудачное"),
                Map.entry("common.notAvailable", "Недоступно"),

                // ToolCallAccuracy
                Map.entry(
                        "toolCallAccuracy.description",
                        "Метрика оценивает точность вызовов инструментов агента относительно ожидаемых эталонных вызовов. "
                                + "Сравнивает фактические вызовы инструментов с ожидаемыми с помощью F1-оценки."),
                Map.entry("toolCallAccuracy.step.alignToolCalls.title", "Сопоставление вызовов инструментов"),
                Map.entry(
                        "toolCallAccuracy.step.alignToolCalls.desc",
                        "Сопоставление фактических вызовов с эталонными по имени инструмента и аргументам."),
                Map.entry("toolCallAccuracy.step.computePrecisionRecall.title", "Вычисление precision и recall"),
                Map.entry(
                        "toolCallAccuracy.step.computePrecisionRecall.desc",
                        "Расчёт precision (верные вызовы / всего фактических) и recall (верные вызовы / всего эталонных)."),
                Map.entry("toolCallAccuracy.step.computeScore.title", "Вычисление F1-оценки"),
                Map.entry(
                        "toolCallAccuracy.step.computeScore.desc",
                        "Вычисление итоговой F1-оценки как гармонического среднего precision и recall."),
                Map.entry("toolCallAccuracy.formula", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry("toolCallAccuracy.mode.strict", "СТРОГИЙ"),
                Map.entry("toolCallAccuracy.mode.flexible", "ГИБКИЙ"),
                Map.entry("toolCallAccuracy.modeLabel", "Режим"),
                Map.entry("toolCallAccuracy.precisionLabel", "Precision"),
                Map.entry("toolCallAccuracy.recallLabel", "Recall"),
                Map.entry("toolCallAccuracy.matchedLabel", "Совпадает"),
                Map.entry("toolCallAccuracy.notMatchedLabel", "Не совпадает"),
                Map.entry("toolCallAccuracy.toolName", "Инструмент"),
                Map.entry("toolCallAccuracy.arguments", "Аргументы"),
                Map.entry("toolCallAccuracy.matchScore", "Оценка совпадения"),
                Map.entry(
                        "toolCallAccuracy.meaning.excellent",
                        "Отлично - все вызовы инструментов точно совпадают с ожидаемыми"),
                Map.entry("toolCallAccuracy.meaning.good", "Хорошо - большинство вызовов инструментов корректны"),
                Map.entry(
                        "toolCallAccuracy.meaning.moderate",
                        "Средне - часть вызовов инструментов некорректна или отсутствует"),
                Map.entry(
                        "toolCallAccuracy.meaning.poor",
                        "Плохо - много вызовов инструментов некорректно или отсутствует"),
                Map.entry("toolCallAccuracy.scale.excellent", "Все вызовы совпадают"),
                Map.entry("toolCallAccuracy.scale.good", "Большинство вызовов совпадают"),
                Map.entry("toolCallAccuracy.scale.moderate", "Часть вызовов совпадает"),
                Map.entry("toolCallAccuracy.scale.poor", "Мало вызовов совпадает"),

                // TopicAdherence
                Map.entry(
                        "topicAdherence.description",
                        "Метрика оценивает, соответствуют ли темы разговора ожидаемым референсным темам. "
                                + "Извлекает темы из диалога и классифицирует их относительно эталонных тем."),
                Map.entry("topicAdherence.step.extractTopics.title", "Извлечение тем"),
                Map.entry(
                        "topicAdherence.step.extractTopics.desc",
                        "LLM анализирует диалог для извлечения всех обсуждаемых тем."),
                Map.entry("topicAdherence.step.classifyTopics.title", "Классификация тем"),
                Map.entry(
                        "topicAdherence.step.classifyTopics.desc",
                        "Каждая извлечённая тема классифицируется как соответствующая или не соответствующая эталонным темам."),
                Map.entry("topicAdherence.step.computeScore.title", "Вычисление оценки"),
                Map.entry(
                        "topicAdherence.step.computeScore.desc",
                        "Вычисление итоговой оценки на основе выбранного режима (F1, Precision или Recall)."),
                Map.entry("topicAdherence.conversationLabel", "Диалог"),
                Map.entry("topicAdherence.referenceTopicsLabel", "Референсные темы"),
                Map.entry("topicAdherence.extractedTopicsCount", "Извлечено тем"),
                Map.entry("topicAdherence.extractedLabel", "Извлечено"),
                Map.entry("topicAdherence.onTopicLabel", "По теме"),
                Map.entry("topicAdherence.offTopicLabel", "Не по теме"),
                Map.entry("topicAdherence.modeLabel", "Режим"),
                Map.entry("topicAdherence.precisionLabel", "Precision"),
                Map.entry("topicAdherence.recallLabel", "Recall"),
                Map.entry("topicAdherence.mode.f1", "F1 (сбалансированный)"),
                Map.entry("topicAdherence.mode.precision", "Precision"),
                Map.entry("topicAdherence.mode.recall", "Recall"),
                Map.entry("topicAdherence.formula.f1", "F1 = 2 × (precision × recall) / (precision + recall)"),
                Map.entry("topicAdherence.formula.precision", "Precision = по теме / всего извлечено"),
                Map.entry("topicAdherence.formula.recall", "Recall = покрыто референсных / всего референсных"),
                Map.entry(
                        "topicAdherence.meaning.excellent",
                        "Отлично - диалог полностью по теме, все референсные темы охвачены"),
                Map.entry("topicAdherence.meaning.good", "Хорошо - диалог в основном по теме"),
                Map.entry("topicAdherence.meaning.moderate", "Средне - есть отклонения от темы"),
                Map.entry("topicAdherence.meaning.poor", "Плохо - значительные отклонения от темы"),
                Map.entry("topicAdherence.scale.excellent", "Все темы соответствуют эталону"),
                Map.entry("topicAdherence.scale.good", "Большинство тем соответствуют"),
                Map.entry("topicAdherence.scale.moderate", "Есть отклонения от темы"),
                Map.entry("topicAdherence.scale.poor", "Значительные отклонения от темы"),

                // ContextRelevance (NVIDIA-style)
                Map.entry(
                        "contextRelevance.description",
                        "Метрика оценивает, релевантны ли извлечённые контексты вопросу пользователя. "
                                + "Каждый контекст оценивается по шкале 0-2, затем нормализуется к 0-1."),
                Map.entry("contextRelevance.step.evaluateContext.title", "Оценка контекста"),
                Map.entry(
                        "contextRelevance.step.evaluateContext.desc",
                        "LLM оценивает релевантность контекста для ответа на вопрос."),
                Map.entry("contextRelevance.rawScoreLabel", "Релевантность"),
                Map.entry("contextRelevance.verdict.fullyRelevant", "Полностью релевантен (2/2)"),
                Map.entry("contextRelevance.verdict.partiallyRelevant", "Частично релевантен (1/2)"),
                Map.entry("contextRelevance.verdict.notRelevant", "Нерелевантен (0/2)"),
                Map.entry("contextRelevance.formula", "Среднее нормализованных оценок (оценка/2) по всем контекстам"),
                Map.entry(
                        "contextRelevance.meaning.excellent", "Отлично - все извлечённые контексты высоко релевантны"),
                Map.entry(
                        "contextRelevance.meaning.good",
                        "Хорошо - большинство контекстов содержат релевантную информацию"),
                Map.entry(
                        "contextRelevance.meaning.moderate",
                        "Средне - контексты имеют смешанную релевантность к вопросу"),
                Map.entry("contextRelevance.meaning.poor", "Плохо - контексты в основном нерелевантны вопросу"),
                Map.entry("contextRelevance.scale.excellent", "Все контексты полностью релевантны"),
                Map.entry("contextRelevance.scale.good", "Большинство контекстов релевантны"),
                Map.entry("contextRelevance.scale.moderate", "Смешанная релевантность"),
                Map.entry("contextRelevance.scale.poor", "В основном нерелевантные контексты"),
                Map.entry("contextRelevance.avgLabel", "Среднее"),

                // ResponseGroundedness (NVIDIA-style)
                Map.entry(
                        "responseGroundedness.description",
                        "Метрика оценивает, обоснован ли ответ извлечёнными контекстами. "
                                + "Использует шкалу 0-2, нормализованную к 0-1."),
                Map.entry("responseGroundedness.step.heuristics.title", "Применение эвристик"),
                Map.entry(
                        "responseGroundedness.step.heuristics.desc",
                        "Проверка точных совпадений или содержания ответа в контексте."),
                Map.entry("responseGroundedness.step.evaluate.title", "Оценка обоснованности"),
                Map.entry(
                        "responseGroundedness.step.evaluate.desc",
                        "LLM оценивает, насколько ответ подтверждается контекстом."),
                Map.entry("responseGroundedness.rawScoreLabel", "Обоснованность"),
                Map.entry("responseGroundedness.heuristics.match", "Эвристика сработала - ответ обоснован"),
                Map.entry(
                        "responseGroundedness.heuristics.noMatch", "Эвристика не сработала - используется LLM оценка"),
                Map.entry("responseGroundedness.verdict.fullyGrounded", "Полностью обоснован (2/2)"),
                Map.entry("responseGroundedness.verdict.partiallyGrounded", "Частично обоснован (1/2)"),
                Map.entry("responseGroundedness.verdict.notGrounded", "Необоснован (0/2)"),
                Map.entry("responseGroundedness.formula", "Нормализованная оценка = сырая_оценка / 2"),
                Map.entry("responseGroundedness.calculation.heuristic", "Эвристическое совпадение = 1.0"),
                Map.entry(
                        "responseGroundedness.meaning.excellent",
                        "Отлично - ответ полностью подтверждается контекстом"),
                Map.entry("responseGroundedness.meaning.good", "Хорошо - ответ в основном подтверждается контекстом"),
                Map.entry("responseGroundedness.meaning.moderate", "Средне - ответ частично подтверждается контекстом"),
                Map.entry(
                        "responseGroundedness.meaning.poor",
                        "Плохо - ответ содержит значительную неподтверждённую информацию"),
                Map.entry("responseGroundedness.scale.excellent", "Ответ полностью обоснован контекстом"),
                Map.entry("responseGroundedness.scale.good", "Ответ в основном обоснован"),
                Map.entry("responseGroundedness.scale.moderate", "Ответ частично обоснован"),
                Map.entry("responseGroundedness.scale.poor", "Ответ содержит неподтверждённые утверждения"),

                // AnswerAccuracy (NVIDIA-style)
                Map.entry(
                        "answerAccuracy.description",
                        "Метрика оценивает, насколько точно ответ AI соответствует эталонному ответу. "
                                + "Использует шкалу 0-2, нормализованную к 0-1."),
                Map.entry("answerAccuracy.step.initial.title", "Первичная оценка"),
                Map.entry("answerAccuracy.step.initial.desc", "LLM оценивает точность ответа относительно эталона."),
                Map.entry("answerAccuracy.step.confirm.title", "Подтверждающая оценка"),
                Map.entry(
                        "answerAccuracy.step.confirm.desc",
                        "Вторая LLM подтверждает или корректирует первичную оценку."),
                Map.entry("answerAccuracy.rawScoreLabel", "Точность"),
                Map.entry("answerAccuracy.initialAssessmentLabel", "Первичная оценка"),
                Map.entry("answerAccuracy.finalScoreLabel", "Итоговая оценка"),
                Map.entry("answerAccuracy.verdict.fullyCorrect", "Полностью верно (2/2)"),
                Map.entry("answerAccuracy.verdict.partiallyCorrect", "Частично верно (1/2)"),
                Map.entry("answerAccuracy.verdict.incorrect", "Неверно (0/2)"),
                Map.entry("answerAccuracy.formula", "Нормализованная оценка = сырая_оценка / 2"),
                Map.entry("answerAccuracy.meaning.excellent", "Отлично - ответ точно соответствует эталону"),
                Map.entry("answerAccuracy.meaning.good", "Хорошо - ответ в основном соответствует эталону"),
                Map.entry("answerAccuracy.meaning.moderate", "Средне - ответ частично соответствует эталону"),
                Map.entry("answerAccuracy.meaning.poor", "Плохо - ответ неточен или противоречит эталону"),
                Map.entry("answerAccuracy.scale.excellent", "Ответ полностью соответствует эталону"),
                Map.entry("answerAccuracy.scale.good", "Ответ в основном соответствует эталону"),
                Map.entry("answerAccuracy.scale.moderate", "Ответ частично соответствует эталону"),
                Map.entry("answerAccuracy.scale.poor", "Ответ неточен"),

                // BLEU Score (NLP metric)
                Map.entry(
                        "bleuScore.description",
                        "BLEU (Bilingual Evaluation Understudy) измеряет перекрытие n-грамм между "
                                + "ответом и эталоном. Чем выше скор, тем больше схожесть текстов."),
                Map.entry("bleuScore.step1.title", "Входные тексты"),
                Map.entry("bleuScore.step1.desc", "Ответ и эталонный текст для сравнения."),
                Map.entry("bleuScore.step2.title", "Конфигурация"),
                Map.entry("bleuScore.step2.desc", "Параметры конфигурации метрики BLEU."),
                Map.entry("bleuScore.step2.output", "Макс. n-грамм: %d, Сглаживание: %s"),
                Map.entry("bleuScore.step3.title", "Вычисление точности n-грамм"),
                Map.entry("bleuScore.step3.desc", "Расчёт точности для n-грамм от 1 до %d."),
                Map.entry("bleuScore.step4.title", "Вычисление BLEU скора"),
                Map.entry("bleuScore.step4.desc", "Комбинирование точностей n-грамм со штрафом за краткость."),
                Map.entry("bleuScore.formula", "BLEU = BP × exp(Σ wₙ × log(pₙ))"),
                Map.entry("bleuScore.meaning.excellent", "Отлично - тексты практически идентичны"),
                Map.entry("bleuScore.meaning.good", "Хорошо - высокое перекрытие n-грамм"),
                Map.entry("bleuScore.meaning.moderate", "Средне - частичное перекрытие n-грамм"),
                Map.entry("bleuScore.meaning.poor", "Плохо - низкое сходство текстов"),
                Map.entry("bleuScore.scale.excellent", "Почти идентичные тексты"),
                Map.entry("bleuScore.scale.good", "Высокое сходство"),
                Map.entry("bleuScore.scale.moderate", "Умеренное сходство"),
                Map.entry("bleuScore.scale.poor", "Низкое сходство"),

                // ROUGE Score (NLP metric)
                Map.entry(
                        "rougeScore.description",
                        "ROUGE (Recall-Oriented Understudy for Gisting Evaluation) измеряет перекрытие "
                                + "между ответом и эталоном по униграммам, биграммам или наибольшей общей подпоследовательности."),
                Map.entry("rougeScore.step1.title", "Входные тексты"),
                Map.entry("rougeScore.step1.desc", "Ответ и эталонный текст для сравнения."),
                Map.entry("rougeScore.step2.title", "Конфигурация"),
                Map.entry("rougeScore.step2.desc", "Параметры конфигурации метрики ROUGE."),
                Map.entry("rougeScore.step2.output", "Тип: %s, Режим: %s"),
                Map.entry("rougeScore.step3.title", "Вычисление перекрытия %s"),
                Map.entry("rougeScore.rouge1.desc", "Подсчёт совпадений униграмм (отдельных слов) между текстами."),
                Map.entry(
                        "rougeScore.rouge2.desc",
                        "Подсчёт совпадений биграмм (двух последовательных слов) между текстами."),
                Map.entry("rougeScore.rougeL.desc", "Поиск наибольшей общей подпоследовательности между текстами."),
                Map.entry("rougeScore.step4.title", "Вычисление скора"),
                Map.entry("rougeScore.step4.desc", "Расчёт %s из статистики перекрытия."),
                Map.entry("rougeScore.mode.precision", "Точность"),
                Map.entry("rougeScore.mode.recall", "Полнота"),
                Map.entry("rougeScore.mode.fmeasure", "F-мера"),
                Map.entry("rougeScore.formula.precision", "Precision = совпадения / длина_ответа"),
                Map.entry("rougeScore.formula.recall", "Recall = совпадения / длина_эталона"),
                Map.entry("rougeScore.formula.fmeasure", "F1 = 2 × (P × R) / (P + R)"),
                Map.entry("rougeScore.meaning.excellent", "Отлично - очень высокое перекрытие текстов"),
                Map.entry("rougeScore.meaning.good", "Хорошо - значительное перекрытие текстов"),
                Map.entry("rougeScore.meaning.moderate", "Средне - частичное перекрытие текстов"),
                Map.entry("rougeScore.meaning.poor", "Плохо - низкое перекрытие текстов"),
                Map.entry("rougeScore.scale.excellent", "Очень высокое перекрытие"),
                Map.entry("rougeScore.scale.good", "Хорошее перекрытие"),
                Map.entry("rougeScore.scale.moderate", "Частичное перекрытие"),
                Map.entry("rougeScore.scale.poor", "Низкое перекрытие"),

                // chrF Score (NLP metric)
                Map.entry(
                        "chrfScore.description",
                        "chrF (Character n-gram F-score) измеряет перекрытие на уровне символов между текстами. "
                                + "chrF++ также включает словесные n-граммы для повышения точности."),
                Map.entry("chrfScore.step1.title", "Входные тексты"),
                Map.entry("chrfScore.step1.desc", "Ответ и эталонный текст для сравнения."),
                Map.entry("chrfScore.step2.title", "Конфигурация"),
                Map.entry("chrfScore.step2.desc", "Параметры конфигурации метрики chrF."),
                Map.entry(
                        "chrfScore.step2.output",
                        "Вариант: %s, Символьные n-граммы: %d, Словесные n-граммы: %d, Beta: %.1f"),
                Map.entry("chrfScore.step3.title", "Вычисление перекрытия символьных n-грамм"),
                Map.entry("chrfScore.step3.desc", "Расчёт совпадений символьных n-грамм (от 1 до %d)."),
                Map.entry("chrfScore.step4.title", "Вычисление перекрытия словесных n-грамм"),
                Map.entry("chrfScore.step4.desc", "Расчёт совпадений словесных n-грамм (от 1 до %d)."),
                Map.entry("chrfScore.step5.title", "Вычисление chrF скора"),
                Map.entry("chrfScore.step5.desc", "Комбинирование символьных и словесных скоров с beta=%.1f."),
                Map.entry("chrfScore.formula", "chrF = (1 + β²) × (P × R) / (β² × P + R)"),
                Map.entry("chrfScore.meaning.excellent", "Отлично - очень высокое сходство на уровне символов"),
                Map.entry("chrfScore.meaning.good", "Хорошо - высокое перекрытие символов"),
                Map.entry("chrfScore.meaning.moderate", "Средне - частичное перекрытие символов"),
                Map.entry("chrfScore.meaning.poor", "Плохо - низкое сходство символов"),
                Map.entry("chrfScore.scale.excellent", "Почти идентичные символы"),
                Map.entry("chrfScore.scale.good", "Высокое сходство символов"),
                Map.entry("chrfScore.scale.moderate", "Частичное перекрытие символов"),
                Map.entry("chrfScore.scale.poor", "Низкое сходство символов"),

                // String Similarity (NLP metric)
                Map.entry(
                        "stringSimilarity.description",
                        "Метрика строкового сходства измеряет редакционное расстояние между текстами "
                                + "с помощью алгоритмов Левенштейна, Джаро, Джаро-Винклера или Хэмминга."),
                Map.entry("stringSimilarity.step1.title", "Входные тексты"),
                Map.entry("stringSimilarity.step1.desc", "Ответ и эталонный текст для сравнения."),
                Map.entry("stringSimilarity.step2.title", "Конфигурация"),
                Map.entry("stringSimilarity.step2.desc", "Параметры конфигурации строкового сходства."),
                Map.entry("stringSimilarity.step2.output", "Алгоритм: %s, Учёт регистра: %s"),
                Map.entry("stringSimilarity.step3.title", "Вычисление сходства %s"),
                Map.entry("stringSimilarity.caseSensitive.yes", "Да"),
                Map.entry("stringSimilarity.caseSensitive.no", "Нет"),
                Map.entry(
                        "stringSimilarity.algorithm.levenshtein",
                        "Подсчёт минимального числа операций (вставка/удаление/замена) для преобразования одного текста в другой."),
                Map.entry(
                        "stringSimilarity.algorithm.hamming",
                        "Подсчёт позиций, где символы различаются (требуются строки одинаковой длины)."),
                Map.entry(
                        "stringSimilarity.algorithm.jaro",
                        "Вычисление сходства на основе совпадающих символов и транспозиций."),
                Map.entry("stringSimilarity.algorithm.jaroWinkler", "Сходство Джаро с бонусом за совпадающий префикс."),
                Map.entry("stringSimilarity.formula.levenshtein", "Сходство = 1 - (расстояние / макс_длина)"),
                Map.entry("stringSimilarity.formula.hamming", "Сходство = 1 - (различающиеся_позиции / длина)"),
                Map.entry("stringSimilarity.formula.jaro", "Jaro = (m/|s₁| + m/|s₂| + (m-t)/m) / 3"),
                Map.entry("stringSimilarity.formula.jaroWinkler", "JW = Jaro + (префикс × p × (1 - Jaro))"),
                Map.entry("stringSimilarity.meaning.excellent", "Отлично - строки практически идентичны"),
                Map.entry("stringSimilarity.meaning.good", "Хорошо - строки очень похожи"),
                Map.entry("stringSimilarity.meaning.moderate", "Средне - строки частично похожи"),
                Map.entry("stringSimilarity.meaning.poor", "Плохо - строки довольно разные"),
                Map.entry("stringSimilarity.scale.excellent", "Почти идентичные строки"),
                Map.entry("stringSimilarity.scale.good", "Очень похожие строки"),
                Map.entry("stringSimilarity.scale.moderate", "Частичное сходство"),
                Map.entry("stringSimilarity.scale.poor", "Разные строки"));
    }
}
