package ai.qa.solutions.config;

import ai.qa.solutions.execution.MultiModelExecutor;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import ai.qa.solutions.metrics.retrieval.ContextEntityRecallMetric;
import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.metrics.retrieval.ContextRecallMetric;
import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SpringAiRagasMetricsProperties.class)
public class SpringAiRagasMetricsAutoconfiguration {

    @Bean
    public AspectCriticMetric aspectCriticMetric(final MultiModelExecutor executor) {
        return AspectCriticMetric.builder().executor(executor).build();
    }

    @Bean
    public SimpleCriteriaScoreMetric simpleCriteriaScoreMetric(final ChatClient.Builder chatClientBuilder) {
        return SimpleCriteriaScoreMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public RubricsScoreMetric rubricsScoreMetric(final ChatClient.Builder chatClientBuilder) {
        return RubricsScoreMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public ContextPrecisionMetric contextPrecisionMetric(final ChatClient.Builder chatClientBuilder) {
        return ContextPrecisionMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public ContextRecallMetric contextRecallMetric(final ChatClient.Builder chatClientBuilder) {
        return ContextRecallMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public ContextEntityRecallMetric contextEntityRecallMetric(final ChatClient.Builder chatClientBuilder) {
        return ContextEntityRecallMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public NoiseSensitivityMetric noiseSensitivityMetric(final ChatClient.Builder chatClientBuilder) {
        return NoiseSensitivityMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public FaithfulnessMetric faithfulnessMetric(final ChatClient.Builder chatClientBuilder) {
        return FaithfulnessMetric.builder()
                .chatClient(chatClientBuilder.build())
                .build();
    }

    @Bean
    public ResponseRelevancyMetric responseRelevancyMetric(
            final ChatClient.Builder chatClientBuilder, final List<EmbeddingModel> embeddingModels) {
        return ResponseRelevancyMetric.builder()
                .chatClient(chatClientBuilder.build())
                .embeddingModel(embeddingModels.get(0))
                .build();
    }
}
