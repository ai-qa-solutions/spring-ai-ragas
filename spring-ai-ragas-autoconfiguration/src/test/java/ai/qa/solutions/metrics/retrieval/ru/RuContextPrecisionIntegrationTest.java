package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.ContextPrecisionMetric;
import ai.qa.solutions.sample.Sample;
import java.util.List;
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
@DisplayName("Интеграционные тесты метрики Context Precision с русскоязычными примерами")
@SpringBootTest(classes = RuContextPrecisionIntegrationTest.ContextPrecisionIntegrationTestConfiguration.class)
class RuContextPrecisionIntegrationTest {

    @Configuration
    public static class ContextPrecisionIntegrationTestConfiguration {}

    @Autowired
    private ContextPrecisionMetric contextPrecisionMetric;

    // ==================== ТЕСТЫ АВТООПРЕДЕЛЕНИЯ СТРАТЕГИИ ====================

    @Test
    @DisplayName("Context Precision: Автоопределение на основе ответа (без эталона)")
    void testContextPrecision_AutoDetectResponse() {
        log.info("=== Тест Context Precision - Автоопределение на основе ответа ===");

        Sample sample = Sample.builder()
                .userInput("Где находится Красная площадь?")
                .response("Красная площадь находится в центре Москвы, столицы России.")
                .retrievedContexts(List.of(
                        "Красная площадь — главная площадь Москвы, расположенная в центре города.",
                        "Москва является столицей Российской Федерации и крупнейшим городом страны.",
                        "Сегодня на улице прекрасная погода для прогулки."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                // Стратегия не указана - будет автоопределение
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка Context Precision (автоопределение на основе ответа): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.5, "Ожидается разумная оценка для смешанных релевантных контекстов, получен: " + score);
    }

    @Test
    @DisplayName("Context Precision: Автоопределение на основе эталона (предпочтительно)")
    void testContextPrecision_AutoDetectReference() {
        log.info("=== Тест Context Precision - Автоопределение на основе эталона (предпочтительно) ===");

        Sample sample = Sample.builder()
                .userInput("Что такое фотосинтез?")
                .response("Фотосинтез — это процесс, используемый растениями.")
                .reference(
                        "Фотосинтез — это процесс, при котором растения используют солнечный свет, углекислый газ и воду для производства глюкозы и кислорода.")
                .retrievedContexts(List.of(
                        "Фотосинтез — это биологический процесс, при котором растения преобразуют световую энергию в химическую с помощью хлорофилла.",
                        "Процесс включает реакцию: 6CO₂ + 6H₂O + световая энергия → C₆H₁₂O₆ + 6O₂.",
                        "Растения — автотрофы, которые могут производить собственную пищу посредством фотосинтеза."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                // Стратегия не указана - будет автоопределение на основе эталона
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Эталон: {}", sample.getReference());
        log.info("Оценка Context Precision (автоопределение на основе эталона): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для релевантных контекстов с эталоном, получен: " + score);
    }

    // ==================== ТЕСТЫ ЯВНЫХ СТРАТЕГИЙ ====================

    @Test
    @DisplayName("Context Precision: Явная стратегия на основе эталона")
    void testContextPrecision_ExplicitReference() {
        log.info("=== Тест Context Precision - Явная стратегия на основе эталона ===");

        Sample sample = Sample.builder()
                .userInput("Кто написал роман 'Война и мир'?")
                .response("Толстой написал книгу.")
                .reference("Роман 'Война и мир' написал Лев Николаевич Толстой.")
                .retrievedContexts(List.of(
                        "Лев Толстой — великий русский писатель, автор романа 'Война и мир'.",
                        "Россия — крупнейшая страна в мире по территории.",
                        "Футбол играется двумя командами по одиннадцать игроков."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Precision (явная стратегия на основе эталона): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score >= 0.4, "Ожидается разумная оценка со смешанными контекстами, получен: " + score);
    }

    @Test
    @DisplayName("Context Precision: Явная стратегия на основе ответа")
    void testContextPrecision_ExplicitResponse() {
        log.info("=== Тест Context Precision - Явная стратегия на основе ответа ===");

        Sample sample = Sample.builder()
                .userInput("Как работает искусственный интеллект?")
                .response(
                        "Искусственный интеллект использует алгоритмы для изучения паттернов в данных и создания прогнозов.")
                .reference(
                        "Искусственный интеллект — это подраздел информатики, позволяющий компьютерам обучаться без явного программирования.")
                .retrievedContexts(List.of(
                        "Алгоритмы машинного обучения учатся на обучающих данных для создания прогнозов.",
                        "Нейронные сети — популярная техника ИИ, вдохновленная человеческим мозгом.",
                        "Прогноз погоды показывает дождь завтра с вероятностью 80%."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Precision (явная стратегия на основе ответа): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    // ==================== ТЕСТЫ КАЧЕСТВА ====================

    @Test
    @DisplayName("Context Precision: Идеальное упорядочивание со всеми релевантными контекстами")
    void testContextPrecision_PerfectOrdering() {
        log.info("=== Тест Context Precision - Идеальное упорядочивание ===");

        Sample sample = Sample.builder()
                .userInput("Что такое квантовая механика?")
                .reference(
                        "Квантовая механика — это раздел физики, описывающий поведение материи и энергии на атомном и субатомном уровне.")
                .retrievedContexts(List.of(
                        "Квантовая механика — это раздел физики, изучающий поведение материи на атомном уровне.",
                        "Квантовая механика описывает физические свойства природы в масштабе атомов и субатомных частиц.",
                        "Это фундаментальная теория в физике, объясняющая поведение материи и энергии на очень малых масштабах.",
                        "Квантовая механика — основа современной физики для понимания микромира."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Precision (идеальное упорядочивание): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(
                score >= 0.9,
                "Ожидается очень высокая оценка для идеально упорядоченных релевантных контекстов, получен: " + score);
    }

    @Test
    @DisplayName("Context Precision: Плохое упорядочивание с нерелевантным контекстом первым")
    void testContextPrecision_PoorOrdering() {
        log.info("=== Тест Context Precision - Плохое упорядочивание ===");

        Sample sample = Sample.builder()
                .userInput("Что такое блокчейн?")
                .response(
                        "Блокчейн — это технология распределенного реестра, которая поддерживает постоянно растущий список записей.")
                .retrievedContexts(List.of(
                        "Цены на продукты значительно выросли в этом году.",
                        "Блокчейн использует криптографические хеши для связывания блоков данных.",
                        "Технология блокчейн обеспечивает прозрачность и безопасность транзакций.",
                        "Криптовалюты, такие как Bitcoin, построены на технологии блокчейн."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Precision (плохое упорядочивание): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        assertTrue(score <= 0.7, "Ожидается низкая оценка когда нерелевантный контекст первый, получен: " + score);
    }

    @Test
    @DisplayName("Context Precision: Смешанная релевантность - реалистичный сценарий")
    void testContextPrecision_MixedRelevance() {
        log.info("=== Тест Context Precision - Смешанная релевантность ===");

        Sample sample = Sample.builder()
                .userInput("Объясните парниковый эффект")
                .reference(
                        "Парниковый эффект — это естественный процесс, при котором определенные газы в атмосфере Земли задерживают тепло от солнца, согревая планету.")
                .retrievedContexts(List.of(
                        "Парниковые газы, такие как CO₂ и метан, задерживают тепло в атмосфере Земли.",
                        "Парниковый эффект необходим для жизни на Земле, поскольку он поддерживает тепло на планете.",
                        "Популярные туристические направления включают тропические пляжи и горные курорты.",
                        "Без парникового эффекта Земля была бы слишком холодной для поддержания большинства форм жизни.",
                        "Изменение климата связано с усиленным парниковым эффектом от человеческой деятельности."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Оценка Context Precision (смешанная релевантность): {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Большинство контекстов релевантны, поэтому должна быть достаточно высокая оценка
        assertTrue(
                score >= 0.6,
                "Ожидается выше среднего оценка для в основном релевантных контекстов, получен: " + score);
    }

    // ==================== АСИНХРОННЫЕ ТЕСТЫ ====================

    @Test
    @DisplayName("Асинхронная оценка")
    void testAsyncEvaluation() throws Exception {
        log.info("=== Тест асинхронной оценки ===");

        Sample sample = Sample.builder()
                .userInput("Что такое машинное обучение?")
                .response(
                        "Машинное обучение — это метод анализа данных, который автоматизирует построение аналитических моделей.")
                .retrievedContexts(List.of(
                        "Машинное обучение использует алгоритмы для анализа данных и выявления закономерностей.",
                        "Алгоритмы машинного обучения могут обучаться на исторических данных.",
                        "Нейронные сети — это один из популярных подходов в машинном обучении."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                .build();

        long startTime = System.currentTimeMillis();
        CompletableFuture<Double> asyncScore = contextPrecisionMetric.singleTurnScoreAsync(config, sample);
        Double score = asyncScore.get();
        long endTime = System.currentTimeMillis();

        log.info("Время выполнения асинхронной оценки: {} мс", (endTime - startTime));
        log.info("Результат асинхронной оценки: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    @DisplayName("Параллельная оценка: Стратегии на основе эталона vs ответа")
    void testParallelEvaluationComparison() {
        log.info("=== Тест параллельной оценки - Стратегии на основе эталона vs ответа ===");

        Sample sample = Sample.builder()
                .userInput("Объясните изменение климата")
                .response(
                        "Изменение климата относится к долгосрочным изменениям глобальных температур и погодных условий.")
                .reference(
                        "Изменение климата — это долгосрочное потепление планеты, в первую очередь вызванное человеческой деятельностью, которая увеличивает концентрацию парниковых газов в атмосфере.")
                .retrievedContexts(List.of(
                        "Человеческая деятельность, такая как сжигание ископаемого топлива, способствует глобальному потеплению.",
                        "Последствия изменения климата включают повышение уровня моря и экстремальные погодные явления.",
                        "Популярные места отдыха включают пляжи и горы.",
                        "Парниковые газы задерживают тепло в атмосфере Земли, вызывая глобальное потепление."))
                .build();

        // Создание конфигураций для обеих стратегий
        ContextPrecisionMetric.ContextPrecisionConfig referenceConfig =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                        .build();

        ContextPrecisionMetric.ContextPrecisionConfig responseConfig =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                        .build();

        long startTime = System.currentTimeMillis();

        // Параллельное выполнение
        CompletableFuture<Double> referenceFuture =
                contextPrecisionMetric.singleTurnScoreAsync(referenceConfig, sample);
        CompletableFuture<Double> responseFuture = contextPrecisionMetric.singleTurnScoreAsync(responseConfig, sample);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(referenceFuture, responseFuture);
        allFutures.join();
        long endTime = System.currentTimeMillis();

        Double referenceScore = referenceFuture.join();
        Double responseScore = responseFuture.join();

        log.info("Время параллельного выполнения: {} мс", (endTime - startTime));
        log.info("Context Precision на основе эталона: {}", referenceScore);
        log.info("Context Precision на основе ответа: {}", responseScore);

        assertNotNull(referenceScore);
        assertNotNull(responseScore);

        assertTrue(referenceScore >= 0.0 && referenceScore <= 1.0);
        assertTrue(responseScore >= 0.0 && responseScore <= 1.0);

        // Оценка на основе эталона обычно более точная, поскольку эталон предоставляет золотой стандарт
        log.info("Оценка на основе эталона обычно более точная благодаря золотому стандарту эталона");
    }

    // ==================== ГРАНИЧНЫЕ СЛУЧАИ ====================

    @Test
    @DisplayName("Граничный случай: Пустые найденные контексты")
    void testEmptyRetrievedContexts() {
        log.info("=== Тест граничного случая - Пустые найденные контексты ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Столица Франции — Париж.")
                .retrievedContexts(List.of())
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Оценка для пустых контекстов: {}", score);

        assertNotNull(score);
        assertEquals(0.0, score, "Ожидается 0.0 для пустых найденных контекстов, получен: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Один релевантный контекст")
    void testSingleRelevantContext() {
        log.info("=== Тест граничного случая - Один релевантный контекст ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Италии?")
                .response("Столица Италии — Рим.")
                .retrievedContexts(List.of("Рим является столицей и крупнейшим городом Италии."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Оценка для одного релевантного контекста: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.8, "Ожидается высокая оценка для одного релевантного контекста, получен: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Все нерелевантные контексты")
    void testAllIrrelevantContexts() {
        log.info("=== Тест граничного случая - Все нерелевантные контексты ===");

        Sample sample = Sample.builder()
                .userInput("Что такое машинное обучение?")
                .response("Машинное обучение — это подраздел искусственного интеллекта.")
                .retrievedContexts(List.of(
                        "Сегодня прекрасная погода для пикника на природе.",
                        "Пицца — популярное итальянское блюдо с разнообразными начинками.",
                        "Футбол играется двумя командами по одиннадцать игроков."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig config =
                ContextPrecisionMetric.ContextPrecisionConfig.builder().build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Оценка для всех нерелевантных контекстов: {}", score);

        assertNotNull(score);
        assertTrue(score <= 0.2, "Ожидается низкая оценка для всех нерелевантных контекстов, получен: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Откат с эталона на ответ")
    void testReferenceFallbackToResponse() {
        log.info("=== Тест граничного случая - Откат с эталона на ответ ===");

        Sample sample = Sample.builder()
                .userInput("Что такое возобновляемая энергия?")
                .response("Возобновляемая энергия происходит из природных источников, которые восполняются сами собой.")
                .reference("") // Пустой эталон
                .retrievedContexts(List.of(
                        "Солнечная и ветровая энергия — примеры возобновляемых источников энергии.",
                        "Возобновляемая энергия помогает сократить выбросы парниковых газов."))
                .build();

        // Запрашиваем оценку на основе эталона, но откатится на оценку на основе ответа из-за пустого эталона
        ContextPrecisionMetric.ContextPrecisionConfig config = ContextPrecisionMetric.ContextPrecisionConfig.builder()
                .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                .build();

        Double score = contextPrecisionMetric.singleTurnScore(config, sample);

        log.info("Оценка для отката с эталона: {}", score);

        assertNotNull(score);
        assertTrue(score >= 0.0 && score <= 1.0);
        // Должна получиться разумная оценка при использовании оценки на основе ответа
        assertTrue(
                score >= 0.5, "Ожидается разумная оценка после отката на оценку на основе ответа, получен: " + score);
    }

    @Test
    @DisplayName("Сравнение стратегий: Эталон vs Ответ с одними и теми же данными")
    void testStrategyComparison() {
        log.info("=== Тест сравнения стратегий - Одни и те же данные ===");

        Sample sample = Sample.builder()
                .userInput("Что вызывает землетрясения?")
                .response("Землетрясения вызываются движением тектонических плит.")
                .reference(
                        "Землетрясения происходят, когда тектонические плиты в земной коре внезапно сдвигаются и высвобождают энергию, создавая сейсмические волны.")
                .retrievedContexts(List.of(
                        "Тектонические плиты в земной коре могут внезапно сдвигаться, вызывая землетрясения.",
                        "Когда тектонические плиты сталкиваются или скользят друг мимо друга, они могут вызывать землетрясения.",
                        "Сейсмические волны — это энергия, высвобождающаяся во время землетрясения.",
                        "Лучшее время для посещения Японии — сезон цветения сакуры."))
                .build();

        ContextPrecisionMetric.ContextPrecisionConfig referenceConfig =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.REFERENCE_BASED)
                        .build();

        ContextPrecisionMetric.ContextPrecisionConfig responseConfig =
                ContextPrecisionMetric.ContextPrecisionConfig.builder()
                        .evaluationStrategy(ContextPrecisionMetric.EvaluationStrategy.RESPONSE_BASED)
                        .build();

        Double referenceScore = contextPrecisionMetric.singleTurnScore(referenceConfig, sample);
        Double responseScore = contextPrecisionMetric.singleTurnScore(responseConfig, sample);

        log.info("Оценка на основе эталона: {}", referenceScore);
        log.info("Оценка на основе ответа: {}", responseScore);

        assertNotNull(referenceScore);
        assertNotNull(responseScore);
        assertTrue(referenceScore >= 0.0 && referenceScore <= 1.0);
        assertTrue(responseScore >= 0.0 && responseScore <= 1.0);

        // Обе должны дать разумные оценки, поскольку большинство контекстов релевантны
        assertTrue(referenceScore >= 0.5, "Ожидается разумная оценка на основе эталона");
        assertTrue(responseScore >= 0.5, "Ожидается разумная оценка на основе ответа");
    }
}
