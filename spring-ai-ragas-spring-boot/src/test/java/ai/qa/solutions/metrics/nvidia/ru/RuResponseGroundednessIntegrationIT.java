package ai.qa.solutions.metrics.nvidia.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.nvidia.ResponseGroundednessMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * Интеграционные тесты метрики Response Groundedness (NVIDIA-стиль) - Русский язык.
 * <p>
 * Тестируют оценку обоснованности ответа извлечёнными контекстами.
 * <p>
 * Ключевые характеристики:
 * - Шкала оценки 0-2, нормализованная к 0-1
 * - 0: Необоснован, 1: Частично обоснован, 2: Полностью обоснован
 * - Поддержка эвристических сокращений для точных совпадений
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Response Groundedness - Валидация на русском языке")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuResponseGroundednessIntegrationIT.ResponseGroundednessIntegrationTestConfiguration.class)
class RuResponseGroundednessIntegrationIT {

    @Configuration
    public static class ResponseGroundednessIntegrationTestConfiguration {}

    @Autowired
    private ResponseGroundednessMetric responseGroundednessMetric;

    @Nested
    @DisplayName("Тесты оценки обоснованности")
    class GroundednessEvaluationTests {

        @Test
        @DisplayName("Полностью обоснованный ответ - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testFullyGroundedResponse() {
            log.info("=== Тест: полностью обоснованный ответ ===");

            final Sample sample = Sample.builder()
                    .response("Москва — столица России.")
                    .retrievedContexts(List.of(
                            "Россия — крупнейшая страна мира. Москва является столицей и крупнейшим городом России. "
                                    + "Население страны составляет около 146 миллионов человек."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Ответ: Москва — столица России.");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.7, "Полностью обоснованный ответ должен дать высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Частично обоснованный ответ - ОЖИДАЕТСЯ УМЕРЕННЫЙ БАЛЛ")
        void testPartiallyGroundedResponse() {
            log.info("=== Тест: частично обоснованный ответ ===");

            final Sample sample = Sample.builder()
                    .response("Москва — красивая столица России с потрясающими ресторанами и романтичной атмосферой.")
                    .retrievedContexts(List.of("Москва является столицей России. Она расположена на реке Москва."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Ответ: Москва — красивая столица России с потрясающими ресторанами...");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.2 && score <= 0.8,
                    "Частично обоснованный ответ должен дать умеренный балл. Получено: " + score);
        }

        @Test
        @DisplayName("Необоснованный ответ - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
        void testUngroundedResponse() {
            log.info("=== Тест: необоснованный ответ ===");

            final Sample sample = Sample.builder()
                    .response("Берлин — столица Германии с населением 3,6 миллиона человек.")
                    .retrievedContexts(List.of("Москва является столицей России. Она известна Кремлём."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Ответ: Берлин — столица Германии...");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.4, "Необоснованный ответ должен дать низкий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты эвристических сокращений")
    class HeuristicShortcutsTests {

        @Test
        @DisplayName("Точное совпадение ответа - ОЖИДАЕТСЯ ИДЕАЛЬНЫЙ БАЛЛ")
        void testExactMatchResponse() {
            log.info("=== Тест: точное совпадение ответа ===");

            final Sample sample = Sample.builder()
                    .response("Москва — столица России")
                    .retrievedContexts(List.of("Москва — столица России"))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(true)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Ответ точно совпадает с контекстом");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score == 1.0, "Точное совпадение должно вернуть 1.0. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Крайние случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Технический ответ с техническим контекстом")
        void testTechnicalContent() {
            log.info("=== Тест: технический контент ===");

            final Sample sample = Sample.builder()
                    .response(
                            "TCP использует трёхстороннее рукопожатие: SYN, SYN-ACK, ACK для установления соединения.")
                    .retrievedContexts(List.of("Трёхстороннее рукопожатие TCP — это метод установления соединения. "
                            + "Оно включает три шага: сначала клиент отправляет пакет SYN, "
                            + "затем сервер отвечает SYN-ACK, и наконец клиент отправляет ACK."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric.singleTurnScore(config, sample);

            log.info("Ответ: TCP использует трёхстороннее рукопожатие...");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.6,
                    "Технический ответ, обоснованный контекстом, должен дать хороший балл. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронная оценка работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: асинхронная оценка ===");

            final Sample sample = Sample.builder()
                    .response("Кремль находится в Москве")
                    .retrievedContexts(List.of("Кремль — это известная достопримечательность, расположенная в Москве."))
                    .build();

            final ResponseGroundednessMetric.ResponseGroundednessConfig config =
                    ResponseGroundednessMetric.ResponseGroundednessConfig.builder()
                            .useHeuristicShortcuts(false)
                            .build();

            final Double score = responseGroundednessMetric
                    .singleTurnScoreAsync(config, sample)
                    .join();

            log.info("Асинхронная оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.6, "Асинхронная оценка должна работать корректно. Получено: " + score);
        }
    }
}
