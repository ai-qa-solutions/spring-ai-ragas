package ai.qa.solutions.allure.listener;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("RenderConfig")
class RenderConfigTest {

    @Test
    @DisplayName("defaults() should have all flags true except methodology")
    void defaultsShouldHaveExpectedFlags() {
        final RenderConfig defaults = RenderConfig.defaults();

        assertThat(defaults.htmlAttachment()).isTrue();
        assertThat(defaults.markdownAttachment()).isTrue();
        assertThat(defaults.summary()).isTrue();
        assertThat(defaults.explanation()).isTrue();
        assertThat(defaults.methodology()).isFalse();
        assertThat(defaults.executionLog()).isTrue();
        assertThat(defaults.excludedModels()).isTrue();
    }

    @ParameterizedTest(name = "html={0}, md={1} -> anyAttachmentEnabled=true")
    @CsvSource({"true,  true", "true,  false", "false, true"})
    @DisplayName("anyAttachmentEnabled() should return true when either attachment is enabled")
    void anyAttachmentEnabledShouldReturnTrueWhenEitherEnabled(final boolean html, final boolean md) {
        final RenderConfig config = new RenderConfig(html, md, true, true, false, true, true);

        assertThat(config.anyAttachmentEnabled()).isTrue();
    }

    @Test
    @DisplayName("anyAttachmentEnabled() should return false when both attachments are disabled")
    void anyAttachmentEnabledShouldReturnFalseWhenBothDisabled() {
        final RenderConfig config = new RenderConfig(false, false, true, true, true, true, true);

        assertThat(config.anyAttachmentEnabled()).isFalse();
    }

    @ParameterizedTest(
            name = "summary={0}, explanation={1}, methodology={2}, executionLog={3}, excludedModels={4} -> any=true")
    @CsvSource({
        "true,  false, false, false, false",
        "false, true,  false, false, false",
        "false, false, true,  false, false",
        "false, false, false, true,  false",
        "false, false, false, false, true"
    })
    @DisplayName("anySectionEnabled() should return true when at least one section is enabled")
    void anySectionEnabledShouldReturnTrueWhenAtLeastOneEnabled(
            final boolean summary,
            final boolean explanation,
            final boolean methodology,
            final boolean executionLog,
            final boolean excludedModels) {
        final RenderConfig config =
                new RenderConfig(true, true, summary, explanation, methodology, executionLog, excludedModels);

        assertThat(config.anySectionEnabled()).isTrue();
    }

    @Test
    @DisplayName("anySectionEnabled() should return false when all sections are disabled")
    void anySectionEnabledShouldReturnFalseWhenAllDisabled() {
        final RenderConfig config = new RenderConfig(true, true, false, false, false, false, false);

        assertThat(config.anySectionEnabled()).isFalse();
    }
}
