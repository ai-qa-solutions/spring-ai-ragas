package ai.qa.solutions.allure.nlp;

import ai.qa.solutions.allure.explanation.BleuScoreExplanation;
import ai.qa.solutions.allure.explanation.ChrfScoreExplanation;
import ai.qa.solutions.allure.explanation.RougeScoreExplanation;
import ai.qa.solutions.allure.explanation.ScoreExplanation;
import ai.qa.solutions.allure.explanation.StringSimilarityExplanation;
import ai.qa.solutions.allure.listener.AllureAttachmentWriter;
import ai.qa.solutions.allure.methodology.MethodologyLoader;
import ai.qa.solutions.allure.model.EvaluationReportData;
import ai.qa.solutions.allure.template.FreemarkerTemplateEngine;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for generating Allure attachments for NLP (non-LLM) metrics.
 * <p>
 * NLP metrics don't use the MultiModelExecutor and AllureMetricExecutionListener,
 * so this helper provides a way to generate Allure reports for them.
 *
 * <p>Example usage in tests:
 * <pre>{@code
 * @Test
 * void testBleuScore() {
 *     Sample sample = Sample.builder()
 *         .response("The quick brown fox")
 *         .reference("The quick brown fox")
 *         .build();
 *     BleuScoreConfig config = BleuScoreConfig.builder().build();
 *     Double score = bleuScoreMetric.singleTurnScore(config, sample);
 *
 *     AllureNlpMetricHelper.attachBleuScore(score, sample.getResponse(),
 *         sample.getReference(), config.getMaxNgram(), config.isSmoothing(), "en");
 * }
 * }</pre>
 */
@Slf4j
public final class AllureNlpMetricHelper {

    private static final FreemarkerTemplateEngine TEMPLATE_ENGINE = new FreemarkerTemplateEngine();
    private static final AllureAttachmentWriter ATTACHMENT_WRITER =
            new AllureAttachmentWriter(Allure.getLifecycle(), TEMPLATE_ENGINE);

    private AllureNlpMetricHelper() {
        // utility class
    }

    /**
     * Attaches BLEU score explanation to Allure report.
     *
     * @param score     the calculated score
     * @param response  the response text
     * @param reference the reference text
     * @param maxNgram  maximum n-gram order
     * @param smoothing whether smoothing was used
     * @param language  the language code ("en" or "ru")
     */
    public static void attachBleuScore(
            final Double score,
            final String response,
            final String reference,
            final int maxNgram,
            final boolean smoothing,
            final String language) {
        final BleuScoreExplanation explanation = BleuScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .maxNgram(maxNgram)
                .smoothing(smoothing)
                .build();

        attachReport("BLEU Score", "bleu-score", explanation, score, response, reference, language);
    }

    /**
     * Attaches ROUGE score explanation to Allure report.
     *
     * @param score     the calculated score
     * @param response  the response text
     * @param reference the reference text
     * @param rougeType the ROUGE type (ROUGE_1, ROUGE_2, ROUGE_L)
     * @param mode      the scoring mode (PRECISION, RECALL, FMEASURE)
     * @param language  the language code ("en" or "ru")
     */
    public static void attachRougeScore(
            final Double score,
            final String response,
            final String reference,
            final String rougeType,
            final String mode,
            final String language) {
        final RougeScoreExplanation explanation = RougeScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .rougeType(rougeType)
                .mode(mode)
                .build();

        attachReport("ROUGE Score", "rouge-score", explanation, score, response, reference, language);
    }

    /**
     * Attaches chrF score explanation to Allure report.
     *
     * @param score          the calculated score
     * @param response       the response text
     * @param reference      the reference text
     * @param charNgramOrder character n-gram order
     * @param wordNgramOrder word n-gram order (0 for chrF, >0 for chrF++)
     * @param beta           beta parameter
     * @param language       the language code ("en" or "ru")
     */
    public static void attachChrfScore(
            final Double score,
            final String response,
            final String reference,
            final int charNgramOrder,
            final int wordNgramOrder,
            final double beta,
            final String language) {
        final String variant = wordNgramOrder > 0 ? "chrF++" : "chrF";
        final ChrfScoreExplanation explanation = ChrfScoreExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .charNgramOrder(charNgramOrder)
                .wordNgramOrder(wordNgramOrder)
                .beta(beta)
                .build();

        attachReport(variant + " Score", "chrf-score", explanation, score, response, reference, language);
    }

    /**
     * Attaches String Similarity explanation to Allure report.
     *
     * @param score           the calculated score
     * @param response        the response text
     * @param reference       the reference text
     * @param distanceMeasure the distance measure algorithm
     * @param caseSensitive   whether comparison is case sensitive
     * @param language        the language code ("en" or "ru")
     */
    public static void attachStringSimilarity(
            final Double score,
            final String response,
            final String reference,
            final String distanceMeasure,
            final boolean caseSensitive,
            final String language) {
        final StringSimilarityExplanation explanation = StringSimilarityExplanation.builder()
                .score(score)
                .language(language)
                .response(response)
                .reference(reference)
                .distanceMeasure(distanceMeasure)
                .caseSensitive(caseSensitive)
                .build();

        attachReport("String Similarity", "string-similarity", explanation, score, response, reference, language);
    }

    private static void attachReport(
            final String metricName,
            final String metricType,
            final ScoreExplanation explanation,
            final Double score,
            final String response,
            final String reference,
            final String language) {
        try {
            final MethodologyLoader methodologyLoader = new MethodologyLoader(language);
            final String methodologyMarkdown = methodologyLoader.loadMethodologyMarkdown(metricType);
            final String methodologyHtml = methodologyLoader.loadMethodologyHtml(metricType);

            final EvaluationReportData reportData = EvaluationReportData.builder()
                    .metricName(metricName)
                    .metricDescription(explanation.getSimpleDescription())
                    .response(response)
                    .reference(reference)
                    .aggregatedScore(score)
                    .scoreExplanation(explanation)
                    .methodologyMarkdown(methodologyMarkdown)
                    .methodologyHtml(methodologyHtml)
                    .language(language)
                    .build();

            ATTACHMENT_WRITER.writeHtmlAttachment(reportData);
            log.debug("Attached {} HTML Allure report", metricName);
        } catch (final Exception e) {
            log.error("Failed to attach {} Allure report", metricName, e);
        }
    }
}
