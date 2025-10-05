package ai.qa.solutions.config;

import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagEvalProperties.class)
public class RagEvalAutoConfiguration {

    @Bean
    public AspectCriticMetric aspectCriticMetric(final ChatClient.Builder chatClientBuilder) {
        return new AspectCriticMetric(chatClientBuilder.build());
    }

    @Bean
    public SimpleCriteriaScoreMetric simpleCriteriaScoreMetric(final ChatClient.Builder chatClientBuilder) {
        return new SimpleCriteriaScoreMetric(chatClientBuilder.build());
    }

    @Bean
    public RubricsScoreMetric rubricsScoreMetric(final ChatClient.Builder chatClientBuilder) {
        return new RubricsScoreMetric(chatClientBuilder.build());
    }

    @Bean
    public ContextPrecisionMetric contextPrecisionMetric(final ChatClient.Builder chatClientBuilder) {
        return new ContextPrecisionMetric(chatClientBuilder.build());
    }

    @Bean
    public ContextRecallMetric contextRecallMetric(final ChatClient.Builder chatClientBuilder) {
        return new ContextRecallMetric(chatClientBuilder.build());
    }

    @Bean
    public ContextEntityRecallMetric contextEntityRecallMetric(final ChatClient.Builder chatClientBuilder) {
        return new ContextEntityRecallMetric(chatClientBuilder.build());
    }

    @Bean
    public NoiseSensitivityMetric noiseSensitivityMetric(final ChatClient.Builder chatClientBuilder) {
        return new NoiseSensitivityMetric(
                chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor()).build());
    }
}
