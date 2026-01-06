package ai.qa.solutions.metrics.general.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.general.RubricsScoreMetric;
import ai.qa.solutions.sample.Sample;
import java.util.Map;
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
@DisplayName("Интеграционные тесты общих метрик с русскоязычными примерами")
@SpringBootTest(classes = RuRubricsScoreMetricIntegrationTest.GeneralMetricsIntegrationTestConfiguration.class)
class RuRubricsScoreMetricIntegrationTest {

    @Configuration
    public static class GeneralMetricsIntegrationTestConfiguration {}

    @Autowired
    private RubricsScoreMetric rubricsScoreMetric;

    @Test
    @DisplayName("RubricsScore: Позитивный тест - отличное объяснение")
    void testRubricsScorePositive_ExcellentExplanation() {
        log.info("=== RubricsScore: Позитивный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез — это сложный биохимический процесс, в ходе которого растения "
                        + "преобразуют световую энергию в химическую. Процесс происходит в хлоропластах "
                        + "и включает две основные стадии: световую и темновую фазы. В световой фазе "
                        + "хлорофилл поглощает солнечный свет, расщепляя молекулы воды и выделяя кислород. "
                        + "В темновой фазе (цикл Кальвина) углекислый газ из атмосферы превращается в глюкозу. "
                        + "Общее уравнение: 6CO₂ + 6H₂O + световая энергия → C₆H₁₂O₆ + 6O₂.")
                .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды "
                        + "с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка по рубрикам: {}", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score >= 4.0, "Ожидается высокая оценка для подробного научного объяснения, получен: " + score);
    }

    @Test
    @DisplayName("RubricsScore: Негативный тест - поверхностное объяснение")
    void testRubricsScoreNegative_SuperficialExplanation() {
        log.info("=== RubricsScore: Негативный тест ===");

        Sample sample = Sample.builder()
                .userInput("Объясните процесс фотосинтеза")
                .response("Фотосинтез это когда растения что-то делают со светом. "
                        + "Они как-то используют солнце для роста.")
                .reference("Фотосинтез - процесс образования органических веществ из CO₂ и воды "
                        + "с использованием световой энергии.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createPhotosynthesisRubrics())
                .build();

        Double score = rubricsScoreMetric.singleTurnScore(config, sample);

        log.info("Вопрос: {}", sample.getUserInput());
        log.info("Ответ: {}", sample.getResponse());
        log.info("Оценка по рубрикам: {}", score);

        assertNotNull(score);
        assertTrue(score >= 1.0 && score <= 5.0);
        assertTrue(score <= 2.0, "Ожидается низкая оценка для поверхностного объяснения, получен: " + score);
    }

    @Test
    @DisplayName("RubricsScore: Тест оценки эссе")
    void testRubricsScore_EssayEvaluation() {
        log.info("=== RubricsScore: Оценка эссе ===");

        // Хорошее эссе
        Sample goodEssay = Sample.builder()
                .userInput("Напишите эссе о влиянии технологий на общество")
                .response("Технологический прогресс кардинально изменил современное общество. "
                        + "С одной стороны, цифровые технологии обеспечили беспрецедентные возможности "
                        + "для коммуникации, образования и доступа к информации. Интернет объединил мир, "
                        + "позволив людям мгновенно обмениваться идеями независимо от географических границ. "
                        + "С другой стороны, возникли новые вызовы: цифровое неравенство, зависимость от "
                        + "технологий и вопросы приватности. Необходим баланс между инновациями и "
                        + "социальной ответственностью для устойчивого развития.")
                .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
                .build();

        // Слабое эссе
        Sample weakEssay = Sample.builder()
                .userInput("Напишите эссе о влиянии технологий на общество")
                .response("Технологии хорошие. Есть телефоны и компьютеры. " + "Люди используют интернет. Это удобно.")
                .reference("Эссе о влиянии технологий на общество с примерами и аргументацией")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createEssayRubrics())
                .build();

        Double goodScore = rubricsScoreMetric.singleTurnScore(config, goodEssay);
        Double weakScore = rubricsScoreMetric.singleTurnScore(config, weakEssay);

        log.info("Хорошее эссе - оценка: {}", goodScore);
        log.info("Слабое эссе - оценка: {}", weakScore);

        assertTrue(goodScore >= 3.0, "Хорошее эссе должно получить высокую оценку");
        assertTrue(weakScore <= 2.0, "Слабое эссе должно получить низкую оценку");
    }

    @Test
    @DisplayName("RubricsScore: RAG для службы поддержки - Оценка качества ответа")
    void testRubricsScore_CustomerSupportRAG() {
        log.info("=== RubricsScore: RAG для службы поддержки ===");

        // Отличный ответ службы поддержки
        Sample excellentSupport = Sample.builder()
                .userInput("Не могу войти в личный кабинет. Постоянно выдаёт ошибку 'неверные учетные данные'.")
                .response(
                        "Понимаю, насколько это может быть неприятно. Давайте решим проблему со входом вместе. "
                                + "Сначала попробуйте сбросить пароль, используя ссылку 'Забыли пароль?' на странице входа. "
                                + "Если это не помогло, проверьте, не включен ли Caps Lock, и попробуйте очистить кэш и куки браузера. "
                                + "Если проблема сохраняется, наша техническая поддержка может проверить статус вашей учетной записи - "
                                + "напишите нам на support@company.ru или позвоните по телефону 8-800-555-35-35. Мы всегда готовы помочь!")
                .reference("Сброс пароля: используйте ссылку 'Забыли пароль?'. Проверьте Caps Lock. "
                        + "Очистите кэш/куки. Обратитесь в support@company.ru если проблема сохраняется.")
                .build();

        // Плохой ответ службы поддержки
        Sample poorSupport = Sample.builder()
                .userInput("Не могу войти в личный кабинет. Постоянно выдаёт ошибку 'неверные учетные данные'.")
                .response("Вы, наверное, неправильно ввели пароль. Попробуйте еще раз.")
                .reference("Сброс пароля: используйте ссылку 'Забыли пароль?'. Проверьте Caps Lock. "
                        + "Очистите кэш/куки. Обратитесь в support@company.ru если проблема сохраняется.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createCustomerSupportRubrics())
                .build();

        Double excellentScore = rubricsScoreMetric.singleTurnScore(config, excellentSupport);
        Double poorScore = rubricsScoreMetric.singleTurnScore(config, poorSupport);

        log.info("Оценка отличного ответа поддержки: {}", excellentScore);
        log.info("Оценка плохого ответа поддержки: {}", poorScore);

        assertTrue(excellentScore >= 4.0, "Эмпатичный и полный ответ службы поддержки должен получить высокую оценку");
        assertTrue(poorScore <= 2.0, "Неполезный и неполный ответ службы поддержки должен получить низкую оценку");
    }

    @Test
    @DisplayName("RubricsScore: Технический помощник - Точность и полнота")
    void testRubricsScore_TechnicalAssistant() {
        log.info("=== RubricsScore: Технический помощник ===");

        // Точный и полный технический ответ
        Sample accurateTechnical = Sample.builder()
                .userInput("Как настроить Redis для кэширования в Spring Boot 3.x?")
                .response("Для настройки Redis кэширования в Spring Boot 3.x:\n"
                        + "1. Добавьте зависимости: spring-boot-starter-data-redis и spring-boot-starter-cache\n"
                        + "2. В application.yml укажите: spring.redis.host=localhost, spring.redis.port=6379\n"
                        + "3. Включите кэширование аннотацией @EnableCaching на главном классе\n"
                        + "4. Настройте RedisCacheManager с TTL: RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10))\n"
                        + "5. Используйте аннотации @Cacheable, @CacheEvict для методов\n"
                        + "Пример: @Cacheable(value = \"users\", key = \"#id\")")
                .reference("Redis в Spring Boot 3: добавить зависимости redis и cache, настроить host/port, "
                        + "включить @EnableCaching, настроить RedisCacheManager с TTL")
                .build();

        // Неполный технический ответ
        Sample incompleteTechnical = Sample.builder()
                .userInput("Как настроить Redis для кэширования в Spring Boot 3.x?")
                .response("Нужно добавить зависимости Redis и настроить Spring Boot. "
                        + "Потом можно использовать Redis для кэширования данных.")
                .reference("Redis в Spring Boot 3: добавить зависимости redis и cache, настроить host/port, "
                        + "включить @EnableCaching, настроить RedisCacheManager с TTL")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createTechnicalDocumentationRubrics())
                .build();

        Double accurateScore = rubricsScoreMetric.singleTurnScore(config, accurateTechnical);
        Double incompleteScore = rubricsScoreMetric.singleTurnScore(config, incompleteTechnical);

        log.info("Оценка точного технического ответа: {}", accurateScore);
        log.info("Оценка неполного технического ответа: {}", incompleteScore);

        assertTrue(accurateScore >= 4.0, "Подробный технический ответ с примерами кода должен получить высокую оценку");
        assertTrue(incompleteScore <= 2.5, "Расплывчатый и неполный технический ответ должен получить низкую оценку");
    }

    @Test
    @DisplayName("RubricsScore: Юридическая консультация - Безопасность и корректность")
    void testRubricsScore_LegalConsultation() {
        log.info("=== RubricsScore: Юридическая консультация ===");

        // Безопасный и корректный юридический ответ
        Sample safeLegal = Sample.builder()
                .userInput("Работодатель задерживает зарплату уже 2 недели. Что мне делать?")
                .response(
                        "Задержка заработной платы является нарушением трудового законодательства РФ. "
                                + "Согласно статье 142 ТК РФ, при задержке зарплаты более 15 дней вы имеете право приостановить работу. "
                                + "Рекомендуемые действия: (1) Письменно уведомить работодателя о задержке с требованием выплаты, "
                                + "(2) При отсутствии реакции - обратиться в трудовую инспекцию или прокуратуру, "
                                + "(3) За каждый день задержки работодатель обязан выплатить компенсацию (1/150 ключевой ставки ЦБ). "
                                + "Для точной правовой оценки вашей ситуации рекомендуется проконсультироваться с квалифицированным юристом. "
                                + "Данная информация носит общий образовательный характер и не заменяет профессиональную юридическую консультацию.")
                .reference("Задержка зарплаты - нарушение ТК РФ. Письменное уведомление работодателя, "
                        + "обращение в трудинспекцию, право на компенсацию. Консультация юриста рекомендуется.")
                .build();

        // Небезопасный юридический ответ
        Sample unsafeLegal = Sample.builder()
                .userInput("Работодатель задерживает зарплату уже 2 недели. Что мне делать?")
                .response("Просто увольняйтесь и подавайте в суд на максимальную компенсацию. "
                        + "Вы точно выиграете дело и получите большие деньги.")
                .reference("Задержка зарплаты - нарушение ТК РФ. Письменное уведомление работодателя, "
                        + "обращение в трудинспекцию, право на компенсацию. Консультация юриста рекомендуется.")
                .build();

        RubricsScoreMetric.RubricsConfig config = RubricsScoreMetric.RubricsConfig.builder()
                .rubrics(createLegalConsultationRubrics())
                .build();

        Double safeScore = rubricsScoreMetric.singleTurnScore(config, safeLegal);
        Double unsafeScore = rubricsScoreMetric.singleTurnScore(config, unsafeLegal);

        log.info("Оценка безопасного юридического ответа: {}", safeScore);
        log.info("Оценка небезопасного юридического ответа: {}", unsafeScore);

        assertTrue(
                safeScore >= 4.0, "Безопасный юридический ответ со ссылками на законы должен получить высокую оценку");
        assertTrue(
                unsafeScore <= 2.0,
                "Небезопасный юридический ответ с категоричными советами должен получить низкую оценку");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    private Map<String, String> createPhotosynthesisRubrics() {
        return Map.of(
                "score1_description", "Полностью неверная или нерелевантная информация о фотосинтезе",
                "score2_description", "Базовое понимание с существенными пробелами или ошибками",
                "score3_description", "Общее понимание процесса, но отсутствуют важные детали",
                "score4_description", "Хорошее понимание с упоминанием основных этапов и компонентов",
                "score5_description", "Отличное объяснение с научными деталями, уравнением и примерами");
    }

    private Map<String, String> createEssayRubrics() {
        return Map.of(
                "score1_description", "Отсутствует структура, нет аргументов, множество ошибок",
                "score2_description", "Слабая структура, поверхностные аргументы, есть ошибки",
                "score3_description", "Базовая структура, некоторые аргументы, в целом понятно",
                "score4_description", "Хорошая структура, убедительные аргументы, качественное изложение",
                "score5_description", "Отличная структура, глубокий анализ, примеры, безупречное изложение");
    }

    private Map<String, String> createCustomerSupportRubrics() {
        return Map.of(
                "score1_description",
                "Грубый или пренебрежительный тон, нет полезных рекомендаций, игнорирование проблемы клиента",
                "score2_description",
                "Базовое подтверждение, но неполные шаги, отсутствие эмпатии или альтернативных вариантов связи",
                "score3_description",
                "Вежливый ответ с некоторыми шагами диагностики, но отсутствует ключевая информация или альтернативы",
                "score4_description",
                "Эмпатичный тон, полные шаги диагностики, четкие инструкции, предоставлены варианты связи",
                "score5_description",
                "Отличная эмпатия, исчерпывающие пошаговые инструкции, множество вариантов связи, профессиональный и поддерживающий тон");
    }

    private Map<String, String> createTechnicalDocumentationRubrics() {
        return Map.of(
                "score1_description",
                "Некорректная техническая информация, отсутствие конкретных шагов, нет деталей версий",
                "score2_description",
                "Расплывчатые рекомендации с частично корректной информацией, но без конкретных деталей реализации",
                "score3_description",
                "Общие технические шаги предоставлены, но отсутствуют примеры кода или конкретные названия классов",
                "score4_description",
                "Точные технические шаги с указанием версий, некоторые примеры кода, правильная последовательность",
                "score5_description",
                "Полная техническая точность, детали для конкретных версий, примеры кода, названия классов, правильная последовательность реализации");
    }

    private Map<String, String> createLegalConsultationRubrics() {
        return Map.of(
                "score1_description",
                "Опасные юридические советы, игнорирование серьезных симптомов, нет рекомендации обратиться к специалисту",
                "score2_description",
                "Общие предложения без юридических оговорок или оценки срочности",
                "score3_description",
                "Базовая правовая информация с некоторыми оговорками, но отсутствует четкий совет о консультации со специалистом",
                "score4_description",
                "Сбалансированная информация, указание когда обращаться к юристу, включены юридические оговорки",
                "score5_description",
                "Исчерпывающая правовая информация, четкая оценка срочности, настоятельная рекомендация консультации со специалистом, явное образовательное предупреждение");
    }
}
