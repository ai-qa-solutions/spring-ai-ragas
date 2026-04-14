package ai.qa.solutions.metric.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PromptTemplateResolver Tests")
class PromptTemplateResolverTest {

    @Test
    @DisplayName("Should resolve English prompt from classpath")
    void shouldResolveEnglishPromptFromClasspath() {
        final String result = PromptTemplateResolver.resolve("aspect-critic", "en", "fallback");
        assertThat(result).isNotNull();
        assertThat(result).contains("{definition}");
        assertThat(result).contains("{user_input}");
        assertThat(result).contains("{response}");
        assertThat(result).isNotEqualTo("fallback");
    }

    @Test
    @DisplayName("Should resolve Russian prompt from classpath")
    void shouldResolveRussianPromptFromClasspath() {
        final String result = PromptTemplateResolver.resolve("aspect-critic", "ru", "fallback");
        assertThat(result).isNotNull();
        assertThat(result).contains("{definition}");
        assertThat(result).contains("{user_input}");
        assertThat(result).contains("{response}");
        assertThat(result).isNotEqualTo("fallback");
        // Russian prompt should be different from English
        final String english = PromptTemplateResolver.resolve("aspect-critic", "en", "fallback");
        assertThat(result).isNotEqualTo(english);
    }

    @Test
    @DisplayName("Should fallback to English when language not found")
    void shouldFallbackToEnglishWhenLanguageNotFound() {
        final String result = PromptTemplateResolver.resolve("aspect-critic", "ja", "fallback");
        // Should get the English version, not the fallback
        final String english = PromptTemplateResolver.resolve("aspect-critic", "en", "fallback");
        assertThat(result).isEqualTo(english);
    }

    @Test
    @DisplayName("Should fallback to default when resource missing")
    void shouldFallbackToDefaultWhenResourceMissing() {
        final String result = PromptTemplateResolver.resolve("nonexistent-metric", "en", "my-default");
        assertThat(result).isEqualTo("my-default");
    }

    @Test
    @DisplayName("Should return null when no default provided and resource missing")
    void shouldReturnNullWhenNoDefaultProvided() {
        final String result = PromptTemplateResolver.resolve("nonexistent-metric", "en", null);
        assertThat(result).isNull();
    }
}
