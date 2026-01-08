package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.retrieval.FaithfulnessMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
@DisplayName("Интеграционные тесты метрики Faithfulness - русскоязычные примеры")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuFaithfulnessMetricIntegrationIT.FaithfulnessMetricTestConfiguration.class)
class RuFaithfulnessMetricIntegrationIT {

    @Configuration
    public static class FaithfulnessMetricTestConfiguration {}

    @Autowired
    private FaithfulnessMetric faithfulnessMetric;

    // ==================== ПОЗИТИВНЫЕ ТЕСТЫ - ВЫСОКАЯ ТОЧНОСТЬ ====================

    @Test
    @DisplayName("Идеальная точность - все утверждения подтверждены контекстом")
    void testPerfectFaithfulness() {
        log.info("=== Тест идеальной точности ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица России?")
                .response("Столица России - Москва.")
                .retrievedContexts(List.of("Москва — столица Российской Федерации, город федерального значения, "
                        + "административный центр Центрального федерального округа и центр Московской области."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Контекст: {}", sample.getRetrievedContexts());
        log.info("Оценка точности: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.9, "Ожидается высокая оценка для полностью подтвержденного ответа, получен: " + score);
    }

    @Test
    @DisplayName("Высокая точность - биографическая информация")
    void testHighFaithfulness_Biography() {
        log.info("=== Высокая точность - Биография ===");

        Sample sample = Sample.builder()
                .userInput("Кто такой Лев Толстой?")
                .response("Лев Толстой был русским писателем и философом. "
                        + "Он является одним из величайших писателей мира. "
                        + "Он написал романы 'Война и мир' и 'Анна Каренина'.")
                .retrievedContexts(List.of(
                        "Лев Николаевич Толстой (1828-1910) — один из наиболее известных русских писателей и философов, "
                                + "признанный одним из величайших писателей мира. "
                                + "Автор романов 'Война и мир' и 'Анна Каренина'."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка точности: {}", score);

        assertNotNull(score);
        assertTrue(
                score >= 0.85,
                "Ожидается высокая оценка для хорошо подтвержденных биографических фактов, получен: " + score);
    }

    @Test
    @DisplayName("Высокая точность - научная информация")
    void testHighFaithfulness_ScientificInfo() {
        log.info("=== Высокая точность - Научная информация ===");

        Sample sample = Sample.builder()
                .userInput("Что такое фотосинтез?")
                .response("Фотосинтез - это процесс, при котором растения преобразуют световую энергию в химическую. "
                        + "В процессе фотосинтеза растения поглощают углекислый газ и выделяют кислород.")
                .retrievedContexts(
                        List.of(
                                "Фотосинтез — это процесс преобразования энергии света в энергию химических связей органических веществ. "
                                        + "В процессе фотосинтеза растения поглощают углекислый газ из атмосферы и выделяют кислород."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка точности: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для научно точного описания, получен: " + score);
    }

    // ==================== НЕГАТИВНЫЕ ТЕСТЫ - НИЗКАЯ ТОЧНОСТЬ ====================

    @Test
    @DisplayName("Низкая точность - неверная дата")
    void testLowFaithfulness_IncorrectDate() {
        log.info("=== Низкая точность - Неверная дата ===");

        Sample sample = Sample.builder()
                .userInput("Когда родился Пушкин?")
                .response("Александр Пушкин родился 10 июня 1799 года в Москве.")
                .retrievedContexts(List.of(
                        "Александр Сергеевич Пушкин родился 6 июня (26 мая по старому стилю) 1799 года в Москве. "
                                + "Великий русский поэт, драматург и прозаик."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Контекст: {}", sample.getRetrievedContexts());
        log.info("Оценка точности: {} (содержит неверную дату)", score);

        assertNotNull(score);
        assertTrue(
                score <= 0.6, "Ожидается низкая оценка из-за неверной даты (10 июня вместо 6 июня), получен: " + score);
    }

    @Test
    @DisplayName("Низкая точность - галлюцинации")
    void testLowFaithfulness_Hallucination() {
        log.info("=== Низкая точность - Галлюцинации ===");

        Sample sample = Sample.builder()
                .userInput("Какие курсы изучает Иван?")
                .response("Иван изучает структуры данных, алгоритмы и искусственный интеллект. "
                        + "Также он работает ассистентом преподавателя.")
                .retrievedContexts(List.of("Иван - студент МГУ, обучающийся на факультете компьютерных наук. "
                        + "В этом семестре он записан на курсы по структурам данных, алгоритмам и базам данных. "
                        + "Иван часто остается в библиотеке допоздна, работая над проектами."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка точности: {} (содержит галлюцинации)", score);

        assertNotNull(score);
        assertTrue(
                score <= 0.7,
                "Ожидается низкая оценка из-за выдуманного курса (ИИ) и работы ассистентом, получен: " + score);
    }

    @Test
    @DisplayName("Очень низкая точность - полностью не связанный контекст")
    void testZeroFaithfulness_UnrelatedContext() {
        log.info("=== Очень низкая точность - Несвязанный контекст ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о Достоевском")
                .response("Фёдор Достоевский был великим русским композитором, "
                        + "написавшим множество известных симфоний в 19 веке.")
                .retrievedContexts(List.of("Квантовая механика — это раздел физики, изучающий поведение материи "
                        + "и энергии на атомном и субатомном уровнях."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Контекст: {}", sample.getRetrievedContexts());
        log.info("Оценка точности: {} (несвязанный контекст)", score);

        assertNotNull(score);
        assertTrue(score <= 0.3, "Ожидается очень низкая оценка для несвязанного контекста, получен: " + score);
    }

    // ==================== ГРАНИЧНЫЕ СЛУЧАИ ====================

    @Test
    @DisplayName("Множественные контексты - частичная поддержка")
    void testMultipleContexts_PartialSupport() {
        log.info("=== Множественные контексты - Частичная поддержка ===");

        Sample sample = Sample.builder()
                .userInput("Что известно о глобальном потеплении?")
                .response("Глобальное потепление вызвано выбросами парниковых газов от деятельности человека. "
                        + "Оно приводит к повышению уровня моря и экстремальным погодным явлениям. "
                        + "Ученые прогнозируют повышение температуры на 5 градусов к 2100 году.")
                .retrievedContexts(List.of(
                        "Глобальное потепление относится к долгосрочному повышению средней температуры Земли. "
                                + "Основной причиной является деятельность человека, особенно сжигание ископаемого топлива, "
                                + "что приводит к выбросам парниковых газов.",
                        "Повышение глобальных температур вызывает повышение уровня моря и увеличение частоты "
                                + "экстремальных погодных явлений, таких как ураганы и засухи."))
                .build();

        Double score = faithfulnessMetric.singleTurnScore(sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка точности: {} (частичная поддержка из нескольких контекстов)", score);

        assertNotNull(score);
        assertTrue(
                score >= 0.5 && score <= 0.8,
                "Ожидается средняя оценка - часть утверждений подтверждена, прогноз отсутствует в контексте, получен: "
                        + score);
    }

    // ==================== АСИНХРОННЫЕ ТЕСТЫ ====================

    @Test
    @DisplayName("Асинхронная оценка")
    void testAsyncEvaluation() throws Exception {
        log.info("=== Асинхронная оценка точности ===");

        Sample sample = Sample.builder()
                .userInput("Что такое машинное обучение?")
                .response("Машинное обучение - это подобласть искусственного интеллекта, "
                        + "которая использует данные и алгоритмы для обучения.")
                .retrievedContexts(List.of("Машинное обучение — это подобласть искусственного интеллекта, "
                        + "которая фокусируется на использовании данных и алгоритмов для имитации процесса обучения, "
                        + "постепенно повышая точность."))
                .build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = faithfulnessMetric.singleTurnScoreAsync(sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Время выполнения асинхронной оценки: {} мс", (endTime - startTime));
        log.info("Оценка точности: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для соответствующего определения МО, получен: " + score);
    }

    @Test
    @DisplayName("Параллельная оценка нескольких примеров")
    void testParallelEvaluation() {
        log.info("=== Параллельная оценка точности ===");

        Sample sample1 = Sample.builder()
                .userInput("Что такое Python?")
                .response("Python - это высокоуровневый язык программирования.")
                .retrievedContexts(List.of(
                        "Python — высокоуровневый интерпретируемый язык программирования, известный своей простотой."))
                .build();

        Sample sample2 = Sample.builder()
                .userInput("Что такое Java?")
                .response("Java - это объектно-ориентированный язык программирования.")
                .retrievedContexts(List.of(
                        "Java — это объектно-ориентированный язык программирования, разработанный для переносимости."))
                .build();

        Sample sample3 = Sample.builder()
                .userInput("Что такое JavaScript?")
                .response("JavaScript - это язык программирования.")
                .retrievedContexts(
                        List.of(
                                "JavaScript — это язык программирования, являющийся одной из ключевых технологий World Wide Web."))
                .build();

        long startTime = System.currentTimeMillis();

        CompletableFuture<Double> future1 = faithfulnessMetric.singleTurnScoreAsync(sample1);
        CompletableFuture<Double> future2 = faithfulnessMetric.singleTurnScoreAsync(sample2);
        CompletableFuture<Double> future3 = faithfulnessMetric.singleTurnScoreAsync(sample3);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        allFutures.join();

        long endTime = System.currentTimeMillis();

        Double score1 = future1.join();
        Double score2 = future2.join();
        Double score3 = future3.join();

        log.info("Время параллельного выполнения: {} мс", (endTime - startTime));
        log.info("Точность Python: {}", score1);
        log.info("Точность Java: {}", score2);
        log.info("Точность JavaScript: {}", score3);

        assertNotNull(score1);
        assertNotNull(score2);
        assertNotNull(score3);

        // Все должны иметь высокую точность
        assertTrue(score1 >= 0.8, "Определение Python должно быть точным");
        assertTrue(score2 >= 0.8, "Определение Java должно быть точным");
        assertTrue(score3 >= 0.8, "Определение JavaScript должно быть точным");
    }
}
