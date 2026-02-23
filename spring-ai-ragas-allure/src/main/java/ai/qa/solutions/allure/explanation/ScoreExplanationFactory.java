package ai.qa.solutions.allure.explanation;

import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.metric.metadata.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory that creates {@link ScoreExplanation} instances from typed metadata records.
 * <p>
 * Creates score explanations using typed {@code instanceof} dispatch on metadata records provided by metrics
 * via {@link ai.qa.solutions.execution.listener.dto.MetricEvaluationResult#getMetadata()}.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * ScoreExplanationFactory factory = new ScoreExplanationFactory();
 * Optional<ScoreExplanation> explanation = factory.create(result, "en");
 * }</pre>
 */
@Slf4j
public class ScoreExplanationFactory {

    /**
     * Creates a score explanation from the evaluation result's typed metadata.
     *
     * @param result   the metric evaluation result containing score, metadata and sample
     * @param language the report language ("en" or "ru")
     * @return optional explanation, empty if result/metadata is null or metric type not supported
     */
    public Optional<ScoreExplanation> create(final MetricEvaluationResult result, final String language) {
        if (result == null || result.getMetadata() == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(createExplanation(result, language));
        } catch (final Exception e) {
            log.warn(
                    "Failed to create explanation from metadata {}: {}",
                    result.getMetadata().getClass().getSimpleName(),
                    e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("ChainedIfElse")
    private ScoreExplanation createExplanation(final MetricEvaluationResult result, final String language) {
        final Double score = result.getAggregatedScore();
        final var metadata = result.getMetadata();

        if (metadata instanceof FaithfulnessMetadata fm) {
            return createFaithfulnessExplanation(score, fm, language);
        } else if (metadata instanceof AspectCriticMetadata am) {
            return createAspectCriticExplanation(score, am, language);
        } else if (metadata instanceof SimpleCriteriaMetadata sm) {
            return createSimpleCriteriaExplanation(score, sm, language);
        } else if (metadata instanceof RubricsMetadata rm) {
            return createRubricsExplanation(score, rm, language);
        } else if (metadata instanceof ContextPrecisionMetadata cpm) {
            return createContextPrecisionExplanation(score, cpm, language);
        } else if (metadata instanceof ContextRecallMetadata crm) {
            return createContextRecallExplanation(score, crm, language);
        } else if (metadata instanceof ContextEntityRecallMetadata cem) {
            return createContextEntityRecallExplanation(score, cem, language);
        } else if (metadata instanceof NoiseSensitivityMetadata nsm) {
            return createNoiseSensitivityExplanation(score, nsm, language);
        } else if (metadata instanceof ResponseRelevancyMetadata rrm) {
            return createResponseRelevancyExplanation(score, rrm, language);
        } else if (metadata instanceof FactualCorrectnessMetadata fcm) {
            return createFactualCorrectnessExplanation(score, fcm, language);
        } else if (metadata instanceof AnswerCorrectnessMetadata acm) {
            return createAnswerCorrectnessExplanation(score, acm, language);
        } else if (metadata instanceof SemanticSimilarityMetadata ssm) {
            return createSemanticSimilarityExplanation(score, ssm, language);
        } else if (metadata instanceof AgentGoalAccuracyMetadata agm) {
            return createAgentGoalAccuracyExplanation(score, agm, language);
        } else if (metadata instanceof ToolCallAccuracyMetadata tcm) {
            return createToolCallAccuracyExplanation(score, tcm, language);
        } else if (metadata instanceof TopicAdherenceMetadata tam) {
            return createTopicAdherenceExplanation(score, tam, language);
        } else if (metadata instanceof BleuScoreMetadata bm) {
            return createBleuScoreExplanation(score, bm, result, language);
        } else if (metadata instanceof RougeScoreMetadata rsm) {
            return createRougeScoreExplanation(score, rsm, result, language);
        } else if (metadata instanceof ChrfScoreMetadata csm) {
            return createChrfScoreExplanation(score, csm, result, language);
        } else if (metadata instanceof StringSimilarityMetadata stm) {
            return createStringSimilarityExplanation(score, stm, result, language);
        } else if (metadata instanceof AnswerAccuracyMetadata aam) {
            return createAnswerAccuracyExplanation(score, aam, language);
        } else if (metadata instanceof ContextRelevanceMetadata cxm) {
            return createContextRelevanceExplanation(score, cxm, language);
        } else if (metadata instanceof ResponseGroundednessMetadata rgm) {
            return createResponseGroundednessExplanation(score, rgm, language);
        } else if (metadata instanceof HallucinationMetadata) {
            return null; // TODO: add HallucinationExplanation in follow-up
        } else {
            log.debug(
                    "No explanation factory method for metadata type: {}",
                    metadata.getClass().getSimpleName());
            return null;
        }
    }

    // =========================================================================
    // Faithfulness
    // =========================================================================

    private ScoreExplanation createFaithfulnessExplanation(
            final Double score, final FaithfulnessMetadata fm, final String language) {
        // Use first model's data for display (aggregated view)
        final List<String> statements = extractFirstModelList(fm.extractedStatements());
        final List<FaithfulnessExplanation.StatementVerdict> verdicts = extractFaithfulnessVerdicts(fm.verdicts());

        if (verdicts.isEmpty()) {
            return null;
        }

        return FaithfulnessExplanation.builder()
                .score(score)
                .language(language)
                .aiResponse("") // Response is available from the sample, not metadata
                .statements(statements)
                .verdicts(verdicts)
                .build();
    }

    private List<FaithfulnessExplanation.StatementVerdict> extractFaithfulnessVerdicts(
            final Map<String, List<FaithfulnessMetadata.StatementVerdictSummary>> verdictsMap) {
        if (verdictsMap == null || verdictsMap.isEmpty()) {
            return List.of();
        }
        // Use first model's verdicts
        final List<FaithfulnessMetadata.StatementVerdictSummary> firstModelVerdicts =
                verdictsMap.values().iterator().next();
        return firstModelVerdicts.stream()
                .map(v -> FaithfulnessExplanation.StatementVerdict.builder()
                        .statement(v.statement())
                        .passed(v.verdict() == 1)
                        .reason(v.reason())
                        .build())
                .toList();
    }

    // =========================================================================
    // AspectCritic
    // =========================================================================

    private ScoreExplanation createAspectCriticExplanation(
            final Double score, final AspectCriticMetadata am, final String language) {
        // Determine aggregate pass/fail from model verdicts via majority voting
        final boolean passed = determineAspectCriticPassed(am.modelVerdicts(), score);

        // Get first model's reasoning for display
        final String reasoning = extractFirstModelValue(am.modelReasonings());

        return AspectCriticExplanation.builder()
                .score(score)
                .language(language)
                .aspectName(null) // Will use default
                .aspectDefinition(am.definition())
                .aiResponse("")
                .passed(passed)
                .reasoning(reasoning)
                .strictness(am.strictness())
                .modelIterationResults(am.modelVerdicts() != null ? am.modelVerdicts() : Map.of())
                .build();
    }

    private boolean determineAspectCriticPassed(final Map<String, List<Boolean>> modelVerdicts, final Double score) {
        if (score != null) {
            return score >= 0.5;
        }
        if (modelVerdicts == null || modelVerdicts.isEmpty()) {
            return false;
        }
        // Majority voting across models
        int passCount = 0;
        int failCount = 0;
        for (final List<Boolean> iterations : modelVerdicts.values()) {
            final long passVotes =
                    iterations.stream().filter(Boolean::booleanValue).count();
            if (passVotes > iterations.size() / 2) {
                passCount++;
            } else {
                failCount++;
            }
        }
        return passCount > failCount;
    }

    // =========================================================================
    // SimpleCriteria
    // =========================================================================

    private ScoreExplanation createSimpleCriteriaExplanation(
            final Double score, final SimpleCriteriaMetadata sm, final String language) {
        // Compute average raw score from first model
        final int avgRawScore = computeAverageRawScore(sm.modelRawScores());
        final String reasoning = extractFirstModelValue(sm.modelReasonings());

        // Build model scores map (int per model)
        final Map<String, Integer> modelScoresMap = new java.util.LinkedHashMap<>();
        if (sm.modelRawScores() != null) {
            sm.modelRawScores().forEach((modelId, scores) -> {
                if (!scores.isEmpty()) {
                    modelScoresMap.put(modelId, (int) Math.round(scores.get(0)));
                }
            });
        }

        return SimpleCriteriaExplanation.builder()
                .score(score)
                .language(language)
                .criteriaName(null)
                .criteriaDefinition(sm.definition())
                .aiResponse("")
                .modelScores(modelScoresMap)
                .rawScore(avgRawScore)
                .minScore((int) sm.minScore())
                .maxScore((int) sm.maxScore())
                .reasoning(reasoning)
                .build();
    }

    private int computeAverageRawScore(final Map<String, List<Double>> modelRawScores) {
        if (modelRawScores == null || modelRawScores.isEmpty()) {
            return 0;
        }
        // Use first model's first score
        return modelRawScores.values().stream()
                .filter(list -> !list.isEmpty())
                .findFirst()
                .map(list -> (int) Math.round(list.get(0)))
                .orElse(0);
    }

    // =========================================================================
    // Rubrics
    // =========================================================================

    private ScoreExplanation createRubricsExplanation(
            final Double score, final RubricsMetadata rm, final String language) {
        // Build rubric levels from the rubrics map
        final List<RubricsScoreExplanation.RubricLevel> rubricLevels = new ArrayList<>();
        if (rm.rubrics() != null) {
            rm.rubrics().forEach((key, description) -> {
                final int level = extractLevelFromKey(key);
                if (level > 0) {
                    rubricLevels.add(RubricsScoreExplanation.RubricLevel.builder()
                            .level(level)
                            .description(description)
                            .build());
                }
            });
        }
        rubricLevels.sort((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));

        // Get first model's score and reasoning
        final int selectedLevel = rm.modelScores() != null && !rm.modelScores().isEmpty()
                ? rm.modelScores().values().iterator().next()
                : 1;
        final String reasoning =
                rm.modelReasonings() != null && !rm.modelReasonings().isEmpty()
                        ? rm.modelReasonings().values().iterator().next()
                        : "";

        return RubricsScoreExplanation.builder()
                .score(score)
                .language(language)
                .rubricLevels(rubricLevels)
                .selectedLevel(selectedLevel)
                .aiResponse("")
                .reasoning(reasoning)
                .minLevel(0)
                .maxLevel(0) // Will be computed from rubric levels
                .build();
    }

    private int extractLevelFromKey(final String key) {
        if (key == null) {
            return 0;
        }
        // Extract numeric part from keys like "score1_description", "score2_description", etc.
        try {
            final String digits = key.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    // =========================================================================
    // ContextPrecision
    // =========================================================================

    private ScoreExplanation createContextPrecisionExplanation(
            final Double score, final ContextPrecisionMetadata cpm, final String language) {
        // Build context relevance items from first model's results
        final List<ContextPrecisionExplanation.ContextRelevance> contexts = new ArrayList<>();
        final List<Double> precisionAtK = new ArrayList<>();

        if (cpm.modelRelevanceResults() != null && !cpm.modelRelevanceResults().isEmpty()) {
            final List<Boolean> firstModelResults =
                    cpm.modelRelevanceResults().values().iterator().next();
            int cumulativeRelevant = 0;
            for (int i = 0; i < firstModelResults.size(); i++) {
                final boolean relevant = firstModelResults.get(i);
                if (relevant) {
                    cumulativeRelevant++;
                }
                contexts.add(ContextPrecisionExplanation.ContextRelevance.builder()
                        .position(i + 1)
                        .contextText("Context " + (i + 1))
                        .relevant(relevant)
                        .build());
                precisionAtK.add(relevant ? (double) cumulativeRelevant / (i + 1) : 0.0);
            }
        }

        return ContextPrecisionExplanation.builder()
                .score(score)
                .language(language)
                .userInput("")
                .contexts(contexts)
                .precisionAtK(precisionAtK)
                .build();
    }

    // =========================================================================
    // ContextRecall
    // =========================================================================

    private ScoreExplanation createContextRecallExplanation(
            final Double score, final ContextRecallMetadata crm, final String language) {
        // Build classifications from first model's data
        final List<ContextRecallExplanation.ReferenceClassification> classifications = new ArrayList<>();

        if (crm.classifications() != null && !crm.classifications().isEmpty()) {
            final List<ContextRecallMetadata.ClassificationSummary> firstModelClassifications =
                    crm.classifications().values().iterator().next();
            for (final ContextRecallMetadata.ClassificationSummary cs : firstModelClassifications) {
                classifications.add(ContextRecallExplanation.ReferenceClassification.builder()
                        .statement(cs.statement())
                        .found(cs.attributed() == 1)
                        .reason(cs.reason())
                        .build());
            }
        }

        return ContextRecallExplanation.builder()
                .score(score)
                .language(language)
                .reference("")
                .contexts("")
                .classifications(classifications)
                .build();
    }

    // =========================================================================
    // ContextEntityRecall
    // =========================================================================

    private ScoreExplanation createContextEntityRecallExplanation(
            final Double score, final ContextEntityRecallMetadata cem, final String language) {
        final List<String> refEntities = extractFirstModelList(cem.referenceEntities());
        final List<String> ctxEntities = extractFirstModelList(cem.contextEntities());

        // Common entities from first model
        final List<String> foundEntities;
        if (cem.commonEntities() != null && !cem.commonEntities().isEmpty()) {
            final Set<String> firstCommon =
                    cem.commonEntities().values().iterator().next();
            foundEntities = new ArrayList<>(firstCommon);
        } else {
            foundEntities = List.of();
        }

        final List<String> missingEntities = new ArrayList<>(refEntities);
        missingEntities.removeAll(foundEntities);

        return ContextEntityRecallExplanation.builder()
                .score(score)
                .language(language)
                .reference("")
                .contexts("")
                .referenceEntities(refEntities)
                .contextEntities(ctxEntities)
                .foundEntities(foundEntities)
                .missingEntities(missingEntities)
                .build();
    }

    // =========================================================================
    // NoiseSensitivity
    // =========================================================================

    private ScoreExplanation createNoiseSensitivityExplanation(
            final Double score, final NoiseSensitivityMetadata nsm, final String language) {
        final List<String> refStatements = extractFirstModelList(nsm.referenceStatements());
        final List<String> respStatements = extractFirstModelList(nsm.responseStatements());

        // Build empty matches (detailed match info is not stored in metadata)
        return NoiseSensitivityExplanation.builder()
                .score(score)
                .language(language)
                .reference("")
                .aiResponse("")
                .referenceStatements(refStatements)
                .responseStatements(respStatements)
                .matches(List.of())
                .mode(nsm.mode())
                .build();
    }

    // =========================================================================
    // ResponseRelevancy
    // =========================================================================

    private ScoreExplanation createResponseRelevancyExplanation(
            final Double score, final ResponseRelevancyMetadata rrm, final String language) {
        // Build generated questions from first model
        final List<String> questions = extractFirstModelList(rrm.generatedQuestions());
        final List<ResponseRelevancyExplanation.GeneratedQuestion> genQuestions = questions.stream()
                .map(q -> ResponseRelevancyExplanation.GeneratedQuestion.builder()
                        .question(q)
                        .similarity(0.0) // Individual similarity not tracked per question
                        .build())
                .toList();

        // Build per-model similarity results
        final List<ResponseRelevancyExplanation.ModelSimilarityResult> modelResults = new ArrayList<>();
        if (rrm.similarityScores() != null) {
            rrm.similarityScores()
                    .forEach((modelId, similarity) ->
                            modelResults.add(ResponseRelevancyExplanation.ModelSimilarityResult.builder()
                                    .modelId(modelId)
                                    .similarity(similarity)
                                    .build()));
        }

        return ResponseRelevancyExplanation.builder()
                .score(score)
                .language(language)
                .originalQuestion("")
                .aiResponse("")
                .generatedQuestions(genQuestions)
                .modelResults(modelResults)
                .build();
    }

    // =========================================================================
    // FactualCorrectness
    // =========================================================================

    private ScoreExplanation createFactualCorrectnessExplanation(
            final Double score, final FactualCorrectnessMetadata fcm, final String language) {
        final List<String> responseClaims = extractFirstModelList(fcm.responseClaims());
        final List<String> referenceClaims = extractFirstModelList(fcm.referenceClaims());

        final List<FactualCorrectnessExplanation.ClaimVerdict> precisionVerdicts =
                extractNliVerdicts(fcm.precisionVerdicts());
        final List<FactualCorrectnessExplanation.ClaimVerdict> recallVerdicts =
                extractNliVerdicts(fcm.recallVerdicts());

        return FactualCorrectnessExplanation.builder()
                .score(score)
                .language(language)
                .response("")
                .reference("")
                .responseClaims(responseClaims)
                .referenceClaims(referenceClaims)
                .precisionVerdicts(precisionVerdicts)
                .recallVerdicts(recallVerdicts)
                .mode(fcm.mode())
                .build();
    }

    private List<FactualCorrectnessExplanation.ClaimVerdict> extractNliVerdicts(
            final Map<String, List<FactualCorrectnessMetadata.NliVerdictSummary>> verdictsMap) {
        if (verdictsMap == null || verdictsMap.isEmpty()) {
            return List.of();
        }
        final List<FactualCorrectnessMetadata.NliVerdictSummary> first =
                verdictsMap.values().iterator().next();
        return first.stream()
                .map(v -> FactualCorrectnessExplanation.ClaimVerdict.builder()
                        .claim(v.claim())
                        .verdict(v.verdict())
                        .reason(v.reason())
                        .build())
                .toList();
    }

    // =========================================================================
    // AnswerCorrectness
    // =========================================================================

    private ScoreExplanation createAnswerCorrectnessExplanation(
            final Double score, final AnswerCorrectnessMetadata acm, final String language) {
        return AnswerCorrectnessExplanation.builder()
                .score(score)
                .language(language)
                .response("")
                .reference("")
                .factualScore(acm.factualScore())
                .semanticScore(acm.semanticScore())
                .factualWeight(acm.normalizedFactualWeight())
                .semanticWeight(acm.normalizedSemanticWeight())
                .build();
    }

    // =========================================================================
    // SemanticSimilarity
    // =========================================================================

    private ScoreExplanation createSemanticSimilarityExplanation(
            final Double score, final SemanticSimilarityMetadata ssm, final String language) {
        final List<SemanticSimilarityExplanation.ModelSimilarityResult> modelResults = new ArrayList<>();
        if (ssm.embeddingModelScores() != null) {
            ssm.embeddingModelScores()
                    .forEach((modelId, similarity) ->
                            modelResults.add(SemanticSimilarityExplanation.ModelSimilarityResult.builder()
                                    .modelId(modelId)
                                    .similarity(similarity)
                                    .build()));
        }

        return SemanticSimilarityExplanation.builder()
                .score(score)
                .language(language)
                .response("")
                .reference("")
                .modelResults(modelResults)
                .threshold(ssm.threshold())
                .build();
    }

    // =========================================================================
    // AgentGoalAccuracy
    // =========================================================================

    private ScoreExplanation createAgentGoalAccuracyExplanation(
            final Double score, final AgentGoalAccuracyMetadata agm, final String language) {
        final boolean goalAchieved = score != null && score >= 0.5;

        // Build model results for the explanation
        final List<ModelStepResult> modelResults = new ArrayList<>();
        if (agm.modelVerdicts() != null) {
            agm.modelVerdicts().forEach((modelId, verdict) -> {
                final String reasoning =
                        agm.modelReasonings() != null ? agm.modelReasonings().getOrDefault(modelId, "") : "";
                modelResults.add(ModelStepResult.builder()
                        .modelId(modelId)
                        .success(true)
                        .verdict(verdict)
                        .reasoning(reasoning)
                        .build());
            });
        }

        // Calculate agreement
        final long passCount = agm.modelVerdicts() != null
                ? agm.modelVerdicts().values().stream()
                        .filter(Boolean::booleanValue)
                        .count()
                : 0;
        final int totalModels =
                agm.modelVerdicts() != null ? agm.modelVerdicts().size() : 0;
        final boolean hasDisagreement = totalModels > 0 && passCount > 0 && passCount < totalModels;
        final double agreementPercent =
                totalModels > 0 ? (double) Math.max(passCount, totalModels - passCount) / totalModels * 100 : 100.0;

        return AgentGoalAccuracyExplanation.builder()
                .score(score)
                .language(language)
                .mode(agm.mode())
                .conversation("")
                .referenceGoal("")
                .inferredGoal(agm.inferredGoal())
                .goalAchieved(goalAchieved)
                .inferGoalModelResults(List.of())
                .modelResults(modelResults)
                .hasModelDisagreement(hasDisagreement)
                .agreementPercent(agreementPercent)
                .build();
    }

    // =========================================================================
    // ToolCallAccuracy
    // =========================================================================

    private ScoreExplanation createToolCallAccuracyExplanation(
            final Double score, final ToolCallAccuracyMetadata tcm, final String language) {
        final List<ToolCallAccuracyExplanation.ToolCallMatch> matches = new ArrayList<>();
        if (tcm.matches() != null) {
            for (final ToolCallAccuracyMetadata.ToolCallMatchSummary ms : tcm.matches()) {
                matches.add(ToolCallAccuracyExplanation.ToolCallMatch.builder()
                        .toolName(ms.actualCallName())
                        .matched(ms.matched())
                        .matchScore(ms.matchScore())
                        .build());
            }
        }

        return ToolCallAccuracyExplanation.builder()
                .score(score)
                .language(language)
                .mode(tcm.mode())
                .precision(tcm.precision())
                .recall(tcm.recall())
                .truePositives(tcm.truePositives())
                .falsePositives(tcm.falsePositives())
                .falseNegatives(tcm.falseNegatives())
                .matches(matches)
                .build();
    }

    // =========================================================================
    // TopicAdherence
    // =========================================================================

    private ScoreExplanation createTopicAdherenceExplanation(
            final Double score, final TopicAdherenceMetadata tam, final String language) {
        // Build classifications from first model
        final List<TopicAdherenceExplanation.TopicClassificationItem> classifications = new ArrayList<>();
        if (tam.modelClassifications() != null && !tam.modelClassifications().isEmpty()) {
            final List<TopicAdherenceMetadata.TopicClassificationSummary> firstModelClassifications =
                    tam.modelClassifications().values().iterator().next();
            for (final TopicAdherenceMetadata.TopicClassificationSummary tc : firstModelClassifications) {
                classifications.add(TopicAdherenceExplanation.TopicClassificationItem.builder()
                        .extractedTopic(tc.extractedTopic())
                        .onTopic(tc.onTopic())
                        .matchedReferenceTopic(tc.matchedReferenceTopic())
                        .reasoning(tc.reasoning())
                        .build());
            }
        }

        // Compute precision and recall
        final long onTopicCount = classifications.stream()
                .filter(TopicAdherenceExplanation.TopicClassificationItem::isOnTopic)
                .count();
        final double precision = classifications.isEmpty() ? 0.0 : (double) onTopicCount / classifications.size();
        final long coveredRef = classifications.stream()
                .filter(TopicAdherenceExplanation.TopicClassificationItem::isOnTopic)
                .map(TopicAdherenceExplanation.TopicClassificationItem::getMatchedReferenceTopic)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .count();
        final double recall =
                tam.referenceTopics() != null && !tam.referenceTopics().isEmpty()
                        ? (double) coveredRef / tam.referenceTopics().size()
                        : 0.0;

        return TopicAdherenceExplanation.builder()
                .score(score)
                .language(language)
                .mode(tam.mode())
                .precision(precision)
                .recall(recall)
                .extractedTopics(tam.extractedTopics() != null ? tam.extractedTopics() : List.of())
                .referenceTopics(tam.referenceTopics() != null ? tam.referenceTopics() : List.of())
                .classifications(classifications)
                .build();
    }

    // =========================================================================
    // NLP Metrics (non-LLM)
    // =========================================================================

    private ScoreExplanation createBleuScoreExplanation(
            final Double score,
            final BleuScoreMetadata bm,
            final MetricEvaluationResult result,
            final String language) {
        return BleuScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(result.getSample() != null ? result.getSample().getResponse() : "")
                .reference(result.getSample() != null ? result.getSample().getReference() : "")
                .maxNgram(bm.maxNgram())
                .smoothing(bm.smoothing())
                .build();
    }

    private ScoreExplanation createRougeScoreExplanation(
            final Double score,
            final RougeScoreMetadata rsm,
            final MetricEvaluationResult result,
            final String language) {
        return RougeScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(result.getSample() != null ? result.getSample().getResponse() : "")
                .reference(result.getSample() != null ? result.getSample().getReference() : "")
                .rougeType(rsm.rougeType())
                .mode(rsm.mode())
                .build();
    }

    private ScoreExplanation createChrfScoreExplanation(
            final Double score,
            final ChrfScoreMetadata csm,
            final MetricEvaluationResult result,
            final String language) {
        return ChrfScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(result.getSample() != null ? result.getSample().getResponse() : "")
                .reference(result.getSample() != null ? result.getSample().getReference() : "")
                .charNgramOrder(csm.charNgramOrder())
                .wordNgramOrder(csm.wordNgramOrder())
                .beta(csm.beta())
                .build();
    }

    private ScoreExplanation createStringSimilarityExplanation(
            final Double score,
            final StringSimilarityMetadata stm,
            final MetricEvaluationResult result,
            final String language) {
        return StringSimilarityExplanation.builder()
                .score(score)
                .language(language)
                .response(result.getSample() != null ? result.getSample().getResponse() : "")
                .reference(result.getSample() != null ? result.getSample().getReference() : "")
                .distanceMeasure(stm.distanceMeasure())
                .caseSensitive(stm.caseSensitive())
                .build();
    }

    // =========================================================================
    // NVIDIA Metrics
    // =========================================================================

    private ScoreExplanation createAnswerAccuracyExplanation(
            final Double score, final AnswerAccuracyMetadata aam, final String language) {
        // Use first model's initial judgment
        int rawScore = 0;
        String reasoning = "";
        if (aam.initialJudgments() != null && !aam.initialJudgments().isEmpty()) {
            final AnswerAccuracyMetadata.JudgmentSummary first =
                    aam.initialJudgments().values().iterator().next();
            rawScore = first.rawScore();
            reasoning = first.reasoning();
        }

        // Confirmation judgment
        Integer confirmationScore = null;
        String confirmationReasoning = null;
        if (aam.usedDualJudge()
                && aam.confirmedJudgments() != null
                && !aam.confirmedJudgments().isEmpty()) {
            final AnswerAccuracyMetadata.JudgmentSummary confirmed =
                    aam.confirmedJudgments().values().iterator().next();
            confirmationScore = confirmed.rawScore();
            confirmationReasoning = confirmed.reasoning();
        }

        return AnswerAccuracyExplanation.builder()
                .score(score)
                .language(language)
                .response("")
                .reference("")
                .rawScore(rawScore)
                .reasoning(reasoning)
                .usedDualJudge(aam.usedDualJudge())
                .confirmationScore(confirmationScore)
                .confirmationReasoning(confirmationReasoning)
                .build();
    }

    private ScoreExplanation createContextRelevanceExplanation(
            final Double score, final ContextRelevanceMetadata cxm, final String language) {
        final List<ContextRelevanceExplanation.ContextEvaluation> evaluations = new ArrayList<>();
        if (cxm.contextScores() != null) {
            for (int i = 0; i < cxm.contextScores().size(); i++) {
                final double ctxScore = cxm.contextScores().get(i);
                final int ctxRawScore = (int) Math.round(ctxScore * 2); // Convert 0-1 back to 0-2
                evaluations.add(ContextRelevanceExplanation.ContextEvaluation.builder()
                        .context("Context " + (i + 1))
                        .rawScore(ctxRawScore)
                        .normalizedScore(ctxScore)
                        .reasoning("")
                        .build());
            }
        }

        return ContextRelevanceExplanation.builder()
                .score(score)
                .language(language)
                .userInput("")
                .contextEvaluations(evaluations)
                .build();
    }

    private ScoreExplanation createResponseGroundednessExplanation(
            final Double score, final ResponseGroundednessMetadata rgm, final String language) {
        final int rawScore = score != null ? (int) Math.round(score * 2) : 0;

        return ResponseGroundednessExplanation.builder()
                .score(score)
                .language(language)
                .response("")
                .context("")
                .rawScore(rawScore)
                .reasoning("")
                .usedHeuristics(rgm.usedHeuristicShortcuts())
                .build();
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Extracts the first model's list from a per-model map.
     */
    private List<String> extractFirstModelList(final Map<String, List<String>> perModelMap) {
        if (perModelMap == null || perModelMap.isEmpty()) {
            return List.of();
        }
        return perModelMap.values().iterator().next();
    }

    /**
     * Extracts the first model's single value from a per-model map of lists.
     */
    private String extractFirstModelValue(final Map<String, List<String>> perModelMap) {
        if (perModelMap == null || perModelMap.isEmpty()) {
            return "";
        }
        final List<String> firstList = perModelMap.values().iterator().next();
        return firstList != null && !firstList.isEmpty() ? firstList.get(0) : "";
    }
}
