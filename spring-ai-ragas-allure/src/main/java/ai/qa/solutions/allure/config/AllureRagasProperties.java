package ai.qa.solutions.allure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Allure RAGAS integration.
 * <p>
 * These properties control the behavior of the {@link ai.qa.solutions.allure.listener.AllureMetricExecutionListener}.
 *
 * <h3>Configuration Example:</h3>
 * <pre>{@code
 * spring:
 *   ai:
 *     ragas:
 *       allure:
 *         enabled: true
 *         language: en
 *         include-prompts: true
 *         include-responses: true
 *         include-stack-traces: true
 * }</pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.ai.ragas.allure")
public class AllureRagasProperties {

    /**
     * Enable Allure integration for RAGAS metrics.
     * <p>
     * When enabled, an {@link ai.qa.solutions.allure.listener.AllureMetricExecutionListener}
     * will be automatically registered to generate Allure attachments for each metric evaluation.
     * <p>
     * Default: {@code false}
     */
    private boolean enabled = false;

    /**
     * Language for methodology documentation.
     * <p>
     * Supported values: "en" (English), "ru" (Russian).
     * If the requested language is not available, falls back to English.
     * <p>
     * Default: {@code "en"}
     */
    private String language = "en";

    /**
     * Include full prompts in the report.
     * <p>
     * When enabled, the complete LLM prompts are included in the execution log.
     * Disable if prompts contain sensitive information.
     * <p>
     * Default: {@code true}
     */
    private boolean includePrompts = true;

    /**
     * Include full JSON responses in the report.
     * <p>
     * When enabled, the complete JSON responses from LLM calls are included.
     * Disable if responses are very large or contain sensitive data.
     * <p>
     * Default: {@code true}
     */
    private boolean includeResponses = true;

    /**
     * Include full stack traces for failed models.
     * <p>
     * When enabled, complete stack traces are included for model exclusion events.
     * Useful for debugging but may increase report size.
     * <p>
     * Default: {@code true}
     */
    private boolean includeStackTraces = true;

    /**
     * Maximum length for prompts in the report (0 = no limit).
     * <p>
     * If a prompt exceeds this length, it will be truncated with "..." suffix.
     * Set to 0 to include full prompts without truncation.
     * <p>
     * Default: {@code 0} (no limit)
     */
    private int maxPromptLength = 0;

    /**
     * Maximum length for responses in the report (0 = no limit).
     * <p>
     * If a response exceeds this length, it will be truncated with "..." suffix.
     * Set to 0 to include full responses without truncation.
     * <p>
     * Default: {@code 0} (no limit)
     */
    private int maxResponseLength = 0;
}
