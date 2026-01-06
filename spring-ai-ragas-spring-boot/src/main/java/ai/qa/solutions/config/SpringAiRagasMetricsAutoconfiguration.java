package ai.qa.solutions.config;

import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.execution.listener.LoggingMetricExecutionListener;
import ai.qa.solutions.execution.listener.MetricExecutionListener;
import ai.qa.solutions.metric.AbstractMultiModelMetric;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import java.net.http.HttpClient;
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
@EnableConfigurationProperties(SpringAiRagasMetricsProperties.class)
@ConditionalOnProperty(
        prefix = "spring.ai.ragas.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SpringAiRagasMetricsAutoconfiguration {

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
    public LoggingMetricExecutionListener loggingMetricExecutionListener(
            final SpringAiRagasMetricsProperties properties) {
        final SpringAiRagasMetricsProperties.Logging loggingConfig = properties.getLogging();
        final SpringAiRagasMetricsProperties.Logging.Level level = loggingConfig.getLevel();

        final int chartWidth =
                switch (level) {
                    case MINIMAL, NORMAL -> loggingConfig.getChartWidth();
                    case VERBOSE -> Math.max(120, loggingConfig.getChartWidth());
                };

        final boolean showStepDetails = level != SpringAiRagasMetricsProperties.Logging.Level.MINIMAL;

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
        log.info("Configuring RestClient to use HTTP/1.1 to avoid concurrent stream limits");
        return builder -> {
            HttpClient httpClient =
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            builder.requestFactory(new JdkClientHttpRequestFactory(httpClient));
        };
    }

    // ==================== Metric Beans ====================

    @Bean
    public AspectCriticMetric aspectCriticMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(AspectCriticMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public SimpleCriteriaScoreMetric simpleCriteriaScoreMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(
                SimpleCriteriaScoreMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public RubricsScoreMetric rubricsScoreMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(RubricsScoreMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public ContextPrecisionMetric contextPrecisionMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(ContextPrecisionMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public ContextRecallMetric contextRecallMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(ContextRecallMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public ContextEntityRecallMetric contextEntityRecallMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(
                ContextEntityRecallMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public NoiseSensitivityMetric noiseSensitivityMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(NoiseSensitivityMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public FaithfulnessMetric faithfulnessMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(FaithfulnessMetric.builder().executor(executor).build(), listeners);
    }

    @Bean
    public ResponseRelevancyMetric responseRelevancyMetric(
            final MultiModelExecutor executor, final List<MetricExecutionListener> listeners) {
        return withListeners(
                ResponseRelevancyMetric.builder().executor(executor).build(), listeners);
    }

    // ==================== Helper Methods ====================

    /**
     * Adds all listeners to a metric.
     *
     * @param metric    the metric to configure
     * @param listeners list of listeners to add
     * @param <T>       the metric type
     * @return the configured metric
     */
    private <T extends AbstractMultiModelMetric<?>> T withListeners(
            final T metric, final List<MetricExecutionListener> listeners) {
        if (listeners != null) {
            listeners.forEach(metric::addListener);
            log.debug("Added {} listeners to {}", listeners.size(), metric.getName());
        }
        return metric;
    }
}
