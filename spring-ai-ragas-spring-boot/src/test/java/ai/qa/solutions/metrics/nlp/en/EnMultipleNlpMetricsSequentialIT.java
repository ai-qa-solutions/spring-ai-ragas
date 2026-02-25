package ai.qa.solutions.metrics.nlp.en;

import static org.assertj.core.api.Assertions.assertThat;

import ai.qa.solutions.metrics.nlp.BleuScoreMetric;
import ai.qa.solutions.metrics.nlp.BleuScoreMetric.BleuScoreConfig;
import ai.qa.solutions.metrics.nlp.ChrfScoreMetric;
import ai.qa.solutions.metrics.nlp.ChrfScoreMetric.ChrfScoreConfig;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric;
import ai.qa.solutions.metrics.nlp.RougeScoreMetric.RougeScoreConfig;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric;
import ai.qa.solutions.metrics.nlp.StringSimilarityMetric.StringSimilarityConfig;
import ai.qa.solutions.sample.Sample;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Validates that multiple NLP metrics evaluated sequentially in a single test method
 * each produce their own Allure step with attachments.
 * <p>
 * Regression test for GitHub issue #1: ThreadLocal stack corruption caused only the first
 * metric's Allure step to appear in reports.
 */
@Slf4j
@EnableAutoConfiguration
@Feature("Sequential Metrics Allure Report (Issue #1)")
@DisplayName("Multiple NLP Metrics - Sequential Evaluation in Single Test")
@SpringBootTest(classes = EnMultipleNlpMetricsSequentialIT.TestConfig.class)
class EnMultipleNlpMetricsSequentialIT {

    @Configuration
    public static class TestConfig {}

    @Autowired
    private BleuScoreMetric bleuScoreMetric;

    @Autowired
    private RougeScoreMetric rougeScoreMetric;

    @Autowired
    private ChrfScoreMetric chrfScoreMetric;

    @Autowired
    private StringSimilarityMetric stringSimilarityMetric;

    @Test
    @DisplayName("4 NLP metrics sequentially — all must appear in Allure report")
    @Description("Evaluates BLEU, ROUGE, chrF, and StringSimilarity sequentially. "
            + "Before the fix (issue #1), only the first metric's step was visible in Allure.")
    void allFourMetricsShouldProduceAllureSteps() {
        final Sample sample = Sample.builder()
                .response("The quick brown fox jumps over the lazy dog.")
                .reference("A fast brown fox leaps over a sleepy dog.")
                .build();

        // Metric 1: BLEU
        log.info("=== Metric 1: BLEU Score ===");
        final Double bleuScore =
                bleuScoreMetric.singleTurnScore(BleuScoreConfig.builder().build(), sample);
        log.info("BLEU Score: {}", String.format("%.4f", bleuScore));
        assertThat(bleuScore).isNotNull().isBetween(0.0, 1.0);

        // Metric 2: ROUGE-L
        log.info("=== Metric 2: ROUGE Score ===");
        final Double rougeScore =
                rougeScoreMetric.singleTurnScore(RougeScoreConfig.builder().build(), sample);
        log.info("ROUGE-L Score: {}", String.format("%.4f", rougeScore));
        assertThat(rougeScore).isNotNull().isBetween(0.0, 1.0);

        // Metric 3: chrF
        log.info("=== Metric 3: chrF Score ===");
        final Double chrfScore =
                chrfScoreMetric.singleTurnScore(ChrfScoreConfig.builder().build(), sample);
        log.info("chrF Score: {}", String.format("%.4f", chrfScore));
        assertThat(chrfScore).isNotNull().isBetween(0.0, 1.0);

        // Metric 4: String Similarity (Levenshtein)
        log.info("=== Metric 4: String Similarity ===");
        final Double similarityScore = stringSimilarityMetric.singleTurnScore(
                StringSimilarityConfig.builder().build(), sample);
        log.info("String Similarity Score: {}", String.format("%.4f", similarityScore));
        assertThat(similarityScore).isNotNull().isBetween(0.0, 1.0);

        log.info("=== All 4 metrics completed. Check Allure report for 4 separate steps. ===");
    }
}
