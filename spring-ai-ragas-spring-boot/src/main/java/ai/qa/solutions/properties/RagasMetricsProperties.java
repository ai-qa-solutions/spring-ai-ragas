package ai.qa.solutions.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring AI RAGAS metrics.
 * <p>
 * Example YAML configuration:
 * <pre>{@code
 * spring:
 *   ai:
 *     ragas:
 *       metrics:
 *         enabled: true
 *         logging:
 *           enabled: true
 *           level: verbose    # minimal | normal | verbose
 *           chart-width: 100
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.ai.ragas.metrics")
public class RagasMetricsProperties {

    /**
     * Whether metrics beans should be created.
     */
    private Boolean enabled = true;

    /**
     * Logging configuration for metric execution.
     */
    private Logging logging = new Logging();

    /**
     * Custom prompt templates for metric evaluation.
     * Overrides default and classpath-based prompts.
     */
    private Prompts prompts = new Prompts();

    @Getter
    @Setter
    public static class Logging {

        /**
         * Whether to enable logging listener for metric execution.
         */
        private boolean enabled = true;

        /**
         * Logging level: minimal, normal, or verbose.
         * <ul>
         *   <li>minimal - only start/end events</li>
         *   <li>normal - start/end + step progress (default)</li>
         *   <li>verbose - all details with wider charts</li>
         * </ul>
         */
        private Level level = Level.VERBOSE;

        /**
         * Width of ASCII bar charts in characters.
         */
        private int chartWidth = 100;

        /**
         * Height of ASCII bar charts (0 for auto-height based on model count).
         */
        private int chartHeight = 0;

        public enum Level {
            /**
             * Only log metric start and final result.
             */
            MINIMAL,

            /**
             * Log metric start, each step progress, and final result.
             */
            NORMAL,

            /**
             * Log everything with wider charts (120 chars).
             */
            VERBOSE
        }
    }

    @Getter
    @Setter
    public static class Prompts {

        /** Custom prompt template for AspectCriticMetric. */
        private String aspectCritic;

        /** Custom prompt template for SimpleCriteriaScoreMetric. */
        private String simpleCriteriaScore;

        /** Custom prompt template for RubricsScoreMetric. */
        private String rubricsScore;
    }
}
