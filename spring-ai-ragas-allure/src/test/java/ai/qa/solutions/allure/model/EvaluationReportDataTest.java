package ai.qa.solutions.allure.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EvaluationReportData")
class EvaluationReportDataTest {

    @Nested
    @DisplayName("getFormattedScore")
    class GetFormattedScore {

        @Test
        @DisplayName("should format score as percentage")
        void shouldFormatScoreAsPercentage() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .aggregatedScore(0.8567)
                    .build();

            assertThat(data.getFormattedScore()).isEqualTo("85.67%");
        }

        @Test
        @DisplayName("should return N/A when score is null")
        void shouldReturnNaWhenNull() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getFormattedScore()).isEqualTo("N/A");
        }
    }

    @Nested
    @DisplayName("getScoreClass")
    class GetScoreClass {

        @Test
        @DisplayName("should return excellent for high scores")
        void shouldReturnExcellent() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .aggregatedScore(0.85)
                    .build();

            assertThat(data.getScoreClass()).isEqualTo("excellent");
        }

        @Test
        @DisplayName("should return good for scores 0.6-0.8")
        void shouldReturnGood() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .aggregatedScore(0.7)
                    .build();

            assertThat(data.getScoreClass()).isEqualTo("good");
        }

        @Test
        @DisplayName("should return moderate for scores 0.4-0.6")
        void shouldReturnModerate() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .aggregatedScore(0.5)
                    .build();

            assertThat(data.getScoreClass()).isEqualTo("moderate");
        }

        @Test
        @DisplayName("should return poor for low scores")
        void shouldReturnPoor() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .aggregatedScore(0.2)
                    .build();

            assertThat(data.getScoreClass()).isEqualTo("poor");
        }

        @Test
        @DisplayName("should return unknown when score is null")
        void shouldReturnUnknown() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getScoreClass()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("getTotalDurationMs")
    class GetTotalDurationMs {

        @Test
        @DisplayName("should return duration in milliseconds")
        void shouldReturnDurationMs() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .totalDuration(Duration.ofSeconds(3))
                    .build();

            assertThat(data.getTotalDurationMs()).isEqualTo(3000);
        }

        @Test
        @DisplayName("should return 0 when duration is null")
        void shouldReturnZeroWhenNull() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getTotalDurationMs()).isZero();
        }
    }

    @Nested
    @DisplayName("hasExclusions")
    class HasExclusions {

        @Test
        @DisplayName("should return true when exclusions present")
        void shouldReturnTrueWhenPresent() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .exclusions(List.of(ModelExclusionData.builder()
                            .modelId("model-1")
                            .errorMessage("Failed")
                            .build()))
                    .build();

            assertThat(data.hasExclusions()).isTrue();
        }

        @Test
        @DisplayName("should return false when no exclusions")
        void shouldReturnFalseWhenEmpty() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.hasExclusions()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasEmbeddingModels")
    class HasEmbeddingModels {

        @Test
        @DisplayName("should return true when embedding models present")
        void shouldReturnTrueWhenPresent() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .embeddingModelIds(List.of("embedding-1"))
                    .build();

            assertThat(data.hasEmbeddingModels()).isTrue();
        }

        @Test
        @DisplayName("should return false when no embedding models")
        void shouldReturnFalseWhenEmpty() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.hasEmbeddingModels()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasContexts")
    class HasContexts {

        @Test
        @DisplayName("should return true when contexts present")
        void shouldReturnTrueWhenPresent() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .retrievedContexts(List.of("context 1", "context 2"))
                    .build();

            assertThat(data.hasContexts()).isTrue();
        }

        @Test
        @DisplayName("should return false when no contexts")
        void shouldReturnFalseWhenEmpty() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.hasContexts()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasReference")
    class HasReference {

        @Test
        @DisplayName("should return true when reference present")
        void shouldReturnTrueWhenPresent() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .reference("The expected answer")
                    .build();

            assertThat(data.hasReference()).isTrue();
        }

        @Test
        @DisplayName("should return false when reference is null")
        void shouldReturnFalseWhenNull() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.hasReference()).isFalse();
        }

        @Test
        @DisplayName("should return false when reference is blank")
        void shouldReturnFalseWhenBlank() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .reference("   ")
                    .build();

            assertThat(data.hasReference()).isFalse();
        }
    }

    @Nested
    @DisplayName("getSuccessfulModelCount")
    class GetSuccessfulModelCount {

        @Test
        @DisplayName("should return count of model scores")
        void shouldReturnCount() {
            final EvaluationReportData data = EvaluationReportData.builder()
                    .metricName("Test")
                    .modelScores(Map.of("model-1", 0.8, "model-2", 0.7, "model-3", 0.9))
                    .build();

            assertThat(data.getSuccessfulModelCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return 0 when no scores")
        void shouldReturnZeroWhenEmpty() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getSuccessfulModelCount()).isZero();
        }
    }

    @Nested
    @DisplayName("configToJson")
    class ConfigToJson {

        @Test
        @DisplayName("should convert config to JSON")
        void shouldConvertToJson() {
            final TestConfig config = new TestConfig("value1", 42);

            final String json = EvaluationReportData.configToJson(config);

            assertThat(json).contains("\"field1\"");
            assertThat(json).contains("\"value1\"");
            assertThat(json).contains("\"field2\"");
            assertThat(json).contains("42");
        }

        @Test
        @DisplayName("should return null string for null config")
        void shouldReturnNullForNullConfig() {
            final String json = EvaluationReportData.configToJson(null);

            assertThat(json).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("should have default empty lists")
        void shouldHaveDefaultEmptyLists() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getRetrievedContexts()).isEmpty();
            assertThat(data.getModelIds()).isEmpty();
            assertThat(data.getEmbeddingModelIds()).isEmpty();
            assertThat(data.getExcludedModels()).isEmpty();
            assertThat(data.getSteps()).isEmpty();
            assertThat(data.getExclusions()).isEmpty();
        }

        @Test
        @DisplayName("should have default empty map for modelScores")
        void shouldHaveDefaultEmptyMap() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getModelScores()).isEmpty();
        }

        @Test
        @DisplayName("should have default language en")
        void shouldHaveDefaultLanguage() {
            final EvaluationReportData data =
                    EvaluationReportData.builder().metricName("Test").build();

            assertThat(data.getLanguage()).isEqualTo("en");
        }
    }

    record TestConfig(String field1, int field2) {}
}
