package ai.qa.solutions.metrics.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
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

@DisplayName("FaithfulnessMetric Tests")
class FaithfulnessMetricTest {

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
            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("FaithfulnessMetric");
        }

        @Test
        @DisplayName("Should allow custom statement generator template")
        void shouldAllowCustomStatementTemplate() {
            String customTemplate = "Custom: {question} {answer}";

            FaithfulnessMetric metric = FaithfulnessMetric.builder()
                    .executor(executor)
                    .statementGeneratorTemplate(customTemplate)
                    .build();

            assertThat(metric).isNotNull();
        }

        @Test
        @DisplayName("Should allow custom NLI template")
        void shouldAllowCustomNliTemplate() {
            String customTemplate = "Custom NLI: {context} {statements}";

            FaithfulnessMetric metric = FaithfulnessMetric.builder()
                    .executor(executor)
                    .nliStatementTemplate(customTemplate)
                    .build();

            assertThat(metric).isNotNull();
        }
    }

    @Nested
    @DisplayName("StatementsResponse Record")
    class StatementsResponseTests {

        @Test
        @DisplayName("Should store statements list")
        void shouldStoreStatementsList() {
            List<String> statements = List.of("Statement 1", "Statement 2", "Statement 3");
            FaithfulnessMetric.StatementsResponse response = new FaithfulnessMetric.StatementsResponse(statements);

            assertThat(response.statements()).containsExactly("Statement 1", "Statement 2", "Statement 3");
        }

        @Test
        @DisplayName("Should handle empty statements")
        void shouldHandleEmptyStatements() {
            FaithfulnessMetric.StatementsResponse response = new FaithfulnessMetric.StatementsResponse(List.of());

            assertThat(response.statements()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null statements")
        void shouldHandleNullStatements() {
            FaithfulnessMetric.StatementsResponse response = new FaithfulnessMetric.StatementsResponse(null);

            assertThat(response.statements()).isNull();
        }
    }

    @Nested
    @DisplayName("VerdictsResponse Record")
    class VerdictsResponseTests {

        @Test
        @DisplayName("Should store verdicts list")
        void shouldStoreVerdictsList() {
            List<FaithfulnessMetric.StatementVerdict> verdicts = List.of(
                    new FaithfulnessMetric.StatementVerdict("stmt1", "reason1", 1),
                    new FaithfulnessMetric.StatementVerdict("stmt2", "reason2", 0));

            FaithfulnessMetric.VerdictsResponse response = new FaithfulnessMetric.VerdictsResponse(verdicts);

            assertThat(response.verdicts()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty verdicts")
        void shouldHandleEmptyVerdicts() {
            FaithfulnessMetric.VerdictsResponse response = new FaithfulnessMetric.VerdictsResponse(List.of());

            assertThat(response.verdicts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("StatementVerdict Record")
    class StatementVerdictTests {

        @Test
        @DisplayName("Should store all fields")
        void shouldStoreAllFields() {
            FaithfulnessMetric.StatementVerdict verdict =
                    new FaithfulnessMetric.StatementVerdict("The statement", "The reasoning", 1);

            assertThat(verdict.statement()).isEqualTo("The statement");
            assertThat(verdict.reason()).isEqualTo("The reasoning");
            assertThat(verdict.verdict()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null fields")
        void shouldHandleNullFields() {
            FaithfulnessMetric.StatementVerdict verdict = new FaithfulnessMetric.StatementVerdict(null, null, null);

            assertThat(verdict.statement()).isNull();
            assertThat(verdict.reason()).isNull();
            assertThat(verdict.verdict()).isNull();
        }
    }

    @Nested
    @DisplayName("FaithfulnessConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should allow empty config")
        void shouldAllowEmptyConfig() {
            FaithfulnessMetric.FaithfulnessConfig config =
                    FaithfulnessMetric.FaithfulnessConfig.builder().build();

            assertThat(config).isNotNull();
            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            FaithfulnessMetric.FaithfulnessConfig config = FaithfulnessMetric.FaithfulnessConfig.builder()
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
            assertThat(FaithfulnessMetric.DEFAULT_STATEMENT_GENERATOR_TEMPLATE)
                    .contains("{question}")
                    .contains("{answer}");
        }

        @Test
        @DisplayName("Statement generator should contain example")
        void statementGeneratorShouldContainExample() {
            assertThat(FaithfulnessMetric.DEFAULT_STATEMENT_GENERATOR_TEMPLATE)
                    .contains("Albert Einstein")
                    .contains("statements");
        }

        @Test
        @DisplayName("NLI template should contain required placeholders")
        void nliTemplateShouldContainPlaceholders() {
            assertThat(FaithfulnessMetric.DEFAULT_NLI_STATEMENT_TEMPLATE)
                    .contains("{context}")
                    .contains("{statements}");
        }

        @Test
        @DisplayName("NLI template should contain verdict instructions")
        void nliTemplateShouldContainVerdictInstructions() {
            assertThat(FaithfulnessMetric.DEFAULT_NLI_STATEMENT_TEMPLATE)
                    .contains("verdict")
                    .contains("1")
                    .contains("0");
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 1.0 when all statements are faithful")
        void shouldReturn1WhenAllFaithful() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1", "stmt2")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(List.of(
                                    new FaithfulnessMetric.StatementVerdict("stmt1", "supported", 1),
                                    new FaithfulnessMetric.StatementVerdict("stmt2", "supported", 1))));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is a programming language.")
                    .retrievedContexts(List.of("Java is a high-level programming language."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when no statements are faithful")
        void shouldReturn0WhenNoneFaithful() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1", "stmt2")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(List.of(
                                    new FaithfulnessMetric.StatementVerdict("stmt1", "not supported", 0),
                                    new FaithfulnessMetric.StatementVerdict("stmt2", "not supported", 0))));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java is wrong.")
                    .retrievedContexts(List.of("Java is a programming language."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.5 when half statements are faithful")
        void shouldReturn05WhenHalfFaithful() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1", "stmt2")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(List.of(
                                    new FaithfulnessMetric.StatementVerdict("stmt1", "supported", 1),
                                    new FaithfulnessMetric.StatementVerdict("stmt2", "not supported", 0))));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Mixed response.")
                    .retrievedContexts(List.of("Java is a programming language."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should return 0.0 when verdicts list is empty")
        void shouldReturn0WhenEmptyVerdicts() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(List.of()));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when verdicts are null")
        void shouldReturn0WhenNullVerdicts() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class, new FaithfulnessMetric.VerdictsResponse(null));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle null verdict in StatementVerdict")
        void shouldHandleNullVerdictValue() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1", "stmt2")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(List.of(
                                    new FaithfulnessMetric.StatementVerdict("stmt1", "supported", 1),
                                    new FaithfulnessMetric.StatementVerdict("stmt2", "unknown", null))));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Response.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            Double score = metric.singleTurnScore(sample);

            // 1 faithful out of 2, null counts as not faithful
            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should throw when all models fail at step 1")
        void shouldThrowWhenAllModelsFailAtStep1() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withModelError("model-1", new RuntimeException("Model failed"));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(
                                    List.of(new FaithfulnessMetric.StatementVerdict("stmt1", "supported", 1))));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should work with config and specified models")
        void shouldWorkWithConfigAndModels() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1", "model-2"))
                    .withResponse(
                            FaithfulnessMetric.StatementsResponse.class,
                            new FaithfulnessMetric.StatementsResponse(List.of("stmt1")))
                    .withResponse(
                            FaithfulnessMetric.VerdictsResponse.class,
                            new FaithfulnessMetric.VerdictsResponse(
                                    List.of(new FaithfulnessMetric.StatementVerdict("stmt1", "supported", 1))));

            FaithfulnessMetric metric =
                    FaithfulnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .userInput("What is Java?")
                    .response("Java.")
                    .retrievedContexts(List.of("Context."))
                    .build();

            FaithfulnessMetric.FaithfulnessConfig config = FaithfulnessMetric.FaithfulnessConfig.builder()
                    .model("model-1")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            assertThat(score).isEqualTo(1.0);
        }
    }
}
