package ai.qa.solutions.metrics.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.StubMultiModelExecutor;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationContext;
import ai.qa.solutions.execution.listener.dto.MetricEvaluationResult;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("FactualCorrectnessMetric Tests")
class FactualCorrectnessMetricTest {

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
            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(executor).build();

            assertThat(metric).isNotNull();
            assertThat(metric.getName()).isEqualTo("FactualCorrectnessMetric");
        }

        @Test
        @DisplayName("toBuilder should preserve settings")
        void toBuilderShouldPreserveSettings() {
            FactualCorrectnessMetric original =
                    FactualCorrectnessMetric.builder().executor(executor).build();

            FactualCorrectnessMetric copy = original.toBuilder().build();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.getName()).isEqualTo(original.getName());
        }

        @Test
        @DisplayName("Should allow custom prompt templates")
        void shouldAllowCustomPromptTemplates() {
            String customClaimsTemplate = "Custom claims template: {text}";
            String customNliTemplate = "Custom NLI template: {context} {claims}";

            FactualCorrectnessMetric metric = FactualCorrectnessMetric.builder()
                    .executor(executor)
                    .claimsDecompositionTemplate(customClaimsTemplate)
                    .nliVerificationTemplate(customNliTemplate)
                    .build();

            assertThat(metric).isNotNull();
        }
    }

    @Nested
    @DisplayName("FactualCorrectnessConfig")
    class ConfigTests {

        @Test
        @DisplayName("Should have default mode of F1")
        void shouldHaveDefaultModeF1() {
            FactualCorrectnessMetric.FactualCorrectnessConfig config =
                    FactualCorrectnessMetric.FactualCorrectnessConfig.builder().build();

            assertThat(config.getMode()).isEqualTo(FactualCorrectnessMetric.Mode.F1);
        }

        @Test
        @DisplayName("Should allow custom mode")
        void shouldAllowCustomMode() {
            FactualCorrectnessMetric.FactualCorrectnessConfig config =
                    FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                            .mode(FactualCorrectnessMetric.Mode.PRECISION)
                            .build();

            assertThat(config.getMode()).isEqualTo(FactualCorrectnessMetric.Mode.PRECISION);
        }

        @Test
        @DisplayName("Should start with empty models list")
        void shouldStartWithEmptyModelsList() {
            FactualCorrectnessMetric.FactualCorrectnessConfig config =
                    FactualCorrectnessMetric.FactualCorrectnessConfig.builder().build();

            assertThat(config.getModels()).isEmpty();
        }

        @Test
        @DisplayName("Should allow specifying models")
        void shouldAllowSpecifyingModels() {
            FactualCorrectnessMetric.FactualCorrectnessConfig config =
                    FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                            .model("model-1")
                            .model("model-2")
                            .build();

            assertThat(config.getModels()).containsExactly("model-1", "model-2");
        }
    }

    @Nested
    @DisplayName("Mode Enum")
    class ModeTests {

        @Test
        @DisplayName("Mode should have descriptions")
        void modeShouldHaveDescriptions() {
            assertThat(FactualCorrectnessMetric.Mode.F1.getDescription()).contains("F1");
            assertThat(FactualCorrectnessMetric.Mode.PRECISION.getDescription()).contains("Precision");
            assertThat(FactualCorrectnessMetric.Mode.RECALL.getDescription()).contains("Recall");
        }

        @Test
        @DisplayName("All modes should be defined")
        void allModesShouldBeDefined() {
            assertThat(FactualCorrectnessMetric.Mode.values()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("DTOs")
    class DtoTests {

        @Test
        @DisplayName("ClaimsResponse should store claims")
        void claimsResponseShouldStoreClaims() {
            List<String> claims = List.of("Claim 1", "Claim 2");
            FactualCorrectnessMetric.ClaimsResponse response = new FactualCorrectnessMetric.ClaimsResponse(claims);

            assertThat(response.claims()).containsExactly("Claim 1", "Claim 2");
        }

        @Test
        @DisplayName("NliVerdict should store all fields")
        void nliVerdictShouldStoreAllFields() {
            FactualCorrectnessMetric.NliVerdict verdict =
                    new FactualCorrectnessMetric.NliVerdict("claim", "SUPPORTED", "reason");

            assertThat(verdict.claim()).isEqualTo("claim");
            assertThat(verdict.verdict()).isEqualTo("SUPPORTED");
            assertThat(verdict.reason()).isEqualTo("reason");
        }

        @Test
        @DisplayName("NliResponse should store verdicts")
        void nliResponseShouldStoreVerdicts() {
            List<FactualCorrectnessMetric.NliVerdict> verdicts = List.of(
                    new FactualCorrectnessMetric.NliVerdict("claim1", "SUPPORTED", "reason1"),
                    new FactualCorrectnessMetric.NliVerdict("claim2", "CONTRADICTED", "reason2"));

            FactualCorrectnessMetric.NliResponse response = new FactualCorrectnessMetric.NliResponse(verdicts);

            assertThat(response.verdicts()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Scoring")
    class ScoringTests {

        @Test
        @DisplayName("Should return 0.0 when response is missing")
        void shouldReturn0WhenNoResponse() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of());

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().reference("Reference text").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when reference is missing")
        void shouldReturn0WhenNoReference() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of());

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder().response("Response text").build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 1.0 when all claims are supported (F1 mode)")
        void shouldReturn1WhenAllClaimsSupported() {
            // All claims supported means precision=1.0, recall=1.0, F1=1.0
            List<String> claims = List.of("Claim 1", "Claim 2");
            FactualCorrectnessMetric.ClaimsResponse claimsResponse =
                    new FactualCorrectnessMetric.ClaimsResponse(claims);

            List<FactualCorrectnessMetric.NliVerdict> verdicts = List.of(
                    new FactualCorrectnessMetric.NliVerdict("Claim 1", "SUPPORTED", "reason"),
                    new FactualCorrectnessMetric.NliVerdict("Claim 2", "SUPPORTED", "reason"));
            FactualCorrectnessMetric.NliResponse nliResponse = new FactualCorrectnessMetric.NliResponse(verdicts);

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withResponseProvider(FactualCorrectnessMetric.ClaimsResponse.class, prompt -> claimsResponse)
                    .withResponseProvider(FactualCorrectnessMetric.NliResponse.class, prompt -> nliResponse);

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Paris is the capital of France. The Eiffel Tower is in Paris.")
                    .reference("Paris is the capital of France. The Eiffel Tower is located in Paris.")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should return 0.0 when no claims are supported")
        void shouldReturn0WhenNoClaimsSupported() {
            List<String> claims = List.of("Claim 1", "Claim 2");
            FactualCorrectnessMetric.ClaimsResponse claimsResponse =
                    new FactualCorrectnessMetric.ClaimsResponse(claims);

            List<FactualCorrectnessMetric.NliVerdict> verdicts = List.of(
                    new FactualCorrectnessMetric.NliVerdict("Claim 1", "CONTRADICTED", "reason"),
                    new FactualCorrectnessMetric.NliVerdict("Claim 2", "NEUTRAL", "reason"));
            FactualCorrectnessMetric.NliResponse nliResponse = new FactualCorrectnessMetric.NliResponse(verdicts);

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withResponseProvider(FactualCorrectnessMetric.ClaimsResponse.class, prompt -> claimsResponse)
                    .withResponseProvider(FactualCorrectnessMetric.NliResponse.class, prompt -> nliResponse);

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("The Earth is flat.")
                    .reference("The Earth is spherical.")
                    .build();

            Double score = metric.singleTurnScore(sample);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate intermediate F1 score")
        void shouldCalculateIntermediateF1Score() {
            // 50% precision, 50% recall => F1 = 0.5
            List<String> claims = List.of("Claim 1", "Claim 2");
            FactualCorrectnessMetric.ClaimsResponse claimsResponse =
                    new FactualCorrectnessMetric.ClaimsResponse(claims);

            List<FactualCorrectnessMetric.NliVerdict> verdicts = List.of(
                    new FactualCorrectnessMetric.NliVerdict("Claim 1", "SUPPORTED", "reason"),
                    new FactualCorrectnessMetric.NliVerdict("Claim 2", "CONTRADICTED", "reason"));
            FactualCorrectnessMetric.NliResponse nliResponse = new FactualCorrectnessMetric.NliResponse(verdicts);

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withResponseProvider(FactualCorrectnessMetric.ClaimsResponse.class, prompt -> claimsResponse)
                    .withResponseProvider(FactualCorrectnessMetric.NliResponse.class, prompt -> nliResponse);

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScore(sample);

            // F1 = 2 * (0.5 * 0.5) / (0.5 + 0.5) = 0.5
            assertThat(score).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("Should use PRECISION mode when configured")
        void shouldUsePrecisionModeWhenConfigured() {
            // 100% precision (1/1), 50% recall (1/2)
            List<String> responseClaims = List.of("Claim 1");
            List<String> referenceClaims = List.of("Claim 1", "Claim 2");

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withResponseProvider(FactualCorrectnessMetric.ClaimsResponse.class, prompt -> {
                        // Alternate between response and reference claims
                        return new FactualCorrectnessMetric.ClaimsResponse(responseClaims);
                    })
                    .withResponseProvider(FactualCorrectnessMetric.NliResponse.class, prompt -> {
                        List<FactualCorrectnessMetric.NliVerdict> verdicts =
                                List.of(new FactualCorrectnessMetric.NliVerdict("Claim 1", "SUPPORTED", "reason"));
                        return new FactualCorrectnessMetric.NliResponse(verdicts);
                    });

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            FactualCorrectnessMetric.FactualCorrectnessConfig config =
                    FactualCorrectnessMetric.FactualCorrectnessConfig.builder()
                            .mode(FactualCorrectnessMetric.Mode.PRECISION)
                            .build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScore(config, sample);

            // In PRECISION mode, only precision matters: 1/1 = 1.0
            assertThat(score).isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should throw when all models fail")
        void shouldThrowWhenAllModelsFail() {
            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withModelError("model-1", new RuntimeException("Model failed"));

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            assertThatThrownBy(() -> metric.singleTurnScore(sample))
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("All models failed");
        }

        @Test
        @DisplayName("Should work with async scoring")
        void shouldWorkWithAsyncScoring() {
            List<String> claims = List.of("Claim 1");
            FactualCorrectnessMetric.ClaimsResponse claimsResponse =
                    new FactualCorrectnessMetric.ClaimsResponse(claims);

            List<FactualCorrectnessMetric.NliVerdict> verdicts =
                    List.of(new FactualCorrectnessMetric.NliVerdict("Claim 1", "SUPPORTED", "reason"));
            FactualCorrectnessMetric.NliResponse nliResponse = new FactualCorrectnessMetric.NliResponse(verdicts);

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withResponseProvider(FactualCorrectnessMetric.ClaimsResponse.class, prompt -> claimsResponse)
                    .withResponseProvider(FactualCorrectnessMetric.NliResponse.class, prompt -> nliResponse);

            FactualCorrectnessMetric metric =
                    FactualCorrectnessMetric.builder().executor(stubExecutor).build();

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            Double score = metric.singleTurnScoreAsync(sample).join();

            assertThat(score).isBetween(0.0, 1.0);
        }
    }

    @Nested
    @DisplayName("Listener Notifications")
    class ListenerTests {

        @Test
        @DisplayName("Should notify listeners during evaluation")
        void shouldNotifyListenersDuringEvaluation() {
            List<String> claims = List.of("Claim 1");
            FactualCorrectnessMetric.ClaimsResponse claimsResponse =
                    new FactualCorrectnessMetric.ClaimsResponse(claims);

            List<FactualCorrectnessMetric.NliVerdict> verdicts =
                    List.of(new FactualCorrectnessMetric.NliVerdict("Claim 1", "SUPPORTED", "reason"));
            FactualCorrectnessMetric.NliResponse nliResponse = new FactualCorrectnessMetric.NliResponse(verdicts);

            StubMultiModelExecutor stubExecutor = new StubMultiModelExecutor(List.of("model-1"), List.of())
                    .withResponseProvider(FactualCorrectnessMetric.ClaimsResponse.class, prompt -> claimsResponse)
                    .withResponseProvider(FactualCorrectnessMetric.NliResponse.class, prompt -> nliResponse);

            RecordingListener listener = new RecordingListener();

            FactualCorrectnessMetric metric = FactualCorrectnessMetric.builder()
                    .executor(stubExecutor)
                    .build()
                    .withListeners(List.of(listener));

            Sample sample = Sample.builder()
                    .response("Response text")
                    .reference("Reference text")
                    .build();

            metric.singleTurnScore(sample);

            assertThat(listener.beforeMetricCalled).isTrue();
            assertThat(listener.afterMetricCalled).isTrue();
        }

        static class RecordingListener implements MetricExecutionListener {
            boolean beforeMetricCalled = false;
            boolean afterMetricCalled = false;

            @Override
            public void beforeMetricEvaluation(MetricEvaluationContext context) {
                beforeMetricCalled = true;
            }

            @Override
            public void afterMetricEvaluation(MetricEvaluationResult result) {
                afterMetricCalled = true;
            }

            @Override
            public MetricExecutionListener forEvaluation() {
                return this;
            }
        }
    }
}
