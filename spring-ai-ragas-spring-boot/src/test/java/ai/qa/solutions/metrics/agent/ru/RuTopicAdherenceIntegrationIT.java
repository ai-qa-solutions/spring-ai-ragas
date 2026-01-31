package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.TopicAdherenceMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
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
                    .userInputMessages(List.of(
                            new HumanMessage("Хочу забронировать билет на самолёт в Париж на следующую неделю."),
                            new AIMessage("С удовольствием помогу вам забронировать билеты. "
                                    + "Какие даты вас интересуют и из какого города вы планируете вылетать?"),
                            new HumanMessage("Вылечу из Москвы в понедельник, вернусь в пятницу."),
                            new AIMessage("Нашёл несколько вариантов. Есть прямой рейс Аэрофлота в 19:00 "
                                    + "за 8500 рублей туда-обратно. Оформить бронирование?"),
                            new HumanMessage("Да, пожалуйста, забронируйте этот рейс."),
                            new AIMessage("Ваш рейс забронирован! Номер подтверждения: SU12345. "
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
                    .userInputMessages(List.of(
                            new HumanMessage("Хочу научиться готовить борщ."),
                            new AIMessage("Отличный выбор! Борщ — классика русской кухни. Кстати, "
                                    + "вы слышали о криптовалюте Bitcoin?"),
                            new HumanMessage("Нет, меня интересует только борщ."),
                            new AIMessage("Понятно, но если говорить об инвестициях, "
                                    + "фондовый рынок сейчас очень волатильный."),
                            new HumanMessage("Давайте вернёмся к рецепту борща."),
                            new AIMessage("Конечно! А ещё смотрели вчерашний футбольный матч? "
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
                    .userInputMessages(List.of(
                            new HumanMessage("Мой ноутбук не включается после обновления Windows."),
                            new AIMessage("Понимаю, это неприятно. Давайте разберёмся вместе. "
                                    + "Горят ли какие-нибудь индикаторы при нажатии кнопки питания?"),
                            new HumanMessage("Да, индикатор питания мигает и гаснет."),
                            new AIMessage("Это указывает на получение питания, но сбой загрузки. "
                                    + "Попробуйте принудительную перезагрузку: удерживайте кнопку питания 15 секунд."),
                            new HumanMessage("Сделал! Теперь вижу экран восстановления Windows."),
                            new AIMessage("Отлично! Экран восстановления означает, что проблему можно решить. "
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
                    .userInputMessages(List.of(
                            new HumanMessage("Расскажите про алгоритмы машинного обучения."),
                            new AIMessage("Алгоритмы машинного обучения делятся на три категории: "
                                    + "обучение с учителем, без учителя и обучение с подкреплением."),
                            new HumanMessage("В чём разница между ними?"),
                            new AIMessage("Обучение с учителем использует размеченные данные для предсказаний. "
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
                    .userInputMessages(List.of(
                            new HumanMessage("Давайте поговорим о программировании на Python."),
                            new AIMessage("Python отличный! Но вы задумывались о том, чтобы завести питона-змею? "
                                    + "Это очень интересные животные."),
                            new HumanMessage("Нет, я имел в виду язык программирования."),
                            new AIMessage("О, верно! Кстати о языках, знаете ли вы, что французский — язык любви? "
                                    + "Très romantique!"),
                            new HumanMessage("Пожалуйста, давайте про Python-код."),
                            new AIMessage(
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
                    .userInputMessages(List.of(
                            new HumanMessage("Хочу планировать здоровый образ жизни."),
                            new AIMessage("Отличная цель! Здоровый образ жизни включает три ключевых области: "
                                    + "питание, физическую активность и психическое здоровье."),
                            new HumanMessage("Расскажите подробнее о каждой."),
                            new AIMessage(
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
                    .userInputMessages(List.of(
                            new HumanMessage("Объясните компоненты современного веб-приложения."),
                            new AIMessage("Современное веб-приложение использует JavaScript для интерактивности."),
                            new HumanMessage("А что насчёт других компонентов?"),
                            new AIMessage("JavaScript-фреймворки вроде React очень популярны. "
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
                    .userInputMessages(List.of(
                            new HumanMessage("Как правильно заваривать чай?"),
                            new AIMessage("Для идеального чая вскипятите свежую воду. "
                                    + "Прогрейте чайник, добавьте одну чайную ложку чая на чашку, "
                                    + "залейте горячей водой и настаивайте 3-5 минут."),
                            new HumanMessage("Какой температуры должна быть вода?"),
                            new AIMessage("Для чёрного чая используйте воду около 95-100°C. "
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
                    .userInputMessages(List.of(
                            new HumanMessage("Какая сегодня погода?"),
                            new AIMessage("Сейчас солнечно, температура +22°C.")))
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
