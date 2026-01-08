package ai.qa.solutions.metric;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Metric Interface Tests")
class MetricTest {

    @Nested
    @DisplayName("Default getName() Method")
    class GetNameTests {

        @Test
        @DisplayName("Should return simple class name by default")
        void shouldReturnSimpleClassName() {
            // Given
            Metric<TestConfig> metric = new TestMetric();

            // When
            String name = metric.getName();

            // Then
            assertThat(name).isEqualTo("TestMetric");
        }

        @Test
        @DisplayName("Should return empty string for anonymous implementations")
        void shouldReturnEmptyStringForAnonymous() {
            // Given
            Metric<TestConfig> metric = new Metric<>() {
                @Override
                public Double singleTurnScore(TestConfig config, Sample sample) {
                    return 0.0;
                }

                @Override
                public CompletableFuture<Double> singleTurnScoreAsync(TestConfig config, Sample sample) {
                    return CompletableFuture.completedFuture(0.0);
                }
            };

            // When
            String name = metric.getName();

            // Then - getSimpleName() returns empty string for anonymous classes
            assertThat(name).isEmpty();
        }
    }

    @Nested
    @DisplayName("MetricConfiguration Interface")
    class MetricConfigurationTests {

        @Test
        @DisplayName("Should allow empty implementation")
        void shouldAllowEmptyImplementation() {
            // Given/When
            Metric.MetricConfiguration config = new Metric.MetricConfiguration() {};

            // Then
            assertThat(config).isNotNull();
        }
    }

    // Test implementations

    static class TestConfig implements Metric.MetricConfiguration {}

    static class TestMetric implements Metric<TestConfig> {
        @Override
        public Double singleTurnScore(TestConfig config, Sample sample) {
            return 1.0;
        }

        @Override
        public CompletableFuture<Double> singleTurnScoreAsync(TestConfig config, Sample sample) {
            return CompletableFuture.completedFuture(1.0);
        }
    }
}
