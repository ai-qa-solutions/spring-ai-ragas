package ai.qa.solutions.metrics.agent.en;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.agent.ToolCallAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for Tool Call Accuracy Metric - English Language.
 * <p>
 * Tests the evaluation of agent's tool calls against expected reference tool calls.
 * This metric is computation-based (no LLM calls) and calculates F1 score.
 * <p>
 * Key characteristics:
 * - STRICT mode: requires exact match of tool names and arguments
 * - FLEXIBLE mode: allows partial argument matching with configurable threshold
 * - Returns F1 score (harmonic mean of precision and recall)
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Tool Call Accuracy Metric - English Language Validation")
@SpringBootTest(classes = EnToolCallAccuracyIntegrationIT.ToolCallAccuracyIntegrationTestConfiguration.class)
class EnToolCallAccuracyIntegrationIT {

    @Configuration
    public static class ToolCallAccuracyIntegrationTestConfiguration {}

    @Autowired
    private ToolCallAccuracyMetric toolCallAccuracyMetric;

    @Nested
    @DisplayName("STRICT Mode Tests")
    class StrictModeTests {

        @Test
        @DisplayName("Perfect match - all tool calls correct - EXPECTED SCORE 1.0")
        void testPerfectMatch() {
            log.info("=== Perfect Tool Call Match Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall(
                                    "search_flights", Map.of("from", "NYC", "to", "LAX", "date", "2024-01-15")),
                            new Sample.ToolCall("book_flight", Map.of("flight_id", "UA123", "passengers", 1))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall(
                                    "search_flights", Map.of("from", "NYC", "to", "LAX", "date", "2024-01-15")),
                            new Sample.ToolCall("book_flight", Map.of("flight_id", "UA123", "passengers", 1))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Tool calls match exactly");
            log.info("Score: {}", score);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Complete mismatch - no correct calls - EXPECTED SCORE 0.0")
        void testCompleteMismatch() {
            log.info("=== Complete Tool Call Mismatch Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("get_weather", Map.of("city", "NYC")),
                            new Sample.ToolCall("send_email", Map.of("to", "test@example.com"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search_flights", Map.of("from", "NYC", "to", "LAX")),
                            new Sample.ToolCall("book_flight", Map.of("flight_id", "UA123"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("No matching tool calls");
            log.info("Score: {}", score);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Partial match - some correct calls - EXPECTED MODERATE SCORE")
        void testPartialMatch() {
            log.info("=== Partial Tool Call Match Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("search_flights", Map.of("from", "NYC", "to", "LAX")),
                            new Sample.ToolCall("get_weather", Map.of("city", "LAX")),
                            new Sample.ToolCall("book_flight", Map.of("flight_id", "UA123"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("search_flights", Map.of("from", "NYC", "to", "LAX")),
                            new Sample.ToolCall("book_flight", Map.of("flight_id", "UA123"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("2 out of 3 actual calls match 2 out of 2 reference calls");
            log.info("Score: {}", score);

            // Precision: 2/3, Recall: 2/2=1, F1 = 2*(2/3)*1/(2/3+1) = 0.8
            assertThat(score).isBetween(0.7, 0.9);
        }

        @Test
        @DisplayName("Same tool name but different arguments - EXPECTED SCORE 0.0")
        void testSameToolDifferentArgs() {
            log.info("=== Same Tool Different Arguments Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search_flights", Map.of("from", "NYC", "to", "SFO"))))
                    .referenceToolCalls(
                            List.of(new Sample.ToolCall("search_flights", Map.of("from", "NYC", "to", "LAX"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Tool names match but arguments differ");
            log.info("Score: {}", score);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("FLEXIBLE Mode Tests")
    class FlexibleModeTests {

        @Test
        @DisplayName("Partial argument match above threshold - EXPECTED MATCH")
        void testPartialArgumentMatchAboveThreshold() {
            log.info("=== Partial Argument Match Above Threshold Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall(
                            "search_flights", Map.of("from", "NYC", "to", "LAX", "date", "2024-01-15"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall(
                            "search_flights",
                            Map.of("from", "NYC", "to", "LAX", "date", "2024-01-15", "class", "economy"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.5)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("3 out of 4 reference args match (75%)");
            log.info("Score: {}", score);

            // 3/4 = 0.75 >= 0.5 threshold, so it matches
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Partial argument match below threshold - EXPECTED NO MATCH")
        void testPartialArgumentMatchBelowThreshold() {
            log.info("=== Partial Argument Match Below Threshold Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("search_flights", Map.of("from", "NYC"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall(
                            "search_flights",
                            Map.of("from", "NYC", "to", "LAX", "date", "2024-01-15", "class", "economy"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.8)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("1 out of 4 reference args match (25%), threshold is 80%");
            log.info("Score: {}", score);

            // 1/4 = 0.25 < 0.8 threshold, so no match
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Multiple tools with partial matches")
        void testMultipleToolsPartialMatches() {
            log.info("=== Multiple Tools with Partial Matches Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("search_hotels", Map.of("city", "NYC", "checkin", "2024-01-15")),
                            new Sample.ToolCall("book_hotel", Map.of("hotel_id", "H123", "rooms", 1))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall(
                                    "search_hotels",
                                    Map.of("city", "NYC", "checkin", "2024-01-15", "checkout", "2024-01-17")),
                            new Sample.ToolCall("book_hotel", Map.of("hotel_id", "H123", "rooms", 1, "guests", 2))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.5)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Both calls match above 50% threshold");
            log.info("Score: {}", score);

            // search_hotels: 2/3 = 0.67 >= 0.5, book_hotel: 2/3 = 0.67 >= 0.5
            // Both match, so F1 = 1.0
            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Empty arguments in both - EXPECTED MATCH")
        void testEmptyArgumentsBoth() {
            log.info("=== Empty Arguments in Both Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("get_time", Map.of())))
                    .referenceToolCalls(List.of(new Sample.ToolCall("get_time", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Both have empty arguments");
            log.info("Score: {}", score);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Null arguments treated as empty")
        void testNullArgumentsTreatedAsEmpty() {
            log.info("=== Null Arguments Treated As Empty Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("get_time", null)))
                    .referenceToolCalls(List.of(new Sample.ToolCall("get_time", null)))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Both have null arguments (treated as empty)");
            log.info("Score: {}", score);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Extra actual calls reduces precision")
        void testExtraActualCallsReducesPrecision() {
            log.info("=== Extra Actual Calls Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("tool_a", Map.of()),
                            new Sample.ToolCall("tool_b", Map.of()),
                            new Sample.ToolCall("tool_c", Map.of()),
                            new Sample.ToolCall("tool_d", Map.of())))
                    .referenceToolCalls(List.of(new Sample.ToolCall("tool_a", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("1 match out of 4 actual calls");
            log.info("Score: {}", score);

            // Precision: 1/4 = 0.25, Recall: 1/1 = 1.0, F1 = 2*0.25*1/(0.25+1) = 0.4
            assertThat(score).isBetween(0.3, 0.5);
        }

        @Test
        @DisplayName("Missing actual calls reduces recall")
        void testMissingActualCallsReducesRecall() {
            log.info("=== Missing Actual Calls Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("tool_a", Map.of())))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("tool_a", Map.of()),
                            new Sample.ToolCall("tool_b", Map.of()),
                            new Sample.ToolCall("tool_c", Map.of()),
                            new Sample.ToolCall("tool_d", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("1 match out of 4 reference calls");
            log.info("Score: {}", score);

            // Precision: 1/1 = 1.0, Recall: 1/4 = 0.25, F1 = 2*1*0.25/(1+0.25) = 0.4
            assertThat(score).isBetween(0.3, 0.5);
        }

        @Test
        @DisplayName("Async scoring works correctly")
        void testAsyncScoring() {
            log.info("=== Async Scoring Test ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("test_tool", Map.of("key", "value"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("test_tool", Map.of("key", "value"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score =
                    toolCallAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Async Score: {}", score);

            assertThat(score).isEqualTo(1.0);
        }
    }
}
