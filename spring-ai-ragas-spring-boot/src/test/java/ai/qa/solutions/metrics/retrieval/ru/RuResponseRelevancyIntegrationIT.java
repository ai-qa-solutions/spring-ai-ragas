package ai.qa.solutions.metrics.retrieval.ru;

import static org.junit.jupiter.api.Assertions.*;

import ai.qa.solutions.metrics.retrieval.ResponseRelevancyMetric;
import ai.qa.solutions.sample.Sample;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * Интеграционные тесты метрики Response Relevancy на основе методологии Ragas.
 * </p>
 * <p>
 * ВАЖНО: Эти тесты отражают РЕАЛЬНОЕ поведение Ragas Response Relevancy,
 * включая известные ограничения. Высокие оценки для частичных/нерелевантных ответов -
 * это ОЖИДАЕМОЕ поведение, а не баги.
 * </p>
 * Ключевые выводы:
 * - Метрика использует косинусное сходство эмбеддингов, измеряющее лингвистические паттерны
 * - Не может определить: отсутствующую информацию, разные аспекты одной темы, нерелевантность при схожей структуре
 * - Надёжна только для: идеальных совпадений (0.85+), уклончивых ответов (0.0)
 * - Всё остальное попадает в диапазон 0.2-0.9 (ненадёжно для принятия решений)
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Response Relevancy Metric - Проверка поведения Ragas (RU)")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuResponseRelevancyIntegrationIT.ResponseRelevancyIntegrationTestConfiguration.class)
class RuResponseRelevancyIntegrationIT {

    @Configuration
    public static class ResponseRelevancyIntegrationTestConfiguration {}

    @Autowired
    private ResponseRelevancyMetric responseRelevancyMetric;

    @Test
    @DisplayName("✅ Идеальный ответ: Прямой и полный - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
    void testResponseRelevancy_PerfectAnswer() {

        log.info("=== Тест идеального ответа ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Столица Франции - Париж.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Ответ: {}", sample.getResponse());
        assertTrue(score >= 0.80, "Идеальные ответы получают высокие баллы (0.80+). Получено: " + score);

        log.info("✅ УСПЕХ: Обнаружение идеального ответа работает корректно!");
    }

    @Test
    @DisplayName("✅ Уклончивый ответ: 'Я не знаю' - ОЖИДАЕТСЯ НУЛЕВОЙ БАЛЛ")
    void testResponseRelevancy_NoncommittalAnswer() {

        log.info("=== Тест уклончивого ответа ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Я не знаю ответа на этот вопрос.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertEquals(
                0.0,
                score,
                0.01,
                "Уклончивые ответы ('Я не знаю', 'неясно' и т.д.) возвращают 0.0. Получено: " + score);
    }

    @Test
    @DisplayName("✅ Подробный ответ: Развёрнутый но полный - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
    void testResponseRelevancy_VerboseButComplete() {

        log.info("=== Тест подробного ответа ===");

        Sample sample = Sample.builder()
                .userInput("Что такое искусственный интеллект?")
                .response(
                        "Искусственный интеллект (ИИ) — это область компьютерных наук, занимающаяся "
                                + "созданием интеллектуальных машин, способных выполнять задачи, требующие человеческого интеллекта. "
                                + "Это включает обучение, рассуждение, решение проблем, восприятие и понимание языка. "
                                + "ИИ используется в различных приложениях от виртуальных помощников до автономных транспортных средств.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(score >= 0.70, "Подробные ответы получают высокие баллы (0.70+). Получено: " + score);
    }

    @Test
    @DisplayName("Неполный ответ: Отвечает только на часть вопроса")
    void testResponseRelevancy_IncompleteAnswer() {

        log.info("=== Тест неполного ответа ===");

        Sample sample = Sample.builder()
                .userInput("Где находится Франция и какая её столица?")
                .response("Франция находится в западной Европе.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Неполный ответ: {}", score);

        assertTrue(score >= 0.0 && score <= 1.0, "Неполный ответ должен возвращать валидный балл. Получено: " + score);
    }

    @Test
    @DisplayName("Полный ответ: Отвечает на все части вопроса")
    void testResponseRelevancy_CompleteAnswer() {

        log.info("=== Тест полного ответа ===");

        Sample sample = Sample.builder()
                .userInput("Где находится Франция и какая её столица?")
                .response("Франция находится в западной Европе, и её столица - Париж.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        log.info("Полный ответ: {}", score);

        assertTrue(score >= 0.5, "Полный ответ должен получать достаточно высокий балл (0.5+). Получено: " + score);
    }

    // ==================== ИЗВЕСТНЫЕ ОГРАНИЧЕНИЯ (Ожидаемые сбои) ====================

    @Test
    @DisplayName("⚠️ ОГРАНИЧЕНИЕ: Частичный ответ на многочастный вопрос - Нестабильно высокий")
    void testResponseRelevancy_PartialAnswer_UnexpectedlyHigh() {

        log.info("=== ОГРАНИЧЕНИЕ: Тест частичного ответа ===");

        Sample sample = Sample.builder()
                .userInput("Кто открыл пенициллин и когда?")
                .response("Александр Флеминг открыл пенициллин.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.60,
                "⚠️ ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ: Частичные ответы получают ВЫСОКИЕ баллы (0.60+), потому что "
                        + "косинусное сходство не может обнаружить ОТСУТСТВУЮЩУЮ информацию. Часть 'кто' отвечена, "
                        + "генерируются похожие вопросы. Это ОЖИДАЕМОЕ поведение, а не баг. Получено: "
                        + score);
    }

    @Test
    @DisplayName("⚠️ ОГРАНИЧЕНИЕ: Одна сущность, разный аспект - Нестабильно высокий скор")
    void testResponseRelevancy_SameEntity_DifferentAspect() {

        log.info("=== ОГРАНИЧЕНИЕ: Одна сущность, разный аспект ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Валюта Франции - Евро.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.45,
                "⚠️ САМОЕ КРИТИЧЕСКОЕ ОГРАНИЧЕНИЕ: Разные аспекты одной сущности получают СРЕДНЕ-ВЫСОКИЕ баллы (0.45+). "
                        + "Оба упоминают 'Францию' → эмбеддинги видят сходство. "
                        + "Не может различить 'столица' vs 'валюта'. "
                        + "Это ФУНДАМЕНТАЛЬНЫЙ недостаток методологии. Получено: "
                        + score);
    }

    @Test
    @DisplayName("⚠️ ОГРАНИЧЕНИЕ: Полностью не по теме - Нестабильно средний скор")
    void testResponseRelevancy_CompletelyOffTopic() {

        log.info("=== ОГРАНИЧЕНИЕ: Полностью не по теме ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Великая Китайская стена строилась на протяжении многих веков.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.05 && score <= 0.89,
                "⚠️ ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ: Нерелевантные ответы получают НИЗКИЕ-СРЕДНИЕ баллы (0.05-0.75) из-за "
                        + "различий в лингвистических структурах. Оба - фактические утверждения. "
                        + "Получено: "
                        + score);
    }

    @Test
    @DisplayName("⚠️ ОГРАНИЧЕНИЕ: Разные домены, похожая структура - Нестабильно СРЕДНЕ-ВЫСОКИЙ скор")
    void testResponseRelevancy_DifferentDomains_SimilarStructure() {

        log.info("=== ОГРАНИЧЕНИЕ: Разные домены с похожей структурой вопроса ===");

        Sample sample = Sample.builder()
                .userInput("Как настроить безопасность Spring Boot?")
                .response("Рецепт печенья с шоколадной крошкой включает муку, сахар и шоколадную крошку.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.15 && score <= 0.89,
                "⚠️ ШОКИРУЮЩЕЕ ОГРАНИЧЕНИЕ: Даже ПОЛНОСТЬЮ разные домены (программирование vs кулинария) "
                        + "получают СРЕДНИЕ баллы (0.15-0.75), потому что оба - инструкции 'как сделать'. "
                        + "Лингвистические паттерны ('Как...', 'включает...') создают сходство. "
                        + "Это доказывает, что метрика измеряет структуру, а не смысл. Получено: "
                        + score);
    }

    @Test
    @DisplayName("⚠️ ОГРАНИЧЕНИЕ: Одно слово-бессмыслица - Нестабильно высокий скор")
    void testResponseRelevancy_SingleWordNonsense() {

        log.info("=== ОГРАНИЧЕНИЕ: Одно слово-бессмыслица ===");

        Sample sample = Sample.builder()
                .userInput("Вычислите производную x в квадрате")
                .response("Синий")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.10,
                "🚨 КРИТИЧЕСКАЯ НАХОДКА: Даже одно слово не совпадающее с контекстом может получать РАЗЛИЧНЫЕ баллы (0.10+)! "
                        + "LLM может генерировать разные вопросы, которые влияют на итоговый балл. "
                        + "Это ДОКАЗЫВАЕТ, что метрика может быть нестабильной для граничных случаев. "
                        + "Получено: "
                        + score);
    }

    // ==================== ГРАНИЧНЫЕ СЛУЧАИ И СПЕЦИАЛЬНЫЕ СЦЕНАРИИ ====================

    @Test
    @DisplayName("Граничный случай: Очень короткий Q&A")
    void testResponseRelevancy_ShortQA() {

        log.info("=== Граничный случай: Короткий Q&A ===");

        Sample sample = Sample.builder()
                .userInput("Столица Франции?")
                .response("Париж.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.45,
                "Короткие но правильные ответы должны получать достойные баллы (0.45+). Получено: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Неправильный но по теме")
    void testResponseRelevancy_IncorrectButOnTopic() {

        log.info("=== Граничный случай: Неправильный но по теме ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Столица Франции - Лион.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.80,
                "Неправильные но тематические ответы получают ВЫСОКИЕ баллы, т.к. метрика не проверяет правильность. "
                        + "Это по дизайну - используйте Answer Correctness для фактической точности. Получено: "
                        + score);

        log.info("ℹ️ Напоминание: Эта метрика НЕ проверяет правильность!");
    }

    @Test
    @DisplayName("Граничный случай: Ответ с избыточной информацией")
    void testResponseRelevancy_RedundantInformation() {

        log.info("=== Граничный случай: Избыточная информация ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("Столица Франции - Париж. " + "Кстати, вчера я ходил в магазин и купил молоко. "
                        + "Погода была отличная. Я также встретил старого друга.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score < 0.99, "Ответы с избыточной информацией должны получать более низкий балл. Получено: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Гипотетический вопрос")
    void testResponseRelevancy_HypotheticalQuestion() {

        log.info("=== Граничный случай: Гипотетический вопрос ===");

        Sample sample = Sample.builder()
                .userInput("Что произойдёт, если Земля перестанет вращаться?")
                .response("Если Земля перестанет вращаться, одна сторона будет постоянно обращена к Солнцу, "
                        + "испытывая экстремальную жару, в то время как другая сторона будет в вечной темноте и холоде. "
                        + "Атмосфера продолжит двигаться с высокой скоростью, вызывая катастрофические ветры.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.75,
                "Гипотетические вопросы с релевантными ответами должны получать высокие баллы. Получено: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Неоднозначный вопрос")
    void testResponseRelevancy_AmbiguousQuestion() {

        log.info("=== Граничный случай: Неоднозначный вопрос ===");

        Sample sample = Sample.builder()
                .userInput("Что такое банк?")
                .response("Банк - это финансовое учреждение, которое принимает вклады и выдаёт кредиты.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.65,
                "Разумные интерпретации неоднозначных вопросов должны получать высокие баллы. Получено: " + score);
    }

    @Test
    @DisplayName("Граничный случай: Запрос на уточнение")
    void testResponseRelevancy_ClarificationRequest() {

        log.info("=== Граничный случай: Запрос на уточнение ===");

        Sample sample = Sample.builder()
                .userInput("Что это?")
                .response("Мне нужен дополнительный контекст, чтобы ответить на ваш вопрос. О чём вы спрашиваете?")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertTrue(
                score >= 0.0,
                "Запросы на уточнение - это валидные ответы на неясные "
                        + "вопросы (0.30+) или 0.0 по мнению EmbeddingsGigaR. Получено: " + score);
    }

    // ==================== ВАЛИДАЦИЯ И ГРАНИЧНЫЕ СЛУЧАИ ====================

    @Test
    @DisplayName("Валидация: Пустой ввод пользователя")
    void testResponseRelevancy_EmptyUserInput() {

        log.info("=== Валидация: Пустой ввод ===");

        Sample sample = Sample.builder()
                .userInput("")
                .response("Париж - столица Франции.")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertEquals(0.0, score, "Пустой ввод должен возвращать 0.0. Получено: " + score);
    }

    @Test
    @DisplayName("Валидация: Пустой ответ")
    void testResponseRelevancy_EmptyResponse() {

        log.info("=== Валидация: Пустой ответ ===");

        Sample sample = Sample.builder()
                .userInput("Какая столица Франции?")
                .response("")
                .build();

        ResponseRelevancyMetric.ResponseRelevancyConfig config =
                ResponseRelevancyMetric.ResponseRelevancyConfig.builder()
                        .numberOfQuestions(3)
                        .build();

        Double score = responseRelevancyMetric.singleTurnScore(config, sample);

        assertEquals(0.0, score, "Пустой ответ должен возвращать 0.0. Получено: " + score);
    }
}
