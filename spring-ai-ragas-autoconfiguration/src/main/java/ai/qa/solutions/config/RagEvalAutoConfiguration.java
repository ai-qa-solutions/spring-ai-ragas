package ai.qa.solutions.config;

import ai.qa.solutions.llm.LLMEvaluationService;
import ai.qa.solutions.metrics.general.AspectCriticMetric;
import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.metrics.general.SimpleCriteriaScoreMetric;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RagEvalProperties.class)
public class RagEvalAutoConfiguration {

    @Bean
    public LLMEvaluationService llmEvaluationService(
            final ChatClient.Builder chatClientBuilder) {
        return new LLMEvaluationService(chatClientBuilder);
    }

    @Bean
    public AspectCriticMetric aspectCriticMetric(final LLMEvaluationService llmService) {
        final AspectCriticMetric metric = new AspectCriticMetric();
        metric.setLlmService(llmService);
        return metric;
    }

    @Bean
    public SimpleCriteriaScoreMetric simpleCriteriaScoreMetric(final LLMEvaluationService llmService) {
        final SimpleCriteriaScoreMetric metric = new SimpleCriteriaScoreMetric();
        metric.setLlmService(llmService);
        return metric;
    }

    @Bean
    public RubricsScoreMetric rubricsScoreMetric(final LLMEvaluationService llmService) {
        final RubricsScoreMetric metric = new RubricsScoreMetric();
        metric.setLlmService(llmService);
        return metric;
    }
}
