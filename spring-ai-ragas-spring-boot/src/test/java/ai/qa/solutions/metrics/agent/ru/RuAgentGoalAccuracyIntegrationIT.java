package ai.qa.solutions.metrics.agent.ru;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.qa.solutions.metrics.agent.AgentGoalAccuracyMetric;
import ai.qa.solutions.sample.Sample;
import ai.qa.solutions.sample.message.AIMessage;
import ai.qa.solutions.sample.message.HumanMessage;
import ai.qa.solutions.sample.message.ToolCall;
import ai.qa.solutions.sample.message.ToolMessage;
import java.util.List;
import java.util.Map;
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
 * Интеграционные тесты для метрики Agent Goal Accuracy - Русский язык.
 */
@Slf4j
@EnableAutoConfiguration
@SuppressWarnings("LoggingSimilarMessage")
@DisplayName("Метрика Agent Goal Accuracy - Валидация на русском языке")
@EnabledIfEnvironmentVariable(named = "OPENROUTER_API_KEY", matches = ".*")
@SpringBootTest(classes = RuAgentGoalAccuracyIntegrationIT.AgentGoalAccuracyIntegrationTestConfiguration.class)
class RuAgentGoalAccuracyIntegrationIT {

    @Configuration
    public static class AgentGoalAccuracyIntegrationTestConfiguration {}

    @Autowired
    private AgentGoalAccuracyMetric agentGoalAccuracyMetric;

    @Nested
    @DisplayName("Тесты режима WITH_REFERENCE")
    class WithReferenceModeTests {

        @Test
        @DisplayName("Цель явно достигнута - ОЖИДАЕМЫЙ ВЫСОКИЙ БАЛЛ (1.0)")
        void testGoalClearlyAchieved() {
            log.info("=== Тест: Цель явно достигнута ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Нужен билет на поезд Москва-Питер на завтра"),
                                    new AIMessage(
                                            "Проверяю доступные рейсы на завтра.",
                                            List.of(new ToolCall(
                                                    "search_trains",
                                                    Map.of("from", "Moscow", "to", "SPB", "date", "tomorrow")))),
                                    new ToolMessage(
                                            """
                                    {"trains": [
                                        {"id": "SAPSAN-08", "time": "08:00", "price": 4500},
                                        {"id": "LASTOCHKA-10", "time": "10:30", "price": 2800}
                                    ]}"""),
                                    new AIMessage(
                                            """
                                    Нашёл два варианта: Сапсан в 8:00 за 4500 рублей \
                                    или Ласточка в 10:30 за 2800 рублей. Какой предпочитаете?"""),
                                    new HumanMessage("Давай Сапсан, мне пораньше надо"),
                                    new AIMessage(
                                            "Бронирую Сапсан...",
                                            List.of(new ToolCall("book_train", Map.of("train_id", "SAPSAN-08")))),
                                    new ToolMessage(
                                            """
                                    {"status": "confirmed", "booking_ref": "СП12345", "seat": "5В"}"""),
                                    new AIMessage(
                                            """
                                    Билет на Сапсан забронирован. Отправление завтра в 8:00, место 5В. \
                                    Номер брони: СП12345. Электронный билет отправлен на вашу почту.""")))
                    .reference("Забронировать билет на поезд из Москвы в Санкт-Петербург")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать билет на поезд из Москвы в Санкт-Петербург");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Явно достигнутая цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Цель не достигнута - ОЖИДАЕМЫЙ НИЗКИЙ БАЛЛ (0.0)")
        void testGoalNotAchieved() {
            log.info("=== Тест: Цель не достигнута ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Забронируй мне поезд до Питера на завтра"),
                                    new AIMessage(
                                            "Проверяю доступность билетов.",
                                            List.of(new ToolCall(
                                                    "search_trains",
                                                    Map.of("from", "Moscow", "to", "SPB", "date", "tomorrow")))),
                                    new ToolMessage(
                                            """
                                    {"error": "SERVICE_UNAVAILABLE", "message": "РЖД API временно недоступен"}"""),
                                    new AIMessage("К сожалению, система бронирования РЖД временно недоступна."),
                                    new HumanMessage("И что делать?"),
                                    new AIMessage(
                                            """
                                    Приношу извинения за неудобства. Рекомендую попробовать позже \
                                    или обратиться в кассу на вокзале.""")))
                    .reference("Забронировать билет на поезд из Москвы в Санкт-Петербург")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать билет на поезд из Москвы в Санкт-Петербург");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Недостигнутая цель должна иметь низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Решение проблемы клиента - ОЖИДАЕМЫЙ ВЫСОКИЙ БАЛЛ")
        void testCustomerSupportResolution() {
            log.info("=== Тест: Решение проблемы клиента ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Алло, заказ 12345 где? Две недели жду уже!"),
                            new AIMessage(
                                    "Приношу извинения за задержку. Сейчас проверю статус вашего заказа.",
                                    List.of(new ToolCall("get_order_status", Map.of("order_id", "12345")))),
                            new ToolMessage(
                                    """
                                    {"order_id": "12345", "status": "LOST", "amount": 5500,
                                     "details": "Посылка утеряна при сортировке на складе"}"""),
                            new AIMessage(
                                    "Заказ найден. К сожалению, посылка была утеряна при сортировке. Оформляю возврат...",
                                    List.of(new ToolCall(
                                            "process_refund", Map.of("order_id", "12345", "amount", 5500)))),
                            new ToolMessage(
                                    """
                                    {"status": "REFUND_INITIATED", "amount": 5500, "eta_days": "3-5"}"""),
                            new AIMessage(
                                    """
                                    Оформила полный возврат 5500 рублей — средства поступят в течение 3-5 рабочих дней."""),
                            new HumanMessage("Ну ладно, спасибо хоть за это"),
                            new AIMessage(
                                    "Ещё раз приношу извинения за ситуацию. Если возникнут вопросы — обращайтесь.")))
                    .reference("Решить проблему клиента с недоставленным заказом путём возврата или замены")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Решить проблему клиента с недоставленным заказом");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Решённая проблема клиента должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Бронирование с вызовами инструментов - ОЖИДАЕМЫЙ ВЫСОКИЙ БАЛЛ (1.0)")
        void testBookingWithToolCalls() {
            log.info("=== Тест: Бронирование с вызовами инструментов ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(
                                    new HumanMessage("Забронируй мне авиабилет Москва-Сочи на 15 марта"),
                                    new AIMessage(
                                            "Ищу доступные рейсы...",
                                            List.of(new ToolCall(
                                                    "search_flights",
                                                    Map.of("from", "MOW", "to", "AER", "date", "2024-03-15")))),
                                    new ToolMessage(
                                            """
                                    {"flights": [
                                        {"id": "SU1234", "time": "08:00", "price": 5500},
                                        {"id": "S72456", "time": "14:30", "price": 4200}
                                    ]}"""),
                                    new AIMessage(
                                            """
                                    Нашёл два рейса на 15 марта:
                                    1. SU1234 в 08:00 — 5500₽
                                    2. S72456 в 14:30 — 4200₽
                                    Какой предпочитаете?"""),
                                    new HumanMessage("Давай второй, подешевле"),
                                    new AIMessage(
                                            "Бронирую рейс S72456...",
                                            List.of(new ToolCall("book_flight", Map.of("flight_id", "S72456")))),
                                    new ToolMessage(
                                            """
                                    {"status": "confirmed", "booking_ref": "ABC123", "seat": "14A"}"""),
                                    new AIMessage(
                                            """
                                    Готово! Билет забронирован:
                                    - Рейс: S72456 Москва → Сочи
                                    - Дата: 15 марта, вылет в 14:30
                                    - Место: 14A
                                    - Код брони: ABC123
                                    Электронный билет отправлен на вашу почту.""")))
                    .reference("Забронировать авиабилет из Москвы в Сочи")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Забронировать авиабилет из Москвы в Сочи");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5,
                    "Успешное бронирование с инструментами должно иметь высокий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Тесты режима WITHOUT_REFERENCE")
    class WithoutReferenceModeTests {

        @Test
        @DisplayName("Вывод цели и оценка - успешная задача")
        void testInferGoalSuccessfulTask() {
            log.info("=== Тест: Вывод цели - успешная задача ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Помоги накатать заявление на отпуск, а то сам не умею красиво"),
                            new AIMessage(
                                    """
                                    Конечно, подготовила для вас шаблон:

                                    Тема: Заявление на отпуск

                                    Уважаемый Иван Петрович,

                                    Прошу предоставить мне ежегодный отпуск с [дата] по [дата]. \
                                    Все текущие задачи будут завершены, на время отсутствия меня подстрахует коллега.

                                    С уважением,
                                    [Имя]"""),
                            new HumanMessage("Супер, то что надо!")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Помощь в написании письма об отпуске");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Выведенная и достигнутая цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Вывод цели и оценка - неуспешная задача")
        void testInferGoalFailedTask() {
            log.info("=== Тест: Вывод цели - неуспешная задача ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Переведи с французского, плиз"),
                            new AIMessage("С удовольствием помогу с переводом. Пришлите текст."),
                            new HumanMessage("Вот: 'Bonjour, je m'appelle Pierre et je travaille...'"),
                            new AIMessage(
                                    """
                                    Приношу извинения, но в данный момент возникли технические неполадки. \
                                    Не могу выполнить перевод. Попробуйте, пожалуйста, позже."""),
                            new HumanMessage("Блин, мне срочно было надо...")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Неудачный запрос на перевод");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score <= 0.5, "Выведенная, но не достигнутая цель должна иметь низкий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Многоэтапное выполнение задачи")
        void testMultiStepTaskCompletion() {
            log.info("=== Тест: Многоэтапное выполнение задачи ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Надо созвон с командой на следующую пятницу организовать"),
                            new AIMessage("Помогу организовать встречу. В какое время вам удобно?"),
                            new HumanMessage("Часа в 2 дня"),
                            new AIMessage("Принято. Кого пригласить на встречу?"),
                            new HumanMessage("Всех наших — Ваню, Аню и Мишу"),
                            new AIMessage(
                                    "Создаю встречу и бронирую переговорную...",
                                    List.of(
                                            new ToolCall(
                                                    "create_meeting",
                                                    Map.of(
                                                            "date",
                                                            "next_friday",
                                                            "time",
                                                            "14:00",
                                                            "attendees",
                                                            List.of(
                                                                    "ivan@company.ru",
                                                                    "anna@company.ru",
                                                                    "mikhail@company.ru"))),
                                            new ToolCall("book_room", Map.of("room", "A", "date", "next_friday")))),
                            new ToolMessage(
                                    """
                                    {"meeting_id": "MTG-789", "status": "created", "invites_sent": 3}"""),
                            new ToolMessage(
                                    """
                                    {"room": "A", "status": "booked", "date": "next_friday", "time": "14:00"}"""),
                            new AIMessage(
                                    """
                                    Встреча создана на следующую пятницу в 14:00. \
                                    Приглашения отправлены Ивану, Анне и Михаилу. \
                                    Переговорная А забронирована. Добавить повестку?"""),
                            new HumanMessage("Не, всё норм, спасибо!")))
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITHOUT_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Диалог: Многоэтапная организация встречи");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5,
                    "Успешно завершённая многоэтапная задача должна иметь высокий балл. Получено: " + score);
        }
    }

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCasesTests {

        @Test
        @DisplayName("Сложный диалог с неявной целью")
        void testComplexConversationImplicitGoal() {
            log.info("=== Тест: Сложный диалог с неявной целью ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(List.of(
                            new HumanMessage("Что-то меня на работе накрывает последнее время, не знаю что делать"),
                            new AIMessage(
                                    """
                                    Понимаю, это непросто. Хотите обсудить причины перегрузки \
                                    или предпочитаете практические советы по управлению нагрузкой?"""),
                            new HumanMessage("Давай советы какие-нибудь"),
                            new AIMessage(
                                    """
                                    Вот несколько эффективных стратегий:

                                    1. Приоритизируйте задачи по матрице Эйзенхауэра — сначала срочные и важные.
                                    2. Разбивайте крупные проекты на небольшие шаги.
                                    3. Учитесь говорить «нет» несущественным запросам.
                                    4. Делайте регулярные перерывы по технике Помодоро.
                                    5. Делегируйте где возможно.

                                    Рассказать подробнее о каком-либо пункте?"""),
                            new HumanMessage("О, клёво, попробую Помодоро")))
                    .reference("Помочь пользователю справиться со стрессом на работе практическими советами")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score = agentGoalAccuracyMetric.singleTurnScore(config, sample);

            log.info("Цель: Помочь справиться со стрессом на работе");
            log.info("Балл: {}", score);

            assertNotNull(score);
            assertTrue(score >= 0.5, "Достигнутая неявная цель должна иметь высокий балл. Получено: " + score);
        }

        @Test
        @DisplayName("Асинхронное вычисление работает корректно")
        void testAsyncScoring() {
            log.info("=== Тест: Асинхронное вычисление ===");

            final Sample sample = Sample.builder()
                    .userInputMessages(
                            List.of(new HumanMessage("Сколько будет 2 + 2?"), new AIMessage("2 + 2 равно 4.")))
                    .reference("Дать правильный ответ на математический вопрос")
                    .build();

            final AgentGoalAccuracyMetric.AgentGoalAccuracyConfig config =
                    AgentGoalAccuracyMetric.AgentGoalAccuracyConfig.builder()
                            .mode(AgentGoalAccuracyMetric.Mode.WITH_REFERENCE)
                            .build();

            final Double score =
                    agentGoalAccuracyMetric.singleTurnScoreAsync(config, sample).join();

            log.info("Асинхронный балл: {}", score);

            assertNotNull(score);
            assertTrue(
                    score >= 0.5, "Асинхронное вычисление должно работать так же, как синхронное. Получено: " + score);
        }
    }
}
