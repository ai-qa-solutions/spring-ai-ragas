package ai.qa.solutions.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rageval")
public class RagEvalProperties {
    private int batchSize = 10;
    private Duration timeout = Duration.ofMinutes(5);
    private boolean enableMetrics = true;
    private int maxRetries = 3;
}
