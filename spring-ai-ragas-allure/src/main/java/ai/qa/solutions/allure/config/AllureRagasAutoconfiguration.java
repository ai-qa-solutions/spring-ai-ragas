package ai.qa.solutions.allure.config;

import ai.qa.solutions.allure.listener.AllureAttachmentWriter;
import ai.qa.solutions.allure.listener.AllureMetricExecutionListener;
import ai.qa.solutions.allure.methodology.MethodologyLoader;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot autoconfiguration for Allure RAGAS integration.
 * <p>
 * This configuration is activated when:
 * <ul>
 *   <li>{@link AllureLifecycle} is on the classpath</li>
 *   <li>{@code spring.ai.ragas.allure.enabled=true}</li>
 * </ul>
 *
 * <h3>Beans Created:</h3>
 * <ul>
 *   <li>{@link FreemarkerTemplateEngine} - For rendering HTML/MD reports</li>
 *   <li>{@link MethodologyLoader} - For loading metric methodology documentation</li>
 *   <li>{@link AllureAttachmentWriter} - For writing Allure attachments</li>
 *   <li>{@link AllureMetricExecutionListener} - Main listener for metrics</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * # application.yml
 * spring:
 *   ai:
 *     ragas:
 *       allure:
 *         enabled: true
 *         language: en
 * }</pre>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(AllureRagasProperties.class)
@ConditionalOnClass(AllureLifecycle.class)
@ConditionalOnProperty(prefix = "spring.ai.ragas.allure", name = "enabled", havingValue = "true")
public class AllureRagasAutoconfiguration {

    /**
     * Creates the Freemarker template engine for rendering reports.
     *
     * @return configured FreemarkerTemplateEngine
     */
    @Bean
    public FreemarkerTemplateEngine freemarkerTemplateEngine() {
        log.debug("Creating FreemarkerTemplateEngine for Allure RAGAS integration");
        return new FreemarkerTemplateEngine();
    }

    /**
     * Creates the methodology loader for loading metric documentation.
     *
     * @param properties the configuration properties
     * @return configured MethodologyLoader
     */
    @Bean
    public MethodologyLoader methodologyLoader(final AllureRagasProperties properties) {
        log.debug("Creating MethodologyLoader with language={}", properties.getLanguage());
        return new MethodologyLoader(properties.getLanguage());
    }

    /**
     * Creates the Allure attachment writer.
     *
     * @param templateEngine the template engine for rendering
     * @return configured AllureAttachmentWriter
     */
    @Bean
    public AllureAttachmentWriter allureAttachmentWriter(final FreemarkerTemplateEngine templateEngine) {
        log.debug("Creating AllureAttachmentWriter");
        return new AllureAttachmentWriter(Allure.getLifecycle(), templateEngine);
    }

    /**
     * Creates the main metric execution listener for Allure integration.
     * <p>
     * This listener will be automatically registered with all metrics
     * via Spring's dependency injection.
     *
     * @param properties the configuration properties
     * @param templateEngine the template engine
     * @param attachmentWriter the attachment writer
     * @param methodologyLoader the methodology loader
     * @return configured AllureMetricExecutionListener
     */
    @Bean
    public AllureMetricExecutionListener allureMetricExecutionListener(
            final AllureRagasProperties properties,
            final FreemarkerTemplateEngine templateEngine,
            final AllureAttachmentWriter attachmentWriter,
            final MethodologyLoader methodologyLoader) {
        log.info(
                "Creating AllureMetricExecutionListener with language={}, includePrompts={}, includeResponses={}",
                properties.getLanguage(),
                properties.isIncludePrompts(),
                properties.isIncludeResponses());
        return new AllureMetricExecutionListener(properties, templateEngine, attachmentWriter, methodologyLoader);
    }
}
