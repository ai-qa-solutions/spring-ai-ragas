package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Интеграционные тесты метрики Response Relevancy с русскоязычными примерами")
@SpringBootTest(classes = RuResponseRelevancyIntegrationTest.ResponseRelevancyIntegrationTestConfiguration.class)
class RuResponseRelevancyIntegrationTest {

    @Configuration
    public static class ResponseRelevancyIntegrationTestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    // ==================== ТЕСТЫ ОСНОВНОЙ ФУНКЦИОНАЛЬНОСТИ ====================

    @Test
    @DisplayName("Response Relevancy: Полный ответ получает более высокую оценку чем неполный")
    void testResponseRelevancy_CompleteVsIncomplete() {
        log.info("=== Сравнение полного и неполного ответа ===");

        Sample incompleteSample = Sample.builder()
                .userInput("Где находится Франция и какая её столица?")
                .response("Франция находится в западной Европе.")
                .build();

        Sample completeSample = Sample.builder()
                .userInput("Где находится Франция и какая её столица?")
                .response("Франция находится в западной Европе, и её столица - Париж.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double incompleteScore = responseRelevancyMetric.singleTurnScore(config, incompleteSample);
        Double completeScore = responseRelevancyMetric.singleTurnScore(config, completeSample);

        log.info("Оценка неполного ответа: {}", incompleteScore);
        log.info("Оценка полного ответа: {}", completeScore);

        assertNotNull(incompleteScore);
        assertNotNull(completeScore);
        assertTrue(incompleteScore >= 0.0 && incompleteScore <= 1.0);
        assertTrue(completeScore >= 0.0 && completeScore <= 1.0);

        assertTrue(
                completeScore > incompleteScore,
                "Полный ответ должен получать более высокую оценку, чем неполный. Полный: " + completeScore
                        + ", Неполный: " + incompleteScore);
    }

    // ==================== ТЕСТЫ NONCOMMITTAL (УКЛОНЧИВЫЕ ОТВЕТЫ) ====================

    @Test
    @DisplayName("Noncommittal: Уклончивый ответ 'Я не знаю' получает оценку 0.0")
    void testResponseRelevancy_Noncommittal_IDontKnow() {
        log.info("=== Тест Noncommittal - 'Я не знаю' ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Я не знаю, какая столица Франции.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ (уклончивый): {}", sample.getResponse());
        log.info("Оценка Response Relevancy (уклончивый ответ): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Уклончивый ответ 'Я не знаю' должен получать оценку 0.0, получен: " + score);
    }

    @Test
    @DisplayName("Noncommittal: Уклончивый ответ 'Не уверен' получает оценку 0.0")
    void testResponseRelevancy_Noncommittal_NotSure() {
        log.info("=== Тест Noncommittal - 'Не уверен' ===");

        Sample sample = Sample.builder()
                .userInput("Когда была изобретена электрическая лампочка?")
                .response("Не уверен, когда была изобретена электрическая лампочка.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ (уклончивый): {}", sample.getResponse());
        log.info("Оценка Response Relevancy (уклончивый ответ): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, 0.01, "Уклончивый ответ 'Не уверен' должен получать оценку 0.0, получен: " + score);
    }

    @Test
    @DisplayName("Noncommittal: Уклончивый ответ 'У меня нет информации' получает оценку 0.0")
    void testResponseRelevancy_Noncommittal_NoInformation() {
        log.info("=== Тест Noncommittal - 'У меня нет информации' ===");

        Sample sample = Sample.builder()
                .userInput("Какова новаторская особенность смартфона, изобретенного в 2023 году?")
                .response(
                        "Я не знаю о новаторской особенности смартфона, изобретенного в 2023 году, так как у меня нет информации после 2022 года.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ (уклончивый - нет информации): {}", sample.getResponse());
        log.info("Оценка Response Relevancy (уклончивый ответ): {}", score);

        assertNotNull(score);
        assertEquals(
                0.0, score, 0.01, "Уклончивый ответ 'нет информации' должен получать оценку 0.0, получен: " + score);
    }

    @Test
    @DisplayName("Noncommittal vs Committal: Сравнение оценок")
    void testResponseRelevancy_NoncommittalVsCommittal() {
        log.info("=== Сравнение Noncommittal vs Committal ===");

        Sample noncommittalSample = Sample.builder()
                .userInput("Где родился Альберт Эйнштейн?")
                .response("Я не уверен, где родился Альберт Эйнштейн.")
                .build();

        Sample committalSample = Sample.builder()
                .userInput("Где родился Альберт Эйнштейн?")
                .response("Альберт Эйнштейн родился в Германии.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double noncommittalScore = responseRelevancyMetric.singleTurnScore(config, noncommittalSample);
        Double committalScore = responseRelevancyMetric.singleTurnScore(config, committalSample);

        log.info("Оценка уклончивого ответа: {}", noncommittalScore);
        log.info("Оценка нормального ответа: {}", committalScore);

        assertNotNull(noncommittalScore);
        assertNotNull(committalScore);

        assertEquals(0.0, noncommittalScore, 0.01, "Уклончивый ответ должен получать 0.0");
        assertTrue(committalScore >= 0.85, "Нормальный ответ должен получать высокую оценку");
        assertTrue(
                committalScore > noncommittalScore,
                "Нормальный ответ должен получать значительно более высокую оценку, чем уклончивый");
    }

    // ==================== СЛОЖНЫЕ СЦЕНАРИИ ====================

    @Test
    @DisplayName("Сложный сценарий: Научное объяснение")
    void testResponseRelevancy_ScientificExplanation() {
        log.info("=== Тест сложного сценария - Научное объяснение ===");

        Sample sample = Sample.builder()
                .userInput("Что такое фотосинтез?")
                .response(
                        "Фотосинтез — это процесс, при котором растения преобразуют световую энергию в химическую энергию. Растения используют солнечный свет, воду и углекислый газ для производства глюкозы и кислорода.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy (научное объяснение): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для полного научного объяснения, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Историческая информация с датами")
    void testResponseRelevancy_HistoricalInformation() {
        log.info("=== Тест сложного сценария - Историческая информация ===");

        Sample sample = Sample.builder()
                .userInput("Когда началась и закончилась Вторая мировая война?")
                .response("Вторая мировая война началась в 1939 году и закончилась в 1945 году.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy (историческая информация): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для полного исторического ответа, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Технические термины и определения")
    void testResponseRelevancy_TechnicalDefinition() {
        log.info("=== Тест сложного сценария - Технические термины ===");

        Sample sample = Sample.builder()
                .userInput("Что такое машинное обучение?")
                .response(
                        "Машинное обучение — это раздел искусственного интеллекта, который позволяет компьютерам учиться на данных и улучшать свою производительность без явного программирования.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy (техническое определение): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для точного технического определения, получен: " + score);
    }

    @Test
    @DisplayName("Сложный сценарий: Ответ с избыточной информацией")
    void testResponseRelevancy_RedundantInformation() {
        log.info("=== Тест сложного сценария - Избыточная информация ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response(
                        "Столица Франции - Париж. Париж также известен как Город Огней. Эйфелева башня находится в Париже. Париж является одним из самых посещаемых городов в мире. Французская кухня очень известна.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ (с избыточной информацией): {}", sample.getResponse());
        log.info("Оценка Response Relevancy (избыточная информация): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Избыточная информация может снизить оценку, но ответ все еще релевантен
        assertTrue(score >= 0.6, "Ответ с избыточной информацией должен получать умеренную оценку, получен: " + score);
    }

    // ==================== ГРАНИЧНЫЕ СЛУЧАИ ====================

    @Test
    @DisplayName("Граничный случай: Очень короткий вопрос и ответ")
    void testResponseRelevancy_VeryShort() {
        log.info("=== Тест граничного случая - Очень короткий вопрос и ответ ===");

        Sample sample = Sample.builder().userInput("Кто?").response("Эйнштейн.").build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy (очень короткий): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Очень короткие вопросы и ответы все еще должны оцениваться
    }

    @Test
    @DisplayName("Граничный случай: Длинный подробный ответ")
    void testResponseRelevancy_LongDetailedAnswer() {
        log.info("=== Тест граничного случая - Длинный подробный ответ ===");

        Sample sample = Sample.builder()
                .userInput("Расскажите о теории относительности")
                .response(
                        "Теория относительности Эйнштейна состоит из двух частей: специальной и общей теории относительности. "
                                + "Специальная теория относительности, опубликованная в 1905 году, рассматривает движение объектов с постоянной скоростью. "
                                + "Она вводит концепции относительности одновременности, замедления времени и сокращения длины. "
                                + "Общая теория относительности, опубликованная в 1915 году, описывает гравитацию как искривление пространства-времени. "
                                + "Эта теория объясняет движение планет, черные дыры и расширение Вселенной. "
                                + "Обе теории были подтверждены многочисленными экспериментами и наблюдениями.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Длина ответа: {} символов", sample.getResponse().length());
        log.info("Оценка Response Relevancy (длинный ответ): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для подробного релевантного ответа, получен: " + score);
    }

    // ==================== ТЕСТЫ КОНФИГУРАЦИИ ====================

    @Test
    @DisplayName("Конфигурация: Пользовательское количество вопросов (5)")
    void testResponseRelevancy_CustomNumberOfQuestions() {
        log.info("=== Тест конфигурации - Пользовательское количество вопросов ===");

        Sample sample = Sample.builder()
                .userInput("Что такое искусственный интеллект?")
                .response(
                        "Искусственный интеллект — это область компьютерных наук, которая создает машины, способные выполнять задачи, требующие человеческого интеллекта.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(5)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Количество генерируемых вопросов: {}", config.getNumberOfQuestions());
        log.info("Оценка Response Relevancy (5 вопросов): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.85, "Ожидается высокая оценка даже с большим количеством вопросов, получен: " + score);
    }

    @Test
    @DisplayName("Конфигурация: Сравнение 3 vs 5 вопросов")
    void testResponseRelevancy_CompareQuestionCounts() {
        log.info("=== Сравнение конфигураций - 3 vs 5 вопросов ===");

        Sample sample = Sample.builder()
                .userInput("Что вызывает дождь?")
                .response(
                        "Дождь вызывается конденсацией водяного пара в атмосфере, который образует облака и затем выпадает в виде осадков.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config3 =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config5 =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(5)
                        .build();

        Double score3 = responseRelevancyMetric.singleTurnScore(config3, sample);
        Double score5 = responseRelevancyMetric.singleTurnScore(config5, sample);

        log.info("Оценка с 3 вопросами: {}", score3);
        log.info("Оценка с 5 вопросами: {}", score5);
        log.info("Разница: {}", Math.abs(score3 - score5));

        assertNotNull(score3);
        assertNotNull(score5);
        assertTrue(score3 >= 0.0 && score3 <= 1.0);
        assertTrue(score5 >= 0.0 && score5 <= 1.0);

        // Оценки должны быть близки, но могут немного отличаться
        assertTrue(
                Math.abs(score3 - score5) < 0.2,
                "Оценки с разным количеством вопросов должны быть относительно близки");
    }

    // ==================== ТЕСТЫ АСИНХРОННОСТИ ====================

    @Test
    @DisplayName("Async: Асинхронное вычисление оценки")
    void testResponseRelevancy_AsyncScore() throws Exception {
        log.info("=== Тест асинхронного вычисления ===");

        Sample sample = Sample.builder()
                .userInput("Какая самая высокая гора в мире?")
                .response("Эверест — самая высокая гора в мире, её высота составляет 8 848 метров.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        CompletableFuture<Double> scoreFuture = responseRelevancyMetric.singleTurnScoreAsync(config, sample);

        log.info("Асинхронное вычисление запущено...");

        Double score = scoreFuture.get(); // Ожидание результата

        log.info("Оценка Response Relevancy (async): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.85, "Ожидается высокая оценка для прямого ответа, получен: " + score);
    }

    @Test
    @DisplayName("Async: Множественные асинхронные вычисления")
    void testResponseRelevancy_MultipleAsyncScores() throws Exception {
        log.info("=== Тест множественных асинхронных вычислений ===");

        Sample sample1 = Sample.builder()
                .userInput("Кто изобрел телефон?")
                .response("Александр Грэм Белл изобрел телефон.")
                .build();

        Sample sample2 = Sample.builder()
                .userInput("Когда был изобретен интернет?")
                .response("Интернет был разработан в конце 1960-х годов.")
                .build();

        Sample sample3 = Sample.builder()
                .userInput("Что такое ДНК?")
                .response("ДНК — это молекула, которая содержит генетическую информацию.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        // Запуск всех асинхронных вычислений
        CompletableFuture<Double> future1 = responseRelevancyMetric.singleTurnScoreAsync(config, sample1);
        CompletableFuture<Double> future2 = responseRelevancyMetric.singleTurnScoreAsync(config, sample2);
        CompletableFuture<Double> future3 = responseRelevancyMetric.singleTurnScoreAsync(config, sample3);

        // Ожидание всех результатов
        CompletableFuture.allOf(future1, future2, future3).get();

        Double score1 = future1.get();
        Double score2 = future2.get();
        Double score3 = future3.get();

        log.info("Оценка 1: {}", score1);
        log.info("Оценка 2: {}", score2);
        log.info("Оценка 3: {}", score3);

        assertNotNull(score1);
        assertNotNull(score2);
        assertNotNull(score3);

        assertTrue(score1 >= 0.85, "Ожидается высокая оценка для ответа 1");
        assertTrue(score2 >= 0.85, "Ожидается высокая оценка для ответа 2");
        assertTrue(score3 >= 0.85, "Ожидается высокая оценка для ответа 3");
    }

    // ==================== ТЕСТЫ ПРОИЗВОДИТЕЛЬНОСТИ ====================

    @Test
    @DisplayName("Производительность: Измерение времени обработки")
    void testResponseRelevancy_Performance() {
        log.info("=== Тест производительности ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс эволюции")
                .response("Эволюция — это процесс изменения живых организмов с течением времени. "
                        + "Основным механизмом эволюции является естественный отбор, при котором особи с благоприятными признаками "
                        + "имеют больше шансов выжить и размножиться. Мутации создают генетическую изменчивость, "
                        + "которая является материалом для естественного отбора.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        long startTime = System.currentTimeMillis();
        Double score = responseRelevancyMetric.singleTurnScore(config, sample);
        long endTime = System.currentTimeMillis();

        log.info("Время обработки: {} мс", (endTime - startTime));
        log.info("Оценка Response Relevancy: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue((endTime - startTime) < 30000, "Обработка должна завершиться в разумное время (< 30 сек)");
    }

    // ==================== ТЕСТЫ ВАЛИДАЦИИ ====================

    @Test
    @DisplayName("Валидация: Отсутствие user input")
    void testResponseRelevancy_NoUserInput() {
        log.info("=== Тест валидации - Отсутствие user input ===");

        Sample sample = Sample.builder().response("Париж - столица Франции.").build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Оценка при отсутствии user input: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается оценка 0.0 при отсутствии user input");
    }

    @Test
    @DisplayName("Валидация: Отсутствие response")
    void testResponseRelevancy_NoResponse() {
        log.info("=== Тест валидации - Отсутствие response ===");

        Sample sample = Sample.builder().userInput("Какая столица Франции?").build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Оценка при отсутствии response: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается оценка 0.0 при отсутствии response");
    }

    @Test
    @DisplayName("Валидация: Пустые строки")
    void testResponseRelevancy_EmptyStrings() {
        log.info("=== Тест валидации - Пустые строки ===");

        Sample sample = Sample.builder().userInput("").response("").build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Оценка для пустых строк: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается оценка 0.0 для пустых строк");
    }

    // ==================== КАЧЕСТВЕННАЯ ОЦЕНКА ====================

    @Test
    @DisplayName("Качественная оценка: Сравнение различных уровней релевантности")
    void testResponseRelevancy_RelevanceLevels() {
        log.info("=== Качественная оценка - Различные уровни релевантности ===");

        String question = "Что такое глобальное потепление и каковы его причины?";

        Sample highRelevance = Sample.builder()
                .userInput(question)
                .response(
                        "Глобальное потепление — это долгосрочное повышение средней температуры Земли. Основной причиной является увеличение парниковых газов в атмосфере, особенно углекислого газа от сжигания ископаемого топлива.")
                .build();

        Sample mediumRelevance = Sample.builder()
                .userInput(question)
                .response("Глобальное потепление — это повышение температуры Земли.")
                .build();

        Sample lowRelevance = Sample.builder()
                .userInput(question)
                .response("Климат меняется постоянно на протяжении истории Земли.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double highScore = responseRelevancyMetric.singleTurnScore(config, highRelevance);
        Double mediumScore = responseRelevancyMetric.singleTurnScore(config, mediumRelevance);
        Double lowScore = responseRelevancyMetric.singleTurnScore(config, lowRelevance);

        log.info("Высокая релевантность: {}", highScore);
        log.info("Средняя релевантность: {}", mediumScore);
        log.info("Низкая релевантность: {}", lowScore);

        assertNotNull(highScore);
        assertNotNull(mediumScore);
        assertNotNull(lowScore);

        assertTrue(highScore > mediumScore, "Полный ответ должен получать более высокую оценку, чем неполный");
        assertTrue(mediumScore > lowScore, "Неполный ответ должен получать более высокую оценку, чем не по теме");
    }

    @Test
    @DisplayName("Response Relevancy: Высокая релевантность - прямой ответ на вопрос")
    void testResponseRelevancy_HighRelevance_DirectAnswer() {
        log.info("=== Тест Response Relevancy - Высокая релевантность ===");

        Sample sample = Sample.builder()
                .userInput("Когда был проведен первый Суперкубок?")
                .response("Первый Суперкубок был проведен 15 января 1967 года.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для релевантного ответа, получен: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Высокая релевантность - ответ на близкую тему")
    void testResponseRelevancy_HighRelevance_RelatedTopic() {
        log.info("=== Тест Response Relevancy - Релевантный ответ на близкую тему ===");

        // Ответ про расположение Франции релевантен вопросу про столицу,
        // так как обе темы про географию Франции
        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Франция находится в западной Европе, её столица - Париж.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для релевантного ответа, получен: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Нулевая релевантность - уклончивый ответ")
    void testResponseRelevancy_ZeroRelevance_NoncommittalAnswer() {
        log.info("=== Тест Response Relevancy - Нулевая релевантность (уклончивый ответ) ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Я не знаю ответа на этот вопрос.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ (уклончивый): {}", sample.getResponse());
        log.info("Оценка Response Relevancy: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertEquals(0.0, score, 0.01, "Ожидается нулевая оценка для уклончивого ответа, получен: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Высокая релевантность - развернутый ответ")
    void testResponseRelevancy_HighRelevance_DetailedAnswer() {
        log.info("=== Тест Response Relevancy - Развернутый релевантный ответ ===");

        Sample sample = Sample.builder()
                .userInput("Расскажи о столице Франции")
                .response("Париж - столица и крупнейший город Франции. "
                        + "Он расположен на реке Сене в северной части страны. "
                        + "Париж известен своими достопримечательностями, такими как Эйфелева башня, "
                        + "Лувр и Нотр-Дам де Пари.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.8, "Ожидается очень высокая оценка для развернутого релевантного ответа, получен: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Низкая релевантность - избыточная информация")
    void testResponseRelevancy_LowRelevance_RedundantInformation() {
        log.info("=== Тест Response Relevancy - Избыточная нерелевантная информация ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Столица Франции - Париж. " + "Кстати, вчера я ходил в магазин и купил молоко. "
                        + "Погода была отличная, светило солнце. "
                        + "Я также встретил старого друга.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Response Relevancy: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score < 0.8,
                "Ожидается пониженная оценка из-за избыточной нерелевантной информации, получен: " + score);
    }

    @Test
    @DisplayName("Response Relevancy: Проверка с пустым входом")
    void testResponseRelevancy_EmptyInput() {
        log.info("=== Тест Response Relevancy - Пустой вход ===");

        Sample sample = Sample.builder()
                .userInput("")
                .response("Париж - столица Франции.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Оценка Response Relevancy (пустой вход): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается 0.0 для пустого входа");
    }

    @Test
    @DisplayName("Response Relevancy: Проверка с пустым ответом")
    void testResponseRelevancy_EmptyResponse() {
        log.info("=== Тест Response Relevancy - Пустой ответ ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Оценка Response Relevancy (пустой ответ): {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается 0.0 для пустого ответа");
    }

    @Test
    @DisplayName("Response Relevancy: Асинхронный вызов")
    void testResponseRelevancy_AsyncCall() throws Exception {
        log.info("=== Тест Response Relevancy - Асинхронный вызов ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Париж - столица Франции.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.defaultConfig();

        Double score =
                responseRelevancyMetric.singleTurnScoreAsync(config, sample).get();

        log.info("Оценка Response Relevancy (async): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.7, "Ожидается высокая оценка для релевантного ответа");
    }
}
