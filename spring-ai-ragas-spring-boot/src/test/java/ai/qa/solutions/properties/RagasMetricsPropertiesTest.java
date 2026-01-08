package ai.qa.solutions.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@DisplayName("RagasMetricsProperties Tests")
class RagasMetricsPropertiesTest {

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("Should have enabled=true by default")
        void shouldHaveEnabledTrue() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            assertThat(properties.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have logging enabled by default")
        void shouldHaveLoggingEnabled() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            assertThat(properties.getLogging()).isNotNull();
            assertThat(properties.getLogging().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should have VERBOSE level by default")
        void shouldHaveVerboseLevel() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            assertThat(properties.getLogging().getLevel()).isEqualTo(RagasMetricsProperties.Logging.Level.VERBOSE);
        }

        @Test
        @DisplayName("Should have chart-width=100 by default")
        void shouldHaveChartWidth100() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            assertThat(properties.getLogging().getChartWidth()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should have chart-height=0 by default (auto)")
        void shouldHaveChartHeightZero() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            assertThat(properties.getLogging().getChartHeight()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGetters {

        @Test
        @DisplayName("Should set and get enabled")
        void shouldSetAndGetEnabled() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            properties.setEnabled(false);
            assertThat(properties.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should set and get logging enabled")
        void shouldSetAndGetLoggingEnabled() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            properties.getLogging().setEnabled(false);
            assertThat(properties.getLogging().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should set and get logging level")
        void shouldSetAndGetLoggingLevel() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            properties.getLogging().setLevel(RagasMetricsProperties.Logging.Level.MINIMAL);
            assertThat(properties.getLogging().getLevel()).isEqualTo(RagasMetricsProperties.Logging.Level.MINIMAL);
        }

        @Test
        @DisplayName("Should set and get chart width")
        void shouldSetAndGetChartWidth() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            properties.getLogging().setChartWidth(150);
            assertThat(properties.getLogging().getChartWidth()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should set and get chart height")
        void shouldSetAndGetChartHeight() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            properties.getLogging().setChartHeight(25);
            assertThat(properties.getLogging().getChartHeight()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should replace logging object")
        void shouldReplaceLoggingObject() {
            RagasMetricsProperties properties = new RagasMetricsProperties();
            RagasMetricsProperties.Logging newLogging = new RagasMetricsProperties.Logging();
            newLogging.setEnabled(false);
            newLogging.setLevel(RagasMetricsProperties.Logging.Level.NORMAL);
            properties.setLogging(newLogging);

            assertThat(properties.getLogging().isEnabled()).isFalse();
            assertThat(properties.getLogging().getLevel()).isEqualTo(RagasMetricsProperties.Logging.Level.NORMAL);
        }
    }

    @Nested
    @DisplayName("Logging Level Enum")
    class LoggingLevelEnum {

        @Test
        @DisplayName("Should have three levels")
        void shouldHaveThreeLevels() {
            assertThat(RagasMetricsProperties.Logging.Level.values()).hasSize(3);
        }

        @Test
        @DisplayName("Should have MINIMAL level")
        void shouldHaveMinimalLevel() {
            assertThat(RagasMetricsProperties.Logging.Level.MINIMAL).isNotNull();
            assertThat(RagasMetricsProperties.Logging.Level.MINIMAL.name()).isEqualTo("MINIMAL");
        }

        @Test
        @DisplayName("Should have NORMAL level")
        void shouldHaveNormalLevel() {
            assertThat(RagasMetricsProperties.Logging.Level.NORMAL).isNotNull();
            assertThat(RagasMetricsProperties.Logging.Level.NORMAL.name()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("Should have VERBOSE level")
        void shouldHaveVerboseLevel() {
            assertThat(RagasMetricsProperties.Logging.Level.VERBOSE).isNotNull();
            assertThat(RagasMetricsProperties.Logging.Level.VERBOSE.name()).isEqualTo("VERBOSE");
        }

        @Test
        @DisplayName("Should parse level from string")
        void shouldParseLevelFromString() {
            assertThat(RagasMetricsProperties.Logging.Level.valueOf("MINIMAL"))
                    .isEqualTo(RagasMetricsProperties.Logging.Level.MINIMAL);
            assertThat(RagasMetricsProperties.Logging.Level.valueOf("NORMAL"))
                    .isEqualTo(RagasMetricsProperties.Logging.Level.NORMAL);
            assertThat(RagasMetricsProperties.Logging.Level.valueOf("VERBOSE"))
                    .isEqualTo(RagasMetricsProperties.Logging.Level.VERBOSE);
        }
    }

    @Nested
    @DisplayName("Configuration Properties Binding")
    @SpringBootTest(classes = RagasMetricsPropertiesTest.TestConfig.class)
    @TestPropertySource(
            properties = {
                "spring.ai.ragas.metrics.enabled=true",
                "spring.ai.ragas.metrics.logging.enabled=true",
                "spring.ai.ragas.metrics.logging.level=NORMAL",
                "spring.ai.ragas.metrics.logging.chart-width=120",
                "spring.ai.ragas.metrics.logging.chart-height=15"
            })
    class ConfigurationPropertiesBinding {

        @Autowired
        private RagasMetricsProperties properties;

        @Test
        @DisplayName("Should bind enabled property")
        void shouldBindEnabledProperty() {
            assertThat(properties.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should bind logging.enabled property")
        void shouldBindLoggingEnabled() {
            assertThat(properties.getLogging().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should bind logging.level property")
        void shouldBindLoggingLevel() {
            assertThat(properties.getLogging().getLevel()).isEqualTo(RagasMetricsProperties.Logging.Level.NORMAL);
        }

        @Test
        @DisplayName("Should bind logging.chart-width property")
        void shouldBindChartWidth() {
            assertThat(properties.getLogging().getChartWidth()).isEqualTo(120);
        }

        @Test
        @DisplayName("Should bind logging.chart-height property")
        void shouldBindChartHeight() {
            assertThat(properties.getLogging().getChartHeight()).isEqualTo(15);
        }
    }

    @EnableConfigurationProperties(RagasMetricsProperties.class)
    static class TestConfig {}
}
