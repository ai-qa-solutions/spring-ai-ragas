package ai.qa.solutions.metrics.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("NoiseSensitivityMetric Tests")
class NoiseSensitivityMetricTest {

    private MultiModelExecutor executor;

    @BeforeEach
    void setUp() {
        ChatClient mockClient = mock(ChatClient.class);
        ChatClientStore store = new ChatClientStore(Map.of("model-1", mockClient), mockClient);
        executor = new MultiModelExecutor(store, null, new SimpleAsyncTaskExecutor());
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should create metric with executor")
        void shouldCreateWithExecutor() {
            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("NoiseSensitivityMetric");
        }

        @Test
        @DisplayName("Should allow custom statement generator prompt")
        void shouldAllowCustomStatementGeneratorPrompt() {
            String customPrompt = "Custom: {question} {answer}";

            NoiseSensitivityMetric metric = NoiseSensitivityMetric.builder()
                    .executor(executor)
                    .statementGeneratorPrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("Should allow custom faithfulness prompt")
        void shouldAllowCustomFaithfulnessPrompt() {
            String customPrompt = "Custom: {context} {statements}";

            NoiseSensitivityMetric metric = NoiseSensitivityMetric.builder()
                    .executor(executor)
                    .statementFaithfulnessPrompt(customPrompt)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            NoiseSensitivityMetric original =
                    NoiseSensitivityMetric.builder().executor(executor).build();

            NoiseSensitivityMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }
    }

    @Nested
    @DisplayName("StatementsResponse Record")
    class StatementsResponseTests {

        @Test
        @DisplayName("Should store statements list")
        void shouldStoreStatementsList() {
            List<String> statements = List.of("Statement 1", "Statement 2", "Statement 3");
            NoiseSensitivityMetric.StatementsResponse response =
                    new NoiseSensitivityMetric.StatementsResponse(statements);

            assertThat(response.statements()).containsExactly("Statement 1", "Statement 2", "Statement 3");
        }

        @Test
        @DisplayName("Should handle empty statements")
        void shouldHandleEmptyStatements() {
            NoiseSensitivityMetric.StatementsResponse response =
                    new NoiseSensitivityMetric.StatementsResponse(List.of());

            assertThat(response.statements()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null statements")
        void shouldHandleNullStatements() {
            NoiseSensitivityMetric.StatementsResponse response = new NoiseSensitivityMetric.StatementsResponse(null);

            assertThat(response.statements()).isNull();
        }
    }

    @Nested
    @DisplayName("StatementVerdict Record")
    class StatementVerdictTests {

        @Test
        @DisplayName("Should store all fields with true verdict")
        void shouldStoreAllFieldsWithTrueVerdict() {
            NoiseSensitivityMetric.StatementVerdict verdict =
                    new NoiseSensitivityMetric.StatementVerdict("The statement", true, "reasoning");

            assertThat(verdict.statement()).isEqualTo("The statement");
            assertThat(verdict.verdict()).isTrue();
            assertThat(verdict.reason()).isEqualTo("reasoning");
        }

        @Test
        @DisplayName("Should handle false verdict")
        void shouldHandleFalseVerdict() {
            NoiseSensitivityMetric.StatementVerdict verdict =
                    new NoiseSensitivityMetric.StatementVerdict("The statement", false, "not found");

            assertThat(verdict.verdict()).isFalse();
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            NoiseSensitivityMetric.StatementVerdict verdict =
                    new NoiseSensitivityMetric.StatementVerdict(null, null, null);

            assertThat(verdict.statement()).isNull();
            assertThat(verdict.verdict()).isNull();
            assertThat(verdict.reason()).isNull();
        }
    }

    @Nested
    @DisplayName("FaithfulnessVerdictsResponse Record")
    class FaithfulnessVerdictsResponseTests {

        @Test
        @DisplayName("Should store verdicts list")
        void shouldStoreVerdictsList() {
            List<NoiseSensitivityMetric.StatementVerdict> verdicts = List.of(
                    new NoiseSensitivityMetric.StatementVerdict("stmt1", true, "reason1"),
                    new NoiseSensitivityMetric.StatementVerdict("stmt2", false, "reason2"));

            NoiseSensitivityMetric.FaithfulnessVerdictsResponse response =
                    new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(verdicts);

            assertThat(response.verdicts()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty verdicts")
        void shouldHandleEmptyVerdicts() {
            NoiseSensitivityMetric.FaithfulnessVerdictsResponse response =
                    new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of());

            assertThat(response.verdicts()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null verdicts")
        void shouldHandleNullVerdicts() {
            NoiseSensitivityMetric.FaithfulnessVerdictsResponse response =
                    new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(null);

            assertThat(response.verdicts()).isNull();
        }
    }

    @Nested
    @DisplayName("NoiseSensitivityMode Enum")
    class NoiseSensitivityModeTests {

        @Test
        @DisplayName("Should have RELEVANT mode")
        void shouldHaveRelevantMode() {
            assertThat(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT).isNotNull();
        }

        @Test
        @DisplayName("Should have IRRELEVANT mode")
        void shouldHaveIrrelevantMode() {
            assertThat(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT).isNotNull();
        }

        @Test
        @DisplayName("Should have exactly two modes")
        void shouldHaveExactlyTwoModes() {
            assertThat(NoiseSensitivityMetric.NoiseSensitivityMode.values()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("NoiseSensitivityConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should have RELEVANT as default mode")
        void shouldHaveRelevantAsDefaultMode() {
            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder().build();

            assertThat(config.getMode()).isEqualTo(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT);
        }

        @Test
        @DisplayName("Should allow specifying IRRELEVANT mode")
        void shouldAllowSpecifyingIrrelevantMode() {
            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                            .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                            .build();

            assertThat(config.getMode()).isEqualTo(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT);
        }

        @Test
        @DisplayName("Should start with empty models list")
        void shouldStartWithEmptyModelsList() {
            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder().build();

            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                            .model("gpt-4")
                            .model("claude-3")
                            .build();

            assertThat(config.getModels()).containsExactly("gpt-4", "claude-3");
        }
    }

    @Nested
    @DisplayName("Default Prompt Templates")
    class PromptTemplateTests {

        @Test
        @DisplayName("Statement generator should contain required placeholders")
        void statementGeneratorShouldContainPlaceholders() {
            assertThat(NoiseSensitivityMetric.DEFAULT_STATEMENT_GENERATOR_PROMPT)
                    .contains("{question}")
                    .contains("{answer}");
        }

        @Test
        @DisplayName("Statement generator should contain atomic statement instructions")
        void statementGeneratorShouldContainAtomicInstructions() {
            assertThat(NoiseSensitivityMetric.DEFAULT_STATEMENT_GENERATOR_PROMPT)
                    .contains("atomic")
                    .contains("statements");
        }

        @Test
        @DisplayName("Faithfulness prompt should contain required placeholders")
        void faithfulnessPromptShouldContainPlaceholders() {
            assertThat(NoiseSensitivityMetric.DEFAULT_STATEMENT_FAITHFULNESS_PROMPT)
                    .contains("{context}")
                    .contains("{statements}");
        }

        @Test
        @DisplayName("Faithfulness prompt should contain verdict instructions")
        void faithfulnessPromptShouldContainVerdictInstructions() {
            assertThat(NoiseSensitivityMetric.DEFAULT_STATEMENT_FAITHFULNESS_PROMPT)
                    .contains("verdict")
                    .contains("true")
                    .contains("false");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 0.0 when user input is missing")
        void shouldReturn0WhenNoUserInput() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when response is missing")
        void shouldReturn0WhenNoResponse() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when reference is missing")
        void shouldReturn0WhenNoReference() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when contexts are missing")
        void shouldReturn0WhenNoContexts() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should compute score when all steps succeed")
        void shouldComputeScoreWhenAllStepsSucceed() {
            // StatementsResponse is used for DecomposeReference and DecomposeResponse
            // FaithfulnessVerdictsResponse is used for all faithfulness evaluations
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", true, "found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // Score depends on the algorithm - all verdicts are true so no incorrect statements
            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should work with IRRELEVANT mode")
        void shouldWorkWithIrrelevantMode() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", true, "found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                            .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", true, "found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle multiple contexts")
        void shouldHandleMultipleContexts() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1", "Statement 2")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", true, "found"),
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 2", false, "not found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response with multiple statements")
                    .reference("Reference with multiple statements")
                    .retrievedContexts(List.of("Context 1", "Context 2", "Context 3"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle null verdict in response")
        void shouldHandleNullVerdictInResponse() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", null, "unclear"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // null verdict treated as false
            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle empty statements list")
        void shouldHandleEmptyStatementsList() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of()))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of()));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // Empty statements should result in 0.0
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle null statements in response")
        void shouldHandleNullStatementsInResponse() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(null))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of()));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle null verdicts list in faithfulness response")
        void shouldHandleNullVerdictsListInFaithfulnessResponse() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(null));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // null verdicts list should be handled gracefully
            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should compute IRRELEVANT mode with mixed verdicts")
        void shouldComputeIrrelevantModeWithMixedVerdicts() {
            // For IRRELEVANT mode: measure errors from irrelevant contexts
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1", "Statement 2")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", false, "not found"),
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 2", true, "found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Relevant context", "Irrelevant context"))
                    .build();

            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                            .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle RELEVANT mode with all false verdicts")
        void shouldHandleRelevantModeWithAllFalseVerdicts() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1", "Statement 2")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", false, "not found"),
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 2", false, "not found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context 1", "Context 2"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should work with specified models in config")
        void shouldWorkWithSpecifiedModelsInConfig() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1", "model-2"))
                    .withResponse(
                            NoiseSensitivityMetric.StatementsResponse.class,
                            new NoiseSensitivityMetric.StatementsResponse(List.of("Statement 1")))
                    .withResponse(
                            NoiseSensitivityMetric.FaithfulnessVerdictsResponse.class,
                            new NoiseSensitivityMetric.FaithfulnessVerdictsResponse(List.of(
                                    new NoiseSensitivityMetric.StatementVerdict("Statement 1", true, "found"))));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            NoiseSensitivityMetric.NoiseSensitivityConfig config =
                    NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                            .model("model-1")
                            .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when user input is empty string")
        void shouldReturn0WhenUserInputIsEmpty() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("   ")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when response is empty string")
        void shouldReturn0WhenResponseIsEmpty() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("   ")
                    .reference("Reference")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when reference is empty string")
        void shouldReturn0WhenReferenceIsEmpty() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("   ")
                    .retrievedContexts(List.of("Context"))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when contexts list is empty")
        void shouldReturn0WhenContextsListIsEmpty() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"));

            NoiseSensitivityMetric metric =
                    NoiseSensitivityMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("Question")
                    .response("Response")
                    .reference("Reference")
                    .retrievedContexts(List.of())
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }
    }
}
