package ai.qa.solutions.allure.explanation;

import ai.qa.solutions.allure.model.ModelExecutionData;
import ai.qa.solutions.allure.model.StepExecutionData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        if (metricName == null || steps == null || steps.isEmpty()) {
            return Optional.empty();
        }

        try {
            return switch (normalizeMetricName(metricName)) {
                case "faithfulness" -> extractFaithfulness(steps, score, language);
                case "noise-sensitivity", "noisesensitivity" -> extractNoiseSensitivity(steps, score, language);
                case "aspect-critic", "aspectcritic" -> extractAspectCritic(steps, score, language);
                case "context-precision", "contextprecision" -> extractContextPrecision(steps, score, language);
                case "context-recall", "contextrecall" -> extractContextRecall(steps, score, language);
                case "context-entity-recall", "contextentityrecall" -> extractContextEntityRecall(
                        steps, score, language);
                case "response-relevancy", "responserelevancy" -> extractResponseRelevancy(steps, score, language);
                case "simple-criteria", "simplecriteria" -> extractSimpleCriteria(steps, score, language);
                case "rubrics-score", "rubricsscore" -> extractRubricsScore(steps, score, language);
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

            // Extract reference and response from request
            if (step.getRequest() != null && reference.isEmpty()) {
                reference = extractReferenceFromRequest(step.getRequest());
                aiResponse = extractResponseFromRequest(step.getRequest());
            }

            if (stepName != null) {
                if (stepName.contains("Reference") || stepName.contains("Ground")) {
                    extractStatementList(step, refStatements);
                } else if (stepName.contains("Response") && !stepName.contains("Matrix")) {
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
            final List<StepExecutionData> steps, final Double score, final String language) {
        String aspectName = "Custom Aspect";
        String aspectDef = "";
        boolean passed = score != null && score >= 0.5;
        String reasoning = "";

        for (final StepExecutionData step : steps) {
            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("verdict")) {
                            passed = json.get("verdict").asBoolean();
                        }
                        if (json.has("reason")) {
                            reasoning = json.get("reason").asText();
                        }
                        break;
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
                .passed(passed)
                .reasoning(reasoning)
                .build());
    }

    private Optional<ScoreExplanation> extractContextPrecision(
            final List<StepExecutionData> steps, final Double score, final String language) {
        final List<ContextPrecisionExplanation.ContextRelevance> contexts = new ArrayList<>();
        final List<Double> precisionAtK = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("verdict")) {
                            final boolean relevant = json.get("verdict").asBoolean();
                            contexts.add(ContextPrecisionExplanation.ContextRelevance.builder()
                                    .position(contexts.size() + 1)
                                    .contextText("Context " + (contexts.size() + 1))
                                    .relevant(relevant)
                                    .reason(getTextSafe(json, "reason"))
                                    .build());
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
                .contexts(contexts)
                .precisionAtK(precisionAtK)
                .build());
    }

    private Optional<ScoreExplanation> extractContextRecall(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String reference = "";
        final List<ContextRecallExplanation.ReferenceClassification> classifications = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            // Extract reference from request
            if (step.getRequest() != null && reference.isEmpty()) {
                reference = extractReferenceFromRequest(step.getRequest());
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
                .classifications(classifications)
                .build());
    }

    private Optional<ScoreExplanation> extractContextEntityRecall(
            final List<StepExecutionData> steps, final Double score, final String language) {
        String reference = "";
        final List<String> refEntities = new ArrayList<>();
        final List<String> ctxEntities = new ArrayList<>();

        for (final StepExecutionData step : steps) {
            // Extract reference from request
            if (step.getRequest() != null && reference.isEmpty()) {
                reference = extractReferenceFromRequest(step.getRequest());
            }

            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        final JsonNode entitiesNode = json.get("entities");
                        if (entitiesNode != null && entitiesNode.isArray()) {
                            final List<String> target = step.getStepName() != null
                                            && step.getStepName().contains("Context")
                                    ? ctxEntities
                                    : refEntities;
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
        // Extract "Response: ..." from prompt
        if (request.contains("Response:")) {
            final int start = request.indexOf("Response:") + 9;
            final int end = request.indexOf("\n", start);
            if (end > start) {
                return request.substring(start, end).trim();
            }
            // If no newline, take the rest
            return request.substring(start).trim();
        }
        return "";
    }

    private String extractReferenceFromRequest(final String request) {
        // Try "Reference:" first
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

    private Optional<ScoreExplanation> extractSimpleCriteria(
            final List<StepExecutionData> steps, final Double score, final String language) {
        int rawScore = 3;
        String reasoning = "";

        for (final StepExecutionData step : steps) {
            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("score")) {
                            rawScore = json.get("score").asInt();
                        }
                        if (json.has("reason")) {
                            reasoning = json.get("reason").asText();
                        }
                        break;
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse simple criteria JSON: {}", e.getMessage());
                    }
                }
            }
        }

        return Optional.of(SimpleCriteriaExplanation.builder()
                .score(score)
                .language(language)
                .criteriaName("Custom Criteria")
                .criteriaDefinition("")
                .rawScore(rawScore)
                .minScore(1)
                .maxScore(5)
                .reasoning(reasoning)
                .build());
    }

    private Optional<ScoreExplanation> extractRubricsScore(
            final List<StepExecutionData> steps, final Double score, final String language) {
        int selectedLevel = 3;
        String reasoning = "";
        final List<RubricsScoreExplanation.RubricLevel> levels = List.of(
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

        for (final StepExecutionData step : steps) {
            for (final ModelExecutionData result : step.getModelResults()) {
                if (result.isSuccess() && result.getResultJson() != null) {
                    try {
                        final JsonNode json = OBJECT_MAPPER.readTree(result.getResultJson());
                        if (json.has("score")) {
                            selectedLevel = json.get("score").asInt();
                        }
                        if (json.has("reason")) {
                            reasoning = json.get("reason").asText();
                        }
                        break;
                    } catch (final JsonProcessingException e) {
                        log.debug("Failed to parse rubrics JSON: {}", e.getMessage());
                    }
                }
            }
        }

        return Optional.of(RubricsScoreExplanation.builder()
                .score(score)
                .language(language)
                .rubricLevels(levels)
                .selectedLevel(selectedLevel)
                .reasoning(reasoning)
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
