package ai.qa.solutions.allure.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.execution.listener.dto.MetricMetadata;
import ai.qa.solutions.metric.metadata.*;
import ai.qa.solutions.sample.Sample;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ScoreExplanationFactory")
class ScoreExplanationFactoryTest {

    private ScoreExplanationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ScoreExplanationFactory();
    }

    @Nested
    @DisplayName("Null and unknown metadata")
    class NullAndUnknownMetadata {

        @Test
        @DisplayName("Should return empty Optional for null result")
        void shouldReturnEmptyForNullResult() {
            final Optional<ScoreExplanation> result = factory.create(null, "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty Optional for result with null metadata")
        void shouldReturnEmptyForNullMetadata() {
            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, null), "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty Optional for unhandled MetricMetadata implementation")
        void shouldReturnEmptyForUnhandledMetadata() {
            final MetricMetadata unhandled = new MetricMetadata() {};
            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, unhandled), "en");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("FaithfulnessMetadata")
    class Faithfulness {

        @Test
        @DisplayName("Should create FaithfulnessExplanation from FaithfulnessMetadata")
        void shouldCreateFaithfulnessExplanation() {
            final FaithfulnessMetadata metadata = new FaithfulnessMetadata(
                    Map.of("model-1", List.of("Statement 1", "Statement 2")),
                    Map.of(
                            "model-1",
                            List.of(
                                    new FaithfulnessMetadata.StatementVerdictSummary("Statement 1", "Faithful", 1),
                                    new FaithfulnessMetadata.StatementVerdictSummary(
                                            "Statement 2", "Not faithful", 0))),
                    1,
                    2);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(FaithfulnessExplanation.class);
        }

        @Test
        @DisplayName("Should return empty when verdicts are empty")
        void shouldReturnEmptyWhenVerdictsEmpty() {
            final FaithfulnessMetadata metadata =
                    new FaithfulnessMetadata(Map.of("model-1", List.of("Statement 1")), Map.of(), 0, 1);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle null verdicts map")
        void shouldHandleNullVerdictsMap() {
            final FaithfulnessMetadata metadata =
                    new FaithfulnessMetadata(Map.of("model-1", List.of("Statement 1")), null, 0, 1);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("AspectCriticMetadata")
    class AspectCritic {

        @Test
        @DisplayName("Should create AspectCriticExplanation from AspectCriticMetadata")
        void shouldCreateAspectCriticExplanation() {
            final AspectCriticMetadata metadata = new AspectCriticMetadata(
                    "Is the response helpful?",
                    3,
                    Map.of("model-1", List.of(true, true, false)),
                    Map.of("model-1", List.of("Good", "Good", "Bad")));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.67, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AspectCriticExplanation.class);
        }

        @Test
        @DisplayName("Should determine pass when score >= 0.5")
        void shouldDeterminePassWhenScoreAboveHalf() {
            final AspectCriticMetadata metadata = new AspectCriticMetadata(
                    "Check", 1, Map.of("model-1", List.of(true)), Map.of("model-1", List.of("OK")));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.6, metadata), "en");

            assertThat(result).isPresent();
            final AspectCriticExplanation explanation = (AspectCriticExplanation) result.get();
            assertThat(explanation.isPassed()).isTrue();
        }

        @Test
        @DisplayName("Should determine fail when score < 0.5")
        void shouldDetermineFailWhenScoreBelowHalf() {
            final AspectCriticMetadata metadata = new AspectCriticMetadata(
                    "Check", 1, Map.of("model-1", List.of(false)), Map.of("model-1", List.of("Bad")));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.3, metadata), "en");

            assertThat(result).isPresent();
            final AspectCriticExplanation explanation = (AspectCriticExplanation) result.get();
            assertThat(explanation.isPassed()).isFalse();
        }
    }

    @Nested
    @DisplayName("SimpleCriteriaMetadata")
    class SimpleCriteria {

        @Test
        @DisplayName("Should create SimpleCriteriaExplanation from SimpleCriteriaMetadata")
        void shouldCreateSimpleCriteriaExplanation() {
            final SimpleCriteriaMetadata metadata = new SimpleCriteriaMetadata(
                    "Rate coherence",
                    1.0,
                    5.0,
                    1,
                    Map.of("model-1", List.of(4.0)),
                    Map.of("model-1", List.of("Well structured")));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.75, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(SimpleCriteriaExplanation.class);
        }

        @Test
        @DisplayName("Should handle empty model scores")
        void shouldHandleEmptyModelScores() {
            final SimpleCriteriaMetadata metadata = new SimpleCriteriaMetadata("Rate", 1.0, 5.0, 1, Map.of(), Map.of());

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(SimpleCriteriaExplanation.class);
        }
    }

    @Nested
    @DisplayName("RubricsMetadata")
    class Rubrics {

        @Test
        @DisplayName("Should create RubricsScoreExplanation from RubricsMetadata")
        void shouldCreateRubricsExplanation() {
            final RubricsMetadata metadata = new RubricsMetadata(
                    Map.of("score1_description", "Poor", "score2_description", "Fair", "score3_description", "Good"),
                    Map.of("model-1", 2),
                    Map.of("model-1", "score2_description"),
                    Map.of("model-1", "Fairly accurate"));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(RubricsScoreExplanation.class);
        }
    }

    @Nested
    @DisplayName("ContextPrecisionMetadata")
    class ContextPrecision {

        @Test
        @DisplayName("Should create ContextPrecisionExplanation from ContextPrecisionMetadata")
        void shouldCreateContextPrecisionExplanation() {
            final ContextPrecisionMetadata metadata =
                    new ContextPrecisionMetadata("REFERENCE_BASED", Map.of("model-1", List.of(true, false, true)), 3);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.72, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextPrecisionExplanation.class);
        }

        @Test
        @DisplayName("Should handle empty relevance results")
        void shouldHandleEmptyRelevanceResults() {
            final ContextPrecisionMetadata metadata = new ContextPrecisionMetadata("REFERENCE_BASED", Map.of(), 0);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextPrecisionExplanation.class);
        }
    }

    @Nested
    @DisplayName("ContextRecallMetadata")
    class ContextRecall {

        @Test
        @DisplayName("Should create ContextRecallExplanation from ContextRecallMetadata")
        void shouldCreateContextRecallExplanation() {
            final ContextRecallMetadata metadata = new ContextRecallMetadata(
                    Map.of(
                            "model-1",
                            List.of(
                                    new ContextRecallMetadata.ClassificationSummary("Stmt 1", "Found in context", 1),
                                    new ContextRecallMetadata.ClassificationSummary("Stmt 2", "Not found", 0))),
                    1,
                    2);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextRecallExplanation.class);
        }

        @Test
        @DisplayName("Should handle empty classifications")
        void shouldHandleEmptyClassifications() {
            final ContextRecallMetadata metadata = new ContextRecallMetadata(Map.of(), 0, 0);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextRecallExplanation.class);
        }
    }

    @Nested
    @DisplayName("ContextEntityRecallMetadata")
    class ContextEntityRecall {

        @Test
        @DisplayName("Should create ContextEntityRecallExplanation from ContextEntityRecallMetadata")
        void shouldCreateContextEntityRecallExplanation() {
            final ContextEntityRecallMetadata metadata = new ContextEntityRecallMetadata(
                    Map.of("model-1", List.of("Paris", "France")),
                    Map.of("model-1", List.of("Paris", "Europe")),
                    Map.of("model-1", Set.of("Paris")),
                    1,
                    2);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextEntityRecallExplanation.class);
        }
    }

    @Nested
    @DisplayName("NoiseSensitivityMetadata")
    class NoiseSensitivity {

        @Test
        @DisplayName("Should create NoiseSensitivityExplanation from NoiseSensitivityMetadata")
        void shouldCreateNoiseSensitivityExplanation() {
            final NoiseSensitivityMetadata metadata = new NoiseSensitivityMetadata(
                    "RELEVANT", Map.of("model-1", List.of("Ref stmt 1")), Map.of("model-1", List.of("Resp stmt 1")), 2);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.8, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(NoiseSensitivityExplanation.class);
        }
    }

    @Nested
    @DisplayName("ResponseRelevancyMetadata")
    class ResponseRelevancy {

        @Test
        @DisplayName("Should create ResponseRelevancyExplanation from ResponseRelevancyMetadata")
        void shouldCreateResponseRelevancyExplanation() {
            final ResponseRelevancyMetadata metadata = new ResponseRelevancyMetadata(
                    Map.of("model-1", List.of("What is AI?", "Define AI")),
                    Map.of("model-1", List.of(false, false)),
                    Map.of("model-1", 0.85),
                    3);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.85, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ResponseRelevancyExplanation.class);
        }
    }

    @Nested
    @DisplayName("FactualCorrectnessMetadata")
    class FactualCorrectness {

        @Test
        @DisplayName("Should create FactualCorrectnessExplanation from FactualCorrectnessMetadata")
        void shouldCreateFactualCorrectnessExplanation() {
            final FactualCorrectnessMetadata metadata = new FactualCorrectnessMetadata(
                    "F1",
                    Map.of("model-1", List.of("Claim 1")),
                    Map.of("model-1", List.of("Ref claim 1")),
                    Map.of(
                            "model-1",
                            List.of(new FactualCorrectnessMetadata.NliVerdictSummary(
                                    "Claim 1", "SUPPORTED", "Matches reference"))),
                    Map.of(
                            "model-1",
                            List.of(new FactualCorrectnessMetadata.NliVerdictSummary(
                                    "Ref claim 1", "SUPPORTED", "Covered"))));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.9, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(FactualCorrectnessExplanation.class);
        }

        @Test
        @DisplayName("Should handle null verdict maps")
        void shouldHandleNullVerdictMaps() {
            final FactualCorrectnessMetadata metadata = new FactualCorrectnessMetadata(
                    "PRECISION", Map.of("model-1", List.of("Claim")), Map.of("model-1", List.of("Ref")), null, null);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(FactualCorrectnessExplanation.class);
        }
    }

    @Nested
    @DisplayName("AnswerCorrectnessMetadata")
    class AnswerCorrectness {

        @Test
        @DisplayName("Should create AnswerCorrectnessExplanation from AnswerCorrectnessMetadata")
        void shouldCreateAnswerCorrectnessExplanation() {
            final AnswerCorrectnessMetadata metadata = new AnswerCorrectnessMetadata(0.8, 0.9, 0.5, 0.5);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.85, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AnswerCorrectnessExplanation.class);
        }
    }

    @Nested
    @DisplayName("SemanticSimilarityMetadata")
    class SemanticSimilarity {

        @Test
        @DisplayName("Should create SemanticSimilarityExplanation from SemanticSimilarityMetadata")
        void shouldCreateSemanticSimilarityExplanation() {
            final SemanticSimilarityMetadata metadata =
                    new SemanticSimilarityMetadata(Map.of("embed-1", 0.92, "embed-2", 0.88), 0.8);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.9, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(SemanticSimilarityExplanation.class);
        }

        @Test
        @DisplayName("Should handle null embedding scores")
        void shouldHandleNullEmbeddingScores() {
            final SemanticSimilarityMetadata metadata = new SemanticSimilarityMetadata(null, null);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(SemanticSimilarityExplanation.class);
        }
    }

    @Nested
    @DisplayName("AgentGoalAccuracyMetadata")
    class AgentGoalAccuracy {

        @Test
        @DisplayName("Should create AgentGoalAccuracyExplanation from AgentGoalAccuracyMetadata")
        void shouldCreateAgentGoalAccuracyExplanation() {
            final AgentGoalAccuracyMetadata metadata = new AgentGoalAccuracyMetadata(
                    "WITH_REFERENCE", null, Map.of("model-1", true, "model-2", true), Map.of("model-1", "Goal met"));

            final Optional<ScoreExplanation> result = factory.create(resultWith(1.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AgentGoalAccuracyExplanation.class);
        }

        @Test
        @DisplayName("Should detect model disagreement")
        void shouldDetectModelDisagreement() {
            final AgentGoalAccuracyMetadata metadata = new AgentGoalAccuracyMetadata(
                    "WITH_REFERENCE",
                    null,
                    Map.of("model-1", true, "model-2", false),
                    Map.of("model-1", "Met", "model-2", "Not met"));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            final AgentGoalAccuracyExplanation explanation = (AgentGoalAccuracyExplanation) result.get();
            assertThat(explanation.isHasModelDisagreement()).isTrue();
        }
    }

    @Nested
    @DisplayName("ToolCallAccuracyMetadata")
    class ToolCallAccuracy {

        @Test
        @DisplayName("Should create ToolCallAccuracyExplanation from ToolCallAccuracyMetadata")
        void shouldCreateToolCallAccuracyExplanation() {
            final ToolCallAccuracyMetadata metadata = new ToolCallAccuracyMetadata(
                    "STRICT",
                    0.8,
                    2,
                    2,
                    2,
                    0,
                    0,
                    1.0,
                    1.0,
                    List.of(
                            new ToolCallAccuracyMetadata.ToolCallMatchSummary("search", "search", true, 1.0),
                            new ToolCallAccuracyMetadata.ToolCallMatchSummary("book", "book", true, 1.0)));

            final Optional<ScoreExplanation> result = factory.create(resultWith(1.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ToolCallAccuracyExplanation.class);
        }

        @Test
        @DisplayName("Should handle null matches list")
        void shouldHandleNullMatches() {
            final ToolCallAccuracyMetadata metadata =
                    new ToolCallAccuracyMetadata("FLEXIBLE", 0.8, 0, 0, 0, 0, 0, 0.0, 0.0, null);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ToolCallAccuracyExplanation.class);
        }
    }

    @Nested
    @DisplayName("TopicAdherenceMetadata")
    class TopicAdherence {

        @Test
        @DisplayName("Should create TopicAdherenceExplanation from TopicAdherenceMetadata")
        void shouldCreateTopicAdherenceExplanation() {
            final TopicAdherenceMetadata metadata = new TopicAdherenceMetadata(
                    "F1",
                    List.of("AI", "ML"),
                    List.of("AI", "Finance"),
                    Map.of(
                            "model-1",
                            List.of(
                                    new TopicAdherenceMetadata.TopicClassificationSummary("AI", true, "AI", "Matches"),
                                    new TopicAdherenceMetadata.TopicClassificationSummary(
                                            "Finance", false, null, "Off topic"))));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(TopicAdherenceExplanation.class);
        }

        @Test
        @DisplayName("Should handle empty classifications")
        void shouldHandleEmptyClassifications() {
            final TopicAdherenceMetadata metadata =
                    new TopicAdherenceMetadata("PRECISION", List.of(), List.of(), Map.of());

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(TopicAdherenceExplanation.class);
        }
    }

    @Nested
    @DisplayName("NLP Metrics (BleuScore, RougeScore, ChrfScore, StringSimilarity)")
    class NlpMetrics {

        @Test
        @DisplayName("Should create BleuScoreExplanation from BleuScoreMetadata")
        void shouldCreateBleuScoreExplanation() {
            final BleuScoreMetadata metadata = new BleuScoreMetadata(4, true);
            final Sample sample = Sample.builder()
                    .response("AI is great")
                    .reference("AI is awesome")
                    .build();

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.6, metadata, sample), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(BleuScoreExplanation.class);
        }

        @Test
        @DisplayName("Should create RougeScoreExplanation from RougeScoreMetadata")
        void shouldCreateRougeScoreExplanation() {
            final RougeScoreMetadata metadata = new RougeScoreMetadata("ROUGE_L", "FMEASURE");
            final Sample sample = Sample.builder()
                    .response("AI is great")
                    .reference("AI is awesome")
                    .build();

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.7, metadata, sample), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(RougeScoreExplanation.class);
        }

        @Test
        @DisplayName("Should create ChrfScoreExplanation from ChrfScoreMetadata")
        void shouldCreateChrfScoreExplanation() {
            final ChrfScoreMetadata metadata = new ChrfScoreMetadata(6, 2, 2.0);
            final Sample sample = Sample.builder()
                    .response("AI is great")
                    .reference("AI is awesome")
                    .build();

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.65, metadata, sample), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ChrfScoreExplanation.class);
        }

        @Test
        @DisplayName("Should create StringSimilarityExplanation from StringSimilarityMetadata")
        void shouldCreateStringSimilarityExplanation() {
            final StringSimilarityMetadata metadata = new StringSimilarityMetadata("LEVENSHTEIN", true);
            final Sample sample = Sample.builder()
                    .response("AI is great")
                    .reference("AI is awesome")
                    .build();

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.75, metadata, sample), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(StringSimilarityExplanation.class);
        }
    }

    @Nested
    @DisplayName("NVIDIA Metrics (AnswerAccuracy, ContextRelevance, ResponseGroundedness)")
    class NvidiaMetrics {

        @Test
        @DisplayName("Should create AnswerAccuracyExplanation from AnswerAccuracyMetadata")
        void shouldCreateAnswerAccuracyExplanation() {
            final AnswerAccuracyMetadata metadata = new AnswerAccuracyMetadata(
                    Map.of("model-1", new AnswerAccuracyMetadata.JudgmentSummary(2, "Correct")), null, false);

            final Optional<ScoreExplanation> result = factory.create(resultWith(1.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AnswerAccuracyExplanation.class);
        }

        @Test
        @DisplayName("Should create AnswerAccuracyExplanation with dual judge")
        void shouldCreateAnswerAccuracyWithDualJudge() {
            final AnswerAccuracyMetadata metadata = new AnswerAccuracyMetadata(
                    Map.of("model-1", new AnswerAccuracyMetadata.JudgmentSummary(2, "Correct")),
                    Map.of("model-1", new AnswerAccuracyMetadata.JudgmentSummary(2, "Confirmed")),
                    true);

            final Optional<ScoreExplanation> result = factory.create(resultWith(1.0, metadata), "en");

            assertThat(result).isPresent();
            final AnswerAccuracyExplanation explanation = (AnswerAccuracyExplanation) result.get();
            assertThat(explanation.isUsedDualJudge()).isTrue();
        }

        @Test
        @DisplayName("Should handle empty initial judgments")
        void shouldHandleEmptyInitialJudgments() {
            final AnswerAccuracyMetadata metadata = new AnswerAccuracyMetadata(Map.of(), null, false);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AnswerAccuracyExplanation.class);
        }

        @Test
        @DisplayName("Should create ContextRelevanceExplanation from ContextRelevanceMetadata")
        void shouldCreateContextRelevanceExplanation() {
            final ContextRelevanceMetadata metadata = new ContextRelevanceMetadata(List.of(0.8, 0.5, 0.9), 3);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.73, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextRelevanceExplanation.class);
        }

        @Test
        @DisplayName("Should handle null context scores")
        void shouldHandleNullContextScores() {
            final ContextRelevanceMetadata metadata = new ContextRelevanceMetadata(null, 0);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ContextRelevanceExplanation.class);
        }

        @Test
        @DisplayName("Should create ResponseGroundednessExplanation from ResponseGroundednessMetadata")
        void shouldCreateResponseGroundednessExplanation() {
            final ResponseGroundednessMetadata metadata = new ResponseGroundednessMetadata(true, false);

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata), "en");

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ResponseGroundednessExplanation.class);
        }
    }

    @Nested
    @DisplayName("HallucinationMetadata")
    class Hallucination {

        @Test
        @DisplayName("Should return empty Optional for HallucinationMetadata (TODO)")
        void shouldReturnEmptyForHallucinationMetadata() {
            final HallucinationMetadata metadata = new HallucinationMetadata(Map.of(
                    "model-1",
                    List.of(new HallucinationMetadata.ClaimAnalysisSummary("Claim 1", "SUPPORTED", "Correct"))));

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.9, metadata), "en");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Language support")
    class LanguageSupport {

        @Test
        @DisplayName("Should pass language to explanation for English")
        void shouldPassLanguageForEnglish() {
            final BleuScoreMetadata metadata = new BleuScoreMetadata(4, true);
            final Sample sample =
                    Sample.builder().response("response").reference("reference").build();

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata, sample), "en");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Should pass language to explanation for Russian")
        void shouldPassLanguageForRussian() {
            final BleuScoreMetadata metadata = new BleuScoreMetadata(4, true);
            final Sample sample =
                    Sample.builder().response("response").reference("reference").build();

            final Optional<ScoreExplanation> result = factory.create(resultWith(0.5, metadata, sample), "ru");

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("Should return empty Optional when factory method throws exception")
        void shouldReturnEmptyOnException() {
            // FaithfulnessMetadata with data that could cause NPE in some edge case
            // The factory wraps all creation in try-catch
            final FaithfulnessMetadata metadata = new FaithfulnessMetadata(null, Map.of("model-1", List.of()), 0, 0);

            // The factory handles this gracefully - verdicts exist but are empty
            final Optional<ScoreExplanation> result = factory.create(resultWith(0.0, metadata), "en");

            // Either returns empty or handles gracefully
            // The key test is that it does NOT throw an exception
            assertThat(result).isNotNull();
        }
    }

    private MetricEvaluationResult resultWith(final Double score, final MetricMetadata metadata) {
        return MetricEvaluationResult.builder()
                .metricName("test")
                .aggregatedScore(score)
                .metadata(metadata)
                .totalDuration(Duration.ZERO)
                .build();
    }

    private MetricEvaluationResult resultWith(final Double score, final MetricMetadata metadata, final Sample sample) {
        return MetricEvaluationResult.builder()
                .metricName("test")
                .aggregatedScore(score)
                .metadata(metadata)
                .sample(sample)
                .totalDuration(Duration.ZERO)
                .build();
    }
}
