package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
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
 * Интеграционные тесты метрики Topic Adherence - Русский язык.
 * <p>
 * Тестируют оценку соответствия тем разговора ожидаемым референсным темам.
 * <p>
 * Ключевые характеристики:
 * - Режим F1: гармоническое среднее точности и полноты (по умолчанию)
 * - Режим PRECISION: фокус на избежании нерелевантных обсуждений
 * - Режим RECALL: фокус на покрытии всех референсных тем
 * - Возвращает оценку от 0.0 до 1.0
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Topic Adherence - Валидация на русском языке")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuTopicAdherenceIntegrationIT.TopicAdherenceIntegrationTestConfiguration.class)
class RuTopicAdherenceIntegrationIT {

    @Configuration
    public static class TopicAdherenceIntegrationTestConfiguration {}

    @Autowired
    private TopicAdherenceMetric topicAdherenceMetric;

    @Nested
    @DisplayName("Тесты режима F1")
    class F1ModeTests {

        @Test
        @DisplayName("Все темы по теме - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testAllTopicsOnTopic() {
            log.info("=== Тест: все темы по теме ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message(
                                    "user", "Хочу забронировать авиабилеты в Санкт-Петербург на следующую неделю."),
                            new Sample.Message(
                                    "assistant",
                                    "С удовольствием помогу вам забронировать билеты. "
                                            + "Какие даты вас интересуют и из какого города вы планируете вылетать?"),
                            new Sample.Message("user", "Вылечу из Москвы в понедельник, вернусь в пятницу."),
                            new Sample.Message(
                                    "assistant",
                                    "Нашёл несколько вариантов. Есть прямой рейс Аэрофлота в 19:00 "
                                            + "за 8500 рублей туда-обратно. Оформить бронирование?"),
                            new Sample.Message("user", "Да, пожалуйста, забронируйте этот рейс."),
                            new Sample.Message(
                                    "assistant",
                                    "Ваш рейс забронирован! Номер подтверждения: SU12345. "
                                            + "Электронный билет придёт на вашу почту.")))
                    .referenceTopics(List.of("бронирование авиабилетов", "путешествия", "авиаперевозки"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: бронирование авиабилетов, путешествия, авиаперевозки");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Все темы по теме должны дать высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Разговор уходит от темы - ОЖИДАЕТСЯ НИЗКИЙ БАЛЛ")
        void testConversationDriftsOffTopic() {
            log.info("=== Тест: разговор уходит от темы ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Хочу научиться готовить борщ."),
                            new Sample.Message(
                                    "assistant",
                                    "Отличный выбор! Борщ — классика русской кухни. Кстати, "
                                            + "вы слышали о криптовалюте Bitcoin?"),
                            new Sample.Message("user", "Нет, меня интересует только борщ."),
                            new Sample.Message(
                                    "assistant",
                                    "Понятно, но если говорить об инвестициях, "
                                            + "фондовый рынок сейчас очень волатильный."),
                            new Sample.Message("user", "Давайте вернёмся к рецепту борща."),
                            new Sample.Message(
                                    "assistant",
                                    "Конечно! А ещё смотрели вчерашний футбольный матч? "
                                            + "Спартак играл потрясающе!")))
                    .referenceTopics(List.of("русская кухня", "приготовление супов", "рецепты"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: русская кухня, приготовление супов, рецепты");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.6, "Уход от темы должен дать низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Техническая поддержка - ОЖИДАЕТСЯ ВЫСОКИЙ БАЛЛ")
        void testTechnicalSupportConversation() {
            log.info("=== Тест: техническая поддержка ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Мой ноутбук не включается после обновления Windows."),
                            new Sample.Message(
                                    "assistant",
                                    "Понимаю, это неприятно. Давайте разберёмся вместе. "
                                            + "Горят ли какие-нибудь индикаторы при нажатии кнопки питания?"),
                            new Sample.Message("user", "Да, индикатор питания мигает и гаснет."),
                            new Sample.Message(
                                    "assistant",
                                    "Это указывает на получение питания, но сбой загрузки. "
                                            + "Попробуйте принудительную перезагрузку: удерживайте кнопку питания 15 секунд."),
                            new Sample.Message("user", "Сделал! Теперь вижу экран восстановления Windows."),
                            new Sample.Message(
                                    "assistant",
                                    "Отлично! Экран восстановления означает, что проблему можно решить. "
                                            + "Выберите 'Восстановление при загрузке'.")))
                    .referenceTopics(
                            List.of("диагностика компьютера", "операционная система Windows", "оборудование ноутбука"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: диагностика компьютера, Windows, оборудование ноутбука");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Техническая поддержка должна быть по теме. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты режима PRECISION")
    class PrecisionModeTests {

        @Test
        @DisplayName("Высокая точность - без отклонений от темы")
        void testHighPrecisionNoOffTopic() {
            log.info("=== Тест: высокая точность ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Расскажите про алгоритмы машинного обучения."),
                            new Sample.Message(
                                    "assistant",
                                    "Алгоритмы машинного обучения делятся на три категории: "
                                            + "обучение с учителем, без учителя и обучение с подкреплением."),
                            new Sample.Message("user", "В чём разница между ними?"),
                            new Sample.Message(
                                    "assistant",
                                    "Обучение с учителем использует размеченные данные для предсказаний. "
                                            + "Обучение без учителя находит закономерности в неразмеченных данных. "
                                            + "Обучение с подкреплением учится через систему наград и штрафов.")))
                    .referenceTopics(List.of("машинное обучение", "искусственный интеллект", "наука о данных"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.PRECISION)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: машинное обучение, ИИ, наука о данных");
            log.info("Оценка точности: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.6, "Высокая точность ожидается, когда все обсуждаемые темы по теме. Получено: " + score);
        }

        @Test
        @DisplayName("Низкая точность - много отклонений от темы")
        void testLowPrecisionManyOffTopic() {
            log.info("=== Тест: низкая точность ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Давайте поговорим о программировании на Python."),
                            new Sample.Message(
                                    "assistant",
                                    "Python отличный! Но вы задумывались о том, чтобы завести питона-змею? "
                                            + "Это очень интересные животные."),
                            new Sample.Message("user", "Нет, я имел в виду язык программирования."),
                            new Sample.Message(
                                    "assistant", "Ах да, языки! Кстати, французский — язык любви. Très romantique!"),
                            new Sample.Message("user", "Пожалуйста, давайте про Python-код."),
                            new Sample.Message(
                                    "assistant",
                                    "Код — это здорово. А музыкальный код тоже! " + "Вы пробовали играть на гитаре?")))
                    .referenceTopics(List.of("программирование на Python", "разработка ПО", "кодирование"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.PRECISION)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: программирование на Python, разработка ПО, кодирование");
            log.info("Оценка точности: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.6, "Низкая точность ожидается при множестве отклонений от темы. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты режима RECALL")
    class RecallModeTests {

        @Test
        @DisplayName("Высокая полнота - все референсные темы покрыты")
        void testHighRecallAllTopicsCovered() {
            log.info("=== Тест: высокая полнота ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Хочу планировать здоровый образ жизни."),
                            new Sample.Message(
                                    "assistant",
                                    "Отличная цель! Здоровый образ жизни включает три ключевых области: "
                                            + "питание, физическую активность и психическое здоровье."),
                            new Sample.Message("user", "Расскажите подробнее о каждой."),
                            new Sample.Message(
                                    "assistant",
                                    "В питании сосредоточьтесь на натуральных продуктах, овощах и нежирном белке. "
                                            + "Для физической активности старайтесь уделять 30 минут в день — ходьба, плавание или спортзал. "
                                            + "Для психического здоровья практикуйте медитацию, высыпайтесь "
                                            + "и поддерживайте социальные связи.")))
                    .referenceTopics(List.of("питание", "физическая активность", "психическое здоровье"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.RECALL)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: питание, физическая активность, психическое здоровье");
            log.info("Оценка полноты: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Высокая полнота ожидается при покрытии всех референсных тем. Получено: " + score);
        }

        @Test
        @DisplayName("Низкая полнота - покрыто мало референсных тем")
        void testLowRecallFewTopicsCovered() {
            log.info("=== Тест: низкая полнота ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Объясните компоненты современного веб-приложения."),
                            new Sample.Message(
                                    "assistant",
                                    "Современное веб-приложение использует JavaScript для интерактивности."),
                            new Sample.Message("user", "А что насчёт других компонентов?"),
                            new Sample.Message(
                                    "assistant",
                                    "JavaScript-фреймворки вроде React очень популярны. "
                                            + "Многие компании используют React для фронтенда.")))
                    .referenceTopics(List.of(
                            "фронтенд-разработка",
                            "бэкенд-разработка",
                            "системы баз данных",
                            "проектирование API",
                            "безопасность",
                            "развёртывание"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.RECALL)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: фронтенд, бэкенд, БД, API, безопасность, развёртывание");
            log.info("Оценка полноты: {}", score);

            assertNotNull(score);
            assertTrue(
                    score <= 0.5,
                    "Низкая полнота ожидается при покрытии малого числа референсных тем. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Крайние случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Единственная референсная тема полностью покрыта")
        void testSingleTopicFullyCovered() {
            log.info("=== Тест: единственная тема полностью покрыта ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Как правильно заваривать чай?"),
                            new Sample.Message(
                                    "assistant",
                                    "Для идеального чая вскипятите свежую воду. "
                                            + "Прогрейте чайник, добавьте одну чайную ложку чая на чашку, "
                                            + "залейте горячей водой и настаивайте 3-5 минут."),
                            new Sample.Message("user", "Какой температуры должна быть вода?"),
                            new Sample.Message(
                                    "assistant",
                                    "Для чёрного чая используйте воду около 95-100°C. "
                                            + "Зелёный чай предпочитает более прохладную воду около 80°C, "
                                            + "чтобы избежать горечи.")))
                    .referenceTopics(List.of("приготовление чая"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score = topicAdherenceMetric.singleTurnScore(config, sample);

            log.info("Референсные темы: приготовление чая");
            log.info("Оценка: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5,
                    "Единственная сфокусированная тема должна дать высокое соответствие. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронная оценка работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: асинхронная оценка ===");

            final Sample sample = Sample.builder()
                    .messages(List.of(
                            new Sample.Message("user", "Какая сегодня погода?"),
                            new Sample.Message("assistant", "Сейчас солнечно, температура +22°C.")))
                    .referenceTopics(List.of("погода"))
                    .build();

            final TopicAdherenceMetric.TopicAdherenceConfig config = TopicAdherenceMetric.TopicAdherenceConfig.builder()
                    .mode(TopicAdherenceMetric.Mode.F1)
                    .build();

            final Double score =
                    topicAdherenceMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронная оценка: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Асинхронная оценка должна работать идентично синхронной. Получено: " + score);
        }
    }
}
