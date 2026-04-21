package ai.qa.solutions.config;

import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.execution.listener.impl.LoggingMetricExecutionListener;
import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.metrics.agent.ToolCallAccuracyMetric;
import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.metrics.nlp.ChrfScoreMetric;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric;
import ai.qa.solutions.metrics.nvidia.AnswerAccuracyMetric;
import ai.qa.solutions.metrics.nvidia.ContextRelevanceMetric;
import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric;
import ai.qa.solutions.metrics.response.AnswerCorrectnessMetric;
import ai.qa.solutions.metrics.response.FactualCorrectnessMetric;
import ai.qa.solutions.metrics.response.SemanticSimilarityMetric;
import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.properties.RagasMetricsProperties;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Autoconfiguration for Spring AI RAGAS metrics.
 * <p>
 * Creates metric beans and automatically injects configured listeners.
 * <p>
 * Configuration example:
 * <pre>{@code
 * spring:
 *   ai:
 *     ragas:
 *       metrics:
 *         enabled: true
 *         logging:
 *           enabled: true
 *           level: normal  # minimal | normal | verbose
 *           chart-width: 100
 * }</pre>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RagasMetricsProperties.class)
@ConditionalOnProperty(
        prefix = "spring.ai.ragas.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RagasMetricsAutoconfiguration {

    /**
     * Creates the logging listener based on configuration properties.
     *
     * @param properties the RAGAS metrics properties
     * @return configured logging listener, or null if disabled
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "spring.ai.ragas.metrics.logging",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public LoggingMetricExecutionListener loggingMetricExecutionListener(final RagasMetricsProperties properties) {
        final RagasMetricsProperties.Logging loggingConfig = properties.getLogging();
        final RagasMetricsProperties.Logging.Level level = loggingConfig.getLevel();

        final int chartWidth =
                switch (level) {
                    case MINIMAL, NORMAL -> loggingConfig.getChartWidth();
                    case VERBOSE -> Math.max(120, loggingConfig.getChartWidth());
                };

        final boolean showStepDetails = level != RagasMetricsProperties.Logging.Level.MINIMAL;

        log.info(
                "Creating LoggingMetricExecutionListener with level={}, chartWidth={}, showStepDetails={}",
                level,
                chartWidth,
                showStepDetails);

        return new LoggingMetricExecutionListener(chartWidth, loggingConfig.getChartHeight(), showStepDetails);
    }

    /**
     * Customizes RestClient.Builder to use HTTP/1.1 instead of HTTP/2.
     * <p>
     * This is necessary to avoid "too many concurrent streams" errors when running
     * parallel requests to OpenAI-compatible APIs. HTTP/2 has a limit on concurrent
     * streams per connection, while HTTP/1.1 creates separate connections for each request.
     * <p>
     * Spring AI uses RestClient.Builder internally for API calls.
     *
     * @return RestClientCustomizer that configures JDK HttpClient with HTTP/1.1
     */
    @Bean
    public RestClientCustomizer http11RestClientCustomizer() {
        log.info("Configuring RestClient to use HTTP/1.1 with 5 min timeout");
        return builder -> {
            final HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMinutes(5))
                    .build();
            final JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(Duration.ofMinutes(5));
            builder.requestFactory(factory);
        };
    }

    // ==================== Metric Beans ====================

    @Bean
    public AspectCriticMetric aspectCriticMetric(
            final MultiModelExecutor executor,
            final List<MetricExecutionListener> listeners,
            final RagasMetricsProperties properties) {
        return AspectCriticMetric.builder()
                .executor(executor)
                .promptTemplate(resolvePropertyPrompt(properties.getPrompts().getAspectCritic()))
                .build()
                .withListeners(listeners);
    }

    @Bean
    public SimpleCriteriaScoreMetric simpleCriteriaScoreMetric(
            final MultiModelExecutor executor,
            final List<MetricExecutionListener> listeners,
            final RagasMetricsProperties properties) {
        return SimpleCriteriaScoreMetric.builder()
                .executor(executor)
                .promptTemplate(resolvePropertyPrompt(properties.getPrompts().getSimpleCriteriaScore()))
                .build()
                .withListeners(listeners);
    }

    @Bean
    public RubricsScoreMetric rubricsScoreMetric(
            final MultiModelExecutor executor,
            final List<MetricExecutionListener> listeners,
            final RagasMetricsProperties properties) {
        return RubricsScoreMetric.builder()
                .executor(executor)
                .promptTemplate(resolvePropertyPrompt(properties.getPrompts().getRubricsScore()))
                .build()
                .withListeners(listeners);
    }

    @Bean
    public ContextPrecisionMetric contextPrecisionMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ContextPrecisionMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public ContextRecallMetric contextRecallMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ContextRecallMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public ContextEntityRecallMetric contextEntityRecallMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ContextEntityRecallMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public NoiseSensitivityMetric noiseSensitivityMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return NoiseSensitivityMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public FaithfulnessMetric faithfulnessMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return FaithfulnessMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public ResponseRelevancyMetric responseRelevancyMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ResponseRelevancyMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public SemanticSimilarityMetric semanticSimilarityMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return SemanticSimilarityMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public FactualCorrectnessMetric factualCorrectnessMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return FactualCorrectnessMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public AnswerCorrectnessMetric answerCorrectnessMetric(
            final MultiModelExecutor executor,
            final FactualCorrectnessMetric factualCorrectnessMetric,
            final SemanticSimilarityMetric semanticSimilarityMetric,
            final List<MetricExecutionListener> listeners) {
        return AnswerCorrectnessMetric.builder()
                .executor(executor)
                .factualCorrectnessMetric(factualCorrectnessMetric)
                .semanticSimilarityMetric(semanticSimilarityMetric)
                .build()
                .withListeners(listeners);
    }

    // ==================== Agent Metrics ====================

    @Bean
    public AgentGoalAccuracyMetric agentGoalAccuracyMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return AgentGoalAccuracyMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public ToolCallAccuracyMetric toolCallAccuracyMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ToolCallAccuracyMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public TopicAdherenceMetric topicAdherenceMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return TopicAdherenceMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public ContextRelevanceMetric contextRelevanceMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ContextRelevanceMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public ResponseGroundednessMetric responseGroundednessMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return ResponseGroundednessMetric.builder().executor(executor).build().withListeners(listeners);
    }

    @Bean
    public AnswerAccuracyMetric answerAccuracyMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return AnswerAccuracyMetric.builder().executor(executor).build().withListeners(listeners);
    }

    // ==================== NLP Metrics (Non-LLM) ====================

    @Bean
    public BleuScoreMetric bleuScoreMetric(final List<MetricExecutionListener> listeners) {
        return new BleuScoreMetric().withListeners(listeners);
    }

    @Bean
    public RougeScoreMetric rougeScoreMetric(final List<MetricExecutionListener> listeners) {
        return new RougeScoreMetric().withListeners(listeners);
    }

    @Bean
    public ChrfScoreMetric chrfScoreMetric(final List<MetricExecutionListener> listeners) {
        return new ChrfScoreMetric().withListeners(listeners);
    }

    @Bean
    public StringSimilarityMetric stringSimilarityMetric(final List<MetricExecutionListener> listeners) {
        return new StringSimilarityMetric().withListeners(listeners);
    }

    // ==================== Helper Methods ====================

    private String resolvePropertyPrompt(final String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return null;
        }
        if (prompt.startsWith("classpath:")) {
            final String path = prompt.substring("classpath:".length());
            try (final InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                log.warn("Prompt resource not found: {}", path);
            } catch (final Exception e) {
                log.warn("Failed to load prompt resource '{}': {}", path, e.getMessage());
            }
            return null;
        }
        return prompt;
    }
}
