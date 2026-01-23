package ai.qa.solutions.metrics.agent.ru;

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
 * Интеграционные тесты для метрики Tool Call Accuracy - Русский язык.
 * <p>
 * Тестирует оценку вызовов инструментов агента против ожидаемых эталонных вызовов.
 * Эта метрика основана на вычислениях (без вызовов LLM) и рассчитывает F1-оценку.
 * <p>
 * Ключевые характеристики:
 * - Режим STRICT: требует точного совпадения имён инструментов и аргументов
 * - Режим FLEXIBLE: допускает частичное совпадение аргументов с настраиваемым порогом
 * - Возвращает F1-оценку (гармоническое среднее precision и recall)
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Tool Call Accuracy - Валидация на русском языке")
@SpringBootTest(classes = RuToolCallAccuracyIntegrationIT.ToolCallAccuracyIntegrationTestConfiguration.class)
class RuToolCallAccuracyIntegrationIT {

    @Configuration
    public static class ToolCallAccuracyIntegrationTestConfiguration {}

    @Autowired
    private ToolCallAccuracyMetric toolCallAccuracyMetric;

    @Nested
    @DisplayName("Тесты режима STRICT")
    class StrictModeTests {

        @Test
        @DisplayName("Полное совпадение - все вызовы инструментов корректны - ОЖИДАЕМАЯ ОЦЕНКА 1.0")
        void testPerfectMatch() {
            log.info("=== Тест полного совпадения вызовов инструментов ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall(
                                    "поиск_рейсов",
                                    Map.of("откуда", "Москва", "куда", "Петербург", "дата", "2024-01-15")),
                            new Sample.ToolCall("бронирование_рейса", Map.of("номер_рейса", "СВ123", "пассажиров", 1))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall(
                                    "поиск_рейсов",
                                    Map.of("откуда", "Москва", "куда", "Петербург", "дата", "2024-01-15")),
                            new Sample.ToolCall("бронирование_рейса", Map.of("номер_рейса", "СВ123", "пассажиров", 1))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Вызовы инструментов совпадают полностью");
            log.info("Оценка: {}", score);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Полное несовпадение - нет корректных вызовов - ОЖИДАЕМАЯ ОЦЕНКА 0.0")
        void testCompleteMismatch() {
            log.info("=== Тест полного несовпадения вызовов инструментов ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("получить_погоду", Map.of("город", "Москва")),
                            new Sample.ToolCall("отправить_письмо", Map.of("кому", "test@example.com"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("поиск_рейсов", Map.of("откуда", "Москва", "куда", "Петербург")),
                            new Sample.ToolCall("бронирование_рейса", Map.of("номер_рейса", "СВ123"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Нет совпадающих вызовов инструментов");
            log.info("Оценка: {}", score);

            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Частичное совпадение - некоторые вызовы корректны")
        void testPartialMatch() {
            log.info("=== Тест частичного совпадения вызовов инструментов ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("поиск_рейсов", Map.of("откуда", "Москва", "куда", "Петербург")),
                            new Sample.ToolCall("получить_погоду", Map.of("город", "Петербург")),
                            new Sample.ToolCall("бронирование_рейса", Map.of("номер_рейса", "СВ123"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("поиск_рейсов", Map.of("откуда", "Москва", "куда", "Петербург")),
                            new Sample.ToolCall("бронирование_рейса", Map.of("номер_рейса", "СВ123"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("2 из 3 фактических вызовов совпадают с 2 из 2 эталонных");
            log.info("Оценка: {}", score);

            // Precision: 2/3, Recall: 2/2=1, F1 = 2*(2/3)*1/(2/3+1) = 0.8
            assertThat(score).isBetween(0.7, 0.9);
        }

        @Test
        @DisplayName("Одинаковые имена инструментов, но разные аргументы - ОЖИДАЕМАЯ ОЦЕНКА 0.0")
        void testSameToolDifferentArgs() {
            log.info("=== Тест одинаковых инструментов с разными аргументами ===");

            final Sample sample = Sample.builder()
                    .toolCalls(
                            List.of(new Sample.ToolCall("поиск_рейсов", Map.of("откуда", "Москва", "куда", "Казань"))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("поиск_рейсов", Map.of("откуда", "Москва", "куда", "Петербург"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Имена инструментов совпадают, но аргументы отличаются");
            log.info("Оценка: {}", score);

            assertThat(score).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Тесты режима FLEXIBLE")
    class FlexibleModeTests {

        @Test
        @DisplayName("Частичное совпадение аргументов выше порога - ОЖИДАЕТСЯ СОВПАДЕНИЕ")
        void testPartialArgumentMatchAboveThreshold() {
            log.info("=== Тест частичного совпадения аргументов выше порога ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall(
                            "поиск_отелей", Map.of("город", "Москва", "дата_заезда", "2024-01-15"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall(
                            "поиск_отелей",
                            Map.of("город", "Москва", "дата_заезда", "2024-01-15", "дата_выезда", "2024-01-17"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.5)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("2 из 3 эталонных аргументов совпадают (67%)");
            log.info("Оценка: {}", score);

            // 2/3 = 0.67 >= 0.5 threshold, так что совпадает
            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Частичное совпадение аргументов ниже порога - НЕТ СОВПАДЕНИЯ")
        void testPartialArgumentMatchBelowThreshold() {
            log.info("=== Тест частичного совпадения аргументов ниже порога ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("поиск_отелей", Map.of("город", "Москва"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall(
                            "поиск_отелей",
                            Map.of(
                                    "город",
                                    "Москва",
                                    "дата_заезда",
                                    "2024-01-15",
                                    "дата_выезда",
                                    "2024-01-17",
                                    "класс",
                                    "люкс"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.8)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("1 из 4 эталонных аргументов совпадает (25%), порог 80%");
            log.info("Оценка: {}", score);

            // 1/4 = 0.25 < 0.8 threshold, нет совпадения
            assertThat(score).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Несколько инструментов с частичными совпадениями")
        void testMultipleToolsPartialMatches() {
            log.info("=== Тест нескольких инструментов с частичными совпадениями ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("поиск_отелей", Map.of("город", "Москва", "дата_заезда", "2024-01-15")),
                            new Sample.ToolCall("бронирование_отеля", Map.of("номер_отеля", "H123", "номеров", 1))))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall(
                                    "поиск_отелей",
                                    Map.of(
                                            "город",
                                            "Москва",
                                            "дата_заезда",
                                            "2024-01-15",
                                            "дата_выезда",
                                            "2024-01-17")),
                            new Sample.ToolCall(
                                    "бронирование_отеля", Map.of("номер_отеля", "H123", "номеров", 1, "гостей", 2))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.FLEXIBLE)
                            .argumentMatchThreshold(0.5)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("Оба вызова совпадают выше порога 50%");
            log.info("Оценка: {}", score);

            // поиск_отелей: 2/3 = 0.67 >= 0.5, бронирование_отеля: 2/3 = 0.67 >= 0.5
            // Оба совпадают, так что F1 = 1.0
            assertThat(score).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Пустые аргументы в обоих - ОЖИДАЕТСЯ СОВПАДЕНИЕ")
        void testEmptyArgumentsBoth() {
            log.info("=== Тест пустых аргументов в обоих ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("получить_время", Map.of())))
                    .referenceToolCalls(List.of(new Sample.ToolCall("получить_время", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("У обоих пустые аргументы");
            log.info("Оценка: {}", score);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Null аргументы обрабатываются как пустые")
        void testNullArgumentsTreatedAsEmpty() {
            log.info("=== Тест null аргументов как пустых ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("получить_время", null)))
                    .referenceToolCalls(List.of(new Sample.ToolCall("получить_время", null)))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("У обоих null аргументы (обрабатываются как пустые)");
            log.info("Оценка: {}", score);

            assertThat(score).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Лишние фактические вызовы снижают precision")
        void testExtraActualCallsReducesPrecision() {
            log.info("=== Тест лишних фактических вызовов ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(
                            new Sample.ToolCall("инструмент_а", Map.of()),
                            new Sample.ToolCall("инструмент_б", Map.of()),
                            new Sample.ToolCall("инструмент_в", Map.of()),
                            new Sample.ToolCall("инструмент_г", Map.of())))
                    .referenceToolCalls(List.of(new Sample.ToolCall("инструмент_а", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("1 совпадение из 4 фактических вызовов");
            log.info("Оценка: {}", score);

            // Precision: 1/4 = 0.25, Recall: 1/1 = 1.0, F1 = 2*0.25*1/(0.25+1) = 0.4
            assertThat(score).isBetween(0.3, 0.5);
        }

        @Test
        @DisplayName("Недостающие фактические вызовы снижают recall")
        void testMissingActualCallsReducesRecall() {
            log.info("=== Тест недостающих фактических вызовов ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("инструмент_а", Map.of())))
                    .referenceToolCalls(List.of(
                            new Sample.ToolCall("инструмент_а", Map.of()),
                            new Sample.ToolCall("инструмент_б", Map.of()),
                            new Sample.ToolCall("инструмент_в", Map.of()),
                            new Sample.ToolCall("инструмент_г", Map.of())))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score = toolCallAccuracyMetric.singleTurnScore(config, sample);

            log.info("1 совпадение из 4 эталонных вызовов");
            log.info("Оценка: {}", score);

            // Precision: 1/1 = 1.0, Recall: 1/4 = 0.25, F1 = 2*1*0.25/(1+0.25) = 0.4
            assertThat(score).isBetween(0.3, 0.5);
        }

        @Test
        @DisplayName("Асинхронная оценка работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест асинхронной оценки ===");

            final Sample sample = Sample.builder()
                    .toolCalls(List.of(new Sample.ToolCall("тестовый_инструмент", Map.of("ключ", "значение"))))
                    .referenceToolCalls(List.of(new Sample.ToolCall("тестовый_инструмент", Map.of("ключ", "значение"))))
                    .build();

            final ToolCallAccuracyMetric.ToolCallAccuracyConfig config =
                    ToolCallAccuracyMetric.ToolCallAccuracyConfig.builder()
                            .mode(ToolCallAccuracyMetric.Mode.STRICT)
                            .build();

            final Double score =
                    toolCallAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронная оценка: {}", score);

            assertThat(score).isEqualTo(1.0);
        }
    }
}
