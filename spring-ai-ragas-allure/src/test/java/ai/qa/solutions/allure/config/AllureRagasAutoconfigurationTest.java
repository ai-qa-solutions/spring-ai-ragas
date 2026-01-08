package ai.qa.solutions.allure.config;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.allure.listener.AllureAttachmentWriter;
import ai.qa.solutions.allure.listener.AllureMetricExecutionListener;
import ai.qa.solutions.allure.methodology.MethodologyLoader;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("AllureRagasAutoconfiguration")
class AllureRagasAutoconfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AllureRagasAutoconfiguration.class));

    @Nested
    @DisplayName("when enabled")
    class WhenEnabled {

        @Test
        @DisplayName("should create all beans")
        void shouldCreateAllBeans() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.allure.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(FreemarkerTemplateEngine.class);
                        assertThat(context).hasSingleBean(MethodologyLoader.class);
                        assertThat(context).hasSingleBean(AllureAttachmentWriter.class);
                        assertThat(context).hasSingleBean(AllureMetricExecutionListener.class);
                    });
        }

        @Test
        @DisplayName("should use configured language")
        void shouldUseConfiguredLanguage() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.allure.enabled=true", "spring.ai.ragas.allure.language=ru")
                    .run(context -> {
                        final MethodologyLoader loader = context.getBean(MethodologyLoader.class);
                        assertThat(loader.getLanguage()).isEqualTo("ru");
                    });
        }

        @Test
        @DisplayName("should use default language when not specified")
        void shouldUseDefaultLanguage() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.allure.enabled=true")
                    .run(context -> {
                        final MethodologyLoader loader = context.getBean(MethodologyLoader.class);
                        assertThat(loader.getLanguage()).isEqualTo("en");
                    });
        }
    }

    @Nested
    @DisplayName("when disabled")
    class WhenDisabled {

        @Test
        @DisplayName("should not create beans when enabled=false")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.allure.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(AllureMetricExecutionListener.class);
                        assertThat(context).doesNotHaveBean(FreemarkerTemplateEngine.class);
                        assertThat(context).doesNotHaveBean(MethodologyLoader.class);
                        assertThat(context).doesNotHaveBean(AllureAttachmentWriter.class);
                    });
        }

        @Test
        @DisplayName("should not create beans when property not set")
        void shouldNotCreateBeansWhenPropertyNotSet() {
            contextRunner.run(context -> {
                assertThat(context).doesNotHaveBean(AllureMetricExecutionListener.class);
            });
        }
    }

    @Nested
    @DisplayName("properties")
    class PropertiesTest {

        @Test
        @DisplayName("should bind all properties")
        void shouldBindAllProperties() {
            contextRunner
                    .withPropertyValues(
                            "spring.ai.ragas.allure.enabled=true",
                            "spring.ai.ragas.allure.language=ru",
                            "spring.ai.ragas.allure.include-prompts=false",
                            "spring.ai.ragas.allure.include-responses=false",
                            "spring.ai.ragas.allure.include-stack-traces=false",
                            "spring.ai.ragas.allure.max-prompt-length=1000",
                            "spring.ai.ragas.allure.max-response-length=2000")
                    .run(context -> {
                        final AllureRagasProperties props = context.getBean(AllureRagasProperties.class);
                        assertThat(props.isEnabled()).isTrue();
                        assertThat(props.getLanguage()).isEqualTo("ru");
                        assertThat(props.isIncludePrompts()).isFalse();
                        assertThat(props.isIncludeResponses()).isFalse();
                        assertThat(props.isIncludeStackTraces()).isFalse();
                        assertThat(props.getMaxPromptLength()).isEqualTo(1000);
                        assertThat(props.getMaxResponseLength()).isEqualTo(2000);
                    });
        }

        @Test
        @DisplayName("should have sensible defaults")
        void shouldHaveSensibleDefaults() {
            contextRunner
                    .withPropertyValues("spring.ai.ragas.allure.enabled=true")
                    .run(context -> {
                        final AllureRagasProperties props = context.getBean(AllureRagasProperties.class);
                        assertThat(props.getLanguage()).isEqualTo("en");
                        assertThat(props.isIncludePrompts()).isTrue();
                        assertThat(props.isIncludeResponses()).isTrue();
                        assertThat(props.isIncludeStackTraces()).isTrue();
                        assertThat(props.getMaxPromptLength()).isZero();
                        assertThat(props.getMaxResponseLength()).isZero();
                    });
        }
    }
}
