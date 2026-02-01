package ai.qa.solutions.allure.explanation;

import ai.qa.solutions.allure.model.ModelExecutionData;
import ai.qa.solutions.allure.model.StepExecutionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Extracts score explanations from metric execution data.
 * <p>
 * Parses the resultJson from each step to build metric-specific
 * explanation objects that explain why the metric got a specific score.
 */
@Slf4j
public class ScoreExplanationExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Extracts a score explanation for the given metric.
     *
     * @param metricName the metric name
     * @param steps the execution steps
     * @param score the final score
     * @param language the report language ("en" or "ru")
     * @return optional explanation, empty if metric not supported
     */
    public Optional<ScoreExplanation> extract(
            final String metricName, final List<StepExecutionData> steps, final Double score, final String language) {
        return extract(metricName, steps, score, language, null);
    }

    /**
     * Extracts a score explanation for the given metric with config.
     *
     * @param metricName the metric name
     * @param steps the execution steps
     * @param score the final score
     * @param language the report language ("en" or "ru")
     * @param config the metric configuration object (can be null)
     * @return optional explanation, empty if metric not supported
     */
    public Optional<ScoreExplanation> extract(
            final String metricName,
            final List<StepExecutionData> steps,
            final Double score,
            final String language,
            final Object config) {
        return extract(metricName, steps, score, language, config, null);
    }

    /**
     * Extracts a score explanation for the given metric with config and metadata.
     *
     * @param metricName the metric name
     * @param steps the execution steps
     * @param score the final score
     * @param language the report language ("en" or "ru")
     * @param config the metric configuration object (can be null)
     * @param metadata the metric evaluation result metadata (can be null)
     * @return optional explanation, empty if metric not supported
     */
    public Optional<ScoreExplanation> extract(
            final String metricName,
            final List<StepExecutionData> steps,
            final Double score,
            final String language,
            final Object config,
            final Map<String, Object> metadata) {
        if (metricName == null || steps == null || steps.isEmpty()) {
            return Optional.empty();
        }

        try {
            return switch (normalizeMetricName(metricName)) {
                case "faithfulness" -> extractFaithfulness(steps, score, language);
                case "noise-sensitivity", "noisesensitivity" -> extractNoiseSensitivity(steps, score, language);
                case "aspect-critic", "aspectcritic" -> extractAspectCritic(steps, score, language, config);
                case "context-precision", "contextprecision" -> extractContextPrecision(steps, score, language);
                case "context-recall", "contextrecall" -> extractContextRecall(steps, score, language);
                case "context-entity-recall", "contextentityrecall" -> extractContextEntityRecall(
                        steps, score, language);
                case "response-relevancy", "responserelevancy" -> extractResponseRelevancy(steps, score, language);
                case "simple-criteria", "simplecriteria", "simplecriteriascore" -> extractSimpleCriteria(
                        steps, score, language, config);
                case "rubrics-score", "rubricsscore" -> extractRubricsScore(steps, score, language, config);
                case "semantic-similarity", "semanticsimilarity" -> extractSemanticSimilarity(
                        steps, score, language, config);
                case "factual-correctness", "factualcorrectness" -> extractFactualCorrectness(
                        steps, score, language, config);
                case "answer-correctness", "answercorrectness" -> extractAnswerCorrectness(
                        steps, score, language, config);
                case "agent-goal-accuracy", "agentgoalaccuracy" -> extractAgentGoalAccuracy(
                        steps, score, language, config);
                case "tool-call-accuracy", "toolcallaccuracy" -> extractToolCallAccuracy(
                        steps, score, language, config, metadata);
                case "topic-adherence", "topicadherence" -> extractTopicAdherence(steps, score, language, config);
                case "context-relevance", "contextrelevance" -> extractContextRelevance(steps, score, language, config);
                case "response-groundedness", "responsegroundedness" -> extractResponseGroundedness(
                        steps, score, language, config);
                case "answer-accuracy", "answeraccuracy" -> extractAnswerAccuracy(steps, score, language, config);
                case "bleu-score", "bleuscore", "bleu" -> extractBleuScore(steps, score, language, config);
                case "rouge-score", "rougescore", "rouge" -> extractRougeScore(steps, score, language, config);
                case "chrf-score", "chrfscore", "chrf" -> extractChrfScore(steps, score, language, config);
                case "string-similarity", "stringsimilarity" -> extractStringSimilarity(steps, score, language, config);
                default -> {
                    log.debug("No explanation extractor for metric: {}", metricName);
                    yield Optional.empty();
                }
            };
        } catch (final Exception e) {
            log.warn("Failed to extract explanation for metric {}: {}", metricName, e.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeMetricName(final String metricName) {
        return metricName.toLowerCase().replace("metric", "").replace("_", "-").trim();
    }

    private Optional<ScoreExplanation> extractFaithfulness(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String aiResponse = "";
        final List<String> statements = new ArrayList<>();
        final List<FaithfulnessExplanation.StatementVerdict> verdicts = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            // Extract AI response from request
            if (step.getRequest() != null && aiResponse.isEmpty()) {
                aiResponse = extractResponseFromRequest(step.getRequest());
            }

            if ("GenerateStatements".equalsIgnoreCase(step.getStepName())) {
                extractStatements(step, statements);
            } else if ("EvaluateFaithfulness".equalsIgnoreCase(step.getStepName())) {
                extractFaithfulnessVerdicts(step, verdicts);
            }
        }

        if (verdicts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(FaithfulnessExplanation.builder()
                .score(score)
                .language(language)
                .aiResponse(aiResponse)
                .statements(statements)
                .verdicts(verdicts)
                .build());
    }

    private void extractStatements(final StepExecutionData step, final List<String> statements) {
        for (final ModelExecutionData result : step.getModelResults()) {
            if (result.isSuccess() && result.getResultJson() != null) {
                try {
                    final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                    final JsonNode statementsNode = json.get("statements");
                    if (statementsNode != null && statementsNode.isArray()) {
                        for (final JsonNode s : statementsNode) {
                            statements.add(s.asText());
                        }
                        break; // Use first successful model's statements
                    }
                } catch (final JsonProcessingException e) {
                    log.debug("Failed to parse statements JSON: {}", e.getMessage());
                }
            }
        }
    }

    private void extractFaithfulnessVerdicts(
            final StepExecutionData step, final List<FaithfulnessExplanation.StatementVerdict> verdicts) {
        for (final ModelExecutionData result : step.getModelResults()) {
            if (result.isSuccess() && result.getResultJson() != null) {
                try {
                    final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                    final JsonNode verdictsNode = json.get("verdicts");
                    if (verdictsNode != null && verdictsNode.isArray()) {
                        for (final JsonNode v : verdictsNode) {
                            verdicts.add(FaithfulnessExplanation.StatementVerdict.builder()
                                    .statement(getTextSafe(v, "statement"))
                                    .passed(getIntSafe(v, "verdict") == 1)
                                    .reason(getTextSafe(v, "reason"))
                                    .build());
                        }
                        break; // Use first successful model's verdicts
                    }
                } catch (final JsonProcessingException e) {
                    log.debug("Failed to parse verdicts JSON: {}", e.getMessage());
                }
            }
        }
    }

    private Optional<ScoreExplanation> extractNoiseSensitivity(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String reference = "";
        String aiResponse = "";
        final List<String> refStatements = new ArrayList<>();
        final List<String> respStatements = new ArrayList<>();
        final List<NoiseSensitivityExplanation.StatementMatch> matches = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            if (stepName != null) {
                // Extract text from the "Answer:" field in each step's request
                // NoiseSensitivity uses "Answer:" for both reference and response steps
                if (stepName.contains("Reference") || stepName.contains("Ground")) {
                    if (step.getRequest() != null && reference.isEmpty()) {
                        reference = extractResponseFromRequest(step.getRequest());
                    }
                    extractStatementList(step, refStatements);
                } else if (stepName.contains("Response") && !stepName.contains("Matrix")) {
                    if (step.getRequest() != null && aiResponse.isEmpty()) {
                        aiResponse = extractResponseFromRequest(step.getRequest());
                    }
                    extractStatementList(step, respStatements);
                }
            }
        }

        // Create matches from response statements
        for (int i = 0; i < respStatements.size(); i++) {
            matches.add(NoiseSensitivityExplanation.StatementMatch.builder()
                    .statement(respStatements.get(i))
                    .inReference(true) // Simplified - would need actual data
                    .correct(true) // Simplified
                    .analysis("Statement matches reference")
                    .build());
        }

        return Optional.of(NoiseSensitivityExplanation.builder()
                .score(score)
                .language(language)
                .reference(reference)
                .aiResponse(aiResponse)
                .referenceStatements(refStatements)
                .responseStatements(respStatements)
                .matches(matches)
                .mode("RELEVANT")
                .build());
    }

    private void extractStatementList(final StepExecutionData step, final List<String> statements) {
        for (final ModelExecutionData result : step.getModelResults()) {
            if (result.isSuccess() && result.getResultJson() != null) {
                try {
                    final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                    final JsonNode statementsNode = json.get("statements");
                    if (statementsNode != null && statementsNode.isArray()) {
                        for (final JsonNode s : statementsNode) {
                            statements.add(s.asText());
                        }
                        break;
                    }
                } catch (final JsonProcessingException e) {
                    log.debug("Failed to parse statements: {}", e.getMessage());
                }
            }
        }
    }

    private Optional<ScoreExplanation> extractAspectCritic(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String aspectName = "Custom Aspect";
        String aspectDef = "";
        String aiResponse = "";
        boolean passed = score != null && score >= 0.5;
        String reasoning = "";

        // Extract strictness and aspect definition from config
        int strictness = 1;
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("strictness")) {
                    strictness = configJson.get("strictness").asInt(1);
                }
                if (configJson.has("definition")) {
                    aspectDef = configJson.get("definition").asText("");
                    aspectName = extractAspectNameFromDefinition(aspectDef);
                }
            } catch (final Exception e) {
                log.debug("Failed to parse aspect critic config: {}", e.getMessage());
            }
        }

        // Collect iteration results per model for majority voting display
        final Map<String, List<Boolean>> modelIterationResults = new HashMap<>();

        for (final StepExecutionData step : steps) {
            // Extract AI response from request
            if (step.getRequest() != null && aiResponse.isEmpty()) {
                aiResponse = extractResponseFromRequest(step.getRequest());
            }

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("verdict")) {
                            final boolean verdict = json.get("verdict").asBoolean();
                            // Group verdicts by model ID
                            modelIterationResults
                                    .computeIfAbsent(result.getModelId(), k -> new ArrayList<>())
                                    .add(verdict);
                            // Use first successful result's reasoning
                            if (reasoning.isEmpty() && json.has("reasoning")) {
                                reasoning = json.get("reasoning").asText();
                            }
                            if (reasoning.isEmpty() && json.has("reason")) {
                                reasoning = json.get("reason").asText();
                            }
                        }
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse aspect critic JSON: {}", e.getMessage());
                    }
                }
            }
        }

        return Optional.of(AspectCriticExplanation.builder()
                .score(score)
                .language(language)
                .aspectName(aspectName)
                .aspectDefinition(aspectDef)
                .aiResponse(aiResponse)
                .passed(passed)
                .reasoning(reasoning)
                .strictness(strictness)
                .modelIterationResults(modelIterationResults)
                .build());
    }

    private String extractAspectNameFromDefinition(final String definition) {
        if (definition == null || definition.isEmpty()) {
            return "Custom Aspect";
        }
        // Return full definition text - no truncation
        // The UI will handle display formatting
        return definition;
    }

    private Optional<ScoreExplanation> extractContextPrecision(
            final List<StepExecutionData> steps, final Double score, final String language) {
        final List<ContextPrecisionExplanation.ContextRelevance> contexts = new ArrayList<>();
        final List<Double> precisionAtK = new ArrayList<>();
        String userInput = "";

        for (final StepExecutionData step : steps) {
            // Extract user question from request
            if (step.getRequest() != null && userInput.isEmpty()) {
                userInput = extractUserInputFromRequest(step.getRequest());
            }

            // Extract context chunk text from request
            final String contextChunk =
                    step.getRequest() != null ? extractContextChunkFromRequest(step.getRequest()) : "";

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("relevant")) {
                            final boolean relevant = json.get("relevant").asBoolean();
                            contexts.add(ContextPrecisionExplanation.ContextRelevance.builder()
                                    .position(contexts.size() + 1)
                                    .contextText(
                                            contextChunk.isEmpty() ? "Context " + (contexts.size() + 1) : contextChunk)
                                    .relevant(relevant)
                                    .reason(getTextSafe(json, "reasoning"))
                                    .build());
                            break; // Take first successful result for this context
                        }
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse context precision JSON: {}", e.getMessage());
                    }
                }
            }
        }

        // Calculate precision@k
        int relevantSoFar = 0;
        for (int i = 0; i < contexts.size(); i++) {
            if (contexts.get(i).isRelevant()) {
                relevantSoFar++;
            }
            precisionAtK.add((double) relevantSoFar / (i + 1));
        }

        return Optional.of(ContextPrecisionExplanation.builder()
                .score(score)
                .language(language)
                .userInput(userInput)
                .contexts(contexts)
                .precisionAtK(precisionAtK)
                .build());
    }

    private Optional<ScoreExplanation> extractContextRecall(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String reference = "";
        String contexts = "";
        final List<ContextRecallExplanation.ReferenceClassification> classifications = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            // Extract reference and contexts from request
            if (step.getRequest() != null) {
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
                if (contexts.isEmpty()) {
                    contexts = extractContextFromRequest(step.getRequest());
                }
            }

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        final JsonNode classificationsNode = json.get("classifications");
                        if (classificationsNode != null && classificationsNode.isArray()) {
                            for (final JsonNode c : classificationsNode) {
                                classifications.add(ContextRecallExplanation.ReferenceClassification.builder()
                                        .statement(getTextSafe(c, "statement"))
                                        .found(getIntSafe(c, "attributed") == 1)
                                        .reason(getTextSafe(c, "reason"))
                                        .build());
                            }
                            break;
                        }
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse context recall JSON: {}", e.getMessage());
                    }
                }
            }
        }

        return Optional.of(ContextRecallExplanation.builder()
                .score(score)
                .language(language)
                .reference(reference)
                .contexts(contexts)
                .classifications(classifications)
                .build());
    }

    private Optional<ScoreExplanation> extractContextEntityRecall(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String reference = "";
        String contexts = "";
        final List<String> refEntities = new ArrayList<>();
        final List<String> ctxEntities = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();
            final boolean isContextStep = stepName != null && stepName.contains("Context");

            // Extract text from request based on step type
            if (step.getRequest() != null) {
                final String text = extractTextFromRequest(step.getRequest());
                if (!text.isEmpty()) {
                    if (isContextStep && contexts.isEmpty()) {
                        contexts = text;
                    } else if (!isContextStep && reference.isEmpty()) {
                        reference = text;
                    }
                }
            }

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        final JsonNode entitiesNode = json.get("entities");
                        if (entitiesNode != null && entitiesNode.isArray()) {
                            final List<String> target = isContextStep ? ctxEntities : refEntities;
                            for (final JsonNode e : entitiesNode) {
                                target.add(e.asText());
                            }
                            break;
                        }
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse entity recall JSON: {}", e.getMessage());
                    }
                }
            }
        }

        // Find matching entities
        final List<String> found = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        for (final String entity : refEntities) {
            if (ctxEntities.stream().anyMatch(c -> c.equalsIgnoreCase(entity))) {
                found.add(entity);
            } else {
                missing.add(entity);
            }
        }

        return Optional.of(ContextEntityRecallExplanation.builder()
                .score(score)
                .language(language)
                .reference(reference)
                .contexts(contexts)
                .referenceEntities(refEntities)
                .contextEntities(ctxEntities)
                .foundEntities(found)
                .missingEntities(missing)
                .build());
    }

    private Optional<ScoreExplanation> extractResponseRelevancy(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String originalQuestion = "";
        String aiResponse = "";
        final List<ResponseRelevancyExplanation.GeneratedQuestion> questions = new ArrayList<>();
        final List<ResponseRelevancyExplanation.ModelSimilarityResult> modelResults = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            if (step.getRequest() != null && originalQuestion.isEmpty()) {
                // Try to extract question and response from request
                originalQuestion = extractQuestionFromRequest(step.getRequest());
                aiResponse = extractResponseFromRequest(step.getRequest());
            }

            final String stepName = step.getStepName();

            // Extract generated questions from GenerateQuestions step
            if ("GenerateQuestions".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            final JsonNode questionsNode = json.get("questions");
                            if (questionsNode != null && questionsNode.isArray()) {
                                for (final JsonNode q : questionsNode) {
                                    final String questionText = q.isTextual() ? q.asText() : getTextSafe(q, "question");
                                    questions.add(ResponseRelevancyExplanation.GeneratedQuestion.builder()
                                            .question(questionText)
                                            .similarity(0.0)
                                            .build());
                                }
                                break; // Use first successful model's questions
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse response relevancy JSON: {}", e.getMessage());
                        }
                    }
                }
            }

            // Extract similarity scores from ComputeCosineSimilarity step
            if ("ComputeCosineSimilarity".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final double similarity =
                                    Double.parseDouble(result.getResultJson().trim());
                            modelResults.add(ResponseRelevancyExplanation.ModelSimilarityResult.builder()
                                    .modelId(result.getModelId())
                                    .similarity(similarity)
                                    .build());
                        } catch (final NumberFormatException e) {
                            log.debug("Failed to parse similarity score: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return Optional.of(ResponseRelevancyExplanation.builder()
                .score(score)
                .language(language)
                .originalQuestion(originalQuestion)
                .aiResponse(aiResponse)
                .generatedQuestions(questions)
                .modelResults(modelResults)
                .build());
    }

    private String extractQuestionFromRequest(final String request) {
        // Extract "User Question: ..." from prompt
        if (request.contains("User Question:")) {
            final int start = request.indexOf("User Question:") + 14;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
        }
        // Fallback to old format
        if (request.contains("Question:")) {
            final int start = request.indexOf("Question:") + 9;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
        }
        return "";
    }

    private String extractResponseFromRequest(final String request) {
        // Extract "Response: ..." or "AI Response: ..." or "Answer: ..." from prompt
        // Response can be multi-line (e.g., code), so extract until next section marker
        int start = -1;

        // Try "AI Response:" first (used by RubricsScore and other metrics)
        if (request.contains("AI Response:")) {
            start = request.indexOf("AI Response:") + 12;
        } else if (request.contains("Answer:")) {
            // Used by FaithfulnessMetric
            start = request.indexOf("Answer:") + 7;
        } else if (request.contains("Response:")) {
            start = request.indexOf("Response:") + 9;
        }

        if (start > 0) {
            // Find where response ends - at the next section marker
            int end = request.length();
            final String[] sectionMarkers = {
                "Reference:",
                "Evaluation Rubrics:",
                "Instructions:",
                "CRITICAL INSTRUCTIONS:",
                "IMPORTANT:",
                "Rubrics:",
                "Context:",
                "Criteria:",
                "Example:",
                "Output:",
                "Now generate",
                "Your task:",
                "You must:",
                "Please ",
                "\n\n1.",
                "\n1."
            };
            for (final String marker : sectionMarkers) {
                final int markerIdx = request.indexOf(marker, start);
                if (markerIdx > start && markerIdx < end) {
                    end = markerIdx;
                }
            }
            return request.substring(start, end).trim();
        }
        return "";
    }

    private String extractReferenceFromRequest(final String request) {
        // Try "Reference Answer:" first (used by ContextRecall)
        if (request.contains("Reference Answer:")) {
            final int start = request.indexOf("Reference Answer:") + 17;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        // Try "Reference:" next
        if (request.contains("Reference:")) {
            final int start = request.indexOf("Reference:") + 10;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        // Fallback to "Ground Truth:"
        if (request.contains("Ground Truth:")) {
            final int start = request.indexOf("Ground Truth:") + 13;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        return "";
    }

    private String extractUserInputFromRequest(final String request) {
        // Try "Question:" first
        if (request.contains("Question:")) {
            final int start = request.indexOf("Question:") + 9;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        // Fallback to "User Input:"
        if (request.contains("User Input:")) {
            final int start = request.indexOf("User Input:") + 11;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        return "";
    }

    private String extractContextFromRequest(final String request) {
        // Extract multi-line context between "Context:" and the next section
        if (request.contains("Context:")) {
            final int start = request.indexOf("Context:") + 8;
            // Find where context ends - at "Reference Answer:" or "Reference:" or "Instructions:"
            int end = request.length();
            final int refAnswerIdx = request.indexOf("Reference Answer:", start);
            final int refIdx = request.indexOf("Reference:", start);
            final int instrIdx = request.indexOf("Instructions:", start);

            if (refAnswerIdx > start && refAnswerIdx < end) {
                end = refAnswerIdx;
            }
            if (refIdx > start && refIdx < end) {
                end = refIdx;
            }
            if (instrIdx > start && instrIdx < end) {
                end = instrIdx;
            }

            return request.substring(start, end).trim();
        }
        return "";
    }

    private String extractTextFromRequest(final String request) {
        // Extract multi-line text between "Text:" and the next section
        // Used by ContextEntityRecallMetric
        if (request.contains("Text:")) {
            final int start = request.indexOf("Text:") + 5;
            // Find where text ends - at "Instructions:" or "Examples:"
            int end = request.length();
            final String[] sectionMarkers = {"Instructions:", "Examples:", "Respond with"};
            for (final String marker : sectionMarkers) {
                final int markerIdx = request.indexOf(marker, start);
                if (markerIdx > start && markerIdx < end) {
                    end = markerIdx;
                }
            }
            return request.substring(start, end).trim();
        }
        return "";
    }

    private String extractContextChunkFromRequest(final String request) {
        // Extract multi-line context chunk between "Retrieved Context Chunk:" and the next section
        // Used by ContextPrecisionMetric
        if (request.contains("Retrieved Context Chunk:")) {
            final int start = request.indexOf("Retrieved Context Chunk:") + 24;
            // Find where chunk ends - at "Instructions:" or "Respond with"
            int end = request.length();
            final String[] sectionMarkers = {"Instructions:", "Respond with"};
            for (final String marker : sectionMarkers) {
                final int markerIdx = request.indexOf(marker, start);
                if (markerIdx > start && markerIdx < end) {
                    end = markerIdx;
                }
            }
            return request.substring(start, end).trim();
        }
        return "";
    }

    private Optional<ScoreExplanation> extractSimpleCriteria(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String reasoning = "";
        String aiResponse = "";
        String criteriaName = "Custom Criteria";
        String criteriaDef = "";
        final Map<String, Integer> modelScores = new HashMap<>();

        // Extract criteria definition from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("definition")) {
                    criteriaDef = configJson.get("definition").asText("");
                    criteriaName = criteriaDef;
                }
            } catch (final Exception e) {
                log.debug("Failed to parse simple criteria config: {}", e.getMessage());
            }
        }

        for (final StepExecutionData step : steps) {
            // Extract AI response from request
            if (step.getRequest() != null && aiResponse.isEmpty()) {
                aiResponse = extractResponseFromRequest(step.getRequest());
            }

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("score")) {
                            final int modelScore = json.get("score").asInt();
                            modelScores.put(result.getModelId(), modelScore);
                        }
                        if (reasoning.isEmpty() && json.has("reason")) {
                            reasoning = json.get("reason").asText();
                        }
                        if (reasoning.isEmpty() && json.has("reasoning")) {
                            reasoning = json.get("reasoning").asText();
                        }
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse simple criteria JSON: {}", e.getMessage());
                    }
                }
            }
        }

        // Calculate average raw score from all models
        final int rawScore = modelScores.isEmpty()
                ? 3
                : (int) Math.round(modelScores.values().stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(3));

        return Optional.of(SimpleCriteriaExplanation.builder()
                .score(score)
                .language(language)
                .criteriaName(criteriaName)
                .criteriaDefinition(criteriaDef)
                .aiResponse(aiResponse)
                .modelScores(modelScores)
                .rawScore(rawScore)
                .minScore(1)
                .maxScore(5)
                .reasoning(reasoning)
                .build());
    }

    private Optional<ScoreExplanation> extractRubricsScore(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String reasoning = "";
        String aiResponse = "";

        // Extract rubric levels from config if available
        final List<RubricsScoreExplanation.RubricLevel> levels = extractRubricLevelsFromConfig(config);

        // Determine min/max from levels
        final int minLevel = levels.stream()
                .mapToInt(RubricsScoreExplanation.RubricLevel::getLevel)
                .min()
                .orElse(1);
        final int maxLevel = levels.stream()
                .mapToInt(RubricsScoreExplanation.RubricLevel::getLevel)
                .max()
                .orElse(5);

        // selectedLevel should be based on aggregated score, not a single model
        // score is the aggregated average (e.g., 1.6 for levels 2,2,2,1,1)
        // Round to nearest integer for display, but keep fractional in score
        final int selectedLevel = score != null ? (int) Math.round(score) : (minLevel + maxLevel) / 2;

        // Extract AI response and reasoning from execution steps
        for (final StepExecutionData step : steps) {
            // Extract AI response from request
            if (step.getRequest() != null && aiResponse.isEmpty()) {
                aiResponse = extractResponseFromRequest(step.getRequest());
            }

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("reasoning")) {
                            reasoning = json.get("reasoning").asText();
                        } else if (json.has("reason")) {
                            reasoning = json.get("reason").asText();
                        }
                        if (!reasoning.isEmpty()) {
                            break;
                        }
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse rubrics JSON: {}", e.getMessage());
                    }
                }
            }
            if (!reasoning.isEmpty()) {
                break;
            }
        }

        return Optional.of(RubricsScoreExplanation.builder()
                .score(score)
                .language(language)
                .rubricLevels(levels)
                .selectedLevel(selectedLevel)
                .aiResponse(aiResponse)
                .reasoning(reasoning)
                .minLevel(minLevel)
                .maxLevel(maxLevel)
                .build());
    }

    /**
     * Extracts rubric levels from the metric configuration.
     * <p>
     * Parses config.rubrics map where keys are in format "scoreN_description".
     */
    @SuppressWarnings("unchecked")
    private List<RubricsScoreExplanation.RubricLevel> extractRubricLevelsFromConfig(final Object config) {
        if (config == null) {
            return getDefaultRubricLevels();
        }

        try {
            // Try to get rubrics from config using reflection or serialization
            final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
            if (configJson.has("rubrics") && configJson.get("rubrics").isObject()) {
                final JsonNode rubricsNode = configJson.get("rubrics");
                final List<RubricsScoreExplanation.RubricLevel> levels = new ArrayList<>();
                final Pattern pattern = Pattern.compile("score(\\d+)_description");

                rubricsNode.fieldNames().forEachRemaining(key -> {
                    final Matcher matcher = pattern.matcher(key);
                    if (matcher.matches()) {
                        final int level = Integer.parseInt(matcher.group(1));
                        final String description = rubricsNode.get(key).asText();
                        levels.add(RubricsScoreExplanation.RubricLevel.builder()
                                .level(level)
                                .description(description)
                                .build());
                    }
                });

                if (!levels.isEmpty()) {
                    // Sort by level descending for display
                    levels.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));
                    return levels;
                }
            }
        } catch (final Exception e) {
            log.debug("Failed to extract rubrics from config: {}", e.getMessage());
        }

        return getDefaultRubricLevels();
    }

    private List<RubricsScoreExplanation.RubricLevel> getDefaultRubricLevels() {
        return List.of(
                RubricsScoreExplanation.RubricLevel.builder()
                        .level(5)
                        .description("Excellent")
                        .build(),
                RubricsScoreExplanation.RubricLevel.builder()
                        .level(4)
                        .description("Good")
                        .build(),
                RubricsScoreExplanation.RubricLevel.builder()
                        .level(3)
                        .description("Adequate")
                        .build(),
                RubricsScoreExplanation.RubricLevel.builder()
                        .level(2)
                        .description("Poor")
                        .build(),
                RubricsScoreExplanation.RubricLevel.builder()
                        .level(1)
                        .description("Very Poor")
                        .build());
    }

    private Optional<ScoreExplanation> extractSemanticSimilarity(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        Double threshold = null;
        final List<SemanticSimilarityExplanation.ModelSimilarityResult> modelResults = new ArrayList<>();

        // Extract threshold from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("threshold")) {
                    final double t = configJson.get("threshold").asDouble(0.0);
                    if (t > 0) {
                        threshold = t;
                    }
                }
            } catch (final Exception e) {
                log.debug("Failed to parse semantic similarity config: {}", e.getMessage());
            }
        }

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract response and reference from request (they are embedded together)
            if (step.getRequest() != null && response.isEmpty()) {
                response = extractResponseFromRequest(step.getRequest());
                reference = extractReferenceFromRequest(step.getRequest());
            }

            // Extract similarity scores from ComputeCosineSimilarity step
            if ("ComputeCosineSimilarity".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final double similarity =
                                    Double.parseDouble(result.getResultJson().trim());
                            modelResults.add(SemanticSimilarityExplanation.ModelSimilarityResult.builder()
                                    .modelId(result.getModelId())
                                    .similarity(similarity)
                                    .build());
                        } catch (final NumberFormatException e) {
                            log.debug("Failed to parse similarity score: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return Optional.of(SemanticSimilarityExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .modelResults(modelResults)
                .threshold(threshold)
                .build());
    }

    private Optional<ScoreExplanation> extractFactualCorrectness(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        String mode = "F1";
        final List<String> responseClaims = new ArrayList<>();
        final List<String> referenceClaims = new ArrayList<>();
        final List<FactualCorrectnessExplanation.ClaimVerdict> precisionVerdicts = new ArrayList<>();
        final List<FactualCorrectnessExplanation.ClaimVerdict> recallVerdicts = new ArrayList<>();

        // Extract mode from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("mode")) {
                    mode = configJson.get("mode").asText("F1");
                }
            } catch (final Exception e) {
                log.debug("Failed to parse factual correctness config: {}", e.getMessage());
            }
        }

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract response and reference from request
            if (step.getRequest() != null) {
                if (response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
            }

            // Extract claims from decomposition steps
            if ("DecomposeResponseClaims".equalsIgnoreCase(stepName)) {
                extractClaimsList(step, responseClaims);
            } else if ("DecomposeReferenceClaims".equalsIgnoreCase(stepName)) {
                extractClaimsList(step, referenceClaims);
            } else if ("VerifyClaimsNLI".equalsIgnoreCase(stepName)) {
                // Extract NLI verdicts
                extractNliVerdicts(step, precisionVerdicts);
            }
        }

        return Optional.of(FactualCorrectnessExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .responseClaims(responseClaims)
                .referenceClaims(referenceClaims)
                .precisionVerdicts(precisionVerdicts)
                .recallVerdicts(recallVerdicts)
                .mode(mode)
                .build());
    }

    private void extractClaimsList(final StepExecutionData step, final List<String> claims) {
        for (final ModelExecutionData result : step.getModelResults()) {
            if (result.isSuccess() && result.getResultJson() != null) {
                try {
                    final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                    final JsonNode claimsNode = json.get("claims");
                    if (claimsNode != null && claimsNode.isArray()) {
                        for (final JsonNode c : claimsNode) {
                            claims.add(c.asText());
                        }
                        break; // Use first successful model's claims
                    }
                } catch (final JsonProcessingException e) {
                    log.debug("Failed to parse claims JSON: {}", e.getMessage());
                }
            }
        }
    }

    private Optional<ScoreExplanation> extractAnswerCorrectness(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        double factualScore = 0.0;
        double semanticScore = 0.0;
        double factualWeight = 0.75;
        double semanticWeight = 0.25;

        // Extract weights from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("factualWeight")) {
                    factualWeight = configJson.get("factualWeight").asDouble(0.75);
                }
                if (configJson.has("semanticWeight")) {
                    semanticWeight = configJson.get("semanticWeight").asDouble(0.25);
                }
            } catch (final Exception e) {
                log.debug("Failed to parse answer correctness config: {}", e.getMessage());
            }
        }

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract response and reference from request
            if (step.getRequest() != null) {
                if (response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
            }

            // Extract component scores from their respective steps
            if ("ComputeFactualCorrectness".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            factualScore =
                                    Double.parseDouble(result.getResultJson().trim());
                            break;
                        } catch (final NumberFormatException e) {
                            log.debug("Failed to parse factual score: {}", e.getMessage());
                        }
                    }
                }
            } else if ("ComputeSemanticSimilarity".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            semanticScore =
                                    Double.parseDouble(result.getResultJson().trim());
                            break;
                        } catch (final NumberFormatException e) {
                            log.debug("Failed to parse semantic score: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return Optional.of(AnswerCorrectnessExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .factualScore(factualScore)
                .semanticScore(semanticScore)
                .factualWeight(factualWeight)
                .semanticWeight(semanticWeight)
                .build());
    }

    private void extractNliVerdicts(
            final StepExecutionData step, final List<FactualCorrectnessExplanation.ClaimVerdict> verdicts) {
        for (final ModelExecutionData result : step.getModelResults()) {
            if (result.isSuccess() && result.getResultJson() != null) {
                try {
                    final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());

                    // Try to extract from NliVerificationResult wrapper
                    JsonNode verdictsSource = json;
                    if (json.has("precisionVerdicts")) {
                        verdictsSource = json.get("precisionVerdicts");
                    }

                    final JsonNode verdictsNode = verdictsSource.get("verdicts");
                    if (verdictsNode != null && verdictsNode.isArray()) {
                        for (final JsonNode v : verdictsNode) {
                            verdicts.add(FactualCorrectnessExplanation.ClaimVerdict.builder()
                                    .claim(getTextSafe(v, "claim"))
                                    .verdict(getTextSafe(v, "verdict"))
                                    .reason(getTextSafe(v, "reason"))
                                    .build());
                        }
                        break; // Use first successful model's verdicts
                    }
                } catch (final JsonProcessingException e) {
                    log.debug("Failed to parse NLI verdicts JSON: {}", e.getMessage());
                }
            }
        }
    }

    private String getTextSafe(final JsonNode node, final String field) {
        if (node == null || !node.has(field)) {
            return "";
        }
        return node.get(field).asText("");
    }

    private int getIntSafe(final JsonNode node, final String field) {
        if (node == null || !node.has(field)) {
            return 0;
        }
        return node.get(field).asInt(0);
    }

    private Optional<ScoreExplanation> extractAgentGoalAccuracy(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String mode = "WITH_REFERENCE";
        String conversation = "";
        String referenceGoal = "";
        String inferredGoal = null;
        boolean goalAchieved = score != null && score >= 0.5;
        final List<ModelStepResult> modelResults = new ArrayList<>();
        final List<ModelStepResult> inferGoalModelResults = new ArrayList<>();

        // Extract mode from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("mode")) {
                    mode = configJson.get("mode").asText("WITH_REFERENCE");
                }
            } catch (final Exception e) {
                log.debug("Failed to parse agent goal accuracy config: {}", e.getMessage());
            }
        }

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract conversation from request
            if (step.getRequest() != null && conversation.isEmpty()) {
                conversation = extractConversationFromRequest(step.getRequest());
                referenceGoal = extractGoalFromRequest(step.getRequest());
            }

            // Extract inferred goal from InferGoal step - collect ALL models
            if ("InferGoal".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            final String modelInferredGoal = json.has("inferredGoal")
                                    ? json.get("inferredGoal").asText()
                                    : "";
                            final String reasoning = json.has("reasoning")
                                    ? json.get("reasoning").asText()
                                    : "";

                            // Use first successful as the main inferred goal
                            if (inferredGoal == null && !modelInferredGoal.isEmpty()) {
                                inferredGoal = modelInferredGoal;
                            }

                            inferGoalModelResults.add(ModelStepResult.builder()
                                    .modelId(result.getModelId())
                                    .success(true)
                                    .reasoning(reasoning)
                                    .inferredGoal(modelInferredGoal)
                                    .build());
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse inferred goal JSON: {}", e.getMessage());
                            inferGoalModelResults.add(ModelStepResult.builder()
                                    .modelId(result.getModelId())
                                    .success(false)
                                    .errorMessage("Failed to parse response")
                                    .build());
                        }
                    } else if (!result.isSuccess()) {
                        inferGoalModelResults.add(ModelStepResult.builder()
                                .modelId(result.getModelId())
                                .success(false)
                                .errorMessage(result.getErrorMessage())
                                .build());
                    }
                }
            }

            // Extract verdicts from CompareOutcome or EvaluateOutcome step - collect ALL models
            if ("CompareOutcome".equalsIgnoreCase(stepName) || "EvaluateOutcome".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            final boolean achieved = json.has("goalAchieved")
                                    && json.get("goalAchieved").asBoolean();
                            final String reasoning = json.has("reasoning")
                                    ? json.get("reasoning").asText()
                                    : "";

                            modelResults.add(ModelStepResult.builder()
                                    .modelId(result.getModelId())
                                    .success(true)
                                    .verdict(achieved)
                                    .reasoning(reasoning)
                                    .build());
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse goal verdict JSON: {}", e.getMessage());
                            modelResults.add(ModelStepResult.builder()
                                    .modelId(result.getModelId())
                                    .success(false)
                                    .errorMessage("Failed to parse response")
                                    .build());
                        }
                    } else if (!result.isSuccess()) {
                        modelResults.add(ModelStepResult.builder()
                                .modelId(result.getModelId())
                                .success(false)
                                .errorMessage(result.getErrorMessage())
                                .build());
                    }
                }
            }
        }

        // Calculate agreement
        final long agreedCount = modelResults.stream()
                .filter(r -> r.isSuccess() && Boolean.TRUE.equals(r.getVerdict()))
                .count();
        final long successCount =
                modelResults.stream().filter(ModelStepResult::isSuccess).count();
        final boolean hasDisagreement = successCount > 0 && agreedCount > 0 && agreedCount < successCount;
        final double agreementPercent = successCount > 0
                ? (double) Math.max(agreedCount, successCount - agreedCount) / successCount * 100
                : 100.0;

        return Optional.of(AgentGoalAccuracyExplanation.builder()
                .score(score)
                .language(language)
                .mode(mode)
                .conversation(conversation)
                .referenceGoal(referenceGoal)
                .inferredGoal(inferredGoal)
                .goalAchieved(goalAchieved)
                .inferGoalModelResults(inferGoalModelResults)
                .modelResults(modelResults)
                .hasModelDisagreement(hasDisagreement)
                .agreementPercent(agreementPercent)
                .build());
    }

    private String extractConversationFromRequest(final String request) {
        // Extract "Conversation:" from prompt
        if (request.contains("Conversation:")) {
            final int start = request.indexOf("Conversation:") + 13;
            // Find where conversation ends - at the next section marker
            int end = request.length();
            final String[] sectionMarkers = {"Instructions:", "Goal:", "Expected Outcome:", "Respond with"};
            for (final String marker : sectionMarkers) {
                final int markerIdx = request.indexOf(marker, start);
                if (markerIdx > start && markerIdx < end) {
                    end = markerIdx;
                }
            }
            return request.substring(start, end).trim();
        }
        return "";
    }

    private String extractGoalFromRequest(final String request) {
        // Extract "Expected Outcome:" or "Goal:" from prompt
        if (request.contains("Expected Outcome:")) {
            final int start = request.indexOf("Expected Outcome:") + 17;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        if (request.contains("Goal:")) {
            final int start = request.indexOf("Goal:") + 5;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            return request.substring(start).trim();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Optional<ScoreExplanation> extractToolCallAccuracy(
            final List<StepExecutionData> steps,
            final Double score,
            final String language,
            final Object config,
            final Map<String, Object> metadata) {
        String mode = "STRICT";
        double precision = 0.0;
        double recall = 0.0;
        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        final List<ToolCallAccuracyExplanation.ToolCallMatch> matches = new ArrayList<>();

        // Extract mode from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("mode")) {
                    mode = configJson.get("mode").asText("STRICT");
                }
            } catch (final Exception e) {
                log.debug("Failed to parse tool call accuracy config: {}", e.getMessage());
            }
        }

        // Primary source: extract from metadata (passed by metric via afterMetricEvaluation)
        if (metadata != null) {
            if (metadata.containsKey("precision")) {
                precision = ((Number) metadata.get("precision")).doubleValue();
            }
            if (metadata.containsKey("recall")) {
                recall = ((Number) metadata.get("recall")).doubleValue();
            }
            if (metadata.containsKey("truePositives")) {
                truePositives = ((Number) metadata.get("truePositives")).intValue();
            }
            if (metadata.containsKey("falsePositives")) {
                falsePositives = ((Number) metadata.get("falsePositives")).intValue();
            }
            if (metadata.containsKey("falseNegatives")) {
                falseNegatives = ((Number) metadata.get("falseNegatives")).intValue();
            }
            if (metadata.containsKey("mode")) {
                mode = metadata.get("mode").toString();
            }
            // Extract matches from metadata
            if (metadata.containsKey("matches")) {
                final Object matchesObj = metadata.get("matches");
                if (matchesObj instanceof List<?>) {
                    for (final Object matchObj : (List<?>) matchesObj) {
                        try {
                            final JsonNode m = OBJECT_MAPPER.valueToTree(matchObj);
                            final JsonNode actualCall = m.get("actualCall");
                            final String toolName = actualCall != null ? getTextSafe(actualCall, "name") : "";
                            final Map<String, Object> arguments = new HashMap<>();
                            if (actualCall != null && actualCall.has("arguments")) {
                                actualCall
                                        .get("arguments")
                                        .fields()
                                        .forEachRemaining(entry -> arguments.put(
                                                entry.getKey(), entry.getValue().asText()));
                            }

                            matches.add(ToolCallAccuracyExplanation.ToolCallMatch.builder()
                                    .toolName(toolName)
                                    .arguments(arguments)
                                    .matched(
                                            m.has("matched") && m.get("matched").asBoolean())
                                    .matchScore(
                                            m.has("matchScore")
                                                    ? m.get("matchScore").asDouble()
                                                    : 0.0)
                                    .build());
                        } catch (final Exception e) {
                            log.debug("Failed to parse match from metadata: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // Fallback: try to extract from step results (legacy behavior)
        if (precision == 0.0 && recall == 0.0) {
            for (final StepExecutionData step : steps) {
                final String stepName = step.getStepName();

                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());

                            // Extract precision/recall from ComputePrecisionRecall step
                            if ("ComputePrecisionRecall".equalsIgnoreCase(stepName)) {
                                if (json.has("precision")) {
                                    precision = json.get("precision").asDouble();
                                }
                                if (json.has("recall")) {
                                    recall = json.get("recall").asDouble();
                                }
                                if (json.has("truePositives")) {
                                    truePositives = json.get("truePositives").asInt();
                                }
                                if (json.has("falsePositives")) {
                                    falsePositives = json.get("falsePositives").asInt();
                                }
                                if (json.has("falseNegatives")) {
                                    falseNegatives = json.get("falseNegatives").asInt();
                                }
                            }

                            // Extract matches from AlignToolCalls step
                            if ("AlignToolCalls".equalsIgnoreCase(stepName) && matches.isEmpty()) {
                                final JsonNode matchesNode = json.get("matches");
                                if (matchesNode != null && matchesNode.isArray()) {
                                    for (final JsonNode m : matchesNode) {
                                        final JsonNode actualCall = m.get("actualCall");
                                        final String toolName =
                                                actualCall != null ? getTextSafe(actualCall, "name") : "";
                                        final Map<String, Object> arguments = new HashMap<>();
                                        if (actualCall != null && actualCall.has("arguments")) {
                                            actualCall
                                                    .get("arguments")
                                                    .fields()
                                                    .forEachRemaining(entry -> arguments.put(
                                                            entry.getKey(),
                                                            entry.getValue().asText()));
                                        }

                                        matches.add(ToolCallAccuracyExplanation.ToolCallMatch.builder()
                                                .toolName(toolName)
                                                .arguments(arguments)
                                                .matched(m.has("matched")
                                                        && m.get("matched").asBoolean())
                                                .matchScore(
                                                        m.has("matchScore")
                                                                ? m.get("matchScore")
                                                                        .asDouble()
                                                                : 0.0)
                                                .build());
                                    }
                                }
                            }
                            break;
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse tool call accuracy JSON: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // If no explicit precision/recall found, calculate from score
        if (precision == 0.0 && recall == 0.0 && score != null && score > 0) {
            // Assume equal precision and recall when we only have F1
            precision = score;
            recall = score;
        }

        return Optional.of(ToolCallAccuracyExplanation.builder()
                .score(score)
                .language(language)
                .mode(mode)
                .precision(precision)
                .recall(recall)
                .truePositives(truePositives)
                .falsePositives(falsePositives)
                .falseNegatives(falseNegatives)
                .matches(matches)
                .build());
    }

    @SuppressWarnings("unchecked")
    private Optional<ScoreExplanation> extractTopicAdherence(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String mode = "F1";
        double precision = 0.0;
        double recall = 0.0;
        final List<String> extractedTopics = new ArrayList<>();
        final List<String> referenceTopics = new ArrayList<>();
        final List<TopicAdherenceExplanation.TopicClassificationItem> classifications = new ArrayList<>();

        // Extract mode from config
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("mode")) {
                    mode = configJson.get("mode").asText("F1");
                }
            } catch (final Exception e) {
                log.debug("Failed to parse topic adherence config: {}", e.getMessage());
            }
        }

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract reference topics from request
            if (step.getRequest() != null && referenceTopics.isEmpty()) {
                extractReferenceTopicsFromRequest(step.getRequest(), referenceTopics);
            }

            // Extract topics from ExtractTopics step
            if ("ExtractTopics".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            final JsonNode topicsNode = json.get("topics");
                            if (topicsNode != null && topicsNode.isArray()) {
                                for (final JsonNode t : topicsNode) {
                                    extractedTopics.add(t.asText());
                                }
                                break; // Use first successful model's topics
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse extracted topics JSON: {}", e.getMessage());
                        }
                    }
                }
            }

            // Extract classifications from ClassifyTopics step
            if ("ClassifyTopics".equalsIgnoreCase(stepName)) {
                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            final JsonNode classificationsNode = json.get("classifications");
                            if (classificationsNode != null && classificationsNode.isArray()) {
                                for (final JsonNode c : classificationsNode) {
                                    classifications.add(TopicAdherenceExplanation.TopicClassificationItem.builder()
                                            .extractedTopic(getTextSafe(c, "extractedTopic"))
                                            .onTopic(c.has("onTopic")
                                                    && c.get("onTopic").asBoolean())
                                            .matchedReferenceTopic(getTextSafe(c, "matchedReferenceTopic"))
                                            .reasoning(getTextSafe(c, "reasoning"))
                                            .build());
                                }
                                break; // Use first successful model's classifications
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse topic classifications JSON: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // Calculate precision and recall from classifications
        if (!classifications.isEmpty()) {
            final long onTopicCount = classifications.stream()
                    .filter(TopicAdherenceExplanation.TopicClassificationItem::isOnTopic)
                    .count();
            precision = (double) onTopicCount / classifications.size();

            final long coveredReferenceCount = classifications.stream()
                    .filter(TopicAdherenceExplanation.TopicClassificationItem::isOnTopic)
                    .map(TopicAdherenceExplanation.TopicClassificationItem::getMatchedReferenceTopic)
                    .filter(t -> t != null && !t.isEmpty())
                    .distinct()
                    .count();
            recall = referenceTopics.isEmpty() ? 0.0 : (double) coveredReferenceCount / referenceTopics.size();
        }

        return Optional.of(TopicAdherenceExplanation.builder()
                .score(score)
                .language(language)
                .mode(mode)
                .precision(precision)
                .recall(recall)
                .extractedTopics(extractedTopics)
                .referenceTopics(referenceTopics)
                .classifications(classifications)
                .build());
    }

    private void extractReferenceTopicsFromRequest(final String request, final List<String> referenceTopics) {
        // Extract "Reference Topics:" from prompt
        if (request.contains("Reference Topics")) {
            int start = request.indexOf("Reference Topics");
            // Skip past the label
            start = request.indexOf(":", start);
            if (start > 0) {
                start++;
                // Find where topics list ends
                int end = request.length();
                final String[] sectionMarkers = {"Instructions:", "Respond with", "Your task"};
                for (final String marker : sectionMarkers) {
                    final int markerIdx = request.indexOf(marker, start);
                    if (markerIdx > start && markerIdx < end) {
                        end = markerIdx;
                    }
                }
                final String topicsSection = request.substring(start, end).trim();
                // Parse topics (typically as "- topic1\n- topic2" or comma-separated)
                for (final String line : topicsSection.split("\n")) {
                    String topic = line.trim();
                    if (topic.startsWith("-")) {
                        topic = topic.substring(1).trim();
                    }
                    if (!topic.isEmpty()) {
                        referenceTopics.add(topic);
                    }
                }
            }
        }
    }

    private Optional<ScoreExplanation> extractContextRelevance(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String userInput = "";
        final List<ContextRelevanceExplanation.ContextEvaluation> contextEvaluations = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract user input from request
            if (step.getRequest() != null && userInput.isEmpty()) {
                userInput = extractUserInputFromRequest(step.getRequest());
                if (userInput.isEmpty()) {
                    userInput = extractQuestionFromRequest(step.getRequest());
                }
            }

            // Extract context evaluations from EvaluateRelevance steps
            if (stepName != null && stepName.startsWith("EvaluateRelevance")) {
                // Extract context text from request
                final String context = step.getRequest() != null ? extractContextFromRequest(step.getRequest()) : "";

                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            if (json.has("score")) {
                                final int rawScore = json.get("score").asInt();
                                final double normalizedScore = rawScore / 2.0;
                                final String reasoning = getTextSafe(json, "reasoning");

                                contextEvaluations.add(ContextRelevanceExplanation.ContextEvaluation.builder()
                                        .context(context)
                                        .rawScore(rawScore)
                                        .normalizedScore(normalizedScore)
                                        .reasoning(reasoning)
                                        .build());
                                break; // Use first successful model's evaluation for this context
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse context relevance JSON: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return Optional.of(ContextRelevanceExplanation.builder()
                .score(score)
                .language(language)
                .userInput(userInput)
                .contextEvaluations(contextEvaluations)
                .build());
    }

    private Optional<ScoreExplanation> extractResponseGroundedness(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String context = "";
        int rawScore = 0;
        String reasoning = "";
        boolean usedHeuristics = false;

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Check if heuristics were used
            if (stepName != null && stepName.equals("ApplyHeuristics")) {
                usedHeuristics = true;
                // Extract response from request
                if (step.getRequest() != null && response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
            }

            // Extract groundedness evaluation from EvaluateGroundedness step
            if (stepName != null && stepName.equals("EvaluateGroundedness")) {
                // Extract response and context from request
                if (step.getRequest() != null) {
                    if (response.isEmpty()) {
                        response = extractResponseFromRequest(step.getRequest());
                    }
                    context = extractContextFromRequest(step.getRequest());
                }

                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            if (json.has("score")) {
                                rawScore = json.get("score").asInt();
                                reasoning = getTextSafe(json, "reasoning");
                                break; // Use first successful model's evaluation
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse response groundedness JSON: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        // If heuristics returned early with score 1.0, set rawScore to 2
        if (usedHeuristics && score != null && score >= 1.0 && rawScore == 0) {
            rawScore = 2;
            reasoning = "Heuristic match - response exactly matches or is contained in context";
        }

        return Optional.of(ResponseGroundednessExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .context(context)
                .rawScore(rawScore)
                .reasoning(reasoning)
                .usedHeuristics(usedHeuristics)
                .build());
    }

    private Optional<ScoreExplanation> extractAnswerAccuracy(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        int rawScore = 0;
        String reasoning = "";
        boolean usedDualJudge = false;
        Integer confirmationScore = null;
        String confirmationReasoning = "";

        for (final StepExecutionData step : steps) {
            final String stepName = step.getStepName();

            // Extract initial judgment
            if (stepName != null && stepName.equals("InitialJudgment")) {
                // Extract response and reference from request
                if (step.getRequest() != null) {
                    response = extractResponseFromRequest(step.getRequest());
                    reference = extractReferenceFromRequest(step.getRequest());
                }

                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            if (json.has("score")) {
                                rawScore = json.get("score").asInt();
                                reasoning = getTextSafe(json, "reasoning");
                                break; // Use first successful model's evaluation
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse answer accuracy initial JSON: {}", e.getMessage());
                        }
                    }
                }
            }

            // Extract confirmation judgment if present
            if (stepName != null && stepName.equals("ConfirmJudgment")) {
                usedDualJudge = true;

                for (final ModelExecutionData result : step.getModelResults()) {
                    if (result.isSuccess() && result.getResultJson() != null) {
                        try {
                            final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                            if (json.has("score")) {
                                confirmationScore = json.get("score").asInt();
                                confirmationReasoning = getTextSafe(json, "reasoning");
                                break; // Use first successful model's evaluation
                            }
                        } catch (final JsonProcessingException e) {
                            log.debug("Failed to parse answer accuracy confirmation JSON: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        return Optional.of(AnswerAccuracyExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .rawScore(rawScore)
                .reasoning(reasoning)
                .usedDualJudge(usedDualJudge)
                .confirmationScore(confirmationScore)
                .confirmationReasoning(confirmationReasoning)
                .build());
    }

    // ==================== NLP Metrics (Non-LLM) ====================

    private Optional<ScoreExplanation> extractBleuScore(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        int maxNgram = 4;
        boolean smoothing = true;

        // Extract config parameters
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("maxNgram")) {
                    maxNgram = configJson.get("maxNgram").asInt(4);
                }
                if (configJson.has("smoothing")) {
                    smoothing = configJson.get("smoothing").asBoolean(true);
                }
            } catch (final Exception e) {
                log.debug("Failed to parse BLEU score config: {}", e.getMessage());
            }
        }

        // Extract response and reference from steps (if available)
        for (final StepExecutionData step : steps) {
            if (step.getRequest() != null) {
                if (response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
            }
        }

        return Optional.of(BleuScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .maxNgram(maxNgram)
                .smoothing(smoothing)
                .build());
    }

    private Optional<ScoreExplanation> extractRougeScore(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        String rougeType = "ROUGE_L";
        String mode = "FMEASURE";

        // Extract config parameters
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("rougeType")) {
                    rougeType = configJson.get("rougeType").asText("ROUGE_L");
                }
                if (configJson.has("mode")) {
                    mode = configJson.get("mode").asText("FMEASURE");
                }
            } catch (final Exception e) {
                log.debug("Failed to parse ROUGE score config: {}", e.getMessage());
            }
        }

        // Extract response and reference from steps (if available)
        for (final StepExecutionData step : steps) {
            if (step.getRequest() != null) {
                if (response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
            }
        }

        return Optional.of(RougeScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .rougeType(rougeType)
                .mode(mode)
                .build());
    }

    private Optional<ScoreExplanation> extractChrfScore(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        int charNgramOrder = 6;
        int wordNgramOrder = 0;
        double beta = 2.0;

        // Extract config parameters
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("charNgramOrder")) {
                    charNgramOrder = configJson.get("charNgramOrder").asInt(6);
                }
                if (configJson.has("wordNgramOrder")) {
                    wordNgramOrder = configJson.get("wordNgramOrder").asInt(0);
                }
                if (configJson.has("beta")) {
                    beta = configJson.get("beta").asDouble(2.0);
                }
            } catch (final Exception e) {
                log.debug("Failed to parse chrF score config: {}", e.getMessage());
            }
        }

        // Extract response and reference from steps (if available)
        for (final StepExecutionData step : steps) {
            if (step.getRequest() != null) {
                if (response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
            }
        }

        return Optional.of(ChrfScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .charNgramOrder(charNgramOrder)
                .wordNgramOrder(wordNgramOrder)
                .beta(beta)
                .build());
    }

    private Optional<ScoreExplanation> extractStringSimilarity(
            final List<StepExecutionData> steps, final Double score, final String language, final Object config) {
        String response = "";
        String reference = "";
        String distanceMeasure = "JARO_WINKLER";
        boolean caseSensitive = false;

        // Extract config parameters
        if (config != null) {
            try {
                final JsonNode configJson = OBJECT_MAPPER.valueToTree(config);
                if (configJson.has("distanceMeasure")) {
                    distanceMeasure = configJson.get("distanceMeasure").asText("JARO_WINKLER");
                }
                if (configJson.has("caseSensitive")) {
                    caseSensitive = configJson.get("caseSensitive").asBoolean(false);
                }
            } catch (final Exception e) {
                log.debug("Failed to parse string similarity config: {}", e.getMessage());
            }
        }

        // Extract response and reference from steps (if available)
        for (final StepExecutionData step : steps) {
            if (step.getRequest() != null) {
                if (response.isEmpty()) {
                    response = extractResponseFromRequest(step.getRequest());
                }
                if (reference.isEmpty()) {
                    reference = extractReferenceFromRequest(step.getRequest());
                }
            }
        }

        return Optional.of(StringSimilarityExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .distanceMeasure(distanceMeasure)
                .caseSensitive(caseSensitive)
                .build());
    }
}
