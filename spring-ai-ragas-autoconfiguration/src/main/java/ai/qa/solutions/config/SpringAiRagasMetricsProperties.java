package ai.qa.solutions.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.ai.ragas.metrics")
public class SpringAiRagasMetricsProperties {
    private Boolean enabled = true;
}
