package ai.qa.solutions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.qa.solutions.chatclient.ChatClientStore;
import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.impl.LoggingMetricExecutionListener;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@DisplayName("RagasMetricsAutoconfiguration Tests")
class RagasMetricsAutoconfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RagasMetricsAutoconfiguration.class));

    @Nested
    @DisplayName("Conditional Activation")
    class ConditionalActivation {

        @Test
        @DisplayName("Should not create beans when property is disabled")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=false")
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LoggingMetricExecutionListener.class);
                        assertThat(context).doesNotHaveBean(AspectCriticMetric.class);
                    });
        }

        @Test
        @DisplayName("Should create beans when property is enabled")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.enabled=true")
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
                        assertThat(context).hasSingleBean(RestClientCustomizer.class);
                    });
        }

        @Test
        @DisplayName("Should create beans by default (matchIfMissing=true)")
        void shouldCreateBeansByDefault() {
            contextRunner.withUserConfiguration(MockDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
            });
        }
    }

    @Nested
    @DisplayName("LoggingMetricExecutionListener")
    class LoggingMetricExecutionListenerTests {

        @Test
        @DisplayName("Should create listener with default settings")
        void shouldCreateWithDefaultSettings() {
            contextRunner.withUserConfiguration(MockDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
            });
        }

        @Test
        @DisplayName("Should not create listener when logging is disabled")
        void shouldNotCreateWhenLoggingDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.logging.enabled=false")
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LoggingMetricExecutionListener.class);
                    });
        }

        @Test
        @DisplayName("Should configure listener with MINIMAL level")
        void shouldConfigureWithMinimalLevel() {
            contextRunner
                    .withPropertyValues(
                            "spring.ai.ragas.metrics.logging.level=MINIMAL",
                            "spring.ai.ragas.metrics.logging.chart-width=80")
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
                    });
        }

        @Test
        @DisplayName("Should configure listener with NORMAL level")
        void shouldConfigureWithNormalLevel() {
            contextRunner
                    .withPropertyValues(
                            "spring.ai.ragas.metrics.logging.level=NORMAL",
                            "spring.ai.ragas.metrics.logging.chart-width=100")
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
                    });
        }

        @Test
        @DisplayName("Should configure listener with VERBOSE level and wider chart")
        void shouldConfigureWithVerboseLevel() {
            contextRunner
                    .withPropertyValues(
                            "spring.ai.ragas.metrics.logging.level=VERBOSE",
                            "spring.ai.ragas.metrics.logging.chart-width=80" // Should be upgraded to 120
                            )
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
                    });
        }

        @Test
        @DisplayName("Should respect chart-height configuration")
        void shouldRespectChartHeightConfig() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.metrics.logging.chart-height=20")
                    .withUserConfiguration(MockDependenciesConfig.class)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggingMetricExecutionListener.class);
                    });
        }
    }

    @Nested
    @DisplayName("RestClientCustomizer")
    class RestClientCustomizerTests {

        @Test
        @DisplayName("Should create HTTP/1.1 RestClientCustomizer")
        void shouldCreateHttp11Customizer() {
            contextRunner.withUserConfiguration(MockDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(RestClientCustomizer.class);
            });
        }
    }

    @Nested
    @DisplayName("Metric Beans")
    class MetricBeans {

        @Test
        @DisplayName("Should create all general metrics")
        void shouldCreateGeneralMetrics() {
            contextRunner.withUserConfiguration(MockDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(AspectCriticMetric.class);
                assertThat(context).hasSingleBean(SimpleCriteriaScoreMetric.class);
                assertThat(context).hasSingleBean(RubricsScoreMetric.class);
            });
        }

        @Test
        @DisplayName("Should create all retrieval metrics")
        void shouldCreateRetrievalMetrics() {
            contextRunner.withUserConfiguration(MockDependenciesConfig.class).run(context -> {
                assertThat(context).hasSingleBean(ContextPrecisionMetric.class);
                assertThat(context).hasSingleBean(ContextRecallMetric.class);
                assertThat(context).hasSingleBean(ContextEntityRecallMetric.class);
                assertThat(context).hasSingleBean(NoiseSensitivityMetric.class);
                assertThat(context).hasSingleBean(FaithfulnessMetric.class);
                assertThat(context).hasSingleBean(ResponseRelevancyMetric.class);
            });
        }

        @Test
        @DisplayName("Metrics should have listeners injected")
        void metricsShouldHaveListenersInjected() {
            contextRunner.withUserConfiguration(MockDependenciesConfig.class).run(context -> {
                AspectCriticMetric metric = context.getBean(AspectCriticMetric.class);
                assertThat(metric).isNotNull();
                assertThat(metric.getListeners()).isNotEmpty();
            });
        }
    }

    @Configuration
    static class MockDependenciesConfig {
        @Bean
        MultiModelExecutor multiModelExecutor() {
            ChatClient client = mock(ChatClient.class);
            ChatClientStore store = new ChatClientStore(Map.of("model-1", client), client);
            return new MultiModelExecutor(store, null, new SimpleAsyncTaskExecutor());
        }

        @Bean
        AsyncTaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }
}
