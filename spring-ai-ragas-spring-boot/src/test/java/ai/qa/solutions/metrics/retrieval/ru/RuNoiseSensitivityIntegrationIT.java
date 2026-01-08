package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.NoiseSensitivityMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Тесты метрики чувствительности к шуму - Русский язык (фиктивные данные)")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuNoiseSensitivityIntegrationIT.RuNoiseSensitivityTestConfiguration.class)
class RuNoiseSensitivityIntegrationIT {

    @Configuration
    public static class RuNoiseSensitivityTestConfiguration {}

    @Autowired
    private NoiseSensitivityMetric noiseSensitivityMetric;

    @Test
    @DisplayName("RELEVANT режим: Идеальный ответ - должен иметь 0 шума")
    void testRelevantMode_PerfectResponse() {
        log.info("=== Тестирование RELEVANT режима: Идеальный ответ ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .reference(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .retrievedContexts(List.of(
                        "Башня Зенит была возведена в 1889 году для Всемирной выставки.",
                        "Башня расположена в городе Новара, Гельвеция.",
                        "Маркус Чен был архитектором, который спроектировал башню."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Оценка: {}", score);
        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Идеальный ответ должен иметь 0 шума");
    }

    @Test
    @DisplayName("RELEVANT режим: Ошибка НЕ из релевантного контекста - должен иметь 0 шума")
    void testRelevantMode_ErrorNotFromRelevantContext() {
        log.info("=== Тестирование RELEVANT режима: Ошибка не из релевантного контекста ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Альдория, Конкордия. Она была спроектирована Маркусом Ченом.")
                .reference(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .retrievedContexts(List.of(
                        "Башня Зенит была возведена в 1889 году для Всемирной выставки.",
                        "Башня расположена в городе Новара, Гельвеция.",
                        "Маркус Чен был архитектором, который спроектировал башню."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Оценка: {}", score);
        assertNotNull(score);
        assertEquals(0.0, score, 0.15, "Ошибка (Альдория) НЕ поддерживается релевантным контекстом, поэтому шум = 0");
    }

    @Test
    @DisplayName("RELEVANT режим: Ошибка ИЗ релевантного контекста - должен иметь ВЫСОКИЙ шум")
    void testRelevantMode_ErrorFromRelevantContext() {
        log.info("=== Тестирование RELEVANT режима: Ошибка из релевантного контекста ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Конкордия. Она была спроектирована Маркусом Ченом.")
                .reference(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .retrievedContexts(List.of(
                        "Башня Зенит была возведена в 1889 году для Всемирной выставки.",
                        // РЕЛЕВАНТНЫЙ (поддерживает "построена в 1889") И вводящий в заблуждение (говорит Конкордия)
                        "Башня Зенит была построена в 1889 году и расположена в городе Новара, Конкордия.",
                        "Маркус Чен был архитектором, который спроектировал башню."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Оценка: {}", score);
        assertNotNull(score);
        assertTrue(
                score >= 0.2 && score <= 0.5,
                "Ошибка 'Конкордия' поддерживается релевантным контекстом, ожидается 0.2-0.5, получено: " + score);
    }

    @Test
    @DisplayName("IRRELEVANT режим: Ошибка из нерелевантного контекста - должен иметь ВЫСОКИЙ шум")
    void testIrrelevantMode_ErrorFromIrrelevantContext() {
        log.info("=== Тестирование IRRELEVANT режима: Ошибка из нерелевантного контекста ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Конкордия. Она была спроектирована Маркусом Ченом.")
                .reference(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .retrievedContexts(List.of(
                        "Башня Зенит была возведена в 1889 году для Всемирной выставки.",
                        // НЕРЕЛЕВАНТНЫЙ (не поддерживает факты из reference), но вводящий в заблуждение
                        "Башня расположена в Новаре, столице Конкордии.",
                        "Маркус Чен был архитектором, который спроектировал башню."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Оценка: {}", score);
        assertNotNull(score);
        assertTrue(
                score >= 0.0 && score <= 0.5,
                "Ошибка из нерелевантного вводящего в заблуждение контекста, ожидается 0.0-0.5, получено: " + score);
    }

    @Test
    @DisplayName("IRRELEVANT режим: Идеальный ответ игнорирует нерелевантные контексты - должен иметь 0 шума")
    void testIrrelevantMode_IgnoresIrrelevantContexts() {
        log.info("=== Тестирование IRRELEVANT режима: Игнорирует нерелевантные контексты ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .reference(
                        "Башня Зенит была построена в 1889 году. Она расположена в городе Новара, Гельвеция. Она была спроектирована Маркусом Ченом.")
                .retrievedContexts(List.of(
                        "Башня Зенит была возведена в 1889 году для Всемирной выставки.",
                        "Башня расположена в Новаре, столице Конкордии.", // Нерелевантный + неверный
                        "Конкордианская архитектура известна своей точностью.", // Нерелевантный
                        "Маркус Чен был архитектором, который спроектировал башню."))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.IRRELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);

        log.info("Оценка: {}", score);
        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Ответ не использует нерелевантные контексты, поэтому шум = 0");
    }

    @Test
    @DisplayName("Граничный случай: Нет контекстов - должен вернуть 0")
    void testNoContexts() {
        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response("Башня Зенит была построена в 1889 году.")
                .reference("Башня Зенит была построена в 1889 году.")
                .retrievedContexts(List.of())
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);
        assertEquals(0.0, score);
    }

    @Test
    @DisplayName("Граничный случай: Пустой ответ - должен вернуть 0")
    void testEmptyResponse() {
        Sample sample = Sample.builder()
                .userInput("Расскажите о башне Зенит.")
                .response("")
                .reference("Башня Зенит была построена в 1889 году.")
                .retrievedContexts(List.of("Какой-то контекст"))
                .build();

        NoiseSensitivityMetric.NoiseSensitivityConfig config = NoiseSensitivityMetric.NoiseSensitivityConfig.builder()
                .mode(NoiseSensitivityMetric.NoiseSensitivityMode.RELEVANT)
                .build();

        Double score = noiseSensitivityMetric.singleTurnScore(config, sample);
        assertEquals(0.0, score);
    }
}
