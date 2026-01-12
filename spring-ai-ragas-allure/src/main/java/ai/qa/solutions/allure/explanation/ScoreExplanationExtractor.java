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
}
